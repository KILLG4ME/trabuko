package com.example.screentranslator;

import android.content.Context;
import android.graphics.Bitmap;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.*;

public class OCRProcessor {
    private TextRecognizer textRecognizer;
    private TranslationService translationService;
    
    public interface OCRCallback {
        void onTextDetected(List<TextBlock> textBlocks);
    }
    
    public static class TextBlock {
        public String originalText;
        public String translatedText;
        public float x, y, width, height;
        
        public TextBlock(String text, float x, float y, float width, float height) {
            this.originalText = text;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
    
    public OCRProcessor(Context context) {
        // Inicializar ML Kit OCR - procesamiento local sin internet
        textRecognizer = TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        );
        
        translationService = new TranslationService();
    }
    
    public void processBitmap(Bitmap bitmap, OCRCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        
        Task<Text> result = textRecognizer.process(image)
            .addOnSuccessListener(visionText -> {
                List<TextBlock> textBlocks = new ArrayList<>();
                
                for (Text.TextBlock block : visionText.getTextBlocks()) {
                    for (Text.Line line : block.getLines()) {
                        String text = line.getText();
                        if (text.trim().length() > 2) { // Ignorar texto muy corto
                            Rect boundingBox = line.getBoundingBox();
                            if (boundingBox != null) {
                                TextBlock textBlock = new TextBlock(
                                    text,
                                    boundingBox.left,
                                    boundingBox.top,
                                    boundingBox.width(),
                                    boundingBox.height()
                                );
                                
                                // Traducir el texto
                                textBlock.translatedText = 
                                    translationService.translate(text);
                                textBlocks.add(textBlock);
                            }
                        }
                    }
                }
                
                callback.onTextDetected(textBlocks);
            })
            .addOnFailureListener(e -> {
                e.printStackTrace();
                callback.onTextDetected(new ArrayList<>());
            });
    }
}
