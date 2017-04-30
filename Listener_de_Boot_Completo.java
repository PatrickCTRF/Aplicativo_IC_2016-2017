package com.example.patrick.servico_principal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by patrick on 12/01/17.
 *
 * Esta classe é um listener geral. Qualquer broadcast para iniciar um serviço deste aplicativo passa por aqui para ser verificado, tendo sido enviado pelo sistemo ou pelo próprio app.
 * Os condicionais abaixo identificam qual serviço deve se iniciado e o fazem.
 */

public class Listener_de_Boot_Completo extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {//No Manifeste declaramos qu esta classe é um receiver (listener) para o sinal de boot concluído e para carregador desconectando.
        Log.d("BROADSCAST", "Foi chamado por broadcast");

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BROADSCAST", "Foi foi reconhecido broadcast de boot");
            Intent serviceIntent = new Intent(context, ServicoColetaDados.class);//invocando o serviço qque desejamos rodar ao boot
            context.startService(serviceIntent);//iremos invocá-lo a partir do contexto que nos foi passado pelo broadcast.

            serviceIntent = new Intent(context, ServicoGerenciamento.class);//invocando o serviço qque desejamos rodar ao boot
            context.startService(serviceIntent);

            serviceIntent = new Intent(context, ServicoDownload.class);//invocando o serviço qque desejamos rodar ao boot
            context.startService(serviceIntent);

        }

        if (intent.getAction().equals("com.example.patrick.START_SERVICOCOLETA_DADOS")){
            Log.d("BROADSCAST", "Foi foi reconhecido broadcast de coleta");
            Intent serviceIntent = new Intent(context, ServicoColetaDados.class);//invocando o serviço qque desejamos rodar ao boot
            context.startService(serviceIntent);//iremos invocá-lo a partir do contexto que nos foi passado pelo broadcast.
        }

        if (intent.getAction().equals("com.example.patrick.START_SERVICODOWNLOAD")){
            Log.d("BROADSCAST", "Foi foi reconhecido broadcast de download");
            Intent serviceIntent = new Intent(context, ServicoDownload.class);//invocando o serviço qque desejamos rodar ao boot
            context.startService(serviceIntent);//iremos invocá-lo a partir do contexto que nos foi passado pelo broadcast.
        }

        if (intent.getAction().equals("com.example.patrick.START_SERVICOGERENCIAMENTO")){
            Log.d("BROADSCAST", "Foi foi reconhecido broadcast de gerenciamento");
            Intent serviceIntent = new Intent(context, ServicoGerenciamento.class);//invocando o serviço qque desejamos rodar ao boot
            context.startService(serviceIntent);//iremos invocá-lo a partir do contexto que nos foi passado pelo broadcast.

        }



        if(intent.getAction().equals("com.example.patrick.USER_ACTION")||Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())){//A detecção de USER_ACTION é somente para podermos ativar este if manualmente pelo app BroadcastEnvia.
            Log.d("BROADSCAST", "Foi foi reconhecido broadcast de POWER DISCONNECTED");

            Intent serviceIntent = new Intent(context, ServicoListenerDesconectouCarregador.class);//invocando o serviço qque desejamos rodar ao boot
            context.startService(serviceIntent);//iremos invocá-lo a partir do contexto que nos foi passado pelo broadcast.

        }
    }
}
