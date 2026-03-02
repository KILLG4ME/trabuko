package com.example.screentranslator;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 2;
    
    private MediaProjectionManager mediaProjectionManager;
    private Button btnToggleTranslation;
    private AdManager adManager;
    private int translationActivationCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Inicializar AdMob
        MobileAds.initialize(this, initializationStatus -> {});
        adManager = new AdManager(this);
        
        mediaProjectionManager = (MediaProjectionManager) 
            getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        
        btnToggleTranslation = findViewById(R.id.btnToggleTranslation);
        
        // Verificar permiso de superposición
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
            }
        }
        
        btnToggleTranslation.setOnClickListener(v -> {
            translationActivationCount++;
            
            // Mostrar anuncio interstitial cada 5 activaciones
            if (translationActivationCount % 5 == 0) {
                adManager.showInterstitialAd();
            }
            
            startScreenCapture();
        });
        
        // Configurar banner de AdMob
        adManager.loadBannerAd(findViewById(R.id.adView));
    }

    private void startScreenCapture() {
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE_SCREEN_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                // Iniciar servicio de captura
                Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                
                Toast.makeText(this, "Traductor activado", Toast.LENGTH_SHORT).show();
                btnToggleTranslation.setText("Traductor Activado");
                
                // Crear notificación persistente con switch
                createTranslationNotification();
            } else {
                Toast.makeText(this, "Permiso denegado para capturar pantalla", 
                    Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Permiso de superposición concedido", 
                        Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private void createTranslationNotification() {
        // Crear canal de notificación para Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "translation_channel",
                "Traductor de Pantalla",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Control del traductor de pantalla");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        
        // Crear notificación con switch para activar/desactivar
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "translation_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Traductor de Pantalla")
            .setContentText("Toca para configurar")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true);
            
        // Añadir acción de switch
        Intent toggleIntent = new Intent(this, TranslationToggleReceiver.class);
        PendingIntent togglePendingIntent = PendingIntent.getBroadcast(
            this, 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
        builder.addAction(R.drawable.ic_translate, "Activar/Desactivar", togglePendingIntent);
        
        NotificationManager notificationManager = (NotificationManager) 
            getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1002, builder.build());
    }
}
