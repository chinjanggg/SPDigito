package com.chinjanggg.spdigito;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    @Multipart
    @POST("/upload")
    Call<ResponseBody> postImage(@Part("pid") RequestBody body, @Part MultipartBody.Part image, @Part("upload") RequestBody name);

    @GET("/patientdata")
    Call<ResponseBody> getPatientData(@Query("pid") String pid);

    @FormUrlEncoded
    @POST("/savedata")
    Call<ResponseBody> saveData(@Field("pid") String pid, @Field("sys") int sys, @Field("dia") int dia, @Field("pulse") int pulse);
}
