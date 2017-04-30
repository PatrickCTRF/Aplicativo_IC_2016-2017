package com.example.patrick.servico_principal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.List;

/**
 * Created by patrick on 9/7/16.
 *
 * Esta classe colhe dados de todos os sensores, exceto o GPS, do celular e os fornecce de maneira intuitiva e
 * tratada, para faciclitar a leitura e interpretação.
 *
 */
public class AquisicaoSensores extends ContextWrapper{

    private TextView textBatteryLevel = null;
    private String batteryLevelInfo = "Battery Level", nulidade, info;
    private String eixoX = "", eixoY = "", eixoZ = "";
    private String giroX = "", giroY = "", giroZ = "";
    private SensorManager manager;
    private Sensor sensorA, sensorG;
    private LocationManager locationManager;
    private int qtde = 0;
    private List<Sensor> lista;
    private int level;
    private String statusString;
    private boolean dataAvailable;

    /* Criei classes anônimas separadas para o acelerometro e o giroscopio para poder usar dois registerListener() diferentes*/

    private SensorEventListener acelerometro = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {//Este método é chamado sempre que há mudança nos valores dos sensores do sistema.

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                eixoX = "" + event.values[0];//float relativo ao eixo x.
                eixoY = "" + event.values[1];//float relativo ao eixo y.
                eixoZ = "" + event.values[2];//float relativo ao eixo z.
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    private SensorEventListener giroscopio = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {//Este método é chamado sempre que há mudança nos valores dos sensores do sistema.

            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                giroX = "" + event.values[0];//float relativo ao eixo x.
                giroY = "" + event.values[1];//float relativo ao eixo y.
                giroZ = "" + event.values[2];//float relativo ao eixo z.
            } else {
                giroX = "erro aki";
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    public AquisicaoSensores(Context base) {
        super(base);
        level = 999999;
        statusString = " ";
        dataAvailable = false;
    }


    public void onCreate() {

        registerBatteryLevelReceiver();

        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);//Aki eu tenho um gerenciador para o serviço de sensoreamento do dispositivo.

        sensorG = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);//Obtive o sensor específico para acelerômetro.
        manager.registerListener(giroscopio, sensorG, SensorManager.SENSOR_DELAY_FASTEST);//Registrei um "servico de escuta" para o sensorG q desejo acompanhar.this refere-se a MainActivity, que é um SensorEventListener.

        sensorA = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);//Obtive o sensor específico para acelerômetro.
        manager.registerListener(acelerometro, sensorA, SensorManager.SENSOR_DELAY_FASTEST);//Registrei um "servico de escuta" para o sensorA q desejo acompanhar.this refere-se a MainActivity, que é um SensorEventListener.

        if(sensorG == null) nulidade = "Sensor Inexistente"; nulidade = "";

        lista = manager.getSensorList(Sensor.TYPE_ALL);

        qtde = 0;
        for(Sensor aux : lista){//O enhanced for percorrerá um numero de loops equivalente ao numero de sensores do celular.
            qtde++;//Assim incrementaremos qtde em funçao da qtde de sensores disponiveis.
        }

    }


    protected void onDestroy() {
        try{//Este try/catch evita erros causados por chamadas repetidas de unregisterReceiver
            unregisterReceiver(battery_receiver);
            manager.unregisterListener(acelerometro);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

    }

    public boolean isDataAvailable() {//Retorna true se os dados estão atualizados. Caso contrário, retorna false. Apenas utilize as informações desta classe quando os dados estiverem atualizados.
        return dataAvailable;
    }

    public int getLevel() {
        return level;
    }

    public String getStatusString() {
        return statusString;
    }

    private BroadcastReceiver battery_receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isPresent = intent.getBooleanExtra("present", false);
            String technology = intent.getStringExtra("technology");
            int plugged = intent.getIntExtra("plugged", -1);
            int scale = intent.getIntExtra("scale", -1);
            int health = intent.getIntExtra("health", 0);
            int status = intent.getIntExtra("status", 0);
            int rawlevel = intent.getIntExtra("level", -1);
            int voltage = intent.getIntExtra("voltage", 0);
            int temperature = intent.getIntExtra("temperature", 0);

            dataAvailable = true;

            Bundle bundle = intent.getExtras();

            Log.i("BatteryLevel", bundle.toString());

            if (isPresent) {
                if (rawlevel >= 0 && scale > 0) {
                    level = (rawlevel * 100) / scale;
                }

                info = "Battery Level: " + level + "%\n";
                info += ("Technology: " + technology + "\n");
                info += ("Plugged: " + getPlugTypeString(plugged) + "\n");
                info += ("Health: " + getHealthString(health) + "\n");
                info += ("Status: " + getStatusString(status) + "\n");
                info += ("Voltage: " + voltage + "\n");
                info += ("Temperature: " + temperature + "\n");
                info += ("\nAcelerômetro:\nEixo x: " + eixoX + "\n" + "Eixo y: " + eixoY + "\n" + "Eixo z: " + eixoZ + "\n");//Esteas duas linhas imprimem os sensores de acelerômetro e giroscópio.
                info += ("\nGiroscópio:" + nulidade + "\nGiro x: " + giroX + "\n" + "Giro y: " + giroY + "\n" + "Giro z: " + giroZ + "\n");
                info += ("\nNúmero de Sensores Disponíveis: " + qtde + "\n");


         //       setBatteryLevelText(info + "\n\n" + bundle.toString());
            } else {
       //         setBatteryLevelText("Battery not present!!!");
            }
        }
    };

    private String getPlugTypeString(int plugged) {
        String plugType = "Unknown";

        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                plugType = "AC";
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                plugType = "USB";
                break;
        }

        return plugType;
    }

    private String getHealthString(int health) {
        String healthString = "Unknown";

        switch (health) {
            case BatteryManager.BATTERY_HEALTH_DEAD:
                healthString = "Dead";
                break;
            case BatteryManager.BATTERY_HEALTH_GOOD:
                healthString = "Good";
                break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                healthString = "Over Voltage";
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                healthString = "Over Heat";
                break;
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                healthString = "Failure";
                break;
        }

        return healthString;
    }

    private String getStatusString(int status) {
        statusString = "Unknown";

        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                statusString = "Charging";
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                statusString = "Discharging";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                statusString = "Full";
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                statusString = "Not Charging";
                break;
        }

        return statusString;
    }

   /* private void setBatteryLevelText(String text) {
        textBatteryLevel.setText(text);
    }*/

    private void registerBatteryLevelReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        registerReceiver(battery_receiver, filter);
    }


    public String getInfo() {
        onCreate();
        return info;
    }
}
