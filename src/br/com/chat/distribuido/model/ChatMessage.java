package br.com.chat.distribuido.model;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L; // Boa prática para serialização
    private final String sender;
    private final String content;
    private final String groupName; // Opcional, para identificar mensagens de grupo

    // Construtor para mensagens privadas
    public ChatMessage(String sender, String content) {
        this.sender = sender;
        this.content = content;
        this.groupName = null;
    }

    // Construtor para mensagens de grupo
    public ChatMessage(String sender, String content, String groupName) {
        this.sender = sender;
        this.content = content;
        this.groupName = groupName;
    }

    public String getSender() { return sender; }
    public String getContent() { return content; }
    public String getGroupName() { return groupName; }
    public boolean isGroupMessage() { return groupName != null; }
}