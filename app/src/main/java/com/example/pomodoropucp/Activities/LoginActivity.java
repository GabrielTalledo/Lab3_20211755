package com.example.pomodoropucp.Activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkManager;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.example.pomodoropucp.R;
import com.example.pomodoropucp.Services.DummyService;
import com.example.pomodoropucp.DTOs.Usuario;
import com.example.pomodoropucp.databinding.ActivityLoginBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class LoginActivity extends AppCompatActivity {

    // Binding:
    ActivityLoginBinding binding;

    // Variables:
    TextInputEditText usuario;
    TextInputLayout usuarioLayout;
    TextInputEditText password;
    TextInputLayout passwordLayout;
    Button iniciarSesion;
    boolean validacion = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());

        // Animación/Transición:
        YoYo.with(Techniques.ZoomInUp).duration(2000).playOn(binding.logoPomodoro);

        // Seteo de la vista:
        setContentView(binding.getRoot());

        // Obtenemos los campos y botón:
        usuario = binding.fieldUsuario;
        usuarioLayout = binding.fieldUsuarioLayout;
        password = binding.fieldPassword;
        passwordLayout = binding.fieldPasswordLayout;
        iniciarSesion = binding.buttonIniciarSesion;

        // Línea de código que probablemente no sea de mucha utilidad (algunos workers se ejecutan en background a pesar de volver a correr el app en Android Studio):
        WorkManager.getInstance(binding.getRoot().getContext()).cancelAllWork();

        // Lógica:
        iniciarSesion.setOnClickListener(view -> {
            validacion = revisarCampos();
            if(validacion){
                usuarioLayout.setError(null);
                passwordLayout.setError(null);
                iniciarSesion();
            }
        });
    }

    // Funciones:

    public void iniciarSesion(){

        DummyService dummyService = new Retrofit.Builder()
                .baseUrl("https://dummyjson.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DummyService.class);
        dummyService.login(usuario.getText().toString(),password.getText().toString()).enqueue(new Callback<Usuario>() {
            Usuario user = null;
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                if(response.isSuccessful()){
                    user = response.body();
                    // Vamos a la activity del reloj POMODORO:
                    Intent intent = new Intent(LoginActivity.this, PomodoroActivity.class);
                    intent.putExtra("Usuario", user);
                    startActivity(intent);
                }else{
                    Snackbar.make(binding.getRoot(), "Credenciales incorrectas!", Snackbar.LENGTH_LONG)
                            .setAction("Limpiar", v -> {
                                usuario.setText("");
                                password.setText("");
                                usuarioLayout.setError(null);
                                passwordLayout.setError(null);
                            })
                            .show();
                }
            }
            @Override
            public void onFailure(Call<Usuario> call, Throwable throwable) {
                throwable.printStackTrace();
            }
        });

    }

    public boolean revisarCampos(){
        boolean validacion = true;
        if(usuario.getText().toString().isEmpty()){
            usuarioLayout.setError("El usuario no puede estar vacío");
            validacion = false;
        }
        if(password.getText().toString().isEmpty()){
            passwordLayout.setError("La contraseña no puede estar vacía");
            validacion = false;
        }
        return validacion ;
    }
}