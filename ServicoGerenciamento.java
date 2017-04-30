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
 * Este serviço lê um arquivo de texto que guarda a instrução que o usuário deu para sua intenção de economia de energia (desempenho, economia, automáticco ou desligado).
 *
 * O modo automático é o foco de todo este trabalho, tentando poupar energia sem cortar desempenho. Todos os outros modo permanecem agindo enquanto a bateria estiver acima de 15%
 *
 * Como a maior parte dos serviços são iguais, a descrição detalhada de cada componente foi realizada somente no serviço de coleta, pois era o maior.
 * Se precisar de melhores detalhes olhe naquela classe.
 */
public class ServicoGerenciamento extends Service {

    final Handler handler = new Handler();
    final AquisicaoSensores info = new AquisicaoSensores(this);
    String comando_do_usuario = null;
    MMQ regressao_linear = new MMQ();
    AlarmManager alarm;

    Runnable runnableCode;


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        Toast.makeText(this, "Service Gerenciamento Started", LENGTH_LONG).show();

        info.getInfo();

        alarm = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.example.patrick.START_SERVICOGERENCIAMENTO"),  PendingIntent.FLAG_UPDATE_CURRENT);

        runnableCode = new Runnable() {

            private int contador = 0;
            private int contadorDeLongoPrazo = 0;

            @Override
            public void run() {

                Log.v("SERVICO", "O ServicoGerenciamento foi chamado. Contador: " + contador + "  Contador De Longo Prazo: " + contadorDeLongoPrazo);

                File arquivoComando = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Comando.txt");

                File arquivoModo = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Modo_Atual.txt");

                try {
                    BufferedReader bufferLeitura = new BufferedReader(new FileReader(arquivoComando));

                    comando_do_usuario = bufferLeitura.readLine();
                    bufferLeitura.close();

                    FileWriter escritor = new FileWriter(arquivoModo, false);//apaga o buffer de dados e o fecha.


                    //O modo escolhido pelo usuário precisa passar pela seleção do serviço de gerenciamento antes de intervir nos demais serviços.
                    if(comando_do_usuario.equals("desempenho")) {//Seleciona o modo no qual atuarão os outros serviços.
                        escritor.write("desempenho");

                    }else if(comando_do_usuario.equals("automatico")){
                        avaliaConsumo(escritor, info);

                    }else if(comando_do_usuario.equals("economia")){
                        escritor.write("economia");

                    }else if(comando_do_usuario.equals("desligado")){
                        escritor.write("desligado");

                    }else{
                        escritor.write("desligado");
                        
                    }


                    escritor.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.v("MMQ", "Parâmetro B: " + regressao_linear.getB());
                Log.v("MMQ", "Parâmetro A: " + regressao_linear.getA());

                /*if(false && ++contador<3) {//Este serviço não precisa ficar se repetindo. Acho que poderia até ser 1 aqui. Testar posteriormente.

                    handler.postDelayed(this, 1000);//O serviço se repete múltiplas vezes seguidas para garantir que estamos recebendo uma leitura correta dos sensores.

                } else if( false && ++contadorDeLongoPrazo<50){//Após sucessivas repetições, aguardamos um longo período de tempo para realizar uma nova amostragem.

                    contador = 0;//Reiniciamos o contador de amostragem.
                    handler.postDelayed(this, 600000);//10 minutos.
                }*/

                alarm.set(AlarmManager.RTC, System.currentTimeMillis() + 300000, pendingIntent );//Se reagenda para o tempo atual + 3min.
                handler.removeCallbacks(this);

            }
        };

        handler.post(runnableCode);

        return START_STICKY;
    }


    @Override
    public void onDestroy() {

        alarm.cancel(PendingIntent.getBroadcast(this, 0, new Intent("com.example.patrick.START_SERVICOGERENCIAMENTO"),  PendingIntent.FLAG_UPDATE_CURRENT));
        if(info != null) info.onDestroy();//Deixa de requisitar atualizações ao sistema e remove os listener. Economiza energia e evita relatório de erros.

        Toast.makeText(this, "Service Destroyed", LENGTH_LONG).show();
        handler.removeCallbacks(runnableCode);//Retira todas as chamadas agendadas deste serviço.
        super.onDestroy();

    }

    /**
     * Este método avalia se o usuário está tendendo a ter bateria até o  final do dia o não.
     * O raciocínio é simples: temos nossa regressão linear traçando uma reta de previsão de uso até o final do dia. Se você tem mais bateria do que a reta naquele ponto, você tem energia sobrando para gastar emd esempenho.
     * Se você tem menos energia do que a reta de regressão naquele ponto, provavelmente não terá energia ao final do dia, então deve-se aplicar a economia.
     *
     * Este método é útil para o geerenciamento automático de energia, que é o foco de todo este trabalho.
     *
     * @param escritor
     * @param info
     * @throws IOException
     */
    private void avaliaConsumo(FileWriter escritor, AquisicaoSensores info) throws IOException {

        float percentual_previsto = (float) (regressao_linear.getA() + regressao_linear.getB()*((System.currentTimeMillis()-10800000)%86400000));//nesta variável guardamos o valor do restante de bateria que DEVERIA HAVER neste momento ( de 0,00 a 1,00). Esses -10800000 são para converter o fuso horário para o horário de brasília.
        if(percentual_previsto <= info.getLevel()){
            escritor.write("desempenho");//Se houver mais bateria que o previsto, gaste à vontade.
        }else{
            escritor.write("economia");//Se a bateria estiver abaixo do previsto, economize.
        }
    }

}