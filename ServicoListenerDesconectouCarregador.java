package com.example.patrick.servico_principal;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static android.widget.Toast.LENGTH_LONG;
import static java.lang.Double.parseDouble;

/**
 * Created by patrick on 09/04/17.
 */

public class ServicoListenerDesconectouCarregador extends Service {


    final Handler handler = new Handler();
    final AquisicaoSensores info = new AquisicaoSensores(this);
    File arquivoTensaoInicial = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Tensao_ao_desconectar_carregador.txt");
    File arquivoTempoInicial = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Momento_ao_desconectar_carregador.txt");
    FileWriter escritor;

    Runnable runnableCode;


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        Toast.makeText(this, "Service ListenerDesconectouCarregador Started", LENGTH_LONG).show();

        runnableCode = new Runnable() {

            @Override
            public void run() {

                info.getInfo();

                if(info.getLevel()>100) {

                    info.getInfo();

                    handler.postDelayed(this, 1000);//O serviço se repete múltiplas vezes seguidas para garantir que estamos recebendo uma leitura correta dos sensores.

                }else{

                    try {

                        arquivoTempoInicial.createNewFile();
                        escritor = new FileWriter(arquivoTempoInicial, false);//apaga o buffer de dados e o fecha.
                        escritor.write("" + System.currentTimeMillis() + "\n");
                        escritor.close();

                        arquivoTensaoInicial.createNewFile();
                        escritor = new FileWriter(arquivoTensaoInicial, false);//apaga o buffer de dados e o fecha.
                        escritor.write("" + info.getLevel() + "\n");
                        escritor.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    onDestroy();
                }
            }
        };

        handler.post(runnableCode);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        info.onDestroy();
        Toast.makeText(this, "Service Destroyed", LENGTH_LONG).show();
        handler.removeCallbacks(runnableCode);//Retira todas as chamadas agendadas deste serviço.
        super.onDestroy();

    }

}
