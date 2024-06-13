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
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

/**
 * Main activity in the app.
 */
public class MainActivity extends AppCompatActivity implements Application.ActivityLifecycleCallbacks, MyApplication.OnAdDismissedListener, MyApplication.OnAdFailedListener {

    private GoogleMobileAdsConsentManager googleMobileAdsConsentManager;

    private static boolean isMainActivityVisible;
    public FullScreenDialogFragment fullScreenDialog;
    Button showAd;

    static Boolean isFromSecond = false;

    MyApplication myApp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showAd = findViewById(R.id.showAd);
        myApp = (MyApplication) getApplication();

        myApp.setAdDismissedListener(this);
        myApp.setAdFailedListener(this);


        googleMobileAdsConsentManager =
                GoogleMobileAdsConsentManager.getInstance(getApplicationContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerActivityLifecycleCallbacks(this);
        }
//    fullScreenDialog = new FullScreenDialogFragment();
//    if (fullScreenDialog.isVisible())
//    {
//      fullScreenDialog.dismiss();
//    }

        showAd.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (myApp.willShow && myApp.appOpenAd != null) {
                    fullScreenDialog = new FullScreenDialogFragment();
                    fullScreenDialog.show(getSupportFragmentManager(), "FullScreenDialogFragment");
                }
                Application application = getApplication();
                ((MyApplication) application)
                        .showAdIfAvailable(
                                MainActivity.this,
                                () -> {
                                    Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                                    startActivity(intent);
                                    fullScreenDialog.dismiss();
                                    myApp.willShow = false;
                                });
            }
        });
    }

    private void DismissDialog() {
        if (fullScreenDialog != null) {
            fullScreenDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_menu, menu);
        MenuItem moreMenu = menu.findItem(R.id.action_more);
        moreMenu.setVisible(googleMobileAdsConsentManager.isPrivacyOptionsRequired());
        return true;
    }

    public boolean isMainActivityVisible() {
        return isMainActivityVisible;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        DismissDialog();
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        DismissDialog();
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (activity instanceof MainActivity) {
//            if (isMainActivityVisible) {
                if (!isFromSecond){
                    Log.e("TAG", "onActivityResumed: sdfsaf" );
                    if (myApp.willShow && myApp.appOpenAd != null) {
                        fullScreenDialog = new FullScreenDialogFragment();
                        fullScreenDialog.show(getSupportFragmentManager(), "FullScreenDialogFragment");
                        myApp.willShow = false;
                    } else {
                        if (fullScreenDialog != null) {
                            fullScreenDialog.dismiss();
                        }
                    }
                }else {
                    if(myApp.isShown){
                        Log.e("TAG", "onActivityResumed: hidebanner" );
                    }

                }

//            } else {
//                if (fullScreenDialog != null) {
//                    fullScreenDialog.dismiss();
//                }
//            }
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (activity instanceof MainActivity) {
            isMainActivityVisible = true;
        }
        else {
            isMainActivityVisible = false;
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        if (activity instanceof MainActivity) {
            isMainActivityVisible = true;
        }
        else {
            isMainActivityVisible = false;

        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (activity instanceof MainActivity) {
            isMainActivityVisible = true;
        }
        else {
            isMainActivityVisible = false;

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        View menuItemView = findViewById(item.getItemId());
        PopupMenu popup = new PopupMenu(this, menuItemView);
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
        popup.show();
        popup.setOnMenuItemClickListener(
                popupMenuItem -> {
                    if (popupMenuItem.getItemId() == R.id.privacy_settings) {
                        // Handle changes to user consent.
                        googleMobileAdsConsentManager.showPrivacyOptionsForm(
                                this,
                                formError -> {
                                    if (formError != null) {
                                        Toast.makeText(this, formError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                        return true;
                    }
                    return false;
                });
        return super.onOptionsItemSelected(item);
    }

    /**
     * Override the default implementation when the user presses the back key.
     */
    @Override
    @SuppressWarnings("MissingSuperCall")
    public void onBackPressed() {
        // Move the task containing the MainActivity to the back of the activity stack, instead of
        // destroying it. Therefore, MainActivity will be shown when the user switches back to the app.
        moveTaskToBack(true);
    }

    @Override
    public void onAdDismissed() {
        if (fullScreenDialog != null) {
            fullScreenDialog.dismiss();
//      Application application = getApplication();
//      ((MyApplication) application).loadAd(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

//        if (!isFromSecond) {
//            if (myApp.willShow && myApp.appOpenAd != null) {
//                Log.e("TAG", "onResume: hide banner" );
//                Toast.makeText(MainActivity.this, "hide", Toast.LENGTH_SHORT).show();
//                fullScreenDialog = new FullScreenDialogFragment();
//                fullScreenDialog.show(getSupportFragmentManager(), "FullScreenDialogFragment");
//            }
//        }

    }

    @Override
    public void OnAdFailed() {
        if (fullScreenDialog != null) {
            fullScreenDialog.dismiss();
        }
    }

    void checkForUpdate() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());

// Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

// Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.clientVersionStalenessDays() != null
                    && appUpdateInfo.clientVersionStalenessDays() >= 2
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                // Request the update.
            }
        });
    }
}
