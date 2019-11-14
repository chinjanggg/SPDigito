package com.chinjanggg.spdigito;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ImageConfirmationActivity extends AppCompatActivity {

    ApiService apiService;
    private static final int REQUEST_EXIT = 1;
    TextInputEditText pidCF, nameCF, addressCF, sysCF, diaCF, pulseCF;
    ImageView imgCF;
    String resultData;
    FloatingActionButton btnEdit, btnSave, btnCancel;
    Intent intent;
    Bundle extras;
    String pid, sys, dia, pulse;
    byte[] bitmapData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_confirmation);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Set action bar title and remove back button
        getSupportActionBar().setTitle(R.string.confirmation);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        pidCF = findViewById(R.id.pidCF);
        nameCF = findViewById(R.id.nameCF);
        addressCF = findViewById(R.id.addressCF);
        sysCF = findViewById(R.id.sysCF);
        diaCF = findViewById(R.id.diaCF);
        pulseCF = findViewById(R.id.pulseCF);
        imgCF = findViewById(R.id.imgCF);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editData();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmSaving();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageConfirmationActivity.this.finish();
            }
        });

        initRetrofitClient();
        setData();

    }

    private void initRetrofitClient() {
        Retrofit retrofit = NetworkClient.getRetrofitClient();
        apiService = retrofit.create(ApiService.class);
    }

    private void getPatientData(String pid) {
        Call<ResponseBody> req = apiService.getPatientData(pid);
        req.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.code() == 200) {
                    resultData = response.message();
                    setPatientData(resultData);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getApplicationContext(), R.string.err_msg_load_data, Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
    }
    private void setPatientData(String patientData) {
        Gson g = new Gson();
        PData pdata = g.fromJson(patientData, PData.class);
        if(pdata != null) {
            String pname = pdata.getName();
            String street = pdata.getStreet();
            String city = pdata.getCity();
            String country = pdata.getCountry();
            String zipcode = pdata.getZipcode();
            String address = street+", "+city+", "+country+", "+zipcode;
            nameCF.setText(pname);
            addressCF.setText(address);
        }
    }
    private void setData() {
        intent = getIntent();
        extras = intent.getExtras();
        assert extras != null;
        pid = extras.getString("EXTRA_PID");
        String result = extras.getString("EXTRA_RESULT");
        bitmapData = extras.getByteArray("EXTRA_IMAGE");

        //Set pid
        pidCF.setText(pid);

        //Set patient info
        getPatientData(pid);

        //set result data
        Gson g = new Gson();
        Result res = g.fromJson(result, Result.class);
        sys = String.valueOf(res.getSys());
        dia = String.valueOf(res.getDia());
        pulse = String.valueOf(res.getPulse());
        sysCF.setText(sys);
        diaCF.setText(dia);
        pulseCF.setText(pulse);

        //set image
        Bitmap bmp = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
        imgCF.setImageBitmap(bmp);
    }
    private void editData() {
        Intent resIntent = new Intent(ImageConfirmationActivity.this, ImageEditActivity.class);
        Bundle resExtras = new Bundle();
        String pid = pidCF.getText().toString();
        resExtras.putString("EXTRA_PID", pid);
        Result result = new Result(sys, dia, pulse);
        Gson g = new Gson();
        String res = g.toJson(result);
        resExtras.putString("EXTRA_RESULT", res);
        resExtras.putByteArray("EXTRA_IMAGE", bitmapData);
        resIntent.putExtras(resExtras);

        startActivityForResult(resIntent, REQUEST_EXIT);
        ImageConfirmationActivity.this.finish();
    }
    private void confirmSaving() {
        new AlertDialog.Builder(ImageConfirmationActivity.this)
                .setTitle(R.string.confirm_title)
                .setMessage(R.string.confirm_msg)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveData();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Cancel
                    }
                })
        .show();
    }
    private void saveData() {
        int saveSys = Integer.parseInt(sys);
        int saveDia = Integer.parseInt(dia);
        int savePulse = Integer.parseInt(pulse);
        Call<ResponseBody> req = apiService.saveData(pid, saveSys, saveDia, savePulse);
        req.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.code() == 200) {
                    Toast.makeText(getApplicationContext(), R.string.saved, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK, null);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("EXIT", true);
                    startActivity(intent);
                    ImageConfirmationActivity.this.finish();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getApplicationContext(), R.string.err_msg_save, Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
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
