package com.example.screentranslator;

import android.app.Activity;
import android.content.Context;
import android.widget.LinearLayout;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.interstitial.*;

public class AdManager {
    private InterstitialAd interstitialAd;
    private Context context;
    
    public AdManager(Context context) {
        this.context = context;
        loadInterstitialAd();
    }
    
    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        
        InterstitialAd.load(context, "ca-app-pub-3940256099942544/1033173712", 
            adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitial) {
                interstitialAd = interstitial;
            }
            
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                interstitialAd = null;
            }
        });
    }
    
    public void showInterstitialAd() {
        if (interstitialAd != null && context instanceof Activity) {
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    loadInterstitialAd(); // Cargar nuevo anuncio
                }
            });
            
            interstitialAd.show((Activity) context);
        } else {
            loadInterstitialAd(); // Intentar cargar para la próxima vez
        }
    }
    
    public void loadBannerAd(LinearLayout adContainer) {
        AdView adView = new AdView(context);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
        
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        
        adContainer.addView(adView);
    }
}
