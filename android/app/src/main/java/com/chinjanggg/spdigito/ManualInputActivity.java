package com.chinjanggg.spdigito;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import java.util.regex.Pattern;
import retrofit2.Retrofit;

public class ManualInputActivity extends AppCompatActivity {

    ApiService apiService;
    private TextInputEditText pidInput;
    EditText sysInput, diaInput, pulseInput;
    FloatingActionButton btnUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_input);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getSupportActionBar().setTitle(R.string.manual_form);

        pidInput = findViewById(R.id.pidInput);
        sysInput = findViewById(R.id.sysInput);
        diaInput = findViewById(R.id.diaInput);
        pulseInput = findViewById(R.id.pulseInput);

        //Make pid CAP
        addFilter(pidInput, new InputFilter.AllCaps());
        //Add listener
        addListener();

        btnUpload = findViewById(R.id.btnManualUpload);
        btnUpload.setEnabled(false);
        FloatingActionButton btnCancel = findViewById(R.id.btnCancel);

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postResult();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ManualInputActivity.this.finish();
            }
        });

        initRetrofitClient();
    }

    private void initRetrofitClient() {
        Retrofit retrofit = NetworkClient.getRetrofitClient();
        apiService = retrofit.create(ApiService.class);
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
                if(validateVal(pidInput) && validateVal(sysInput) && validateVal(diaInput) && validateVal(pulseInput)) {
                    btnUpload.setEnabled(true);
                } else {
                    btnUpload.setEnabled(false);
                }
            }
        };
        pidInput.addTextChangedListener(watcher);
        sysInput.addTextChangedListener(watcher);
        diaInput.addTextChangedListener(watcher);
        pulseInput.addTextChangedListener(watcher);
    }

    private void postResult() {
        if(isNetworkConnected()) {
            //Check Patient ID whether it's in correct form
            if(validatePID()) {
                String pid = pidInput.getText().toString();
                String sys = sysInput.getText().toString();
                String dia = diaInput.getText().toString();
                String pulse = pulseInput.getText().toString();
                Intent intent = new Intent(ManualInputActivity.this, ManualConfirmationActivity.class);
                Bundle extras = new Bundle();
                extras.putString("EXTRA_PID", pid);
                Result result = new Result(sys, dia, pulse);
                Gson g = new Gson();
                String res = g.toJson(result);
                extras.putString("EXTRA_RESULT", res);

                intent.putExtras(extras);
                startActivity(intent);
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

    private boolean validatePID() {
        String pid = "";
        if(pidInput.getText() != null)
            pid = pidInput.getText().toString();
        return Pattern.matches("(HR)[0-9]{3}", pid);
    }
    private boolean validateVal(EditText e) {
        return e.getText().toString().trim().length() != 0;
    }

}
