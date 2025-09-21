package br.com.chat.distribuido;

import br.com.chat.distribuido.model.ChatGroup;
import br.com.chat.distribuido.model.UserInfo;
import java.io.*;
import java.net.Socket;
import java.util.stream.Collectors;

/**
 * Gere a comunicação com um único cliente em uma thread dedicada.
 */
public class ClientHandler extends Thread {
    private final Socket socket;
    private final DiscoveryServer server;
    private PrintWriter out;
    private BufferedReader in;
    private UserInfo currentUser;

    public ClientHandler(Socket socket, DiscoveryServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String command;
            while ((command = in.readLine()) != null) {
                handleCommand(command);
            }

        } catch (IOException e) {
            System.out.println("Cliente desconectado: " + (currentUser != null ? currentUser.getUsername() : socket.getRemoteSocketAddress()));
        } finally {
            if (currentUser != null) {
                server.removeUser(currentUser.getUsername());
            }
            try {
                socket.close();
            } catch (IOException e) { /* Ignorar */ }
        }
    }

    private void handleCommand(String command) {
        String[] parts = command.split("::");
        String action = parts[0].toUpperCase();

        switch (action) {
            case "LOGIN":
                if (parts.length == 3) {
                    handleLogin(parts[1], Integer.parseInt(parts[2]));
                }
                break;

            case "LIST_USERS":
                sendMessage("USER_LIST::" + server.getOnlineUsersAsString());
                break;

            case "LIST_GROUPS":
                sendMessage("GROUP_LIST::" + server.getAvailableGroupsAsString());
                break;

            case "GET_USER_ADDRESS":
                if (parts.length == 2) {
                    server.getUserRepository().findUser(parts[1])
                            .ifPresentOrElse(
                                    target -> sendMessage("USER_ADDRESS::" + target),
                                    () -> sendMessage("ERROR::Usuário não encontrado.")
                            );
                }
                break;

            case "CREATE_GROUP":
                if (parts.length == 2 && currentUser != null) {
                    ChatGroup newGroup = server.createGroup(parts[1], currentUser.getUsername());
                    sendMessage("GROUP_CREATED::" + newGroup.getId() + "::" + newGroup.getName());
                    System.out.println("Usuário " + currentUser.getUsername() + " criou o grupo: " + newGroup.getName());
                }
                break;

            case "JOIN_GROUP":
                if (parts.length == 2 && currentUser != null) {
                    server.addUserToGroup(currentUser.getUsername(), Integer.parseInt(parts[1]));
                }
                break;

            case "LEAVE_GROUP":
                if (parts.length == 2 && currentUser != null) {
                    server.removeUserFromGroup(currentUser.getUsername(), Integer.parseInt(parts[1]));
                }
                break;

            case "GET_GROUP_MEMBERS":
                if (parts.length == 2) {
                    sendMessage("GROUP_MEMBERS::" + parts[1] + "::" + server.getGroupMembersAsString(Integer.parseInt(parts[1])));
                }
                break;

            case "LOGOUT":
                try { socket.close(); } catch (IOException e) {}
                break;

            default:
                sendMessage("ERROR::Comando desconhecido.");
                break;
        }
    }

    private void handleLogin(String username, int p2pPort) {
        String ip = socket.getInetAddress().getHostAddress();
        this.currentUser = new UserInfo(username, ip, p2pPort, this);
        boolean success = server.registerUser(this.currentUser);
        if (!success) {
            sendMessage("ERROR::Nome de usuário já em uso.");
            this.currentUser = null;
        } else {
            sendMessage("INFO::Login bem-sucedido.");
            System.out.println("Novo usuário logado: " + currentUser);
        }
    }

    public void sendMessage(String message) {
        if (!socket.isClosed()) {
            out.println(message);
        }
    }
}