package br.com.chat.distribuido;

import br.com.chat.distribuido.interfaces.*;
import br.com.chat.distribuido.model.*;
import br.com.chat.distribuido.repositories.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Servidor de Descoberta flexível, desacoplado das implementações de armazenamento.
 */
public class DiscoveryServer {
    private final int port;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public DiscoveryServer(int port, UserRepository userRepository, GroupRepository groupRepository) {
        this.port = port;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    public void start() {
        System.out.println("Servidor de Descoberta Flexível iniciado na porta " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket, this).start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }

    // --- Lógica de Negócio / Orquestração ---

    public boolean registerUser(UserInfo userInfo) {
        boolean success = userRepository.addUser(userInfo);
        if (success) {
            broadcastMessage("USER_ONLINE::" + userInfo.getUsername());
        }
        return success;
    }

    public void removeUser(String username) {
        if (username == null) return;

        userRepository.removeUser(username).ifPresent(userInfo -> {
            groupRepository.getAllGroups().forEach(group ->
                    groupRepository.removeUserFromGroup(username, group.getId())
            );
            broadcastMessage("USER_OFFLINE::" + username);
            System.out.println("Usuário removido: " + username);
        });
    }

    public ChatGroup createGroup(String groupName, String ownerUsername) {
        return groupRepository.createGroup(groupName, ownerUsername);
    }

    public void addUserToGroup(String username, int groupId) {
        boolean success = groupRepository.addUserToGroup(username, groupId);
        userRepository.findUser(username).ifPresent(user -> {
            if (success) {
                groupRepository.findGroupById(groupId).ifPresent(group ->
                        user.sendMessage("INFO::Você entrou no grupo: " + group.getName())
                );
            } else {
                user.sendMessage("ERROR::Grupo com ID " + groupId + " não encontrado.");
            }
        });
    }

    public void removeUserFromGroup(String username, int groupId) {
        groupRepository.removeUserFromGroup(username, groupId);
        userRepository.findUser(username).ifPresent(user ->
                groupRepository.findGroupById(groupId).ifPresent(group ->
                        user.sendMessage("INFO::Você saiu do grupo: " + group.getName())
                )
        );
    }

    public void broadcastMessage(String message) {
        for (UserInfo user : userRepository.getAllUsers()) {
            user.sendMessage(message);
        }
    }

    // --- Métodos de Consulta (para o ClientHandler) ---

    public String getOnlineUsersAsString() {
        return userRepository.getAllUsers().stream()
                .map(UserInfo::getUsername)
                .collect(Collectors.joining(","));
    }

    public String getAvailableGroupsAsString() {
        return groupRepository.getAllGroups().stream()
                .map(ChatGroup::toString)
                .collect(Collectors.joining(";"));
    }

    public String getGroupMembersAsString(int groupId) {
        return groupRepository.findGroupById(groupId)
                .map(group -> group.getMembers().stream()
                        .map(username -> userRepository.findUser(username).orElse(null))
                        .filter(Objects::nonNull)
                        .map(UserInfo::toString)
                        .collect(Collectors.joining(","))
                ).orElse("");
    }

    // --- Getters para os repositórios ---
    public UserRepository getUserRepository() { return userRepository; }
    public GroupRepository getGroupRepository() { return groupRepository; }

    // --- Ponto de Entrada ---
    public static void main(String[] args) {
        int port = 1600;

        UserRepository userRepo = new InMemoryUserRepository();
        GroupRepository groupRepo = new InMemoryGroupRepository();

        new DiscoveryServer(port, userRepo, groupRepo).start();
    }
}