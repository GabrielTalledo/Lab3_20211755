package com.example.pomodoropucp;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.example.pomodoropucp.Workers.Contador;

import java.util.List;
import java.util.UUID;

public class ContadorViewModel extends ViewModel {

    private final LiveData<List<WorkInfo>> workInfoLiveData;
    int tiempoEstudio = 25;
    int tiempoDescanso = 10;

    public ContadorViewModel(@NonNull Application application) {
        // ESTUDIO:
        Data dataEstudio = new Data.Builder()
                .putInt("TiempoSegundos", tiempoEstudio)
                .build();
        OneTimeWorkRequest estudioRequest = new OneTimeWorkRequest.Builder(Contador.class)
                .setInputData(dataEstudio)
                .addTag("estudioRequest")
                .addTag("myChain")
                .build();

        // DESCANSO:
        Data dataDescanso = new Data.Builder()
                .putInt("TiempoSegundos", tiempoDescanso)
                .build();
        OneTimeWorkRequest descansoRequest = new OneTimeWorkRequest.Builder(Contador.class)
                .setInputData(dataDescanso)
                .addTag("descansoRequest")
                .addTag("myChain")
                .build();

        // Comenzamos la cadena:
        WorkContinuation chain = WorkManager.getInstance(application)
                .beginWith(estudioRequest)
                .then(descansoRequest);


        // Observe the chained work
        workInfoLiveData = chain.getWorkInfosLiveData();

        // Enqueue the work
        chain.enqueue();
    }

    public LiveData<List<WorkInfo>> getWorkInfoLiveData() {
        return workInfoLiveData;
    }
}
