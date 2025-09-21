package br.com.chat.distribuido.p2p;

import br.com.chat.distribuido.model.ChatMessage;
import br.com.chat.distribuido.model.FileMessage;
import java.io.*;
import java.net.Socket;
import java.nio.file.*;

public class PeerHandler extends Thread {
    private final Socket peerSocket;

    public PeerHandler(Socket peerSocket) {
        this.peerSocket = peerSocket;
    }

    @Override
    public void run() {
        try (
                ObjectInputStream in = new ObjectInputStream(peerSocket.getInputStream())
        ) {
            Object receivedObject;
            while ((receivedObject = in.readObject()) != null) {
                if (receivedObject instanceof ChatMessage) {
                    handleChatMessage((ChatMessage) receivedObject);
                } else if (receivedObject instanceof FileMessage) {
                    handleFileMessage((FileMessage) receivedObject);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("\n[P2P] Conexão com peer " + peerSocket.getRemoteSocketAddress() + " perdida.");
        } finally {
            try {
                peerSocket.close();
            } catch (IOException e) { /* Ignorar */ }
        }
    }

    private void handleChatMessage(ChatMessage msg) {
        if (msg.isGroupMessage()) {
            System.out.println("\n[" + msg.getGroupName() + "] " + msg.getSender() + ": " + msg.getContent());
        } else {
            System.out.println("\n[Privado] " + msg.getSender() + ": " + msg.getContent());
        }
        System.out.print("> ");
    }

    private void handleFileMessage(FileMessage fileMsg) {
        Path downloadPath = Paths.get("downloads_p2p");
        try {
            if (!Files.exists(downloadPath)) {
                Files.createDirectories(downloadPath);
            }
            Path filePath = downloadPath.resolve(fileMsg.getFilename());
            Files.write(filePath, fileMsg.getFileContent());
            System.out.println("\n--- NOVO ARQUIVO RECEBIDO P2P ---");
            System.out.println("  De: " + fileMsg.getSender());
            System.out.println("  Nome: " + fileMsg.getFilename());
            System.out.println("  Salvo em: " + filePath.toAbsolutePath());
            System.out.println("---------------------------------");
            System.out.print("> ");
        } catch (IOException e) {
            System.out.println("\n[ERRO] Falha ao salvar arquivo recebido de " + fileMsg.getSender() + ": " + e.getMessage());
        }
    }
}