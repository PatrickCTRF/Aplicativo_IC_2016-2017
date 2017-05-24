package com.example.patrick.servico_principal;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Vibrator;

/**
 * Created by patrick on 21/05/17.
 */

public class VibraEmDiferentesIntervalos extends AsyncTask<Void, Void, Void> {

    Context context;
    int interval;

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public void setContext(Context context){
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... params) {

        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);


        while(interval>0){//Se setarmos um intervalo menor que zero, a AsyncTask morre.
            v.vibrate(500);
            try {
                Thread.sleep(interval + 500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
