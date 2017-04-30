package com.example.patrick.servico_principal;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.FileWriter;

/**
 * Esta classe é para ser rodada no PC, funcionando como o nosso servidor.
 */

class TCPServer {
    public static void main(String argv[]) throws Exception
    {
        String clientSentence = "", aux;
        String capitalizedSentence;
        ServerSocket welcomeSocket = new ServerSocket(6789);
        FileWriter escritor;

        System.out.println("Servidor Rodando.\n");

        File arquivoDados = new File("/home/patrick/Área de Trabalho" + "/" + "_InformacoesDaVidaDoUsuario.txt");

        while(true)
        {
            clientSentence = "";
            Socket connectionSocket = welcomeSocket.accept();
            BufferedReader inFromClient =
                    new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            OutputStream outToClient = connectionSocket.getOutputStream();
            while(!(aux = inFromClient.readLine()).equals("FIM")){
                clientSentence += aux + "\n";
            }
            System.out.println("Received: " + clientSentence);
            capitalizedSentence = "Eu sou servidor\n";
            escritor = new FileWriter(arquivoDados, true);
            escritor.write(clientSentence);
            escritor.close();
            outToClient.write(capitalizedSentence.getBytes());
        }
    }
}