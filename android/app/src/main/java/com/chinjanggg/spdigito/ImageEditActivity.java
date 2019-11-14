package com.chinjanggg.spdigito;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.util.regex.Pattern;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ImageEditActivity extends AppCompatActivity {

    ApiService apiService;
    Bundle extras;
    TextInputEditText pidEdit, nameEdit, addressEdit, sysEdit, diaEdit, pulseEdit;
    ImageView imgResult;
    FloatingActionButton btnSend, btnCancel;
    String resultData;
    byte[] bitmapData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Set action bar title and remove back button
        getSupportActionBar().setTitle(R.string.edit);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        pidEdit = findViewById(R.id.pidEdit);
        nameEdit = findViewById(R.id.nameEdit);
        addressEdit = findViewById(R.id.addressEdit);
        sysEdit = findViewById(R.id.sysEdit);
        diaEdit = findViewById(R.id.diaEdit);
        pulseEdit = findViewById(R.id.pulseEdit);
        imgResult = findViewById(R.id.imgEdit);
        btnSend = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        Intent intent = getIntent();
        extras = intent.getExtras();
        bitmapData = extras.getByteArray("EXTRA_IMAGE");

        //Make pid CAP
        addFilter(pidEdit, new InputFilter.AllCaps());
        //Add listener
        addListener();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!validatePID()) {
                    Toast.makeText(getApplicationContext(), R.string.err_msg_pid, Toast.LENGTH_SHORT).show();
                } else {
                    String pid = pidEdit.getText().toString();
                    String sys = sysEdit.getText().toString();
                    String dia = diaEdit.getText().toString();
                    String pulse = pulseEdit.getText().toString();
                    Intent intent = new Intent(ImageEditActivity.this, ImageConfirmationActivity.class);
                    Bundle extras = new Bundle();
                    extras.putString("EXTRA_PID", pid);
                    Result result = new Result(sys, dia, pulse);
                    Gson g = new Gson();
                    String res = g.toJson(result);
                    extras.putString("EXTRA_RESULT", res);
                    extras.putByteArray("EXTRA_IMAGE", bitmapData);

                    intent.putExtras(extras);
                    startActivity(intent);
                    setResult(RESULT_OK, null);
                    ImageEditActivity.this.finish();
                }
            }
        });


        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageEditActivity.this.finish();
            }
        });

        initRetrofitClient();
        setData();

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
                if(validateVal(pidEdit) || validateVal(sysEdit) || validateVal(diaEdit) || validateVal(pulseEdit)) {
                    btnSend.setEnabled(false);
                } else {
                    btnSend.setEnabled(true);
                }
            }
        };
        pidEdit.addTextChangedListener(watcher);
        sysEdit.addTextChangedListener(watcher);
        diaEdit.addTextChangedListener(watcher);
        pulseEdit.addTextChangedListener(watcher);
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
            String address = street+", "+city+", "+country+" "+zipcode;
            nameEdit.setText(pname);
            addressEdit.setText(address);
        }
    }
    private void setData() {
        String pid = extras.getString("EXTRA_PID");
        String result = extras.getString("EXTRA_RESULT");

        //Set pid
        pidEdit.setText(pid);

        //Set patient info
        getPatientData(pid);

        //set result data
        Gson g = new Gson();
        Result res = g.fromJson(result, Result.class);
        sysEdit.setText(String.valueOf(res.getSys()));
        diaEdit.setText(String.valueOf(res.getDia()));
        pulseEdit.setText(String.valueOf(res.getPulse()));

        //set image
        Bitmap bmp = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
        imgResult.setImageBitmap(bmp);
    }
    private boolean validatePID() {
        String pid = "";
        if(pidEdit.getText() != null)
            pid = pidEdit.getText().toString();
        return Pattern.matches("(HR)[0-9]{3}", pid);
    }
    private boolean validateVal(EditText e) {
        return e.getText().toString().trim().length() == 0;
    }
}
