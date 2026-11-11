package com.iare.club.service;
import com.iare.club.entity.AppUser;
import com.iare.club.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
@Service public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users; public CustomUserDetailsService(UserRepository users){this.users=users;}
    @Override public UserDetails loadUserByUsername(String email){
        AppUser u=users.findByEmailIgnoreCase(email.strip()).orElseThrow(()->new UsernameNotFoundException("Invalid email or password"));
        return User.withUsername(u.getEmail()).password(u.getPasswordHash()).authorities(u.getRole()).build();
    }
}
