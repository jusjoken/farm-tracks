package ca.jusjoken.security;

import java.util.Arrays;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Custom persistent remember-me service that fixes a race condition in the default Spring
 * Security implementation.
 *
 * The default {@link PersistentTokenBasedRememberMeServices} deletes ALL remember-me tokens
 * for a user when a token-series mismatch is detected, treating it as a cookie-theft attack.
 * In a Vaadin application with push connections and polling, multiple concurrent HTTP requests
 * can arrive simultaneously when a session becomes invalid.  The first request rotates the
 * token successfully; any concurrent request still carrying the old (now-stale) cookie
 * triggers the CookieTheftException, wiping all tokens and forcing a full re-login on every
 * device — even though no actual theft occurred.
 *
 * This override removes only the specific stale series on a mismatch instead of purging all
 * user tokens, so other active sessions / devices are not affected.
 */
public class FarmTracksRememberMeServices extends PersistentTokenBasedRememberMeServices {

    private static final Logger log = LoggerFactory.getLogger(FarmTracksRememberMeServices.class);

    private final PersistentTokenRepository tokenRepo;
    private final JdbcTemplate jdbc;
    /**
     * If a token mismatch is detected but the stored token was rotated within this window
     * (in milliseconds), assume a Vaadin concurrent-request race condition rather than actual
     * cookie theft.  The concurrent request is allowed to authenticate using the already-rotated
     * token value without triggering another rotation.
     */
    private static final long CONCURRENT_RACE_WINDOW_MS = 10_000L;


    public FarmTracksRememberMeServices(String key, UserDetailsService userDetailsService,
            PersistentTokenRepository tokenRepository, JdbcTemplate jdbcTemplate) {
        super(key, userDetailsService, tokenRepository);
        this.tokenRepo = tokenRepository;
        this.jdbc = jdbcTemplate;
        setTokenValiditySeconds(60 * 60 * 24 * 30); // 30 days
        setAlwaysRemember(true);
    }

    @Override
    protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request,
            HttpServletResponse response) {

        if (cookieTokens.length != 2) {
            throw new InvalidCookieException(
                    "Cookie token did not contain 2 tokens, but contained '" + Arrays.asList(cookieTokens) + "'");
        }

        String presentedSeries = cookieTokens[0];
        String presentedToken = cookieTokens[1];

        PersistentRememberMeToken token = tokenRepo.getTokenForSeries(presentedSeries);

        if (token == null) {
            throw new RememberMeAuthenticationException(
                    "No persistent token found for series id: " + presentedSeries);
        }

        if (!presentedToken.equals(token.getTokenValue())) {
                // Token mismatch.  Two possible causes:
                //
                // (A) Vaadin concurrent-request race: multiple HTTP requests (push, heartbeat,
                //     resource fetches) arrive simultaneously after session expiry.  Request #1
                //     rotates the token; #2, #3, … present the now-stale cookie but arrive within
                //     milliseconds.  Detect this by checking how recently the stored token was
                //     rotated.  If within the race window, authenticate with the current stored
                //     token (no further rotation) and update the browser cookie — no re-login needed.
                //
                // (B) Genuine stale/stolen cookie (hours/days old): remove only THIS series
                //     (not all user tokens so other devices are unaffected) and force re-login.
                long tokenAgeMs = System.currentTimeMillis() - token.getDate().getTime();
                if (tokenAgeMs <= CONCURRENT_RACE_WINDOW_MS) {
                log.debug("Remember-me concurrent-request race detected: user='{}' series='{}' "
                    + "token rotated {}ms ago via request='{}' — authenticating with current "
                    + "stored token, updating browser cookie (no extra rotation).",
                    token.getUsername(), presentedSeries, tokenAgeMs, request.getRequestURI());
                // Give the browser the already-rotated token so future requests work correctly.
                setCookie(new String[] { token.getSeries(), token.getTokenValue() },
                    getTokenValiditySeconds(), request, response);
                return getUserDetailsService().loadUserByUsername(token.getUsername());
                }
                log.warn("Remember-me token mismatch: user='{}' series='{}' request='{}' "
                    + "token is {}ms old — removing stale series only (not purging all user tokens).",
                    token.getUsername(), presentedSeries, request.getRequestURI(), tokenAgeMs);
                jdbc.update("DELETE FROM persistent_logins WHERE series = ?", presentedSeries);
                cancelCookie(request, response);
                throw new RememberMeAuthenticationException(
                    "Remember-me token mismatch for series '" + presentedSeries + "' — re-login required");
        }

        if (token.getDate().getTime() + getTokenValiditySeconds() * 1000L < System.currentTimeMillis()) {
            log.info("Remember-me token expired for user='{}'", token.getUsername());
            throw new RememberMeAuthenticationException("Remember-me login has expired");
        }

        log.debug("Refreshing persistent login token for user='{}' series='{}'",
                token.getUsername(), token.getSeries());

        PersistentRememberMeToken newToken = new PersistentRememberMeToken(
                token.getUsername(), token.getSeries(), generateTokenData(), new Date());
        try {
            tokenRepo.updateToken(newToken.getSeries(), newToken.getTokenValue(), newToken.getDate());
            // mirrors the private addCookie() method in PersistentTokenBasedRememberMeServices
            setCookie(new String[] { newToken.getSeries(), newToken.getTokenValue() },
                    getTokenValiditySeconds(), request, response);
        } catch (Exception ex) {
            log.error("Failed to update remember-me token for user='{}'", token.getUsername(), ex);
            throw new RememberMeAuthenticationException("Autologin failed due to data access problem");
        }

        return getUserDetailsService().loadUserByUsername(token.getUsername());
    }
}
