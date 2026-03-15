package ca.jusjoken.data.service;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.jusjoken.data.entity.UserUiSettings;

public interface UserUiSettingsRepository extends JpaRepository<UserUiSettings, Integer> {

    Optional<UserUiSettings> findByUsername(String username);
}