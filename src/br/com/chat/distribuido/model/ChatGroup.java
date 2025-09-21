package br.com.chat.distribuido.model;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classe de modelo para um grupo de chat.
 */
public class ChatGroup {
    private final int id;
    private final String name;
    private final Set<String> members = ConcurrentHashMap.newKeySet();

    public ChatGroup(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Set<String> getMembers() { return members; }

    public void addMember(String username) {
        members.add(username);
    }

    public void removeMember(String username) {
        members.remove(username);
    }

    @Override
    public String toString() {
        return id + "," + name + "," + members.size();
    }
}