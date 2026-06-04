package iocode.web.app.config;

import iocode.web.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@RequiredArgsConstructor
public class AppConfig {
    final private UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService(){
        System.out.println("AppConfig:Bean userDetailsService");
        return userRepository::findByUsernameIgnoreCase;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        System.out.println("AppConfig:Bean passwordEncoder");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(){
        System.out.println("AppConfig:Bean authenticationProvider");
        var daoProvider = new DaoAuthenticationProvider(passwordEncoder());
        daoProvider.setUserDetailsService(userDetailsService());
        return daoProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{
        System.out.println("AppConfig:Bean authenticationManager");
        return config.getAuthenticationManager();
    }

    @Bean
    public RestTemplate restTemplate(){
        System.out.println("AppConfig:Bean restTemplate");
        return new RestTemplate();
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService(){
        System.out.println("AppConfig:Bean scheduledExecutorService");
        return Executors.newScheduledThreadPool(1);
    }
}
