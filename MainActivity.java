package com.example.patrick.servico_principal;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import static android.widget.Toast.LENGTH_LONG;

public class MainActivity extends AppCompatActivity {

    public static EditText editTextAddress, editTextPort;
    public static TextView response;
    public static int home_latitude, home_longitude;
    public static boolean atualiza_home;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle savedInstanceState2 = savedInstanceState;
        setContentView(R.layout.activity_main);

        editTextAddress = (EditText) findViewById(R.id.addressEditText);
        editTextPort = (EditText) findViewById(R.id.portEditText);
        response = (TextView) findViewById(R.id.responseTextView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;//Estou retornando False para o menu nao ser exibido. VErificar se é necessário!
    }

    // Método que dará início ao servico de background.
    public void startService(View view) {

        startService(new Intent(getBaseContext(), ServicoDownload.class));

        startService(new Intent(getBaseContext(), ServicoGerenciamento.class));

        startService(new Intent(getBaseContext(), ServicoColetaDados.class));

    }

    // Metodo que parara o servico
    public void stopService(View view) {

        stopService(new Intent(getBaseContext(), ServicoDownload.class));

        stopService(new Intent(getBaseContext(), ServicoGerenciamento.class));

        stopService(new Intent(getBaseContext(), ServicoColetaDados.class));
    }

    /**
     *Os métodos abaixo setam o modo de atuação energética
     */
    public void seta_home(View view) {//Define que o ponto atual é a home do usuario.

        //Você manda daqui um broadcast para avisar ao serviço que você está na nova home. Assim não temos que esperar sair e entrar de novo na home para identificá-la.
        Intent intnet = new Intent("com.example.patrick.ALERTA_PROXIMIDADE");
        intnet.putExtra(LocationManager.KEY_PROXIMITY_ENTERING, true);
        sendBroadcast(intnet);

        startService(new Intent(getBaseContext(), ServicoParaGPS.class));
    }

    public void startPerformance(View view){
        File arquivo = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Comando.txt");

        try {
            FileWriter escritor = new FileWriter(arquivo, false);

            escritor.write("desempenho\n");
            escritor.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        Toast.makeText(this, "Desempenho Máximo", LENGTH_LONG).show();
    }

    public void startEconomy(View view){
        File arquivo = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Comando.txt");

        try {
            FileWriter escritor = new FileWriter(arquivo, false);

            escritor.write("economia\n");
            escritor.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        Toast.makeText(this, "Economizando", LENGTH_LONG).show();
    }

    public void startAuto(View view){
        File arquivo = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Comando.txt");

        try {
            FileWriter escritor = new FileWriter(arquivo, false);

            escritor.write("automatico\n");
            escritor.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        Toast.makeText(this, "Automatizando", LENGTH_LONG).show();
    }

    public void startDesligado(View view){
        File arquivo = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Comando.txt");

        try {
            FileWriter escritor = new FileWriter(arquivo, false);

            escritor.write("desligado\n");
            escritor.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        Toast.makeText(this, "Desligado", LENGTH_LONG).show();
    }
}
