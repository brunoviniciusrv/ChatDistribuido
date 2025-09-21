package br.com.chat.distribuido.server;

import br.com.chat.distribuido.P2PClient;
import java.io.*;
import java.net.Socket;

public class ServerConnection extends Thread {
    private final P2PClient client;
    private final String serverAddress;
    private final int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public ServerConnection(String serverAddress, int serverPort, P2PClient client) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.client = client;
    }

    public boolean connect() {
        try {
            socket = new Socket(serverAddress, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.start(); // Inicia a thread de escuta do servidor
            return true;
        } catch (IOException e) {
            System.out.println("[ERRO] Não foi possível conectar ao Servidor de Descoberta: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void run() {
        try {
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                // Passa a mensagem para o cliente principal processar
                client.handleServerMessage(serverMessage);
            }
        } catch (IOException e) {
            System.out.println("\n[INFO] Conexão com o Servidor de Descoberta perdida.");
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void disconnect() {
        interrupt();
        try {
            if (socket != null && !socket.isClosed()) {
                sendMessage("LOGOUT");
                socket.close();
            }
        } catch (IOException e) { /* Ignorar */ }
    }
}