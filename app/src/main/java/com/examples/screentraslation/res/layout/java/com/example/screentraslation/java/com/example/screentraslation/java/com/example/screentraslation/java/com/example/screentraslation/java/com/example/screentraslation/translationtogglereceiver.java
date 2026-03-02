package com.example.screentranslator;

import android.content.*;
import android.widget.Toast;

public class TranslationToggleReceiver extends BroadcastReceiver {
    private static boolean isTranslating = true;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        isTranslating = !isTranslating;
        
        Intent serviceIntent = new Intent(context, ScreenCaptureService.class);
        if (isTranslating) {
            // Reanudar traducción
            context.startService(serviceIntent);
            Toast.makeText(context, "Traductor activado", Toast.LENGTH_SHORT).show();
        } else {
            // Pausar traducción
            context.stopService(serviceIntent);
            Toast.makeText(context, "Traductor desactivado", Toast.LENGTH_SHORT).show();
        }
    }
}
