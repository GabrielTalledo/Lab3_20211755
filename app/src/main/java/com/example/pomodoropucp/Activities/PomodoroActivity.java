package com.example.pomodoropucp.Activities;

import android.os.Bundle;
import android.transition.Explode;
import android.transition.Slide;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.example.pomodoropucp.R;
import com.example.pomodoropucp.databinding.ActivityLoginBinding;
import com.example.pomodoropucp.databinding.ActivityPomodoroBinding;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class PomodoroActivity extends AppCompatActivity {

    // Binding:
    ActivityPomodoroBinding binding;

    // Variables:


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPomodoroBinding.inflate(getLayoutInflater());

        // Animación/Transición:
        getWindow().setAllowEnterTransitionOverlap(false);
        getWindow().setAllowReturnTransitionOverlap(false);
        getWindow().setEnterTransition(new Explode().setDuration(1500));
        getWindow().setExitTransition(new Slide(Gravity.END).setDuration(700));

        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);



    }


    // Funciones:

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pomodoro, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.buttonLogOut) {
            supportFinishAfterTransition();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}