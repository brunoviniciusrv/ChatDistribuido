package br.com.chat.distribuido.model;

import br.com.chat.distribuido.ClientHandler;

/**
 * Classe de modelo para armazenar informações de um usuário conectado.
 */
public class UserInfo {
    private final String username;
    private final String ipAddress;
    private final int p2pPort;
    private final ClientHandler handler;

    public UserInfo(String username, String ipAddress, int p2pPort, ClientHandler handler) {
        this.username = username;
        this.ipAddress = ipAddress;
        this.p2pPort = p2pPort;
        this.handler = handler;
    }

    public String getUsername() { return username; }
    public String getIpAddress() { return ipAddress; }
    public int getP2pPort() { return p2pPort; }

    public void sendMessage(String message) {
        handler.sendMessage(message);
    }

    @Override
    public String toString() {
        return username + ":" + ipAddress + ":" + p2pPort;
    }
}