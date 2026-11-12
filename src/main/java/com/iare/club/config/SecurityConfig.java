package com.iare.club.config;

import com.iare.club.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

@Configuration
public class SecurityConfig {
    private static final String ADMIN_CONTEXT="MONEY_MINT_ADMIN_SECURITY_CONTEXT";
    private static final String USER_CONTEXT="MONEY_MINT_USER_SECURITY_CONTEXT";
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}

    @Bean SecurityContextRepository adminSecurityContextRepository(){
        HttpSessionSecurityContextRepository repository=new HttpSessionSecurityContextRepository();
        repository.setSpringSecurityContextKey(ADMIN_CONTEXT);return repository;
    }

    @Bean SecurityContextRepository userSecurityContextRepository(){
        HttpSessionSecurityContextRepository repository=new HttpSessionSecurityContextRepository();
        repository.setSpringSecurityContextKey(USER_CONTEXT);return repository;
    }

    @Bean UserDetailsService adminUserDetailsService(
            @Value("${moneymint.admin.email}") String email,
            @Value("${moneymint.admin.password}") String password,
            PasswordEncoder encoder){
        UserDetails admin=User.withUsername(email.strip().toLowerCase()).password(encoder.encode(password)).roles("ADMIN").build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean DaoAuthenticationProvider adminAuthenticationProvider(
            @Qualifier("adminUserDetailsService") UserDetailsService details,PasswordEncoder encoder){
        DaoAuthenticationProvider provider=new DaoAuthenticationProvider();provider.setUserDetailsService(details);provider.setPasswordEncoder(encoder);return provider;
    }

    @Bean DaoAuthenticationProvider userAuthenticationProvider(CustomUserDetailsService details,PasswordEncoder encoder){
        DaoAuthenticationProvider provider=new DaoAuthenticationProvider();provider.setUserDetailsService(details);provider.setPasswordEncoder(encoder);return provider;
    }

    @Bean @Order(1)
    SecurityFilterChain adminSecurity(HttpSecurity http,
            @Qualifier("adminAuthenticationProvider") DaoAuthenticationProvider provider,
            @Qualifier("adminSecurityContextRepository") SecurityContextRepository contextRepository)throws Exception{
        http.securityMatcher("/money","/admin/**").authenticationProvider(provider)
            .securityContext(context->context.securityContextRepository(contextRepository))
            .authorizeHttpRequests(auth->auth.requestMatchers("/money").permitAll().anyRequest().hasRole("ADMIN"))
            .formLogin(form->form.loginPage("/money").loginProcessingUrl("/money").usernameParameter("email")
                .defaultSuccessUrl("/admin/dashboard",true).failureUrl("/money?error").permitAll())
            .logout(logout->logout.logoutUrl("/admin/logout").logoutSuccessUrl("/money?logout")
                .addLogoutHandler((request,response,authentication)->{if(request.getSession(false)!=null)request.getSession(false).removeAttribute(ADMIN_CONTEXT);})
                .invalidateHttpSession(false).clearAuthentication(true))
            .sessionManagement(session->session.sessionFixation(fix->fix.migrateSession()))
            .exceptionHandling(ex->ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/money")));
        return http.build();
    }

    @Bean @Order(2)
    SecurityFilterChain userSecurity(HttpSecurity http,
            @Qualifier("userAuthenticationProvider") DaoAuthenticationProvider provider,
            @Qualifier("userSecurityContextRepository") SecurityContextRepository contextRepository)throws Exception{
        http.authenticationProvider(provider)
            .securityContext(context->context.securityContextRepository(contextRepository))
            .authorizeHttpRequests(auth->auth
                .requestMatchers("/","/signup","/login","/css/**","/js/**","/images/**","/error","/payments/payu/callback","/payments/payu/webhook").permitAll()
                .requestMatchers("/dashboard/**","/products/**","/my-products/**","/scratch/**","/wallet/**","/deposit/**","/investments/**","/api/investments/**","/withdrawal/**","/activity/**","/refer-and-earn/**","/payments/payu/status","/payments/payu/result","/api/advertisements/active","/api/carousel-banners/active").hasRole("USER").anyRequest().denyAll())
            .formLogin(form->form.loginPage("/login").usernameParameter("email")
                .successHandler((request,response,authentication)->{
                    HttpSessionRequestCache cache=new HttpSessionRequestCache();
                    SavedRequest saved=cache.getRequest(request,response);
                    String target=saved==null?request.getContextPath()+"/dashboard":saved.getRedirectUrl();
                    cache.removeRequest(request,response);
                    response.setStatus(303);
                    response.setHeader("Location",target);
                }).failureUrl("/login?error").permitAll())
            .logout(logout->logout.logoutUrl("/logout").logoutSuccessHandler((request,response,authentication)->{
                    response.setStatus(303);
                    response.setHeader("Location",request.getContextPath()+"/login?logout");
                })
                .addLogoutHandler((request,response,authentication)->{if(request.getSession(false)!=null)request.getSession(false).removeAttribute(USER_CONTEXT);})
                .addLogoutHandler((request,response,authentication)->new HttpSessionRequestCache().removeRequest(request,response))
                .invalidateHttpSession(false).clearAuthentication(true))
            .sessionManagement(session->session.sessionFixation(fix->fix.migrateSession()))
            .exceptionHandling(ex->ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));
        http.csrf(csrf->csrf.ignoringRequestMatchers("/payments/payu/callback","/payments/payu/webhook"));
        return http.build();
    }
}
