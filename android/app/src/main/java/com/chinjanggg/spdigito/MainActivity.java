package com.chinjanggg.spdigito;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    CardView btnUploadAct, btnManualAct;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        btnUploadAct = findViewById(R.id.btnUploadAct);
        btnManualAct = findViewById(R.id.btnManualAct);

        btnUploadAct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToUploadAct();
            }
        });

        btnManualAct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToManualAct();
            }
        });

    }

    private void goToUploadAct() {
        Intent intent = new Intent(MainActivity.this, ImageUploadActivity.class);
        startActivity(intent);
    }
    private void goToManualAct() {
        Intent intent = new Intent(MainActivity.this, ManualInputActivity.class);
        startActivity(intent);
    }
}
