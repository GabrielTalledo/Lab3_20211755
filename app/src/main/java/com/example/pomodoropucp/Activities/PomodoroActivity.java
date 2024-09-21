package com.example.pomodoropucp.Activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.transition.Explode;
import android.transition.Slide;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.example.pomodoropucp.DTOs.Usuario;
import com.example.pomodoropucp.R;
import com.example.pomodoropucp.Workers.Contador;
import com.example.pomodoropucp.databinding.ActivityLoginBinding;
import com.example.pomodoropucp.databinding.ActivityPomodoroBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.dialog.MaterialDialogs;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.models.Shape;
import nl.dionsegijn.konfetti.core.models.Size;
import nl.dionsegijn.konfetti.xml.KonfettiView;

public class PomodoroActivity extends AppCompatActivity {

    // Binding:
    ActivityPomodoroBinding binding;

    // Variables:
    int tiempoEstudio = 10; // Modifique para probar :D
    int tiempoDescanso = 9;
    boolean enDescanso = false;
    boolean enCiclo = false;
    MaterialButton buttonCiclo;
    String textContadorDescanso;
    Usuario usuario;
    UUID uuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPomodoroBinding.inflate(getLayoutInflater());


        // Seteo de la vista:
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        actualizarContadorVista(tiempoEstudio,0);
        WorkManager.getInstance(this).cancelAllWork();

        // Usuario:

        usuario = (Usuario) getIntent().getSerializableExtra("Usuario");
        if(usuario != null){
            setearCard(usuario);
            // NOTA: Use Snackbars en vez de Toast para usar mas elementos propios de Material!
            Snackbar.make(binding.getRoot(), "Inicio de sesión exitoso!", Snackbar.LENGTH_LONG).show();
        }



        // Animación/Transición:
        YoYo.with(Techniques.ZoomIn).duration(3000).playOn(binding.cardUsuario);
        YoYo.with(Techniques.SlideInUp).duration(2500).playOn(binding.textContadorDescanso);
        YoYo.with(Techniques.SlideInUp).duration(3200).playOn(binding.textContador);
        YoYo.with(Techniques.SlideInUp).duration(4000).playOn(binding.buttonCiclo);


        // Restaurar en caso se rote la pantalla:





        // Lógica:

        // Botón
        buttonCiclo = (MaterialButton) binding.buttonCiclo;

        if(enCiclo){
            buttonCiclo.setIcon(getDrawable(R.drawable.refresh_24dp));
        }else{
            buttonCiclo.setIcon(getDrawable(R.drawable.play_arrow_24dp));
        }

        // Control del ciclo:
        buttonCiclo.setOnClickListener(view -> {
            buttonCiclo.setIcon(getDrawable(R.drawable.refresh_24dp));
            binding.textContadorDescanso.setText("Descanso: 05:00");
            if(!enCiclo){
                // Comenzamos el ciclo
                enCiclo = true;
                Snackbar.make(binding.getRoot(), "Comenzó el ciclo Pomodoro!", Snackbar.LENGTH_LONG).show();
            }else{
                // Reiniciamos el ciclo
                detenerCuenta(uuid);
                Snackbar.make(binding.getRoot(), "Reinició el ciclo Pomodoro!", Snackbar.LENGTH_LONG).show();
            }

            // 25 minutos:
            iniciarCuenta(tiempoEstudio);
            // Configuramos un observador:
            WorkManager.getInstance(this).getWorkInfoByIdLiveData(uuid).observe(this, workInfo -> {
                if(workInfo != null) {
                    if(workInfo.getState() == WorkInfo.State.RUNNING){
                        Data progreso = workInfo.getProgress();
                        int cuentaActual = progreso.getInt("CuentaActual",0);
                        actualizarContadorVista(tiempoEstudio,cuentaActual);
                    } else if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {

                        // 5 minutos:
                        actualizarContadorVista(tiempoEstudio,workInfo.getOutputData().getInt("CuentaActual",0));
                        binding.textContadorDescanso.setText("En descanso");
                        enDescanso = true;
                        lanzarConfetti(0xFFAED581);
                        iniciarCuenta(tiempoDescanso);

                        WorkManager.getInstance(this).getWorkInfoByIdLiveData(uuid).observe(this, workInfo2 -> {
                            if(workInfo2 != null) {
                                if(workInfo2.getState() == WorkInfo.State.RUNNING){
                                    Data progreso = workInfo2.getProgress();
                                    int cuentaActual = progreso.getInt("CuentaActual",0);
                                    actualizarContadorVista(tiempoDescanso,cuentaActual);
                                } else if (workInfo2.getState() == WorkInfo.State.SUCCEEDED) {
                                    actualizarContadorVista(tiempoEstudio,workInfo.getOutputData().getInt("CuentaActual",0));
                                    lanzarConfetti(0xFF8E4953);
                                    lanzarDialog("¡Atención!","Terminó el tiempo de descanso. Dale al botón de reinicio para empezar otro ciclo.");
                                    buttonCiclo.setIcon(getDrawable(R.drawable.play_arrow_24dp));
                                    binding.textContadorDescanso.setText("Fin de ciclo");
                                    enCiclo = false;
                                }
                            }
                        });

                        // Se comprueba si tiene tareas:
                        lanzarDialog("¡Felicidades!","Empezó el tiempo de descanso!");

                    }
                }
            });
        });

    }

    // Funciones:

    public void setearCard(Usuario usuario){
        binding.textNombreCompleto.setText(usuario.getFullName());
        binding.textCorreo.setText(usuario.getEmail());
        binding.imageGender.setImageDrawable(usuario.isMale()?getDrawable(R.drawable.man_24dp):getDrawable(R.drawable.woman_24dp));
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
        Data data = new Data.Builder()
                .putInt("TiempoSegundos", tiempoSegundos)
                .build();
        WorkRequest workRequest = new OneTimeWorkRequest.Builder(Contador.class)
                .setId(uuid)
                .setInputData(data)
                .build();
        WorkManager.getInstance(this).enqueue(workRequest);
    }

    public void actualizarContadorVista(int tiempoTotal,int cuentaActual){
        int tiempoActual = tiempoTotal-cuentaActual;
        int tiempoMinutos = (int) (tiempoActual/60);
        int tiempoSegundos = tiempoActual%60;
        String tiempoMinutosStr = tiempoMinutos<10?"0"+tiempoMinutos:tiempoMinutos+"";
        String tiempoSegundosStr = tiempoSegundos<10?"0"+tiempoSegundos:tiempoSegundos+"";

        binding.textContador.setText(tiempoMinutosStr+":"+tiempoSegundosStr);
    }

    public void detenerCuenta(UUID uuid){
        WorkManager.getInstance(this).cancelWorkById(uuid);
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
            Intent intent = new Intent(PomodoroActivity.this, LoginActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}