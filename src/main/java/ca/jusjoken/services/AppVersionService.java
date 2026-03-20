package ca.jusjoken.services;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Service;

@Service
public class AppVersionService {

    private final ObjectProvider<GitProperties> gitPropertiesProvider;
    private final String majorVersion;

    public AppVersionService(ObjectProvider<GitProperties> gitPropertiesProvider,
            @Value("${app.version.major:1}") String majorVersion) {
        this.gitPropertiesProvider = gitPropertiesProvider;
        this.majorVersion = majorVersion;
    }

    public String getDisplayVersion() {
        GitProperties gitProperties = gitPropertiesProvider.getIfAvailable();
        String commitCount = gitProperties != null ? gitProperties.get("total.commit.count") : null;
        if (commitCount == null || commitCount.isBlank()) {
            commitCount = "0";
        }
        return majorVersion + "." + commitCount;
    }
}
