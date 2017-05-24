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

import static android.widget.Toast.LENGTH_LONG;
import static java.lang.Double.parseDouble;

/**
 * Created by patrick on 24/03/17.
 *
 * Este serviço colhe amostras de todos os sensores que temos disponíveis no celular. em seguida, ele verificase temos conexão com a internet WiFi e se estamos em home. Em caso
 * afirmativo, ele verifica se há dados antigos guardados o nosso arqivo buffer de dados e os envia para o servidor juntamente com os dados atuais.
 * Caso não haja conexão WiFi, ou o usuário não esteja em home,  os dados são guardados num arquivo buffer do tipo txt.
 *
 * Este serviço atua de acordo com o modo de energia determinado pelo Serviço gerenciador. Se estivermos no modo de economia ou desligado, ele demorará muito maistmepo para voltar a ser
 * executado do que no modo de desempenho, por exemplo.
 *
 * Utilizamos um HANDLER para reagendar este serviço dentro de curtos prazos enquanto aguardamos algma informação breve chegar.
 * Após realizar as devidas tarefas deste serviço, programamos uma reexecução do mesmo daqui a um intervalo de tempo maior através de instâncias da classe AlarmManager.
 */

public class ServicoColetaDados extends Service {

    final Handler handler = new Handler();//Objeto usado para reagendar o período para daqui a curtos períodos de tempo.
    final AquisicaoSensores info = new AquisicaoSensores(this);//Objeto utilizado para obter informações de qualquer sensor do celular, exceto o GPS.
    double home_latitude = 0, home_longitude = 0;//Strings que guardarão a posição da home física do usuário.
    String ip = "10.10.25.184", aux = null, aux2 = null, modo_Desempenho;//ip antigo = "192.168.0.105"//Strings auxiliares.
    int porta = 6789;//Porta usada pelo servidor.
    boolean registrouAlertas = false;//Variável usada par se saber se os alertas de posição GPS - dentro ou fora da home - foram registrados.
    Conectividade conexao = new Conectividade(this);//Objeto utilizado para se obter informações da conexão atual do celular.
    Intent intente = null;//Um intent utilizado para enviar dados a outros aplicativos.
    FileWriter escritor;//Apontador para escrevermos num arquivo.
    BufferedReader checkpoint_Tensao;//Apontador para lermos um arquivo.
    AlarmManager alarm;//Apontador para nosso Gerenciador de alarmes - alarmes que reiniciarão este serviço depois de longos intervalos de tempo.
    PendingIntent pendingIntent;//Um objeto capaz de guardar um intent para ser disparado no futuro para iniciar este mesmo serviço.
    Client myClient;//AsyncTask que envia os dados coletados ao servidor.


    File arquivoDados = new File(Environment.getExternalStorageDirectory().toString() + "/" + "_InformacoesDaVidaDoUsuario.txt");//Arquivo buffer onde se armazenam, temporariamente, os dados amostrados até que haja conexão com o servidor e estejamos em home.

    File arquivoHome = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Latitude_Longitude_Home.txt");//Arquivo que guarda a posição original da home.

    File arquivoEixoX = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Eixo_X_Tempo.txt");//Arquivo vetor com os dados amostrados de momento/tempo de cada amostra.
    File arquivoEixoY = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Eixo_Y_Bateria.txt");//Arquivo vetor com os dados amostrados de bateria/level de cada amostra.
    File arquivoTensaoInicial = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Tensao_ao_desconectar_carregador.txt");//A tensão/carga percentual da bateria ao se desconectar o carregador.
    File arquivoTempoInicial = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Momento_ao_desconectar_carregador.txt");//O momento em que o carregador foi desconectado.

    File arquivoModo = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Modo_Atual.txt");//Arquivo gerado pelo GERENCIADOR, no qual verificamos qual o modo de operação energética atual do celular (economi, desempenho, desligado).

    //Obtém sua localização atual
    Localizador locationListener = new Localizador(this);//Objeto auxiliar para lidarmos com todas as funções desejadas para o GPS.

    Runnable runnableCode;//Objeto qe contém o código essencial deste serviço e que será executado repetdas vezes, sendo reagendado pelo HANDLER a curto prazo até que obtenha-se todas as amostras/estados desejados.


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }//Não utilizei este método neste aplicativo.


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {//Este método é invocado sempre que se inicia o serviço. É o ponto de partida..

        Toast.makeText(this, "Service Coletando Started", LENGTH_LONG).show();//Exibe na tela que este serviço foi iniciado.

        info.getInfo();//Estes 3 objetos demoram certo tempo para obter do sistema as informações desejadas.
        conexao.isConnectedWifi();//Então a primeira ação que eu tomo é mandá-los se atualizarem, para estarem prontos no momento em que me forem necessários (algumas linhas abaixo).
        locationListener.getMyLocation();

        alarm = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);//Recebemos nosso ponteiro manager qe será utilizado para reagendar este serviço.
        pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.example.patrick.START_SERVICOCOLETA_DADOS"),  PendingIntent.FLAG_UPDATE_CURRENT);//Configuramos como será lançado o intent que reagendará este serviço

        try {//Acessamos o arquivo que indica o modo de operação energética atual do dispositivo para sabermos como proceder.

            BufferedReader bufferLeitura = new BufferedReader(new FileReader(arquivoModo));

            modo_Desempenho = bufferLeitura.readLine();//A informação é salva nesta string para utilizarmos no pprograma.
            bufferLeitura.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        runnableCode = new Runnable() {

            /**
             *Utilizo esta variável para mantêr o controle de reagendamento do serviço a curto prazo.
             *Algumas vezes, por exemplo, chegamos ao momento de ler os dados de "info" e "locationListener" e estas ainda nao obtiveram a respostado sistema.
             *Assim, eu nao desativo os sensores e nem deleto o serviço. Eu apenas o reagendo para daqui a um segundo e verifico se as mesmas foram atualizadas.
             *Caso, após certo número de repetições, elasnão tenham sido atualizadas ainda, o contador estoura o limite que eu determinei e
             *deixo para colher esta info numa próxima amostragem. Dessta forma, todas as outras amostras também são descartadas.
             */
            private int contador = 0;

            @Override
            public void run() {//O trecho de código que será repetido indefinidamente.

                Log.v("SERVICO", "O ServicoColetaDados foi chamado. Contador: " + contador + "  Contador De Longo Prazo: ");

                //locationListener.getMyLocation();//Solicita as atualizações de local

                if(!registrouAlertas){//Só precisamos registrar os alertas uma única vez.
                    Log.v("ALERTA DE PROXIMIDADE", "TENTANDO CHAMAR ALERTAS...");

                    try {
                        BufferedReader bufferLeitura = new BufferedReader(new FileReader(arquivoHome));

                        home_latitude = parseDouble(bufferLeitura.readLine());//Lemos no arquivo a latitude e longitude de noossa home.
                        home_longitude = parseDouble(bufferLeitura.readLine());
                        bufferLeitura.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //CUIDADO PARA NÃO REGISTRAR ESTE ALERTA ANTES DE SE OBTER OS VALORES DE HOME LATITUDE E HOME LONGITUDE.
                    //Registrar este alerta aqui nos dá tempo dele responder antes de deixarmos os sensores voltarem ao estado de sleep.
                    //CUIDADO CASO VÁ ALTERAR A POSIÇÃO DESTE BLOCO NO PROGRAMA. Fácil de errar.
                    locationListener.registraAlertaDeProximidade(home_latitude, home_longitude, (float) 22);//Registramos e solicitamos o alerta de proximidade.
                    registrouAlertas = true;
                }

                if(++contador<40 && (locationListener.getIncerteza()>22 || (info.getStatusString() == null || info.getStatusString().equals(" ")))) {//Se a incerteza do GPS for maior que 22m ou nao tivermos dados dos sensores, reagende o serviço para daqui a 1 segundo. Mas se por 40 vezes isto ocorrer, desista desta amostra e aguarde a próxima chamada do serviço
                    Log.v("Serviço Coleta", "Não há precisão");
                    handler.postDelayed(this, 1000);//O serviço se repete múltiplas vezes seguidas para garantir que estamos recebendo uma leitura correta dos sensores.

                } else if(contador < 40){//Se saímos do IF anterior antes do contador estourar, significa que temos os dados desejado. Comecemos a extraí-los da maneira adequada.
                    Log.v("Serviço Coleta", "Há precisão. Contador: " + contador);

                    try {

                        if (locationListener.isInHome && conexao.isConnectedWifi()) {//Considera-se que na home comm conexão wifi há condições de enviar os dados ao servidor.
                            //Se houver condição de enviar os dados ao servidor, envie todos os dados disponíveis.
                            Log.v("HOMEinfo", "ESTÁ NA HOME");

                            BufferedReader leituraDados = new BufferedReader(new FileReader(arquivoDados));

                            arquivoDados.createNewFile();//Se e somente SE NÃO existir o arquivo especificado, iremos criá-lo para evitar erros de arquivos não encontrados.

                            if ((aux = leituraDados.readLine()) != null) {//Se o arquivo nao estiver vazio...

                                Log.v("SERVIDOR", "DADOS SALVOS ENVIANDO");

                                while ((aux2 = leituraDados.readLine()) != null) {//Leia tudo que está no arquivo.
                                    aux += "\n" + aux2;
                                    aux2 = null;
                                }
                                aux += "\n";

                                aux += "\n" + "\n\nTempo atual: " + System.currentTimeMillis() + "\n" + info.getInfo() + "\n\n" + locationListener.getMyLocation() + "\n----------------\n";//Montamos a string de informações para enviar ao servidor.

                                escritor = new FileWriter(arquivoDados, false);//apaga o buffer de dados e o fecha.
                                escritor.write("");
                                escritor.close();

                                myClient = new Client(ip, porta, aux);//Envie para o servidor os dados que estavam salvos.
                                myClient.execute();//A quebra de linha após o aux é para alinhar os dados do arquivo com os do servidor da forma que são recebidos.

                            } else {//Se o arquivo estiver vazio...

                                Log.v("SERVIDOR", "DADOS ENVIANDO");

                                aux = "\n" + "\n\nTempo atual: " + System.currentTimeMillis() + "\n" + info.getInfo() + "\n\n" + locationListener.getMyLocation() + "\n----------------\n";//calendario.get(Calendar.HOUR_OF_DAY) + ":" + calendario.get(Calendar.MINUTE) + ":" + calendario.get(Calendar.SECOND) + "," + calendario.get(Calendar.MILLISECOND)
                                myClient = new Client(ip, porta, aux);//Envia somente os dados atuais.
                                myClient.execute();

                            }

                            intente = new Intent("com.example.patrick.ALERTA_HOME");//Enviamos os dados obtidos para os outros aplicativos interessados.
                            intente.putExtra("HOME", true);
                            intente.putExtra("DADOS", aux);
                            sendBroadcast(intente);


                        } else {//Se nao houver condições de enviar ao servidor, guarde os dados num arquivo.

                            Log.v("HOMEinfo", "NÃO ESTÁ NA HOME");

                            aux = "\n\nTempo atual: " + System.currentTimeMillis() + "\n" + info.getInfo() + "\n\n" + locationListener.getMyLocation() + "\n----------------\n";

                            escritor = new FileWriter(arquivoDados, true);
                            escritor.write(aux);
                            escritor.close();

                            intente = new Intent("com.example.patrick.ALERTA_HOME");//Enviamos os dados obtidos para os outros aplicativos interessados.
                            intente.putExtra("HOME", false);
                            intente.putExtra("DADOS", aux);
                            sendBroadcast(intente);

                        }


//=========================DADOS PARA A REGRESSÃO LINEAR========================================================================================================================================================"



                        Log.v("MMQ", "Verificando se está descarregando.");
                        if(info.getStatusString().equals("Discharging")) {//Só coleta pontos para a regressão de consumo se a bateria estiver sendo usada como alimentação.
                            Log.v("MMQ", "Escrevendo mais um ponto de amostragem nos arquivos vetores.");

                            arquivoTensaoInicial.createNewFile();
                            checkpoint_Tensao = new BufferedReader(new FileReader(arquivoTensaoInicial));
                            arquivoEixoY.createNewFile();//Garantindo que o arquivo existe.
                            escritor = new FileWriter(arquivoEixoY, true);
                            escritor.write("" + ( 100 -Integer.parseInt(checkpoint_Tensao.readLine()) + info.getLevel()) + "\n");//Estamos normalizando o level de bateria no momento de retirada do carregador no valor máximo para facilitar a regressão.
                            escritor.close();//Se naõ entender a equação em cima pense: Uma regressão linear fica mais fácil se sua função sempre parte do mesmo ponto, não é? Assim o que deve mudar de um dia pro outro é só a inclinação da queda de tensão.
                            checkpoint_Tensao.close();

                            arquivoTempoInicial.createNewFile();
                            checkpoint_Tensao = new BufferedReader(new FileReader(arquivoTempoInicial));
                            arquivoEixoX.createNewFile();//Garantindo que o arquivo existe.
                            escritor = new FileWriter(arquivoEixoX, true);
                            escritor.write("" + (System.currentTimeMillis() -Long.parseLong(checkpoint_Tensao.readLine()) + "\n"));//Esses -10800000 são para converter o fuso horário para o horário de brasília.
                            escritor.close();//Este é para que cada regressão inicie onsiderando que o momento dedesconexão do carregador é o momento inicial (zero) da regressão.
                            checkpoint_Tensao.close();

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

//==================================FIM DA OBTENÇÃO===============================================================================================================================================

                    Log.v("ALERTA","Desligando Sensores");
                    desligaSensores();//Desliga os sensores para economizar energia até a próxima chamada do serviço.
                    contador = 0;//Reiniciamos o contador de amostragem.

                    if(modo_Desempenho.equals("desligado")){
                        alarm.set(AlarmManager.RTC, System.currentTimeMillis() + (600000), pendingIntent );//No modo de desligado (amostragem), colete dados a cada 10 minutos.
                    }else if(modo_Desempenho.equals("economia")) {
                        alarm.set(AlarmManager.RTC, System.currentTimeMillis() + (1*3600000), pendingIntent );//No modo de economia, colete dados a cada 1 horas.
                    }else{
                        alarm.set(AlarmManager.RTC, System.currentTimeMillis() + (600000), pendingIntent);//No modo de desempenho, colete dados a cada 10 minutos.
                    }
                    handler.removeCallbacks(this);

                }else{//Caso o contador tenha estourado, os dados obtidos nao estão adequados e vamos apenas reagendar o serviço para daqui a um bom tempo.
                    desligaSensores();
                    contador = 0;//Reiniciamos o contador de amostragem.

                    if(modo_Desempenho.equals("desligado")){
                        alarm.set(AlarmManager.RTC, System.currentTimeMillis() + (8*3600000), pendingIntent );//No modo de economia, colete dados a cada 8 horas.
                    }else if(modo_Desempenho.equals("economia")) {
                        alarm.set(AlarmManager.RTC, System.currentTimeMillis() + (2*3600000), pendingIntent );//No modo de economia, colete dados a cada 2 horas.
                    }else{
                        alarm.set(AlarmManager.RTC, System.currentTimeMillis() + (1800000), pendingIntent);//No modo de desempenho, colete dados a cada meia hora o arqivo.
                    }
                    handler.removeCallbacks(this);


                }
            }
        };

        handler.post(runnableCode);

        return START_STICKY;
    }

    private void encaminhaDadoDepoisDeObter(){
        return;
    }

    private void desligaSensores(){//Este métoddo permite ao celular desligar os sensores e GPS para poupar energia.
        if(locationListener != null)locationListener.removeListener();//Deixa de requisitar atualizações ao sistema e remove este listener. Economiza energia.
        if(info != null) info.onDestroy();//Deixa de requisitar atualizações ao sistema e remove os listener. Economiza energia e evita relatório de erros.
        registrouAlertas = false;//O alerta de posição GPS agr nao estará mais registrado.
        locationListener = new Localizador(this);//Se ocorrer erro no unregisterReceiver de "removeListener", precisaremos de um novo objeto desta classe.
    }

    @Override
    public void onDestroy() {

        alarm.cancel(PendingIntent.getBroadcast(this, 0, new Intent("com.example.patrick.START_SERVICOCOLETA_DADOS"),  PendingIntent.FLAG_UPDATE_CURRENT));//Cancela quaisquer aagendamentos de chamadas para este serviço.
        if(locationListener != null)locationListener.removeListener();//Deixa de requisitar atualizações ao sistema e remove este listener. Economiza energia.
        if(info != null) info.onDestroy();//Deixa de requisitar atualizações ao sistema e remove os listener. Economiza energia e evita relatório de erros.

        Toast.makeText(this, "Service Destroyed", LENGTH_LONG).show();//Exibe na tela que o serviço foi detruído.
        handler.removeCallbacks(runnableCode);//Retira todas as chamadas agendadas deste serviço.
        super.onDestroy();

    }

}
