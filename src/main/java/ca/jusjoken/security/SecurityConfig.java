package ca.jusjoken.security;


import javax.sql.DataSource;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

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
    public SecurityFilterChain vaadinSecurityFilterChain(
            HttpSecurity http,
            UserDetailsService userDetailsService,
            PersistentTokenRepository persistentTokenRepository) throws Exception {
        http.authorizeHttpRequests(registry -> {
            registry.requestMatchers("/assets/**").permitAll();
            
            
            registry.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll();
        });

        http.rememberMe(rememberMe -> rememberMe
                .key("farm-tracks-remember-me-key")
                .tokenRepository(persistentTokenRepository)
                .tokenValiditySeconds(60 * 60 * 24 * 30)
                .alwaysRemember(true)
                .userDetailsService(userDetailsService));

        //http.with(vaadin(), vaadin -> vaadin.loginView("/login", "/"));
        
        //register loginview with the view access checker
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            configurer.loginView(LoginView.class, "/");
        });
    return http.build();    }
}