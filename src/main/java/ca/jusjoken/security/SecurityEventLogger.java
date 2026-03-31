package ca.jusjoken.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;

/**
 * Listens to Spring Security authentication and session events to provide visibility
 * into why users are being redirected to the login screen.
 *
 * Log level is controlled by: logging.level.ca.jusjoken.security=DEBUG (or WARN for less noise).
 * Authentication failures are always logged at WARN; successes at DEBUG.
 */
@Component
public class SecurityEventLogger {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventLogger.class);

    @EventListener
    public void onAuthSuccess(AuthenticationSuccessEvent event) {
        log.debug("Auth success: user='{}' via '{}'",
                event.getAuthentication().getName(),
                event.getAuthentication().getClass().getSimpleName());
    }

    @EventListener
    public void onAuthFailure(AbstractAuthenticationFailureEvent event) {
        log.warn("Auth failure: principal='{}' mechanism='{}' reason='{}'",
                event.getAuthentication().getName(),
                event.getAuthentication().getClass().getSimpleName(),
                event.getException().getMessage());
    }

    @EventListener
    public void onSessionDestroyed(HttpSessionDestroyedEvent event) {
        String users = event.getSecurityContexts().stream()
                .filter(ctx -> ctx.getAuthentication() != null)
                .map(ctx -> ctx.getAuthentication().getName())
                .reduce("", (a, b) -> a.isBlank() ? b : a + ", " + b);
        log.warn("HTTP session destroyed: id='{}' authenticated-user='{}'",
                event.getId(), users.isBlank() ? "none/unauthenticated" : users);
    }
}
