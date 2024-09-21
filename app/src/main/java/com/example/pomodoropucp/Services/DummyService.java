package com.example.pomodoropucp.Services;

import com.example.pomodoropucp.DTOs.Tarea;
import com.example.pomodoropucp.DTOs.TareaTodo;
import com.example.pomodoropucp.DTOs.Usuario;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface DummyService {

    @FormUrlEncoded
    @POST("/auth/login")
    Call<Usuario> login(@Field("username") String username, @Field("password") String password);

    @GET("/todos/user/{userId}")
    Call<TareaTodo> getTodosByUserId(@Path("userId") int userId);

    @FormUrlEncoded
    @PUT("/todos/{todoId}")
    Call<Tarea> updateTarea(@Path("todoId") int todoId, @Field("completed") boolean completed);
}
