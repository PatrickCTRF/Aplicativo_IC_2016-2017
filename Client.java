package com.example.patrick.servico_principal;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;

//IP atual: 192.168.1.108;192.168.0.105

/**
 * Esta classe faz o envio e o recebimento de dados com o servidor.
 */

public class Client extends AsyncTask<Void, Void, Void> {

    String dstAddress;
    int dstPort;
    String response = "";
    TextView textResponse;
    String info;
    Calendar calendario = Calendar.getInstance();


    Client(String addr, int port, TextView textResponse, String info) {//addr é relativo a address = endereço web.
        dstAddress = addr;
        dstPort = port;
        this.textResponse = textResponse;
        this.info = info;
    }

    Client(String addr, int port, String info) {//addr é relativo a address = endereço web.
        Log.v("Conexão Client", "Criando conexão");
        dstAddress = addr;
        dstPort = port;
        this.textResponse = null;
        this.info = info;
        Log.v("Conexão Client", "Criando conexão 2");
    }

    public void setInfo(String info) {
        this.info = info;
    }

    @Override
    protected Void doInBackground(Void... arg0) {

        Log.v("Conexão Client", "Tentando conexão00");
        Socket socket = null;
        Log.v("Conexão Client", "Tentando conexão0");
        try {

            Log.v("Conexão Client", "Tentando conexão");
            socket = new Socket(dstAddress, dstPort);

            String bytesFromServersStringRead = null;
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //calendario = Calendar.getInstance();
            //socket.getOutputStream().write(("" + calendario.get(Calendar.HOUR) + ":" + calendario.get(Calendar.MINUTE) + ":" + calendario.get(Calendar.SECOND) + "," + calendario.get(Calendar.MILLISECOND) + "\n").getBytes());
            Log.v("Conexão Client", "Enviando Dados");
            socket.getOutputStream().write((info + "\nFIM\n").getBytes());
            Log.v("Conexão Client", "Dados enviados");

            bytesFromServersStringRead = inputStream.readLine();
            response += bytesFromServersStringRead;

            Log.v("Conexão Client", "Resposta: " + response);

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            File arquivoDados = new File(Environment.getExternalStorageDirectory().toString() + "/" + "_InformacoesDaVidaDoUsuario.txt");
            try {
                FileWriter escritor = new FileWriter(arquivoDados, true);//apaga o buffer de dados e o fecha.
                escritor.write(info + "\n");
                escritor.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
            response = "IOException: " + e.toString();
        }  finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if(textResponse != null) textResponse.setText(response);
        super.onPostExecute(result);
    }

}