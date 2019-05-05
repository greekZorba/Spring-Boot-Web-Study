package com.web.config;

import com.web.domain.enums.SocialType;
import com.web.oauth.CustomOAuth2Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        http
            .authorizeRequests()
                .antMatchers("/", "/oauth2/**", "/login/**", "/css/**", "/images/**", "/js/**",
                        "/console/**").permitAll()
                .antMatchers("/facebook").hasAuthority(SocialType.FACEBOOK.getRoleType())
                .antMatchers("/google").hasAuthority(SocialType.GOOGLE.getRoleType())
                .antMatchers("/kakao").hasAuthority(SocialType.KAKAO.getRoleType())
                .anyRequest().authenticated()
            .and()
                .oauth2Login()
                .defaultSuccessUrl("/loginSuccess")
                .failureUrl("/loginFailure")
            .and()
                .headers().frameOptions().disable()
            .and()
                .exceptionHandling()
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
            .and()
                .formLogin()
                .successForwardUrl("/board/list")
            .and()
                .logout()
                .logoutSuccessUrl("/logout")
                .logoutSuccessUrl("/")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
            .and()
                .addFilterBefore(filter, CsrfFilter.class)
                .csrf().disable();
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
        OAuth2ClientProperties oAuth2ClientProperties, @Value(
        "${custom.oauth2.kakao.client-id}") String kakaoClientId){

        List<ClientRegistration> registrations = oAuth2ClientProperties.
                getRegistration().keySet().stream()
            .map(client -> getRegistration(oAuth2ClientProperties, client))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        registrations.add(CustomOAuth2Provider.KAKAO.getBuilder("kakao")
            .clientId(kakaoClientId)
            .clientSecret("test") // 필요없지만 null이면 실행 안돼서 임시값
            .jwkSetUri("test") // 필요없지만 null이면 실행 안돼서 임시값
            .build());

        return new InMemoryClientRegistrationRepository(registrations);
    }

    private ClientRegistration getRegistration(OAuth2ClientProperties
              clientProperties, String client) {
        if ("google".equals(client)) {
            OAuth2ClientProperties.Registration registration = clientProperties.
                    getRegistration().get("google");

            return CommonOAuth2Provider.GOOGLE.getBuilder(client)
                    .clientId(registration.getClientId())
                    .clientSecret(registration.getClientSecret())
                    .scope("email", "profile")
                    .build();
        }

        if ("facebook".equals(client)) {
            OAuth2ClientProperties.Registration registration = clientProperties.
                    getRegistration().get("facebook");

            return CommonOAuth2Provider.FACEBOOK.getBuilder(client)
                    .clientId(registration.getClientId())
                    .clientSecret(registration.getClientSecret())
                    .userInfoUri("https://graph.facebook.com/me?field=id,name,email,link")
                    .scope("email")
                    .build();
        }
        return null;
    }
}
