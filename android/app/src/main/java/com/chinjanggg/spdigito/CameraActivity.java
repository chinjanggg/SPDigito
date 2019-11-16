package com.chinjanggg.spdigito;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_EXIT = 1;
    private Camera mCamera;
    private CameraPreview mPreview;
    private Camera.PictureCallback mPicture;
    private boolean cameraFront = false;
    public static Bitmap imageBP;
    public static Bitmap cropImage;
    private int preview_width;
    private final int preview_height = 960;
    private final int CROP_WIDTH = 480;
    private final int CROP_HEIGHT = 640;
    private int startX, startY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Hide action bar
        getSupportActionBar().hide();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setPreviewWidth();
        setCropLocation();

        RelativeLayout cameraPreview = findViewById(R.id.cameraPreview);
        FloatingActionButton btnCapture = findViewById(R.id.btnCamera);
        FloatingActionButton btnSwitch = findViewById(R.id.btnSwitch);

        mCamera =  Camera.open();
        mCamera.setDisplayOrientation(90);
        mPreview = new CameraPreview(this, mCamera);
        cameraPreview.addView(mPreview);
        mCamera.startPreview();

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(null, null, mPicture);
            }
        });

        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get the number of cameras
                int camerasNumber = Camera.getNumberOfCameras();
                if (camerasNumber > 1) {
                    //release the old camera instance
                    //switch camera, from the front and the back and vice versa

                    releaseCamera();
                    chooseCamera();
                }
            }
        });
    }

    private void setPreviewWidth() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        preview_width = displayMetrics.widthPixels;
    }
    private void setCropLocation() {
        startX = (preview_width - CROP_WIDTH) / 2;
        startY = (preview_height - CROP_HEIGHT) / 2;
    }

    public void chooseCamera() {
        //if the camera preview is the front
        if (cameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                mCamera = Camera.open(cameraId);
                mCamera.setDisplayOrientation(90);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview
                mCamera = Camera.open(cameraId);
                mCamera.setDisplayOrientation(90);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        }
    }
    private int findFrontFacingCamera() {

        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }
    private int findBackFacingCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    public void onResume() {
        super.onResume();
        if(mCamera == null) {
            mCamera = Camera.open();
            mCamera.setDisplayOrientation(90);
            mPicture = getPictureCallback();
            mPreview.refreshCamera(mCamera);
            Log.d("nu", "null");
        }else {
            Log.d("nu","no null");
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        //when on Pause, release camera in order to be used from other applications
        releaseCamera();
    }
    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Camera.PictureCallback getPictureCallback() {
        return new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                mCamera.stopPreview();

                //Create image file
                File imageFile = null;
                try {
                    imageFile = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(imageFile != null) {
                    try {
                        FileOutputStream fos = new FileOutputStream(imageFile);
                        fos.write(data);
                        fos.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Uri imageURI = Uri.fromFile(imageFile);
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                //Rotate image if required
                imageBP = rotateImage(bitmap, 90);
                cropImage = cropImage(imageBP);

                //Save rotated image
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                cropImage.compress(Bitmap.CompressFormat.JPEG, 30, bos);
                byte[] rotatedImage = bos.toByteArray();
                try {
                    FileOutputStream fos = new FileOutputStream(imageFile);
                    fos.write(rotatedImage);
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Intent intent = new Intent(CameraActivity.this, ImagePreviewActivity.class);
                startActivityForResult(intent, REQUEST_EXIT);
                CameraActivity.this.finish();
            }
        };
    }
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        //String photoPath = image.getAbsolutePath();
        return File.createTempFile(
                fileName,
                ".jpg",
                storageDir
        );
    }
    private Bitmap rotateIfRequired(Bitmap image, Uri imageURI) throws IOException {
        if(imageURI.getPath() != null) {
            ExifInterface ei = new ExifInterface(imageURI.getPath());
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch(orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(image, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(image, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(image, 270);
                default:
                    return image;
            }
        }
        else {
            return null;
        }
    }
    private Bitmap rotateImage(Bitmap image, int degree) {
        Matrix m = new Matrix();
        m.postRotate(degree);
        Bitmap rotatedImage = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), m, true);
        image.recycle();
        return rotatedImage;
    }
    private Bitmap cropImage(Bitmap image) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int crop_startX = startX * imageWidth / preview_width;
        int crop_startY = startY * imageHeight / preview_height;
        int crop_width = CROP_WIDTH * imageWidth / preview_width;
        int crop_height = CROP_HEIGHT * imageHeight / preview_height;

        return Bitmap.createBitmap(image, crop_startX, crop_startY, crop_width, crop_height);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_EXIT) {
            if(resultCode == RESULT_OK) {
                this.finish();
            }
        }
    }
}