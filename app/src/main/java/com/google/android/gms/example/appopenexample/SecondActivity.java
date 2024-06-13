package com.google.android.gms.example.appopenexample;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SecondActivity extends AppCompatActivity {

    Button showAd;
    MyApplication myApplication;
    FullScreenDialogFragment fullScreenDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        myApplication = new MyApplication();
        showAd = findViewById(R.id.showAd);
        showAd.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (myApplication.willShow)
                {
                    fullScreenDialog = new FullScreenDialogFragment();
                    fullScreenDialog.show(getSupportFragmentManager(), "FullScreenDialogFragment");
                }
                Application application = getApplication();
                ((MyApplication) application)
                        .showAdIfAvailable(
                                SecondActivity.this,
                                () -> {
                                    Intent intent = new Intent(SecondActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    fullScreenDialog.dismiss();
                                    myApplication.willShow = false;
                                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        MainActivity.isFromSecond = true;
        super.onBackPressed();
    }
}