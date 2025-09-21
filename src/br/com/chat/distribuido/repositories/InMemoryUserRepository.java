package br.com.chat.distribuido.repositories;

import br.com.chat.distribuido.interfaces.UserRepository;
import br.com.chat.distribuido.model.UserInfo;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Implementação em memória do UserRepository usando um ConcurrentHashMap.
 */
public class InMemoryUserRepository implements UserRepository {
    private final Map<String, UserInfo> users = new ConcurrentHashMap<>();

    @Override
    public boolean addUser(UserInfo user) {
        return users.putIfAbsent(user.getUsername(), user) == null;
    }

    @Override
    public Optional<UserInfo> removeUser(String username) {
        return Optional.ofNullable(users.remove(username));
    }

    @Override
    public Optional<UserInfo> findUser(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public Collection<UserInfo> getAllUsers() {
        return users.values();
    }
}