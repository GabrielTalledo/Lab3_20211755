package com.example.pomodoropucp.Services;

import com.example.pomodoropucp.DTOs.Usuario;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface DummyService {

    @FormUrlEncoded
    @POST("/auth/login")
    Call<Usuario> login(@Field("username") String username, @Field("password") String password);
}
