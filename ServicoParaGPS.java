package com.example.patrick.servico_principal;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

import static android.widget.Toast.LENGTH_LONG;

/**
 * Created by patrick on 23/01/17.
 *
 * Este serviço somente obtém aas coordenadas da Home, salva-as num arquivo e se encerra.
 * ele é lançado quando o usuário pressiona o botão "seta Home" na tela inicial do app.
 */

public class ServicoParaGPS extends Service {

    final Handler handler = new Handler();

    //Obtém sua localizção atual
    final Localizador coordenadas = new Localizador(this);
    boolean servicoDestruido = false;
    Context contextt = this;
    Runnable runnableCode;


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Toast.makeText(this, "Buscando Coordenadas", LENGTH_LONG).show();
        servicoDestruido = false;

        runnableCode = new Runnable() {

            private int contador = 0;

            @Override
            public void run() {

                Log.v("SERVICO PRINCPAL BOOT", "O serviço principal foi chamado no boot." + contador);



                coordenadas.getMyLocation();//Manda obter as corrdenadas.

                Log.v("1111111111111111", "111111111111111111");

                File arquivo = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Latitude_Longitude_Home.txt");



                if(!(coordenadas.coordenadas_atualizadas() && coordenadas.getIncerteza()<22)){
                    handler.postDelayed(this, 1000);
                    Log.v("2222222222", "2222222222222");
                }else {
                    try {
                        FileWriter escritor = new FileWriter(arquivo, false);

                        Log.v("33333333333333", "333333333333333333");

                        escritor.write("" + coordenadas.getLatitude() + " \n" + coordenadas.getLongitude() + " \n" + coordenadas.getIncerteza() + " \n");//Seta a nova posição de home no arquivo de registro.
                        escritor.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    /*File file = new File(Environment.getExternalStorageDirectory().toString() + "/" + "musicas" + ".zip");

                    try {
                        if(file.createNewFile() || true) {
                            Log.v("CAMINHO", Environment.getExternalStorageDirectory().toString());
                            DownloadFilesTask baixaMusica = new DownloadFilesTask();
                            Log.v("URLL", "Tentando baixar");

                            try {
                                baixaMusica.setContext(contextt, file);
                                baixaMusica.execute(new URL("https://archive.org/compress/return_holmes_0708_librivox/formats=64KBPS%20MP3&file=/return_holmes_0708_librivox.zip"));//http://www.textureking.com/content/img/stock/big/DSC_6849.JPG//https://archive.org/compress/return_holmes_0708_librivox/formats=64KBPS%20MP3&file=/return_holmes_0708_librivox.zip
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                                Log.v("URLL", "Deu erro");
                            }

                            Log.v("URLL", "Esta Baixando");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/

                    onDestroy();
                }

            }
        };

        handler.post(runnableCode);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {


        try {
            if (!servicoDestruido) {//Só execute se ainda nao foi destruído. Senão às vezes ocorrem bugs.
                coordenadas.removeListener();//Deixa de requisitar atualizações ao sistema e remove este listener. Economiza energia.
                servicoDestruido = true;//Serviço já foi destruído automaticamente, isto informa para que nao tentemos destruí-lo de novo, causando bugs.
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        handler.removeCallbacks(runnableCode);
        Toast.makeText(this, "Nova Posição de Home setada", LENGTH_LONG).show();
        super.onDestroy();

    }

}
