package ca.jusjoken.data.service;

import org.springframework.stereotype.Service;

import ca.jusjoken.data.entity.AppSettings;

@Service
public class AppSettingsService {

    private final AppSettingsRepository appSettingsRepository;

    public AppSettingsService(AppSettingsRepository appSettingsRepository) {
        this.appSettingsRepository = appSettingsRepository;
    }

    public AppSettings getAppSettings() {
        return appSettingsRepository.findAll().stream().findFirst().orElseGet(() -> {
            AppSettings defaultSettings = new AppSettings();
            return appSettingsRepository.save(defaultSettings);
        });
    }
}
