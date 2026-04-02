package ca.jusjoken.security;


import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

import ca.jusjoken.views.login.LoginView;
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author birch
 */
@EnableWebSecurity
@Configuration
class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        repository.setCreateTableOnStartup(false);
        return repository;
    }

    @Bean
    public RememberMeServices farmTracksRememberMeServices(
            UserDetailsService userDetailsService,
            PersistentTokenRepository persistentTokenRepository,
            JdbcTemplate jdbcTemplate,
            @Value("${app.security.remember-me.concurrent-race-window-ms:900000}") long concurrentRaceWindowMs,
            @Value("${app.security.remember-me.suppress-login-fail-cookie-clear:true}") boolean suppressLoginFailCookieClear) {
        return new FarmTracksRememberMeServices(
                "farm-tracks-remember-me-key",
                userDetailsService,
                persistentTokenRepository,
                jdbcTemplate,
                concurrentRaceWindowMs,
                suppressLoginFailCookieClear);
    }

    @Bean
    public FilterRegistrationBean<SecurityRedirectTraceFilter> securityRedirectTraceFilter() {
        FilterRegistrationBean<SecurityRedirectTraceFilter> registration =
            new FilterRegistrationBean<>(new SecurityRedirectTraceFilter());
        registration.setName("securityRedirectTraceFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        return registration;
    }

    /**
     * Enables Spring Security's HttpSessionDestroyedEvent, which is required for
     * SecurityEventLogger to receive session-destruction notifications for debugging.
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain vaadinSecurityFilterChain(
            HttpSecurity http,
            RememberMeServices rememberMeServices) throws Exception {
        http.authorizeHttpRequests(registry -> {
            registry.requestMatchers("/assets/**").permitAll();
            registry.requestMatchers("/icons/**").permitAll();
            registry.requestMatchers("/manifest.webmanifest", "/sw.js", "/offline.html").permitAll();
            registry.requestMatchers("/favicon.ico", "/robots.txt").permitAll();
            registry.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll();
        });

        //register loginview with the view access checker
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            configurer.loginView(LoginView.class);
        });

        http.rememberMe(rememberMe -> rememberMe
            .rememberMeServices(rememberMeServices)
            .rememberMeParameter("remember-me")
            .alwaysRemember(true)
            .rememberMeCookieName("remember-me"));

        return http.build();
    }
}