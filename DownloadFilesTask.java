package com.example.patrick.servico_principal;

import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by patrick on 05/03/17.
 */

class DownloadFilesTask extends AsyncTask<URL, Integer, Long> {

    Context context;
    File file;

    protected Long doInBackground(URL... urls) {
        int count = urls.length;//Isto é a quantidade de URLs para baixar.
        long totalSize = 0;
        int tamanhoDoArquivo;
        int contador_auxiliar = 0;


        try {


            for (int i = 0; i < count; i++) {
                totalSize = 0;

                Log.v("URLL", "conectando");

                URLConnection conexao = urls[i].openConnection();//Abrimos a conexao
                conexao.connect();//Conectamos.
                tamanhoDoArquivo = conexao.getContentLength();//Obtém o tamanho do arquivo.

                BufferedInputStream input = new BufferedInputStream(urls[i].openStream());

                FileOutputStream arquivo = new FileOutputStream(file, false);

                byte data[] = new byte[1024];
                int parcialSize = 0;

                Log.v("URLL", "obtendo bytes");
                while ((parcialSize = input.read(data)) != -1){
                    totalSize += parcialSize;

                    //Log.v("URLL", "obteve mais alguns bytes");
                    arquivo.write(data, 0, parcialSize);

                    if(contador_auxiliar <= 0) {publishProgress((int) ((totalSize / (float) tamanhoDoArquivo) * 100)); contador_auxiliar = 500;}
                    contador_auxiliar--;
                }

                publishProgress(-1);

                Log.v("URLL", "obteve tudo");

                arquivo.close();
                input.close();

                // Escape early if cancel() is called
                if (isCancelled()) break;
            }

        }catch (IOException e) {
            e.printStackTrace();
        }

        return totalSize;
    }

    public void setContext(Context context, File file){
        this.file = file;
        this.context = context;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {


    //Log.v("NOTIFICAÇÃO", "chamada");
    super.onProgressUpdate(progress);

        int id = 1;

        NotificationManager mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setContentTitle("Sherlock Download").setContentText("Download em progresso").setSmallIcon(R.mipmap.ic_launcher);

        if(progress[0] >= 0){ mBuilder.setProgress(100, progress[0], false);} else{mBuilder.setProgress(0, 0, false);}
        mNotifyManager.notify(id, mBuilder.build());
        //setProgressPercent(progress[0]);
    }



    protected void onPostExecute(Long result) {
        //showDialog("Downloaded " + result + " bytes");
    }

}