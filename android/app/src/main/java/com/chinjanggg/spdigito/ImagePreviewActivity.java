package com.chinjanggg.spdigito;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ImagePreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Hide action bar
        getSupportActionBar().hide();

        FloatingActionButton btnRetake = findViewById(R.id.btnRetake);
        FloatingActionButton btnCancel = findViewById(R.id.btnCamCancel);
        FloatingActionButton btnConfirm = findViewById(R.id.btnCamConfirm);
        ImageView imagePreview = findViewById(R.id.imagePreview);

        imagePreview.setImageBitmap(CameraActivity.imageBP);

        btnRetake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ImagePreviewActivity.this, CameraActivity.class);
                startActivity(intent);
                ImagePreviewActivity.this.finish();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK, null);
                ImagePreviewActivity.this.finish();
            }
        });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageUploadActivity.imageBP = CameraActivity.cropImage;
                ImageUploadActivity.imgSelect.setImageBitmap(ImageUploadActivity.imageBP);
                setResult(RESULT_OK, null);
                ImagePreviewActivity.this.finish();
            }
        });
    }
}
