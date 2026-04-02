package ca.jusjoken.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * Temporary diagnostics filter: logs redirects to login and 401/403 responses
 * so unexpected auth drops can be correlated with request paths and session ids.
 */
public class SecurityRedirectTraceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SecurityRedirectTraceFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        RedirectCaptureResponseWrapper responseWrapper = new RedirectCaptureResponseWrapper(response);
        filterChain.doFilter(request, responseWrapper);

        String redirectLocation = responseWrapper.getRedirectLocation();
        int status = responseWrapper.getStatus();
        boolean loginRedirect = redirectLocation != null
                && (redirectLocation.contains("/login") || redirectLocation.contains("login?"));
        boolean rememberMeCookieExpired = responseWrapper.hasExpiringRememberMeCookie();

        if (loginRedirect
            || status == HttpServletResponse.SC_UNAUTHORIZED
            || status == HttpServletResponse.SC_FORBIDDEN
                || rememberMeCookieExpired) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String principal = auth != null ? auth.getName() : "none";
            String authType = auth != null ? auth.getClass().getSimpleName() : "none";
            String sessionId = request.getSession(false) != null ? request.getSession(false).getId() : "none";
            String requestedSessionId = request.getRequestedSessionId() != null ? request.getRequestedSessionId() : "none";
            String query = request.getQueryString() != null ? request.getQueryString() : "";
            String pathWithQuery = query.isEmpty() ? request.getRequestURI() : request.getRequestURI() + "?" + query;
            String cookieSummary = summarizeCookies(request.getCookies());
            String responseCookieSummary = responseWrapper.getSetCookieSummary();

            log.warn("Security response trace: method='{}' uri='{}' status={} redirect='{}' principal='{}' auth='{}' sessionId='{}' requestedSessionId='{}' remoteAddr='{}' cookies='{}' responseSetCookie='{}'",
                    request.getMethod(),
                    pathWithQuery,
                    status,
                    redirectLocation == null ? "none" : redirectLocation,
                    principal,
                    authType,
                    sessionId,
                    requestedSessionId,
                    request.getRemoteAddr(),
                cookieSummary,
                responseCookieSummary);

            if (rememberMeCookieExpired) {
                log.error("Security response trace detected remember-me expiry header: method='{}' uri='{}' responseSetCookie='{}'",
                        request.getMethod(), pathWithQuery, responseCookieSummary);
            }
        }
    }

    private String summarizeCookies(Cookie[] cookies) {
        if (cookies == null || cookies.length == 0) {
            return "none";
        }

        boolean hasJsessionId = false;
        boolean hasRememberMe = false;
        int total = cookies.length;
        for (Cookie cookie : cookies) {
            if ("JSESSIONID".equalsIgnoreCase(cookie.getName())) {
                hasJsessionId = true;
            }
            if ("remember-me".equalsIgnoreCase(cookie.getName())) {
                hasRememberMe = true;
            }
        }
        return "count=" + total + ",JSESSIONID=" + hasJsessionId + ",remember-me=" + hasRememberMe;
    }

    private static final class RedirectCaptureResponseWrapper extends HttpServletResponseWrapper {

        private String redirectLocation;
        private final List<String> setCookieHeaders = new ArrayList<>();

        private RedirectCaptureResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addHeader(String name, String value) {
            captureSetCookie(name, value);
            super.addHeader(name, value);
        }

        @Override
        public void addCookie(Cookie cookie) {
            captureSetCookieFromCookie(cookie);
            super.addCookie(cookie);
        }

        @Override
        public void setHeader(String name, String value) {
            captureSetCookie(name, value);
            super.setHeader(name, value);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            this.redirectLocation = location;
            super.sendRedirect(location);
        }

        private String getRedirectLocation() {
            return redirectLocation;
        }

        private void captureSetCookie(String name, String value) {
            if ("Set-Cookie".equalsIgnoreCase(name) && value != null) {
                setCookieHeaders.add(value);
            }
        }

        private void captureSetCookieFromCookie(Cookie cookie) {
            if (cookie == null || cookie.getName() == null) {
                return;
            }

            StringBuilder header = new StringBuilder();
            header.append(cookie.getName()).append("=")
                    .append(cookie.getValue() == null ? "" : cookie.getValue());

            if (cookie.getPath() != null) {
                header.append("; Path=").append(cookie.getPath());
            }
            if (cookie.getDomain() != null) {
                header.append("; Domain=").append(cookie.getDomain());
            }
            if (cookie.getMaxAge() >= 0) {
                header.append("; Max-Age=").append(cookie.getMaxAge());
            }
            if (cookie.getSecure()) {
                header.append("; Secure");
            }
            if (cookie.isHttpOnly()) {
                header.append("; HttpOnly");
            }

            setCookieHeaders.add(header.toString());
        }

        private boolean hasExpiringRememberMeCookie() {
            return setCookieHeaders.stream().anyMatch(header -> {
                if (header == null) {
                    return false;
                }
                String lower = header.toLowerCase();
                if (!lower.startsWith("remember-me=")) {
                    return false;
                }
                return lower.contains("max-age=0") || lower.contains("expires=thu, 01 jan 1970");
            });
        }

        private String getSetCookieSummary() {
            if (setCookieHeaders.isEmpty()) {
                return "none";
            }
            return String.join(" | ", setCookieHeaders);
        }
    }
}
