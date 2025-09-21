package br.com.chat.distribuido;

import br.com.chat.distribuido.model.ChatMessage;
import br.com.chat.distribuido.model.FileMessage;
import br.com.chat.distribuido.p2p.PeerServer;
import br.com.chat.distribuido.server.ServerConnection;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class P2PClient {
    // Configurações
    private final String username;
    private final int p2pPort;

    // Componentes
    private final ServerConnection serverConnection;
    private final PeerServer peerServer;

    // Estado do Cliente (dados pendentes e conexões ativas)
    private final Map<String, Serializable> pendingP2PData = new ConcurrentHashMap<>();
    private final Map<Integer, Serializable> pendingGroupData = new ConcurrentHashMap<>();
    private final Map<String, ObjectOutputStream> peerConnections = new ConcurrentHashMap<>();
    private final Map<Integer, String> knownGroups = new ConcurrentHashMap<>();

    public P2PClient(String serverAddr, int serverPort, String username, int p2pPort) {
        this.username = username;
        this.p2pPort = p2pPort;
        this.serverConnection = new ServerConnection(serverAddr, serverPort, this);
        this.peerServer = new PeerServer(p2pPort);
    }

    public void start() {
        peerServer.start();
        if (serverConnection.connect()) {
            serverConnection.sendMessage("LOGIN::" + username + "::" + p2pPort);
            userInputLoop();
        }
        // Cleanup on exit
        peerServer.shutdown();
        serverConnection.disconnect();
        System.out.println("Cliente encerrado.");
    }

    private void userInputLoop() {
        displayHelp();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String command = scanner.nextLine();
            if (command.isBlank()) continue;
            if (command.equalsIgnoreCase("SAIR")) break;

            handleUserInput(command);
        }
        scanner.close();
    }

    private void handleUserInput(String command) {
        if (command.startsWith("@")) { // Mensagem Privada: @usuario <mensagem>
            String[] parts = command.split(" ", 2);
            if (parts.length == 2) {
                String targetUser = parts[0].substring(1);
                String message = parts[1];
                sendP2PObject(targetUser, new ChatMessage(username, message));
            }
        } else if (command.startsWith("##")) { // Mensagem de Grupo: ##idGrupo <mensagem>
            String[] parts = command.split(" ", 2);
            if (parts.length == 2) {
                try {
                    int groupId = Integer.parseInt(parts[0].substring(2));
                    String message = parts[1];
                    String groupName = knownGroups.getOrDefault(groupId, "Grupo " + groupId);
                    // Coloca a mensagem como pendente e pede a lista de membros
                    pendingGroupData.put(groupId, new ChatMessage(username, message, groupName));
                    serverConnection.sendMessage("GET_GROUP_MEMBERS::" + groupId);
                } catch (NumberFormatException e) {
                    System.out.println("[ERRO] ID de grupo inválido.");
                }
            }
        } else if (command.toUpperCase().startsWith("ENVIAR ")) { // Envio de Arquivo
            handleFileUpload(command);
        } else if (command.equalsIgnoreCase("AJUDA")) {
            displayHelp();
        } else { // Comandos do Servidor
            String[] parts = command.split(" ", 2);
            String action = parts[0].toUpperCase();
            String payload = parts.length > 1 ? parts[1] : "";

            switch (action) {
                case "USUARIOS": serverConnection.sendMessage("LIST_USERS::"); break;
                case "GRUPOS": serverConnection.sendMessage("LIST_GROUPS::"); break;
                case "CRIARGRUPO": serverConnection.sendMessage("CREATE_GROUP::" + payload); break;
                case "ENTRARGRUPO": serverConnection.sendMessage("JOIN_GROUP::" + payload); break;
                case "SAIRGRUPO": serverConnection.sendMessage("LEAVE_GROUP::" + payload); break;
                default: System.out.println("[ERRO] Comando desconhecido. Digite 'AJUDA' para ver a lista."); break;
            }
        }
    }

    private void handleFileUpload(String command) {
        String[] parts = command.split(" ");
        // ENVIAR @usuario /caminho/arquivo.txt
        // ENVIAR ##idGrupo /caminho/arquivo.txt
        if (parts.length < 3) {
            System.out.println("[ERRO] Uso: ENVIAR <@usuario|##idGrupo> /caminho/para/o/arquivo");
            return;
        }

        String target = parts[1];
        String filePath = command.substring(command.indexOf(parts[2]));

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                System.out.println("[ERRO] Arquivo não encontrado: " + filePath);
                return;
            }
            byte[] fileContent = Files.readAllBytes(path);
            FileMessage fileMsg = new FileMessage(username, path.getFileName().toString(), fileContent);

            if (target.startsWith("@")) {
                sendP2PObject(target.substring(1), fileMsg);
            } else if (target.startsWith("##")) {
                int groupId = Integer.parseInt(target.substring(2));
                pendingGroupData.put(groupId, fileMsg);
                serverConnection.sendMessage("GET_GROUP_MEMBERS::" + groupId);
            }
        } catch (IOException e) {
            System.out.println("[ERRO] Falha ao ler o arquivo: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("[ERRO] ID de grupo inválido.");
        }
    }

    // Processa mensagens assíncronas vindas do Servidor de Descoberta
    public void handleServerMessage(String message) {
        System.out.println("\n[SERVIDOR] " + message);
        String[] parts = message.split("::", 2);
        String action = parts[0].toUpperCase();
        String payload = parts.length > 1 ? parts[1] : "";

        switch (action) {
            case "USER_ADDRESS": // username:ip:port
                String[] userInfo = payload.split(":");
                String targetUser = userInfo[0];
                String ip = userInfo[1];
                int port = Integer.parseInt(userInfo[2]);

                Serializable pendingData = pendingP2PData.remove(targetUser);
                if (pendingData != null) {
                    initiateP2PConnection(targetUser, ip, port, pendingData);
                }
                break;

            case "GROUP_MEMBERS": // groupId::user1:ip1:port1,user2:ip2:port2
                String[] groupParts = payload.split("::", 2);
                int groupId = Integer.parseInt(groupParts[0]);
                String membersStr = groupParts.length > 1 ? groupParts[1] : "";

                Serializable pendingGroupItem = pendingGroupData.remove(groupId);
                if (pendingGroupItem != null && !membersStr.isEmpty()) {
                    System.out.println("[P2P] Distribuindo para membros do grupo " + groupId + "...");
                    String[] members = membersStr.split(",");
                    for (String memberInfo : members) {
                        String memberName = memberInfo.split(":")[0];
                        if (!memberName.equals(username)) { // Não envia para si mesmo
                            sendP2PObject(memberName, pendingGroupItem);
                        }
                    }
                }
                break;

            case "GROUP_LIST": // id,name,size;id,name,size
                knownGroups.clear();
                if (!payload.isEmpty()) {
                    String[] groups = payload.split(";");
                    for (String groupInfo : groups) {
                        String[] groupDetails = groupInfo.split(",", 3);
                        knownGroups.put(Integer.parseInt(groupDetails[0]), groupDetails[1]);
                    }
                }
                break;

            case "GROUP_CREATED": // id::name
                String[] createdInfo = payload.split("::", 2);
                knownGroups.put(Integer.parseInt(createdInfo[0]), createdInfo[1]);
                break;
        }
        System.out.print("> ");
    }

    // Inicia a pipeline de envio de um objeto para um peer
    private void sendP2PObject(String targetUsername, Serializable object) {
        try {
            if (peerConnections.containsKey(targetUsername)) {
                // Se já temos uma conexão, usa-a
                ObjectOutputStream oos = peerConnections.get(targetUsername);
                oos.writeObject(object);
                oos.flush();
                System.out.println("[P2P] Mensagem enviada para " + targetUsername + " (conexão existente).");
            } else {
                // Se não, pede o endereço e coloca o dado como pendente
                System.out.println("[P2P] Solicitando endereço de " + targetUsername + " ao servidor...");
                pendingP2PData.put(targetUsername, object);
                serverConnection.sendMessage("GET_USER_ADDRESS::" + targetUsername);
            }
        } catch (IOException e) {
            System.out.println("[P2P ERRO] Falha ao enviar para " + targetUsername + ". Removendo conexão.");
            peerConnections.remove(targetUsername); // Remove conexão quebrada
        }
    }

    private void initiateP2PConnection(String targetUser, String ip, int port, Serializable data) {
        System.out.println("[P2P] Conectando a " + targetUser + " em " + ip + ":" + port);
        try {
            Socket peerSocket = new Socket(ip, port);
            ObjectOutputStream oos = new ObjectOutputStream(peerSocket.getOutputStream());

            // Guarda a conexão para uso futuro
            peerConnections.put(targetUser, oos);
            System.out.println("[P2P] Conexão com " + targetUser + " estabelecida.");

            // Envia o dado que estava pendente
            oos.writeObject(data);
            oos.flush();
            System.out.println("[P2P] Dados pendentes enviados para " + targetUser);

            // ATENÇÃO: Esta implementação não cria um listener de input para a conexão de saída.
            // Para um chat bidirecional completo na mesma conexão, seria necessário um PeerHandler aqui também.
            // Atualmente, a resposta do peer virá por uma conexão separada que ele iniciará.

        } catch (IOException e) {
            System.out.println("[P2P ERRO] Falha ao conectar ou enviar para " + targetUser + ": " + e.getMessage());
            peerConnections.remove(targetUser);
        }
    }

    private void displayHelp() {
        System.out.println("\n--- Comandos do Chat P2P ---");
        System.out.println("  Mensagem Privada: @usuario <sua mensagem>");
        System.out.println("  Mensagem de Grupo:  ##id_do_grupo <sua mensagem>");
        System.out.println("  Enviar Arquivo:     ENVIAR <@usuario|##id_do_grupo> /caminho/completo/do/arquivo");
        System.out.println("  Listar Usuários:    USUARIOS");
        System.out.println("  Listar Grupos:      GRUPOS");
        System.out.println("  Criar Grupo:        CRIARGRUPO <nome do grupo>");
        System.out.println("  Entrar em Grupo:    ENTRARGRUPO <id_do_grupo>");
        System.out.println("  Sair de Grupo:      SAIRGRUPO <id_do_grupo>");
        System.out.println("  Ajuda:              AJUDA");
        System.out.println("  Sair do Chat:       SAIR");
        System.out.println("---------------------------");
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Uso: java br.com.chat.distribuido.P2PClient <server_addr> <server_port> <username> <p2p_port>");
            System.out.println("Exemplo: java br.com.chat.distribuido.P2PClient localhost 1600 ana 9001");
            return;
        }
        String serverAddr = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String username = args[2];
        int p2pPort = Integer.parseInt(args[3]);

        P2PClient client = new P2PClient(serverAddr, serverPort, username, p2pPort);
        client.start();
    }
}