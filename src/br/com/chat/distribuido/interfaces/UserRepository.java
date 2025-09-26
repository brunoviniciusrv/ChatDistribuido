package br.com.chat.distribuido.interfaces;

import br.com.chat.distribuido.model.UserInfo;
import java.util.Collection;
import java.util.Optional;

/**
 * Define o contrato para operações de armazenamento de usuários.
 */
public interface UserRepository {
    boolean addUser(UserInfo user);
    Optional<UserInfo> removeUser(String username);
    Optional<UserInfo> findUser(String username);
    Collection<UserInfo> getAllUsers();
    boolean isIpPortInUse(String ip, int p2pPort);
}