package com.chinjanggg.spdigito;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ImageUploadActivity extends AppCompatActivity {

    ApiService apiService;
    private ImageView imgSelect;
    private Bitmap imageBP;
    private Uri contentURI;
    private static final int CAMERA = 1;
    private static final int GALLERY = 2;
    private String currentPhotoPath;
    private TextInputEditText pidInput;
    private TextView result;
    FloatingActionButton btnSelect, btnUpload, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_upload);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Set action bar title and remove back button
        getSupportActionBar().setTitle(R.string.image_upload);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        btnSelect = findViewById(R.id.btnSelect);
        btnUpload = findViewById(R.id.btnUpload);
        btnCancel = findViewById(R.id.btnCancel);
        pidInput = findViewById(R.id.pidInput);
        imgSelect = findViewById(R.id.selectedImage);
        result = findViewById(R.id.resultTextView);

        //Make pid CAP
        addFilter(pidInput, new InputFilter.AllCaps());
        //Add listener
        addListener();
        btnUpload.setEnabled(false);

        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPicture();
            }
        });
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearText();
                if (imageBP != null) {
                    uploadPicture();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.err_msg_no_img, Toast.LENGTH_LONG).show();
                }
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageUploadActivity.this.finish();
            }
        });

        askPermissions();
        initRetrofitClient();
    }

    private void addFilter(EditText editText, InputFilter filter) {
        InputFilter[] curFilters = editText.getFilters();

        if(curFilters != null) {
            InputFilter[] newFilters = new InputFilter[curFilters.length + 1];
            System.arraycopy(curFilters, 0, newFilters, 0, curFilters.length);
            newFilters[curFilters.length] = filter;
            editText.setFilters(newFilters);
        } else {
            editText.setFilters(new InputFilter[] {filter});
        }

    }
    private void addListener() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(validateVal(pidInput)) {
                    btnUpload.setEnabled(false);
                } else {
                    btnUpload.setEnabled(true);
                }
            }
        };
        pidInput.addTextChangedListener(watcher);
    }
    private void askPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Dexter.withActivity(this)
                    .withPermissions(
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport report) {
                            // check if all permissions are granted
                            if (report.areAllPermissionsGranted()) {
                                Toast.makeText(getApplicationContext(), "All permissions are granted by user!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                            token.continuePermissionRequest();
                        }
                    }).
                    withErrorListener(new PermissionRequestErrorListener() {
                        @Override
                        public void onError(DexterError error) {
                            Toast.makeText(getApplicationContext(), "Some Error! ", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .onSameThread()
                    .check();
        }
    }
    private void initRetrofitClient() {
        Retrofit retrofit = NetworkClient.getRetrofitClient();
        apiService = retrofit.create(ApiService.class);
    }

    private void selectPicture() {
        AlertDialog.Builder pictureSelectDialog = new AlertDialog.Builder(this);
        pictureSelectDialog.setTitle(R.string.select_action);
        String[] items = {
                getString(R.string.take_picture),
                getString(R.string.open_gallery)
        };
        pictureSelectDialog.setItems(items,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                takePictureFromCamera();
                                break;
                            case 1:
                                choosePictureFromGallery();
                                break;
                        }
                    }
                });
        pictureSelectDialog.show();
    }
    private void takePictureFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File imageFile = null;
            try {
                imageFile = createImageFile();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            if (imageFile != null) {
                Uri imageURI = FileProvider.getUriForFile(this,
                        "com.chinjanggg.android.fileprovider",
                        imageFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI);
                startActivityForResult(intent, CAMERA);
            }
        }
    }
    private void choosePictureFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        String[] mimeTypes = {"image/jpeg,", "image/png"};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        }
        startActivityForResult(intent, GALLERY);
    }
    private String getPathFromURI(Uri contentURI) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentURI, proj, null, null, null);
        assert cursor != null;
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(columnIndex);
        cursor.close();

        return path;
    }
    private void setImageView(Bitmap bp) {
        if(bp != null) {
            imgSelect.setImageBitmap(bp);
        } else {
            Toast.makeText(this, "Image Error", Toast.LENGTH_SHORT).show();
        }
    }
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                fileName,
                ".jpg",
                storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    private void removePicture() {
        imgSelect.setImageResource(0);
        currentPhotoPath = "";
        imageBP = null;
    }
    private void clearText() {
        result.setText("");
    }
    private void uploadPicture() {
        if(isNetworkConnected()) {
            //Check Patient ID whether it's in correct form
            if(validatePID()) {
                if (imageBP != null) {
                    Toast.makeText(this, R.string.sending, Toast.LENGTH_LONG).show();

                    try {
                        File filesDir = getApplicationContext().getFilesDir();
                        File file = new File(filesDir, "image" + ".png");

                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        imageBP.compress(Bitmap.CompressFormat.PNG, 0, bos);
                        final byte[] bitmapData = bos.toByteArray();

                        Bitmap resizeImage = resizeImage(imageBP);
                        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
                        resizeImage.compress(Bitmap.CompressFormat.PNG, 0, bos2);
                        final byte[] bpData = bos2.toByteArray();

                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(bitmapData);
                        fos.flush();
                        fos.close();

                        RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
                        MultipartBody.Part body = MultipartBody.Part.createFormData("upload", file.getName(), reqFile);
                        RequestBody name = RequestBody.create(MediaType.parse("text/plain"), "upload");

                        final String pid = pidInput.getText().toString();

                        RequestBody pidReq = RequestBody.create(MediaType.parse("text/plain"), pid);
                        Call<ResponseBody> req = apiService.postImage(pidReq, body, name);
                        req.enqueue(new Callback<ResponseBody>() {
                            String res;
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.code() == 200) {
                                    Intent intent = new Intent(ImageUploadActivity.this, ImageConfirmationActivity.class);
                                    Bundle extras = new Bundle();
                                    extras.putString("EXTRA_PID", pid);
                                    extras.putString("EXTRA_RESULT", response.message());
                                    extras.putByteArray("EXTRA_IMAGE", bpData);
                                    intent.putExtras(extras);
                                    startActivity(intent);
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                res = getString(R.string.err_msg_upload);
                                result.setText(res);
                                result.setTextColor(Color.RED);
                                Toast.makeText(getApplicationContext(), res, Toast.LENGTH_SHORT).show();
                                t.printStackTrace();
                            }
                        });
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, R.string.err_msg_image, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.err_msg_pid, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.err_msg_connection, Toast.LENGTH_LONG).show();
        }
    }
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
    private Bitmap resizeImage(Bitmap image) {
        final int reducedSize = 200;
        int width = image.getWidth();
        int height = image.getHeight();
        int newW, newH;
        float ratio;
        if(width > height) {
            newW = reducedSize;
            ratio = (float) (width / newW);
            newH = (int) (height / ratio);
        } else {
            newH = reducedSize;
            ratio = (float) (height / newH);
            newW = (int) (width / ratio);
        }
        return Bitmap.createScaledBitmap(image, newW, newH, true);
    }

    private boolean validatePID() {
        String pid = "";
        if(pidInput.getText() != null)
            pid = pidInput.getText().toString();
        return Pattern.matches("(HR)[0-9]{3}", pid);
    }
    private boolean validateVal(EditText e) {
        return e.getText().toString().trim().length() == 0;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            clearText();
            switch (requestCode) {
                case CAMERA:
                    try {
                        File file = new File(currentPhotoPath);
                        contentURI = Uri.fromFile(file);
                        imageBP = MediaStore.Images.Media.getBitmap(
                                getApplicationContext().getContentResolver(), contentURI);
                        setImageView(imageBP);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case GALLERY:
                    if (data != null) {
                        contentURI = data.getData();
                        currentPhotoPath = getPathFromURI(contentURI);
                        imageBP = BitmapFactory.decodeFile(currentPhotoPath);
                        setImageView(imageBP);
                    }
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, R.string.cancelled, Toast.LENGTH_LONG).show();
        }
    }
}
