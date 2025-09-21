package br.com.chat.distribuido.repositories;

import br.com.chat.distribuido.interfaces.GroupRepository;
import br.com.chat.distribuido.model.ChatGroup;
import java.util.Collection;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementação em memória do GroupRepository usando um ConcurrentHashMap.
 */
public class InMemoryGroupRepository implements GroupRepository {
    private final Map<Integer, ChatGroup> groups = new ConcurrentHashMap<>();
    private final AtomicInteger nextGroupId = new AtomicInteger(1);

    @Override
    public ChatGroup createGroup(String groupName, String ownerUsername) {
        int id = nextGroupId.getAndIncrement();
        ChatGroup newGroup = new ChatGroup(id, groupName);
        newGroup.addMember(ownerUsername);
        groups.put(id, newGroup);
        return newGroup;
    }

    @Override
    public Optional<ChatGroup> findGroupById(int groupId) {
        return Optional.ofNullable(groups.get(groupId));
    }

    @Override
    public Collection<ChatGroup> getAllGroups() {
        return groups.values();
    }

    @Override
    public boolean addUserToGroup(String username, int groupId) {
        return findGroupById(groupId).map(group -> {
            group.addMember(username);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean removeUserFromGroup(String username, int groupId) {
        return findGroupById(groupId).map(group -> {
            group.removeMember(username);
            return true;
        }).orElse(false);
    }
}