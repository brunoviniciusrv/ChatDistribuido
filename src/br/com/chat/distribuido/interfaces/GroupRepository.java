package br.com.chat.distribuido.interfaces;

import br.com.chat.distribuido.model.ChatGroup;
import java.util.Collection;
import java.util.Optional;

/**
 * Define o contrato para operações de armazenamento de grupos de chat.
 */
public interface GroupRepository {
    ChatGroup createGroup(String groupName, String ownerUsername);
    Optional<ChatGroup> findGroupById(int groupId);
    Collection<ChatGroup> getAllGroups();
    boolean addUserToGroup(String username, int groupId);
    boolean removeUserFromGroup(String username, int groupId);
}