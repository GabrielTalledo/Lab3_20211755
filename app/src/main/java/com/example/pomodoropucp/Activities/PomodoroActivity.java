package com.example.pomodoropucp.Activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.example.pomodoropucp.DTOs.Tarea;
import com.example.pomodoropucp.DTOs.TareaTodo;
import com.example.pomodoropucp.DTOs.Usuario;
import com.example.pomodoropucp.R;
import com.example.pomodoropucp.Services.DummyService;
import com.example.pomodoropucp.Workers.Contador;
import com.example.pomodoropucp.databinding.ActivityPomodoroBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.models.Shape;
import nl.dionsegijn.konfetti.core.models.Size;
import nl.dionsegijn.konfetti.xml.KonfettiView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PomodoroActivity extends AppCompatActivity {

    // Binding:
    ActivityPomodoroBinding binding;

    // Variables:
    int tiempoEstudio = 2; // Modifique para probar :D
    int tiempoDescanso = 160;
    boolean enDescanso = false;
    boolean enCiclo = false;
    boolean finished = false;
    List<Tarea> listaTareas;
    MaterialButton buttonCiclo;
    String textContadorDescanso;
    Usuario usuario;
    UUID uuid;


    // Launcher:

    ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent resultIntent = result.getData();
                    if(resultIntent.getStringExtra("mensaje") != null){
                        Snackbar.make(binding.getRoot(), resultIntent.getStringExtra("mensaje"), Snackbar.LENGTH_LONG).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPomodoroBinding.inflate(getLayoutInflater());

        // Seteo de la vista:
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        // Botón
        buttonCiclo = (MaterialButton) binding.buttonCiclo;
        binding.textContadorDescanso.setText("Descanso: "+actualizarContadorVista(tiempoDescanso,0,false));

        // Restaurar en caso se rote la pantalla:

        if(savedInstanceState != null){
            textContadorDescanso = savedInstanceState.getString("textContador");
            binding.textContadorDescanso.setText(textContadorDescanso);
            enDescanso = savedInstanceState.getBoolean("enDescanso");
            enCiclo = savedInstanceState.getBoolean("enCiclo");
            usuario =  (Usuario) savedInstanceState.getSerializable("usuario");
            uuid = (UUID) savedInstanceState.getSerializable("uuid");
            finished = savedInstanceState.getBoolean("finished");

            if(enCiclo && !finished){
                setearObservador();
            }else if(finished) {
                actualizarContadorVista(0, 0,true);
            }else{
                actualizarContadorVista(tiempoEstudio, 0,true);
            }

        }else{
            actualizarContadorVista(tiempoEstudio,0,true);
            WorkManager.getInstance(binding.getRoot().getContext()).cancelAllWork();
        }

        // Usuario y manejo del intent del Login:
        if(getIntent() != null && savedInstanceState == null){
            WorkManager.getInstance(binding.getRoot().getContext()).cancelAllWork();
            usuario = (Usuario) getIntent().getSerializableExtra("Usuario");
            if(usuario != null){
                // NOTA: Use Snackbars en vez de Toast para usar mas elementos propios de Material!
                Snackbar.make(binding.getRoot(), "Inicio de sesión exitoso!", Snackbar.LENGTH_LONG).show();
            }
        }
        setearCard(usuario);

        // Botón x2:
        if(enCiclo){
            buttonCiclo.setIcon(getDrawable(R.drawable.refresh_24dp));
        }else{
            buttonCiclo.setIcon(getDrawable(R.drawable.play_arrow_24dp));
        }

        // Animación/Transición:
        YoYo.with(Techniques.ZoomIn).duration(3000).playOn(binding.cardUsuario);
        YoYo.with(Techniques.SlideInUp).duration(2500).playOn(binding.textContadorDescanso);
        YoYo.with(Techniques.SlideInUp).duration(3200).playOn(binding.textContador);
        YoYo.with(Techniques.SlideInUp).duration(4000).playOn(binding.buttonCiclo);

        // -----------
        // - Lógica: -
        // -----------

        // Control del ciclo:
        buttonCiclo.setOnClickListener(view -> {
            finished = false;
            enDescanso = false;
            Log.d("pipi", "enCiclo: "+enCiclo);
            Log.d("pipi", "enDescanso: "+enDescanso);
            buttonCiclo.setIcon(getDrawable(R.drawable.refresh_24dp));
            binding.textContadorDescanso.setText("Descanso: "+actualizarContadorVista(tiempoDescanso,0,false));
            if(!enCiclo){
                // Comenzamos el ciclo
                enCiclo = true;
                Snackbar.make(binding.getRoot(), "Comenzó el ciclo Pomodoro!", Snackbar.LENGTH_LONG).show();
            }else{
                // Reiniciamos el ciclo
                detenerCuenta(uuid);
                Snackbar.make(binding.getRoot(), "Reinició el ciclo Pomodoro!", Snackbar.LENGTH_LONG).show();
            }

            WorkManager.getInstance(binding.getRoot().getContext()).cancelWorkById(uuid);

            // Iniciamos cuentas:
            iniciarCuenta(tiempoEstudio);

            // Configuramos un observador:
            setearObservador();
        });

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        textContadorDescanso = binding.textContadorDescanso.getText().toString();
        outState.putString("textContador",textContadorDescanso);
        outState.putBoolean("enDescanso",enDescanso);
        outState.putBoolean("enCiclo",enCiclo);
        outState.putSerializable("usuario",usuario);
        outState.putSerializable("uuid",uuid);
        outState.putBoolean("finished",finished);
    }

    // Funciones:

    public void tareasUsuario(){
        DummyService dummyService = new Retrofit.Builder()
                .baseUrl("https://dummyjson.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DummyService.class);
        dummyService.getTodosByUserId(usuario.getId()).enqueue(new Callback<TareaTodo>() {
            @Override
            public void onResponse(Call<TareaTodo> call, Response<TareaTodo> response) {
                if(response.isSuccessful()){
                    TareaTodo tareaTodo = response.body();
                    listaTareas = tareaTodo.getTodos();

                    if(!listaTareas.isEmpty()){
                        // Vamos a la activity del reloj POMODORO:
                        Intent intent = new Intent(PomodoroActivity.this, TareasActivity.class);
                        intent.putExtra("tareaTodo",tareaTodo);
                        intent.putExtra("usuario",usuario);
                        launcher.launch(intent);
                    }else{
                        lanzarDialog("¡Felicidades!","Empezó el tiempo de descanso!");
                        lanzarConfetti(0xFFAED581);
                    }

                }else{
                    lanzarDialog("¡Felicidades!","Empezó el tiempo de descanso!");
                    lanzarConfetti(0xFFAED581);
                }
            }

            @Override
            public void onFailure(Call<TareaTodo> call, Throwable throwable) {

            }
        });
    }

    public void setearObservador(){
        WorkManager.getInstance(binding.getRoot().getContext()).getWorkInfoByIdLiveData(uuid).observe(PomodoroActivity.this, workInfo -> {
            if(workInfo != null) {
                if(workInfo.getState() == WorkInfo.State.RUNNING){
                    Log.d("aaaa", "primerWORKINFO UWU ");
                    Data progreso = workInfo.getProgress();
                    int cuentaActual = progreso.getInt("CuentaActual",0);
                    actualizarContadorVista(enDescanso?tiempoDescanso:tiempoEstudio,cuentaActual,true);
                } else if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    Log.d("aaaa", "primerWORKINFO acabado ");
                    actualizarContadorVista(enDescanso?tiempoDescanso:tiempoEstudio,workInfo.getOutputData().getInt("CuentaActual",0),true);
                    if(enDescanso && enCiclo){
                        Log.d("aaaa", "pendescandito ");
                        lanzarConfetti(0xFF8E4953);
                        lanzarDialog("¡Atención!","Terminó el tiempo de descanso. Dale al botón de inicio para empezar otro ciclo.");
                        buttonCiclo.setIcon(getDrawable(R.drawable.play_arrow_24dp));
                        binding.textContadorDescanso.setText("Fin de ciclo");
                        enDescanso = false;
                        enCiclo = false;
                        finished = true;
                    }else{

                        //MediaPlayer mediaPlayer = MediaPlayer.create(binding.getRoot().getContext(),R.raw.jazz_riff);
                        //try {
                        //    mediaPlayer.prepare();
                        //    mediaPlayer.start();
                        //} catch (IOException e) {
                        //    e.printStackTrace();
                        //}

                        tareasUsuario();

                        binding.textContadorDescanso.setText("En descanso");
                        enDescanso = true;


                        iniciarCuenta(tiempoDescanso);
                        WorkManager.getInstance(this).getWorkInfoByIdLiveData(uuid).observe(this, workInfo2 -> {
                            if(workInfo2 != null) {
                                if(workInfo2.getState() == WorkInfo.State.RUNNING){
                                    Log.d("aaaa", "segundoWORKINFO acabado UWU");
                                    Data progreso = workInfo2.getProgress();
                                    int cuentaActual = progreso.getInt("CuentaActual",0);
                                    actualizarContadorVista(tiempoDescanso,cuentaActual,true);
                                } else if (workInfo2.getState() == WorkInfo.State.SUCCEEDED) {
                                    Log.d("aaaa", "segundo WORKINFO acabado ");
                                    actualizarContadorVista(tiempoEstudio,workInfo.getOutputData().getInt("CuentaActual",0),true);
                                    lanzarConfetti(0xFF8E4953);
                                    lanzarDialog("¡Atención!","Terminó el tiempo de descanso. Dale al botón de reinicio para empezar otro ciclo.");
                                    buttonCiclo.setIcon(getDrawable(R.drawable.play_arrow_24dp));
                                    binding.textContadorDescanso.setText("Fin de ciclo");
                                    enDescanso = false;
                                    enCiclo = false;
                                    finished = true;
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    public void setearCard(Usuario usuario){
        if(usuario!=null){
            binding.textNombreCompleto.setText(usuario.getFullName());
            binding.textCorreo.setText(usuario.getEmail());
            binding.imageGender.setImageDrawable(usuario.isMale()?getDrawable(R.drawable.man_24dp):getDrawable(R.drawable.woman_24dp));
            binding.bienvenida.setText(usuario.isMale()?"BIENVENIDO:":"BIENVENIDA:");
        }
    }


    public void lanzarDialog(String titulo, String msg){
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(titulo)
                .setMessage(msg)
                .setIcon(R.drawable.celebration_24dp)
                .setPositiveButton("Entendido", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public void lanzarConfetti(Integer color){
        List<Integer> colores = Arrays.asList(
                color
        );
        KonfettiView konfetti = binding.konfettiView;
        konfetti.start(
                new PartyFactory((new Emitter( 400, TimeUnit.MILLISECONDS).max(400)))
                        .spread(360)
                        .position(0,0.25,1,1)
                        .sizes(new Size(8, 50,10))
                        .shapes(Shape.Circle.INSTANCE, Shape.Square.INSTANCE)
                        .colors(colores)
                        .timeToLive(2000)
                        .fadeOutEnabled(true)
                        .build()
        );
    }

    public void iniciarCuenta(int tiempoSegundos){
        uuid = UUID.randomUUID();
        Log.d("pipi", "ENTRE A LA FUNCION!!!!!");
        Data data = new Data.Builder()
                .putInt("TiempoSegundos", tiempoSegundos)
                .build();
        WorkRequest workRequest = new OneTimeWorkRequest.Builder(Contador.class)
                .setId(uuid)
                .setInputData(data)
                .build();
        WorkManager.getInstance(binding.getRoot().getContext()).enqueue(workRequest);
    }

    public String actualizarContadorVista(int tiempoTotal,int cuentaActual,boolean mini){
        int tiempoActual = tiempoTotal-cuentaActual;
        int tiempoMinutos = (int) (tiempoActual/60);
        int tiempoSegundos = tiempoActual%60;
        String tiempoMinutosStr = tiempoMinutos<10?"0"+tiempoMinutos:tiempoMinutos+"";
        String tiempoSegundosStr = tiempoSegundos<10?"0"+tiempoSegundos:tiempoSegundos+"";

        String tiempoStr = tiempoMinutosStr+":"+tiempoSegundosStr;
        if(mini){
            binding.textContador.setText(tiempoStr);
        }
        return tiempoStr;
    }

    public void detenerCuenta(UUID uuid){
        WorkManager.getInstance(binding.getRoot().getContext()).cancelWorkById(uuid);
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
            Intent intent = new Intent(PomodoroActivity.this, LoginActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}