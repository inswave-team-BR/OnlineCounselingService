package kr.or.kosa.visang.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(
				auth -> auth.requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**", "/counselor/**")
				.permitAll().anyRequest().authenticated()).formLogin(form -> form.loginPage("/login").permitAll())
				.logout(logout -> logout.permitAll());
		return http.build();
	}
}