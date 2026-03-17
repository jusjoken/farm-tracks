package ca.jusjoken.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.jusjoken.data.entity.AppUser;
import ca.jusjoken.data.service.AppUserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public DatabaseUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String[] roles = appUser.getRoles().stream()
                .map(Enum::name)
                .toArray(String[]::new);

        if (roles.length == 0) {
            roles = new String[] { "USER" };
        }

        return User.withUsername(appUser.getUsername())
                .password(appUser.getPasswordHash())
                .disabled(!Boolean.TRUE.equals(appUser.getEnabled()))
                .roles(roles)
                .build();
    }
}
