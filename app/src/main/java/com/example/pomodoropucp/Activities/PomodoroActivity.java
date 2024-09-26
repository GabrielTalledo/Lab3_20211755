package com.example.pomodoropucp.Activities;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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

    // ------------
    //  Variables:
    // ------------

    // Tiempos, en segundos, a considerar para los periodos de estudio y descanso:
    int tiempoEstudio = 3; // Modifique para probar :D
    int tiempoDescanso = 4;

    boolean enPausa = false;
    boolean enDescanso = false;
    boolean enCiclo = false;
    boolean finished = false;
    int cuentaActual = 0;
    int numCiclos = 0;
    List<Tarea> listaTareas;
    MaterialButton buttonCiclo;
    String textContadorDescanso;
    Usuario usuario;
    UUID uuid;
    String canal1 = "importanteDefault";


    // Launcher:
    ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent resultIntent = result.getData();
                    if(resultIntent.getStringExtra("mensaje") != null){
                        Snackbar.make(binding.getRoot(), resultIntent.getStringExtra("mensaje"), Snackbar.LENGTH_LONG).show();
                    }

                    if(resultIntent.getStringExtra("TiempoEstudio") != null){
                        tiempoEstudio = Integer.parseInt(resultIntent.getStringExtra("TiempoEstudio"))*60;
                        actualizarContadorVista(tiempoEstudio,0,true);
                    }

                    if(resultIntent.getStringExtra("TiempoDescanso") != null){
                        tiempoDescanso = Integer.parseInt(resultIntent.getStringExtra("TiempoDescanso"))*60;
                        binding.textContadorDescanso.setText("Descanso: "+actualizarContadorVista(tiempoDescanso,0,false));
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

        // Creación del canal de notificaciones:
        crearCanalesNotificacion();

        // Botón
        buttonCiclo = (MaterialButton) binding.buttonCiclo;
        buttonCiclo.setVisibility(View.VISIBLE);
        binding.textContadorDescanso.setText("Descanso: "+actualizarContadorVista(tiempoDescanso,0,false));

        // Restaurar en caso se rote la pantalla:
        // Nota: En el manifiesto, si retira la opción "android:configChanges="orientation|screenSize"" de la actividad
        //       igual se mantiene la información con la siguiente lógica:
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
        binding.textCiclo.setText("Aún no empezó un ciclo!");

        // Botón x2:
        if(enCiclo){
            buttonCiclo.setIcon(getDrawable(R.drawable.pause_24dp));
        }else{
            buttonCiclo.setIcon(getDrawable(R.drawable.play_arrow_24dp));
        }

        // Animación/Transición:
        YoYo.with(Techniques.ZoomIn).duration(3000).playOn(binding.cardUsuario);
        YoYo.with(Techniques.SlideInUp).duration(2500).playOn(binding.textContadorDescanso);
        YoYo.with(Techniques.SlideInUp).duration(3200).playOn(binding.textContador);
        YoYo.with(Techniques.SlideInUp).duration(4000).playOn(binding.buttonCiclo);
        YoYo.with(Techniques.SlideInUp).duration(4500).playOn(binding.buttonReiniciar);

        // -----------
        // - Lógica: -
        // -----------

        // Control del ciclo:

        binding.textContador.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(cuentaActual == 0){
                    Intent intent = new Intent(PomodoroActivity.this, ConfigActivity.class);
                    intent.putExtra("TiempoEstudio",""+tiempoEstudio);
                    intent.putExtra("Nombre",usuario.getFirstName());
                    launcher.launch(intent);
                }
                return false;
            }
        });

        binding.textContadorDescanso.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(cuentaActual == 0){
                    Intent intent = new Intent(PomodoroActivity.this, ConfigActivity.class);
                    intent.putExtra("TiempoDescanso",""+tiempoDescanso);
                    intent.putExtra("Nombre",usuario.getFirstName());
                    launcher.launch(intent);
                }
                return false;
            }
        });


        binding.buttonReiniciar.setOnClickListener(view -> {
            if(numCiclos != 0){
                WorkManager.getInstance(binding.getRoot().getContext()).cancelAllWork();
                buttonCiclo.setVisibility(View.VISIBLE);
                cuentaActual = 0;

                if(!finished && enCiclo){
                    numCiclos -=1;
                }

                enCiclo = false;
                enDescanso = false;
                enPausa = false;
                buttonCiclo.setIcon(getDrawable(R.drawable.play_arrow_24dp));
                binding.textContadorDescanso.setText("Descanso: "+actualizarContadorVista(tiempoDescanso,0,false));
                actualizarContadorVista(tiempoEstudio,0,true);
                if(finished){
                    iniciarCuenta(tiempoEstudio,0);
                    Snackbar.make(binding.getRoot(), "Inició un nuevo ciclo Pomodoro!", Snackbar.LENGTH_LONG).show();
                }else{
                    Snackbar.make(binding.getRoot(), "Reinició el ciclo Pomodoro!", Snackbar.LENGTH_LONG).show();
                }
            }


        });

        buttonCiclo.setOnClickListener(view -> {
            if(enPausa){
                buttonCiclo.setIcon(getDrawable(R.drawable.pause_24dp));
                enPausa = false;
                iniciarCuenta(enDescanso?tiempoDescanso:tiempoEstudio,cuentaActual);
                setearObservador();
            }else{

                if(cuentaActual == 0){

                    // Comenzamos el ciclo
                    finished = false;
                    enDescanso = false;
                    numCiclos += 1;
                    binding.textCiclo.setText("Ciclo N°"+numCiclos);
                    Log.d("pipi", "enCiclo: "+enCiclo);
                    Log.d("pipi", "enDescanso: "+enDescanso);
                    buttonCiclo.setIcon(getDrawable(R.drawable.pause_24dp));
                    binding.textContadorDescanso.setText("Descanso: "+actualizarContadorVista(tiempoDescanso,0,false));
                    if(!enCiclo){
                        enCiclo = true;
                        Snackbar.make(binding.getRoot(), "Comenzó el ciclo Pomodoro!", Snackbar.LENGTH_LONG).show();
                    }else{
                        // Reiniciamos el ciclo
                        detenerCuenta(uuid);
                        Snackbar.make(binding.getRoot(), "Reinició el ciclo Pomodoro!", Snackbar.LENGTH_LONG).show();
                    }

                    WorkManager.getInstance(binding.getRoot().getContext()).cancelWorkById(uuid);

                    // Iniciamos cuentas:
                    iniciarCuenta(tiempoEstudio,0);

                    // Configuramos un observador:
                    setearObservador();


                }else{
                    if(enCiclo){
                        // Pausamos el reloj:
                        WorkManager.getInstance(binding.getRoot().getContext()).cancelWorkById(uuid);
                        buttonCiclo.setIcon(getDrawable(R.drawable.play_arrow_24dp));
                        enPausa = true;
                    }

                }


            }

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
                    Data progreso = workInfo.getProgress();
                    cuentaActual = progreso.getInt("CuentaActual",0);
                    actualizarContadorVista(enDescanso?tiempoDescanso:tiempoEstudio,cuentaActual,true);
                } else if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    notificarImportanceEstudio();
                    actualizarContadorVista(enDescanso?tiempoDescanso:tiempoEstudio,workInfo.getOutputData().getInt("CuentaActual",0),true);
                    if(enDescanso && enCiclo){
                        lanzarConfetti(0xFF8E4953);
                        lanzarDialog("¡Atención!","Terminó el tiempo de descanso. Dale al botón de reinicio para empezar otro ciclo.");
                        buttonCiclo.setIcon(getDrawable(R.drawable.refresh_24dp));
                        binding.textContadorDescanso.setText("Fin de ciclo");
                        enDescanso = false;
                        enCiclo = false;
                        finished = true;
                        cuentaActual = 0;
                        buttonCiclo.setVisibility(View.INVISIBLE);
                        notificarImportanceDescanso();
                        //numCiclos += 1;
                    }else{
                        tareasUsuario();
                        binding.textContadorDescanso.setText("En descanso");
                        enDescanso = true;
                        iniciarCuenta(tiempoDescanso,0);
                        WorkManager.getInstance(this).getWorkInfoByIdLiveData(uuid).observe(this, workInfo2 -> {
                            if(workInfo2 != null) {
                                if(workInfo2.getState() == WorkInfo.State.RUNNING){
                                    Data progreso = workInfo2.getProgress();
                                    cuentaActual = progreso.getInt("CuentaActual",0);
                                    actualizarContadorVista(tiempoDescanso,cuentaActual,true);
                                } else if (workInfo2.getState() == WorkInfo.State.SUCCEEDED) {
                                    actualizarContadorVista(tiempoEstudio,workInfo.getOutputData().getInt("CuentaActual",0),true);
                                    lanzarConfetti(0xFF8E4953);
                                    lanzarDialog("¡Atención!","Terminó el tiempo de descanso. Dale al botón de reinicio para empezar otro ciclo.");
                                    buttonCiclo.setIcon(getDrawable(R.drawable.refresh_24dp));
                                    binding.textContadorDescanso.setText("Finalizó el ciclo");
                                    enDescanso = false;
                                    enCiclo = false;
                                    finished = true;
                                    cuentaActual = 0;
                                    buttonCiclo.setVisibility(View.INVISIBLE);
                                    notificarImportanceDescanso();
                                    //numCiclos += 1;
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

    public void iniciarCuenta(int tiempoSegundos,int cuentaActual){
        uuid = UUID.randomUUID();
        Data data = new Data.Builder()
                .putInt("TiempoSegundos", tiempoSegundos)
                .putInt("CuentaActualContador",cuentaActual)
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

    // Notificaciones:

    public void crearCanalesNotificacion() {

        NotificationChannel channel = new NotificationChannel(canal1,
                "Canal notificaciones default",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Canal para notificaciones con prioridad default");
        channel.enableVibration(true);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        pedirPermisos();
    }

    public void pedirPermisos() {
        // TIRAMISU = 33
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {

            ActivityCompat.requestPermissions(PomodoroActivity.this, new String[]{POST_NOTIFICATIONS}, 101);
        }
    }

    public void notificarImportanceEstudio(){

        //Crear notificación

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canal1)
                .setSmallIcon(R.drawable.logo_pomodoro)
                .setContentTitle("Tiempo de trabajo terminado!")
                .setContentText("El tiempo de descanso ya comenzó :D")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true);

        Notification notification = builder.build();

        //Lanzar notificación
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, notification);
        }

    }

    public void notificarImportanceDescanso(){

        //Crear notificación

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canal1)
                .setSmallIcon(R.drawable.logo_pomodoro)
                .setContentTitle("Tiempo de descanso y ciclo finalizado!")
                .setContentText("Si desea continuar, reinicie el ciclo!")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true);

        Notification notification = builder.build();

        //Lanzar notificación
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, notification);
        }

    }
}