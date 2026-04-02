package ca.jusjoken.security;

import java.util.Arrays;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
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
    private final long concurrentRaceWindowMs;
    private final boolean suppressLoginFailCookieClear;
    /**
     * If a token mismatch is detected but the stored token was rotated within this window
     * (in milliseconds), assume a Vaadin concurrent-request race condition rather than actual
     * cookie theft.  The concurrent request is allowed to authenticate using the already-rotated
     * token value without triggering another rotation.
     */
    public FarmTracksRememberMeServices(String key, UserDetailsService userDetailsService,
            PersistentTokenRepository tokenRepository, JdbcTemplate jdbcTemplate,
            long concurrentRaceWindowMs,
            boolean suppressLoginFailCookieClear) {
        super(key, userDetailsService, tokenRepository);
        this.tokenRepo = tokenRepository;
        this.jdbc = jdbcTemplate;
        this.concurrentRaceWindowMs = Math.max(10_000L, concurrentRaceWindowMs);
        this.suppressLoginFailCookieClear = suppressLoginFailCookieClear;
        setTokenValiditySeconds(60 * 60 * 24 * 30); // 30 days
        setAlwaysRemember(true);
        log.debug("Remember-me service initialized: tokenValiditySeconds={} concurrentRaceWindowMs={} suppressLoginFailCookieClear={}",
            getTokenValiditySeconds(), this.concurrentRaceWindowMs, this.suppressLoginFailCookieClear);
    }

    @Override
    public void loginFail(HttpServletRequest request, HttpServletResponse response) {
        if (suppressLoginFailCookieClear) {
            log.debug("Remember-me loginFail intercepted without cookie clear: {}",
                    buildRequestContext(request));
            return;
        }
        super.loginFail(request, response);
    }

    @Override
    public void loginSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication successfulAuthentication) {
        String username = successfulAuthentication != null ? successfulAuthentication.getName() : "unknown";
        log.debug("Remember-me loginSuccess: issuing canonical cookie for user='{}' {}",
            username, buildRequestContext(request));

        // Use a single controlled issuance path so the browser receives one stable
        // remember-me cookie variant with explicit attributes.
        forceIssuePersistentToken(request, response, successfulAuthentication);
    }

    private void forceIssuePersistentToken(HttpServletRequest request,
            HttpServletResponse response,
            Authentication successfulAuthentication) {
        if (successfulAuthentication == null || successfulAuthentication.getName() == null) {
            log.error("Remember-me manual issuance aborted: missing authentication principal");
            return;
        }

        String username = successfulAuthentication.getName();
        try {
            PersistentRememberMeToken manualToken = new PersistentRememberMeToken(
                    username,
                    generateSeriesData(),
                    generateTokenData(),
                    new Date());

            tokenRepo.createNewToken(manualToken);
            addExplicitRememberMeHeader(manualToken, request, response);

                log.debug("Remember-me manual issuance succeeded for user='{}'", username);
                log.debug("Remember-me manual headers now: '{}'",
                    String.join(" | ", response.getHeaders("Set-Cookie")));
        } catch (Exception ex) {
            log.error("Remember-me manual issuance failed for user='{}'", username, ex);
        }
    }

    private void addExplicitRememberMeHeader(PersistentRememberMeToken token,
            HttpServletRequest request,
            HttpServletResponse response) {
        String encodedValue = encodeCookie(new String[] { token.getSeries(), token.getTokenValue() });
        addExplicitRememberMeHeaderForValue(encodedValue, request, response);
    }

    private void addExplicitRememberMeHeaderForValue(String encodedValue,
            HttpServletRequest request,
            HttpServletResponse response) {
        int maxAge = getTokenValiditySeconds();

        StringBuilder cookie = new StringBuilder();
        cookie.append("remember-me=").append(encodedValue)
                .append("; Max-Age=").append(maxAge)
                .append("; Path=/")
                .append("; HttpOnly")
                .append("; SameSite=Lax");

        if (request.isSecure()) {
            cookie.append("; Secure");
        }

        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void writeRememberMeCookie(String series,
            String tokenValue,
            HttpServletRequest request,
            HttpServletResponse response) {
        String encodedValue = encodeCookie(new String[] { series, tokenValue });
        addExplicitRememberMeHeaderForValue(encodedValue, request, response);
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
            log.warn("Remember-me series not found: series='{}' {}",
                presentedSeries, buildRequestContext(request));
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
                if (tokenAgeMs <= concurrentRaceWindowMs) {
                log.debug("Remember-me concurrent-request race accepted: user='{}' series='{}' "
                    + "tokenAgeMs={} raceWindowMs={} presentedTokenPrefix='{}' storedTokenPrefix='{}' {}",
                    token.getUsername(), presentedSeries, tokenAgeMs, concurrentRaceWindowMs,
                    tokenPrefix(presentedToken), tokenPrefix(token.getTokenValue()),
                    buildRequestContext(request));
                // Give the browser the already-rotated token so future requests work correctly.
                writeRememberMeCookie(token.getSeries(), token.getTokenValue(), request, response);
                return getUserDetailsService().loadUserByUsername(token.getUsername());
                }
                log.debug("Remember-me token mismatch outside race window: user='{}' series='{}' tokenAgeMs={} "
                    + "raceWindowMs={} presentedTokenPrefix='{}' storedTokenPrefix='{}' {} "
                    + "-> rotating token and reissuing remember-me cookie",
                    token.getUsername(), presentedSeries, tokenAgeMs, concurrentRaceWindowMs,
                    tokenPrefix(presentedToken), tokenPrefix(token.getTokenValue()),
                    buildRequestContext(request));

                // Avoid deleting the browser cookie on mismatch. In this app, intermittent
                // concurrent/internal requests can present stale tokens after idle.
                // Self-heal by rotating the stored token and issuing a fresh cookie.
                PersistentRememberMeToken healedToken = new PersistentRememberMeToken(
                        token.getUsername(), token.getSeries(), generateTokenData(), new Date());
                try {
                    tokenRepo.updateToken(healedToken.getSeries(), healedToken.getTokenValue(), healedToken.getDate());
                    writeRememberMeCookie(healedToken.getSeries(), healedToken.getTokenValue(), request, response);
                    return getUserDetailsService().loadUserByUsername(token.getUsername());
                } catch (Exception ex) {
                    log.error("Remember-me mismatch recovery failed for user='{}' series='{}' -> removing stale series",
                            token.getUsername(), presentedSeries, ex);
                    jdbc.update("DELETE FROM persistent_logins WHERE series = ?", presentedSeries);
                    throw new RememberMeAuthenticationException(
                            "Remember-me token recovery failed for series '" + presentedSeries + "'");
                }
        }

        if (token.getDate().getTime() + getTokenValiditySeconds() * 1000L < System.currentTimeMillis()) {
            log.warn("Remember-me token expired: user='{}' series='{}' tokenAgeMs={} maxAgeMs={} {}",
                    token.getUsername(), token.getSeries(),
                    (System.currentTimeMillis() - token.getDate().getTime()),
                    (getTokenValiditySeconds() * 1000L),
                    buildRequestContext(request));
            throw new RememberMeAuthenticationException("Remember-me login has expired");
        }

        log.debug("Refreshing persistent login token for user='{}' series='{}'",
                token.getUsername(), token.getSeries());

        PersistentRememberMeToken newToken = new PersistentRememberMeToken(
                token.getUsername(), token.getSeries(), generateTokenData(), new Date());
        try {
            tokenRepo.updateToken(newToken.getSeries(), newToken.getTokenValue(), newToken.getDate());
            writeRememberMeCookie(newToken.getSeries(), newToken.getTokenValue(), request, response);
        } catch (Exception ex) {
            log.error("Failed to update remember-me token for user='{}'", token.getUsername(), ex);
            throw new RememberMeAuthenticationException("Autologin failed due to data access problem");
        }

        return getUserDetailsService().loadUserByUsername(token.getUsername());
    }

    private String buildRequestContext(HttpServletRequest request) {
        String sessionId = request.getSession(false) != null ? request.getSession(false).getId() : "none";
        String requestedSessionId = request.getRequestedSessionId() != null ? request.getRequestedSessionId() : "none";
        String query = request.getQueryString() != null ? request.getQueryString() : "";
        String pathWithQuery = query.isEmpty() ? request.getRequestURI() : request.getRequestURI() + "?" + query;
        return "request='" + request.getMethod() + " " + pathWithQuery + "'"
                + " sessionId='" + sessionId + "'"
                + " requestedSessionId='" + requestedSessionId + "'"
                + " remoteAddr='" + request.getRemoteAddr() + "'";
    }

    private String tokenPrefix(String token) {
        if (token == null || token.isEmpty()) {
            return "none";
        }
        int end = Math.min(8, token.length());
        return token.substring(0, end);
    }
}
