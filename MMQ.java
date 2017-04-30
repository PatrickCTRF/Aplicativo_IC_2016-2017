package com.example.patrick.servico_principal;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by patrick on 30/03/17.
 *
 * Esta classe serve apenas para realizar a regressão(liner, no caso) de dados passados para esta.
 */

public class MMQ {

    File arquivoEixoX = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Eixo_X_Tempo.txt");
    File arquivoEixoY = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Eixo_Y_Bateria.txt");

    BufferedReader eixoX;
    BufferedReader eixoY;

    ArrayList<Double> arrayX;
    ArrayList<Double> arrayY;

    //Parâmetros do MMQ
    double somatorioXY = 0, mediaX = 0, mediaY = 0, somatorioquadX = 0, somatorioX = 0, somatorioY = 0;
    int n = 0;
    double b = 0, a = 0;// supondo y = bx + a;

    String aux;

    private void cria_array_dados_eixos(){

        arrayX =  new ArrayList<Double>();
        arrayY =  new ArrayList<Double>();

        try {

            eixoX = new BufferedReader(new FileReader(arquivoEixoX));
            eixoY = new BufferedReader(new FileReader(arquivoEixoY));

            while((aux = eixoX.readLine()) != null && !aux.equals("")){//Leia tudo que está no arquivo e que seja válido.
                arrayX.add(new Double(aux));
            }

            while((aux = eixoY.readLine()) != null && !aux.equals("")){//Leia tudo que está no arquivo e que seja válido.
                arrayY.add(new Double(aux));
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void calcula_MMQ(){
        cria_array_dados_eixos();//Esta linha tem que vir primeiro!

        somatorioXY = 0; mediaX = 0; mediaY = 0; somatorioquadX = 0; somatorioX = 0; somatorioY = 0;
        n = arrayX.size();

        b = 0; a = 0;// supondo y = bx + a;

        for (int x=0; x<n; x++) {
            somatorioX += arrayX.get(x).doubleValue();
            somatorioquadX += arrayX.get(x).doubleValue()*arrayX.get(x).doubleValue();
            somatorioXY += arrayX.get(x).doubleValue()*arrayY.get(x).doubleValue();
            somatorioY += arrayY.get(x).doubleValue();
        }

        mediaX = somatorioX/n;
        mediaY = somatorioY/n;

        b = (n*somatorioXY-somatorioX*somatorioY)/(n*somatorioquadX-somatorioX*somatorioX);
        a = mediaY - b*mediaX;

    }

    public double getB() {
        if(arquivoEixoX.exists() && arquivoEixoY.exists()) {//Só realiza as operações se os arquivos existirem.
            calcula_MMQ();
            return b;
        }
        return 888888;//Indica erro pois este valor sempre será negativo para valores de descarga.
    }

    public double getA() {
        if(arquivoEixoX.exists() && arquivoEixoY.exists()) {//Só realiza as operações se os arquivos existirem.
            calcula_MMQ();
            return a;
        }
        return -88888;//Indicação de erro, pois este valor nao deveria poder ser negativo numa regressão de bateria a nao ser q ela já começasse com carga inferior a zero.
    }
}
