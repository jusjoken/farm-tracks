package ca.jusjoken.data.service;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.jusjoken.data.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Integer> {
    Optional<AppUser> findByUsername(String username);
}
