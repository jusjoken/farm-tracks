package ca.jusjoken.data.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.jusjoken.data.entity.AppUser;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AppUser> findAll() {
        return appUserRepository.findAll();
    }

    public Optional<AppUser> findByUsername(String username) {
        return appUserRepository.findByUsername(username);
    }

    /**
     * Save a user. If plainPassword is non-blank, encode and set it.
     * For new users plainPassword is required; for existing users it can be blank
     * to keep the current hash.
     */
    @Transactional
    public AppUser save(AppUser user, String plainPassword) {
        if (plainPassword != null && !plainPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(plainPassword));
        }
        return appUserRepository.save(user);
    }

    @Transactional
    public void delete(AppUser user) {
        appUserRepository.delete(user);
    }

    public boolean usernameExists(String username, Integer excludeId) {
        return appUserRepository.findByUsername(username)
                .filter(u -> !u.getId().equals(excludeId))
                .isPresent();
    }
}
