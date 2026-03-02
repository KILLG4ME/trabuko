package com.example.screentranslator;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class TranslationOverlayService extends Service {
    private WindowManager windowManager;
    private LinearLayout overlayLayout;
    private boolean isShowing = true;
    private Map<String, TextView> activeTranslations = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("textBlocks")) {
            ArrayList<OCRProcessor.TextBlock> textBlocks = 
                intent.getParcelableArrayListExtra("textBlocks");
            
            if (textBlocks != null && !textBlocks.isEmpty()) {
                showTranslations(textBlocks);
            }
        }
        return START_NOT_STICKY;
    }

    private void showTranslations(List<OCRProcessor.TextBlock> textBlocks) {
        // Limpiar traducciones anteriores
        clearOverlay();
        
        for (OCRProcessor.TextBlock block : textBlocks) {
            if (block.translatedText != null && !block.translatedText.isEmpty()) {
                createTranslationOverlay(block);
            }
        }
    }

    private void createTranslationOverlay(OCRProcessor.TextBlock block) {
        TextView textView = new TextView(this);
        textView.setText(block.translatedText);
        textView.setTextColor(Color.WHITE);
        textView.setBackgroundColor(Color.argb(180, 0, 120, 255)); // Azul semitransparente
        textView.setPadding(10, 5, 10, 5);
        textView.setTextSize(14);
        textView.setTypeface(null, Typeface.BOLD);
        
        // Configurar parámetros de ventana
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            (int) block.width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = (int) block.x;
        params.y = (int) block.y;
        
        // Añadir al window manager
        windowManager.addView(textView, params);
        
        // Guardar referencia para limpiar después
        String key = block.x + "_" + block.y;
        activeTranslations.put(key, textView);
    }

    private void clearOverlay() {
        for (TextView view : activeTranslations.values()) {
            try {
                windowManager.removeView(view);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        activeTranslations.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearOverlay();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
