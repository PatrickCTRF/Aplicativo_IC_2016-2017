package com.example.patrick.servico_principal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

import static android.widget.Toast.LENGTH_LONG;
import static java.lang.Double.parseDouble;

/**
 * Created by patrick on 24/03/17.
 *
 * Este serviço, basicamente, serve para decidir quando rodar a asyncTask q executará o download de nosso arquivo - atualmente, um pacote zip de músicas.
 *
 * Como a maior parte dos serviços são iguais, a descrição detalhada de cada componente foi realizada somente no serviço de coleta, pois era o maior.
 * Se precisar de melhores detalhes olhe naquela classe.
 */

public class ServicoDownload extends Service {

    final Handler handler = new Handler();
    final AquisicaoSensores info = new AquisicaoSensores(this);
    boolean registrouAlertas = false;
    Conectividade conexao = new Conectividade(this);
    Context contextt = this;
    String modo_Desempenho = null;
    AlarmManager alarm;

    //Obtém sua localizção atual
    Localizador locationListener;

    Runnable runnableCode;


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        Toast.makeText(this, "Service Download Started", LENGTH_LONG).show();

        info.getInfo();

        alarm = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.example.patrick.START_SERVICODOWNLOAD"),  PendingIntent.FLAG_UPDATE_CURRENT);

        runnableCode = new Runnable() {

            private int contador = 0;
            private int contadorDeLongoPrazo = 0;

            @Override
            public void run() {

                Log.v("SERVICO", "O ServicoDownload foi chamado. Contador: " + contador + "  Contador De Longo Prazo: " + contadorDeLongoPrazo);

                conexao.isConnectedWifi();

                File arquivoModo = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Modo_Atual.txt");

                try {

                    BufferedReader bufferLeitura = new BufferedReader(new FileReader(arquivoModo));

                    modo_Desempenho = bufferLeitura.readLine();
                    bufferLeitura.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(conexao.isConnectedWifi() && !modo_Desempenho.equals("desligado"))//Só faz download por WiFi.
                if((modo_Desempenho.equals("economia") && info.getLevel()>15) || modo_Desempenho.equals("desempenho")){//Se o modo de desempenho estiver  ligado baixe. Se for modo de economia com bateria suficiente, baixe tb. Se for desligado, não baixará.
                    File file = new File(Environment.getExternalStorageDirectory().toString() + "/" + "musicas" + ".zip");//Arquivo onde salvaremos os dados baxados.

                    try {
                        file.createNewFile();
                        Log.v("CAMINHO", Environment.getExternalStorageDirectory().toString());
                        DownloadFilesTask baixaMusica = new DownloadFilesTask();//AsyncTask utilizada para baixar os dados.
                        Log.v("URLL", "Tentando baixar");

                        try {
                            baixaMusica.setContext(contextt, file);//Passamos um arquivo (para poder salvar os dados) e um context (para poder mostrar a notificação na barra de notificação com o progresso).
                            if(modo_Desempenho.equals("economia")){baixaMusica.execute(new URL("https://archive.org/compress/return_holmes_0708_librivox/formats=64KBPS%20MP3&file=/return_holmes_0708_librivox.zip"));}//Se estivermos no modo de economia, baixe o arquivo compacto.
                            else{baixaMusica.execute(new URL("https://archive.org/compress/return_holmes_0708_librivox/formats=128KBPS%20MP3&file=/return_holmes_0708_librivox.zip"));}//Se for modo de desempenho, baixa em alta qualidade.
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            Log.v("URLL", "Deu erro");
                        }

                        Log.v("URLL", "Esta Baixando");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    desligaSensores();//Desliga sensores para economizar.

                    if(modo_Desempenho.equals("desligado")){
                        alarm.set(AlarmManager.RTC, System.currentTimeMillis() + (1*3600000), pendingIntent );//No modo de deligado, não baixe, mas se atualiza a cada 1 horas.
                    }else if(modo_Desempenho.equals("economia")) {
                        alarm.set(AlarmManager.RTC, System.currentTimeMillis() + (4*3600000), pendingIntent );//No modo de economia, baixe a cada 4 horas.
                    }else{
                        alarm.set(AlarmManager.RTC, System.currentTimeMillis() + (1800000), pendingIntent);//No modo de desempenho, baixe a cada meia hora o arqivo.
                    }
                    handler.removeCallbacks(this);
                }
            }
        };

        handler.post(runnableCode);

        return START_STICKY;
    }

    private void desligaSensores(){//Este métoddo permite ao celular desligar os sensores e GPS para poupar energia.
        if(locationListener != null)locationListener.removeListener();//Deixa de requisitar atualizações ao sistema e remove este listener. Economiza energia.
        if(info != null) info.onDestroy();//Deixa de requisitar atualizações ao sistema e remove os listener. Economiza energia e evita relatório de erros.
        registrouAlertas = false;//O  alerta agr nao estará mais registrado.
        locationListener = new Localizador(this);//Se ocorrer erro no unregisterReceiver precisaremos de um novo objeto desta classe.
    }

    @Override
    public void onDestroy() {

        alarm.cancel(PendingIntent.getBroadcast(this, 0, new Intent("com.example.patrick.START_SERVICODOWNLOAD"),  PendingIntent.FLAG_UPDATE_CURRENT));
        if(locationListener != null)locationListener.removeListener();//Deixa de requisitar atualizações ao sistema e remove este listener. Economiza energia.
        if(info != null) info.onDestroy();//Deixa de requisitar atualizações ao sistema e remove os listener. Economiza energia e evita relatório de erros.

        Toast.makeText(this, "Service Destroyed", LENGTH_LONG).show();
        handler.removeCallbacks(runnableCode);//Retira todas as chamadas agendadas deste serviço.
        super.onDestroy();

    }

}
