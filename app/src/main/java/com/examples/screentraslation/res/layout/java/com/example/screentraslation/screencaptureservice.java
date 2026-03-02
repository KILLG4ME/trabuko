package com.example.screentranslator;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.util.*;
import androidx.core.app.NotificationCompat;
import java.nio.*;

public class ScreenCaptureService extends Service {
    private static final String TAG = "ScreenCaptureService";
    private static final int NOTIFICATION_ID = 1001;
    
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private Handler handler;
    private OCRProcessor ocrProcessor;
    private boolean isTranslating = true;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        handler = new Handler(Looper.getMainLooper());
        ocrProcessor = new OCRProcessor(this);
        
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int resultCode = intent.getIntExtra("resultCode", 0);
            Intent data = intent.getParcelableExtra("data");
            
            if (resultCode != 0 && data != null) {
                startScreenCapture(resultCode, data);
            }
        }
        return START_STICKY;
    }

    private void startScreenCapture(int resultCode, Intent data) {
        MediaProjectionManager projectionManager = (MediaProjectionManager) 
            getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        
        mediaProjection = projectionManager.getMediaProjection(resultCode, data);
        
        // Obtener dimensiones de la pantalla
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        int densityDpi = getResources().getDisplayMetrics().densityDpi;

        // Configurar ImageReader para capturar frames
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                if (!isTranslating) return;
                
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    processImageForOCR(image);
                    image.close();
                }
            }
        }, handler);

        // Crear virtual display
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            width, height, densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.getSurface(),
            null, handler
        );
        
        // Registrar callback para cuando se detenga la proyección
        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                stopSelf();
            }
        }, handler);
    }

    private void processImageForOCR(Image image) {
        // Convertir Image a Bitmap para OCR
        Bitmap bitmap = imageToBitmap(image);
        
        // Procesar OCR en segundo plano
        new Thread(() -> {
            ocrProcessor.processBitmap(bitmap, new OCRProcessor.OCRCallback() {
                @Override
                public void onTextDetected(List<TextBlock> textBlocks) {
                    // Enviar texto detectado al servicio de overlay
                    Intent overlayIntent = new Intent(ScreenCaptureService.this, 
                        TranslationOverlayService.class);
                    overlayIntent.putParcelableArrayListExtra("textBlocks", 
                        new ArrayList<>(textBlocks));
                    startService(overlayIntent);
                }
            });
        }).start();
    }

    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();
        
        Bitmap bitmap = Bitmap.createBitmap(
            image.getWidth() + rowPadding / pixelStride,
            image.getHeight(),
            Bitmap.Config.ARGB_8888
        );
        bitmap.copyPixelsFromBuffer(buffer);
        
        return Bitmap.createBitmap(bitmap, 0, 0, image.getWidth(), image.getHeight());
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, "capture_channel")
            .setContentTitle("Capturando pantalla")
            .setContentText("Traductor OCR activo")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "capture_channel",
                "Captura de Pantalla",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        if (imageReader != null) {
            imageReader.close();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
    }
}
