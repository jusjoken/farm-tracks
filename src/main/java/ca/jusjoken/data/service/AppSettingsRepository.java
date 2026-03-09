package ca.jusjoken.data.service;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.jusjoken.data.entity.AppSettings;

public interface AppSettingsRepository  extends JpaRepository<AppSettings, Integer> {



}
