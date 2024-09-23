package com.example.pomodoropucp.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.WorkManager;

import com.example.pomodoropucp.R;
import com.example.pomodoropucp.databinding.ActivityConfigBinding;

public class ConfigActivity extends AppCompatActivity {

    ActivityConfigBinding binding;
    boolean isEstudio = true;
    int tiempo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        Intent intent = getIntent();
        binding.textNombreuwu.setText(""+intent.getStringExtra("Nombre")+"!");

        if(intent.getStringExtra("TiempoEstudio") != null){
            tiempo = Integer.parseInt(intent.getStringExtra("TiempoEstudio"));
            binding.textTiempouwu.setText(binding.textTiempouwu.getText().toString()+" ESTUDIO.");
        }else{
            tiempo = Integer.parseInt(intent.getStringExtra("TiempoDescanso"));
            binding.textTiempouwu.setText(binding.textTiempouwu.getText().toString()+" DESCANSO.");
            isEstudio = false;
        }

        binding.fieldTiempo.setText(""+tiempo/60);

        binding.buttonConfigTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(binding.fieldTiempo.getText().toString().equals("0")){
                    binding.fieldTiempoLayout.setError("El tiempo no puede ser 0 minutos!");
                }else{
                    if(binding.fieldTiempo.getText().toString().isEmpty()){
                        binding.fieldTiempoLayout.setError("Este campo no puede estar vacío!");
                    }else{
                        Intent resultIntent = new Intent();
                        if(isEstudio){
                            resultIntent.putExtra("TiempoEstudio",binding.fieldTiempo.getText().toString());
                        }else{
                            resultIntent.putExtra("TiempoDescanso",binding.fieldTiempo.getText().toString());
                        }
                        setResult(RESULT_OK,resultIntent);
                        supportFinishAfterTransition();
                    }


                }
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

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