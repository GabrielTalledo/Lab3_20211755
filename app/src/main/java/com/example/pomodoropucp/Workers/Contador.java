package com.example.pomodoropucp.Workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class Contador extends Worker {

    public Contador(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        int tiempoSegundos = getInputData().getInt("TiempoSegundos",25*60);
        Log.d("TIEMPO TOTAL",""+tiempoSegundos);
        int cuentaActual;
        for(cuentaActual=0;cuentaActual<tiempoSegundos;cuentaActual++){

            setProgressAsync(new Data.Builder().putInt("CuentaActual",cuentaActual).build());
            try{
                Thread.sleep(1000);
                Log.d("Cuenta",""+cuentaActual);
            }catch (InterruptedException e){
                return Result.failure();
            }
        }

        Data data = new Data.Builder().putInt("CuentaActual",cuentaActual).build();

        return Result.success(data);
    }
}
