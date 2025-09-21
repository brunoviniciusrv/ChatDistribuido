package br.com.chat.distribuido.p2p;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class PeerServer extends Thread {
    private final int p2pPort;
    private ServerSocket serverSocket;

    public PeerServer(int p2pPort) {
        this.p2pPort = p2pPort;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(p2pPort);
            System.out.println("[P2P] Servidor pessoal iniciado. Escutando por peers na porta " + p2pPort);
            while (!Thread.currentThread().isInterrupted()) {
                Socket peerSocket = serverSocket.accept();
                // Para cada peer que se conecta, cria uma nova thread para lidar com ele
                new PeerHandler(peerSocket).start();
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                System.out.println("[P2P ERRO] Não foi possível escutar na porta " + p2pPort + ": " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        interrupt();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) { /* Ignorar */ }
    }
}