package ca.jusjoken.security;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.jusjoken.data.entity.AppUser;
import ca.jusjoken.data.entity.AppUserRole;
import ca.jusjoken.data.service.AppUserRepository;
import jakarta.annotation.PostConstruct;

@Service
public class SecurityBootstrapService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.bootstrap-admin.username:}")
    private String bootstrapAdminUsername;

    @Value("${app.security.bootstrap-admin.password:}")
    private String bootstrapAdminPassword;

    public SecurityBootstrapService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void bootstrapAdminUserIfNeeded() {
        if (appUserRepository.count() > 0) {
            return;
        }

        if (bootstrapAdminUsername == null || bootstrapAdminUsername.isBlank()
                || bootstrapAdminPassword == null || bootstrapAdminPassword.isBlank()) {
            return;
        }

        AppUser admin = new AppUser();
        admin.setUsername(bootstrapAdminUsername.trim());
        admin.setPasswordHash(passwordEncoder.encode(bootstrapAdminPassword));
        admin.setEnabled(true);

        Set<AppUserRole> roles = new HashSet<>();
        roles.add(AppUserRole.ADMIN);
        roles.add(AppUserRole.USER);
        admin.setRoles(roles);

        appUserRepository.save(admin);
    }
}
