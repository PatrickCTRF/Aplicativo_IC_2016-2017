package com.example.patrick.servico_principal;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

/**
 * Created by patrick on 10/19/16.
 *
 * Esta classe nos auxilia a lidar com as funcionalidades do GPS.
 */
public class Localizador extends ContextWrapper implements LocationListener {

    private boolean registrou_manager;//Esta variável evita que fiquemos registrando o anager vaárias vezes a cada chamada de mgetmylocation.
    //1 == registrado, 0 == nao registrado.
    private String myLocation;//Um string que guarda a nossa posição atual em forma de texto para facilitar a escrita em arquivo.
    private LocationManager locationManager;//Este é o manager que usaremos para solicitar acesso à localizaçãoes.
    private double latitude;
    private double longitude;
    private double incerteza;
    private boolean aguardando_coordenadas;
    boolean isInHome;
    PendingIntent intentPendente;

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getIncerteza() {
        return incerteza;
    }

    public boolean coordenadas_atualizadas() {//Retorna true se as coordenaadas estao atualizadas e false se nao.
        return !aguardando_coordenadas;
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        incerteza = location.getAccuracy();

        myLocation = "Latitude = " + location.getLatitude() + "\nLongitude = " + location.getLongitude() + "\nIncerteza = " + location.getAccuracy();

        //Ver os dados através do LOG.
        Log.e("LOCALIZAÇÃO ATUAL", myLocation);

        aguardando_coordenadas = false;//Nao esta mais esperando pra receber  as coodenadas.

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    public String getMyLocation() {//Parece que se chamarmos este método no construtor ocorre erro devido ao contexto dado. Não tenho certeza, mas parece se devido a isto.

        if (!registrou_manager) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
            }

            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);//Solicita atualizações de localização por WiFi para este listener (o próprio  obeto instanciado a partir desta classe).
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);//Solicita atualizações de localização por GPS para este listener (o próprio  obeto instanciado a partir desta classe).

            registrou_manager = true;

            Log.v("LISTENER", "listener REGISTRADO");
        }

        return myLocation;
    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public boolean registraAlertaDeProximidade(double latitude, double longitude, float raio) {//Um alerta a ser disparado sempre que entramos num dado perímetro.

        //O pendingIntent é um objeto que aguarda para lançar um intent no sistema. No caso, solicitei que este fosse lançado na forma de broadcast (getBroadcast), mas poderia lançá-lo para rodar um serviço ou mesmo uma activity, etc.
        intentPendente = PendingIntent.getBroadcast(this, 0, new Intent("com.example.patrick.ALERTA_PROXIMIDADE"), PendingIntent.FLAG_CANCEL_CURRENT);//Esta flag indica que se o pendinIntent for novamente chamado, os EXTRAS do meu intent serão sobrescritos apenas.

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }

        registerAlertaDeProximidadeReceiver();//Registramos o receiver para os brodcasts de proximidade.
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.addProximityAlert(latitude, longitude, raio, -1, intentPendente);//Adiciona um alert de proximidade com as coordenadas dadas.

        return true;//Retorna true se tudo foi registrado corretamente.
    }

    private void registerAlertaDeProximidadeReceiver() {
        IntentFilter filter = new IntentFilter("com.example.patrick.ALERTA_PROXIMIDADE");//Crio um fitro para definir qual tipo de intents eu receberei (Todos com a action ALERTA_PROXIMIDADE).
                                                                                        //A action acima é atribuíada ao Intent na hora que eu estou gerando-o, lá em cima da invocação do método addProximityAlert.
        registerReceiver(proximity_receiver, filter);//Registra no sistema o listener que eu criei.
    }

    public boolean isInHome(){//Quando o alerta for dado, esta variavel permite identificar se estamos entrando ou saindo do perímetroa sob alerta.
        return isInHome;
    }

    private BroadcastReceiver proximity_receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {//Quando dentro do perímetro desejado, a variável isInHome vale true.

            isInHome = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);//O EXTRA relacionado à chave KEY_PROXIMITY_ENTERING possui TRUE quando dentro do perímetro determinado e FALSE caso contrário.
                                                                                //Este intent é enviado pelo alerta gerado pelo método addProximityAlert.
            Log.v("ALERTA DE PROXIMIDADE", "boolean: " + isInHome);

        }

    };

    public void removeListener() {//Deixa de requisitar atualizações ao sistema e remove este listener. Economiza energia.

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        try{//Este try/catch evita erros causados por chamadas repetidas de unregisterReceiver e afins.
            Log.v("LISTENER", "listener REMOVENDO");
            if(intentPendente != null) locationManager.removeProximityAlert(intentPendente);//Remove o alerta de proximidade caso tenha sido registrado.
            locationManager.removeUpdates(this);//Desregistra o Listener.
            unregisterReceiver(proximity_receiver);
            Log.v("LISTENER", "listener REMOVIDO");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }catch (RuntimeException e){
            e.printStackTrace();
        }

    }

    public Localizador(Context base) {//Este construtor já registra o próprio objeto como locationListener.
        super(base);
        myLocation = "O valor da localização não está sendo alterado";
        aguardando_coordenadas = true;
        intentPendente = null;
        incerteza = 99999999;//Inicialmente a incerteza é absoluta.
        registrou_manager = false;
    }
}
