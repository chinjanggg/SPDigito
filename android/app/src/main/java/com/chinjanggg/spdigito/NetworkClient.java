package com.chinjanggg.spdigito;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkClient {

    private static final String baseURL = "http://192.168.1.106:3000/";
    private static final int READ_TIMEOUT = 60000; //OCR Timeout 60s

    public static Retrofit getRetrofitClient() {
        OkHttpClient client = new OkHttpClient.Builder().readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS).build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseURL).client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit;
    }
}
