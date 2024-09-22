package com.example.pomodoropucp.Activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.WorkManager;

import com.example.pomodoropucp.DTOs.Tarea;
import com.example.pomodoropucp.DTOs.TareaTodo;
import com.example.pomodoropucp.DTOs.Usuario;
import com.example.pomodoropucp.R;
import com.example.pomodoropucp.Services.DummyService;
import com.example.pomodoropucp.databinding.ActivityTareasBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TareasActivity extends AppCompatActivity {


    //Binding:
    ActivityTareasBinding binding;

    //Variables:
    Bundle data;
    String mensaje;
    List<Tarea> listaTareas;
    Tarea tareaEditada;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTareasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Back button c:


        // Controlar el intent de la actividad Pomodoro:
        Intent intentTimer= getIntent();
        TareaTodo tareaTodo = (TareaTodo) intentTimer.getSerializableExtra("tareaTodo");
        Usuario usuario = (Usuario) intentTimer.getSerializableExtra("usuario");
        listaTareas = tareaTodo.getTodos();
        binding.textNombre.setText(usuario.getFullName());
        binding.imageGender.setImageDrawable(getDrawable(usuario.isMale() ? R.drawable.man_24dp : R.drawable.woman_24dp));

        // Spinner:
        ArrayList<String> listaTareasMod = new ArrayList<>();
        for(Tarea tarea : listaTareas){
            listaTareasMod.add(tarea.getTodo() + " - " + (tarea.isCompleted()?"Completado":"No completado"));
        }
        adapter = new ArrayAdapter<>(this, R.layout.spinner_layout, listaTareasMod);
        Spinner spinner = binding.spinnerTareas;
        spinner.setAdapter(adapter);

        binding.buttonCambiarEstado.setOnClickListener(view -> {
            tareaEditada = listaTareas.get(binding.spinnerTareas.getSelectedItemPosition());
            tareaEditada.setCompleted(!tareaEditada.isCompleted());

            DummyService dummyService = new Retrofit.Builder()
                    .baseUrl("https://dummyjson.com")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(DummyService.class);
            dummyService.updateTarea(tareaEditada.getId(), !tareaEditada.isCompleted()).enqueue(new Callback<Tarea>() {
                @Override
                public void onResponse(Call<Tarea> call, Response<Tarea> response) {
                    if(response.isSuccessful()){
                        int posicion = binding.spinnerTareas.getSelectedItemPosition();
                        Tarea tarea = response.body();
                        mensaje = "Se cambió exitosamente el estado de la tarea a " + (tareaEditada.isCompleted()?"'Completada'.":"'No completada'.");
                        // Actualizamos el Spinner:
                        actualizarSpinner();
                    }else{
                        mensaje = "No se puedo cambiar de estado a la tarea :(";
                    }

                    Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
                    // No leí bien el enunciado :S
                    //Intent resultIntent = new Intent();
                    //resultIntent.putExtra("mensaje",mensaje);
                    //setResult(RESULT_OK, resultIntent);
                    //supportFinishAfterTransition();
                }

                @Override
                public void onFailure(Call<Tarea> call, Throwable throwable) {

                }
            });


        });
    }

    // Funciones:

    public void actualizarSpinner(){
        ArrayList<String> listaTareasMod = new ArrayList<>();
        for(Tarea tarea : listaTareas){
            listaTareasMod.add(tarea.getTodo() + " - " + (tarea.isCompleted()?"Completado":"No completado"));
        }
        adapter.clear();
        adapter.addAll(listaTareasMod);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pomodoro, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.buttonLogOut) {
            WorkManager.getInstance(binding.getRoot().getContext()).cancelAllWork();
            Intent intent = new Intent(TareasActivity.this, LoginActivity.class);
            startActivity(intent);
            supportFinishAfterTransition();
            return true;
        }

        if(item.getItemId() == android.R.id.home){
            // GoodBye xD:
            Intent resultIntent = new Intent();
            setResult(RESULT_OK, resultIntent);
            supportFinishAfterTransition();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}