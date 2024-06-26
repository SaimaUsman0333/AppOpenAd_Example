/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.example.appopenexample;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback;
import java.util.Date;

/** Application class that initializes, loads and show ads when activities change states. */
public class MyApplication extends Application
    implements ActivityLifecycleCallbacks, DefaultLifecycleObserver {


  // Interface definition
  public interface OnAdDismissedListener {
    void onAdDismissed();
  }

  public interface OnAdFailedListener{
    void OnAdFailed();
  }

  private OnAdDismissedListener adDismissedListener;
  private OnAdFailedListener adFailedListener;


  public void setAdDismissedListener(OnAdDismissedListener listener) {
    this.adDismissedListener = listener;
    appOpenAdManager.loadAd(this);
  }

  public void setAdFailedListener(OnAdFailedListener listener)
  {
    this.adFailedListener = listener;
    appOpenAdManager.loadAd(this);
  }

  private AppOpenAdManager appOpenAdManager;
  private Activity currentActivity;
  private static final String TAG = "MyApplication";

  public boolean willShow = false;
  public boolean isShown = false;

  MainActivity mainActivity;
  public AppOpenAd appOpenAd = null;


  @Override
  public void onCreate() {
    super.onCreate();
    this.registerActivityLifecycleCallbacks(this);

    ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    appOpenAdManager = new AppOpenAdManager();
    mainActivity = new MainActivity();
  }

  /**
   * DefaultLifecycleObserver method that shows the app open ad when the app moves to foreground.
   */
  @Override
  public void onStart(@NonNull LifecycleOwner owner) {
    DefaultLifecycleObserver.super.onStart(owner);
    // Show the ad (if available) when the app moves to foreground.
    //Add delay here
    addDelay();
//    appOpenAdManager.showAdIfAvailable(currentActivity);
  }

  void addDelay()
  {
    Handler handler = new Handler(Looper.getMainLooper()); // Get Handler to main thread

    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        appOpenAdManager.showAdIfAvailable(currentActivity);
      }
    };

// Schedule the runnable with a 2 second delay
    handler.postDelayed(runnable, 10);
  }

  /** ActivityLifecycleCallback methods. */
  @Override
  public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

  }

  @Override
  public void onActivityStarted(@NonNull Activity activity) {
    // An ad activity is started when an ad is showing, which could be AdActivity class from Google
    // SDK or another activity class implemented by a third party mediation partner. Updating the
    // currentActivity only when an ad is not showing will ensure it is not an ad activity, but the
    // one that shows the ad.

    if (!appOpenAdManager.isShowingAd) {
      currentActivity = activity;
    }
    else {
      willShow = true;
    }
  }

  @Override
  public void onActivityResumed(@NonNull Activity activity) {

  }

  @Override
  public void onActivityPaused(@NonNull Activity activity) {

  }

  @Override
  public void onActivityStopped(@NonNull Activity activity) {

  }

  @Override
  public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

  }

  @Override
  public void onActivityDestroyed(@NonNull Activity activity) {

  }

  /**
   * Load an app open ad.
   *
   * @param activity the activity that shows the app open ad
   */
  public void loadAd(@NonNull Activity activity) {
    // We wrap the loadAd to enforce that other classes only interact with MyApplication
    // class.
    appOpenAdManager.loadAd(activity);
  }

  /**
   * Shows an app open ad.
   *
   * @param activity the activity that shows the app open ad
   * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
   */
  public void showAdIfAvailable(
      @NonNull Activity activity, @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
    // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication
    // class.
//    addDelay(activity, onShowAdCompleteListener);
    appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener);
  }

//  void addDelay(Activity activity, OnShowAdCompleteListener onShowAdCompleteListener)
//  {
//    appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener);
//  }

  /**
   * Interface definition for a callback to be invoked when an app open ad is complete
   * (i.e. dismissed or fails to show).
   */
  public interface OnShowAdCompleteListener {
    void onShowAdComplete();
  }

  /** Inner class that loads and shows app open ads. */
  public class AppOpenAdManager {

    private static final String LOG_TAG = "AppOpenAdManager";
    private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921";

    private final GoogleMobileAdsConsentManager googleMobileAdsConsentManager =
        GoogleMobileAdsConsentManager.getInstance(getApplicationContext());

    public boolean isLoadingAd = false;
    public boolean isShowingAd = false;

    FullScreenDialogFragment dialogFragment = new FullScreenDialogFragment();

    /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
    private long loadTime = 0;

    /** Constructor. */
    public AppOpenAdManager() {}

    /**
     * Load an ad.
     *
     * @param context the context of the activity that loads the ad
     */
    private void loadAd(Context context) {
      // Do not load ad if there is an unused ad or one is already loading.
      if (isLoadingAd || isAdAvailable()) {
        return;
      }

      isLoadingAd = true;
      AdRequest request = new AdRequest.Builder().build();
      AppOpenAd.load(
          context,
          AD_UNIT_ID,
          request,
          new AppOpenAdLoadCallback() {
            /**
             * Called when an app open ad has loaded.
             *
             * @param ad the loaded app open ad.
             */
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
              appOpenAd = ad;
              isLoadingAd = false;


              loadTime = (new Date()).getTime();

              Log.d(LOG_TAG, "onAdLoaded.");
              Toast.makeText(context, "onAdLoaded", Toast.LENGTH_SHORT).show();
              willShow = true;
            }

            /**
             * Called when an app open ad has failed to load.
             *
             * @param loadAdError the error.
             */
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
              isLoadingAd = false;
              Log.d(LOG_TAG, "onAdFailedToLoad: " + loadAdError.getMessage());
              Toast.makeText(context, "onAdFailedToLoad", Toast.LENGTH_SHORT).show();
              willShow = false;
            }
          });
    }

    /** Check if ad was loaded more than n hours ago. */
    private boolean wasLoadTimeLessThanNHoursAgo() {
      long dateDifference = (new Date()).getTime() - loadTime;
      long numMilliSecondsPerHour = 3600000;
      return (dateDifference < (numMilliSecondsPerHour * (long) 4));
    }

    /** Check if ad exists and can be shown. */
    private boolean isAdAvailable() {
      // Ad references in the app open beta will time out after four hours, but this time limit
      // may change in future beta versions. For details, see:
      // https://support.google.com/admob/answer/9341964?hl=en
      willShow = true;
      return appOpenAd != null && wasLoadTimeLessThanNHoursAgo();

    }

    /**
     * Show the ad if one isn't already showing.
     *
     * @param activity the activity that shows the app open ad
     */
    private void showAdIfAvailable(@NonNull final Activity activity) {
      showAdIfAvailable(
          activity,
          new OnShowAdCompleteListener() {
            @Override
            public void onShowAdComplete() {
              // Empty because the user will go back to the activity that shows the ad.
              willShow = false;
            }
          });
    }

    /**
     * Show the ad if one isn't already showing.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */
    private void showAdIfAvailable(
        @NonNull final Activity activity,
        @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
//      willShow = true;
      // If the app open ad is already showing, do not show the ad again.
      if (isShowingAd) {
        Log.d(LOG_TAG, "The app open ad is already showing.");
        return;
      }

      // If the app open ad is not available yet, invoke the callback then load the ad.
      if (!isAdAvailable()) {
        Log.d(LOG_TAG, "The app open ad is not ready yet.");

        onShowAdCompleteListener.onShowAdComplete();
        if (googleMobileAdsConsentManager.canRequestAds()) {
          loadAd(currentActivity);
        }
        return;
      }

      Log.d(LOG_TAG, "Will show ad.");

      appOpenAd.setFullScreenContentCallback(
          new FullScreenContentCallback() {
            /** Called when full screen content is dismissed.*/
            @Override
            public void onAdDismissedFullScreenContent() {
              // Set the reference to null so isAdAvailable() returns false.

              if (adDismissedListener != null) {
                adDismissedListener.onAdDismissed();

                appOpenAd = null;
                isShowingAd = false;
                isShown = false;
                willShow = false;
//              mainActivity.fullScreenDialog.dismiss();

                Log.d(LOG_TAG, "onAdDismissedFullScreenContent.");
              Toast.makeText(activity, "onAdDismissedFullScreenContent", Toast.LENGTH_SHORT).show();

                onShowAdCompleteListener.onShowAdComplete();
                if (googleMobileAdsConsentManager.canRequestAds()) {
                  loadAd(activity);
                }
              }
            }

            /** Called when fullscreen content failed to show. */
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {

              if ( adFailedListener!= null)
              {
                adFailedListener.OnAdFailed();
              }

              appOpenAd = null;
              isShowingAd = false;
              isShown = false;
              willShow = false;
              if (dialogFragment!= null)
              {
                dialogFragment.dismiss();
              }

              Log.d(LOG_TAG, "onAdFailedToShowFullScreenContent: " + adError.getMessage());
              Toast.makeText(activity, "onAdFailedToShowFullScreenContent", Toast.LENGTH_SHORT)
                  .show();

              onShowAdCompleteListener.onShowAdComplete();
              if (googleMobileAdsConsentManager.canRequestAds()) {
                loadAd(activity);
              }
            }

            /** Called when fullscreen content is shown. */
            @Override
            public void onAdShowedFullScreenContent() {
              Log.d(LOG_TAG, "onAdShowedFullScreenContent.");
              Toast.makeText(activity, "onAdShowedFullScreenContent", Toast.LENGTH_SHORT).show();
            }
          });

      isShowingAd = true;
      isShown = true;
      appOpenAd.show(activity);
    }
  }
}
