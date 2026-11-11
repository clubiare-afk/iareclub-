package com.iare.club.service;
import com.iare.club.dto.RegistrationRequest;
import com.iare.club.entity.AppUser;
import com.iare.club.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
@Service
public class UserService {
    private final UserRepository users; private final PasswordEncoder encoder; private final ReferralService referrals;
    public UserService(UserRepository users,PasswordEncoder encoder,ReferralService referrals){this.users=users;this.encoder=encoder;this.referrals=referrals;}
    @Transactional public AppUser register(RegistrationRequest r){
        String email=r.getEmail().strip().toLowerCase(Locale.ROOT);
        if(users.existsByEmailIgnoreCase(email)) throw new EmailAlreadyExistsException();
        AppUser u=new AppUser(); u.setFullName(r.getFullName().strip().replaceAll("\\s+"," ")); u.setEmail(email); u.setPasswordHash(encoder.encode(r.getPassword()));
        try{AppUser saved=users.saveAndFlush(u);referrals.ensureCode(saved);referrals.recordSignup(saved,r.getReferralCode());return saved;}catch(DataIntegrityViolationException e){throw new EmailAlreadyExistsException();}
    }
    @Transactional(readOnly=true) public AppUser findByEmail(String email){return users.findByEmailIgnoreCase(email).orElseThrow();}
    public static class EmailAlreadyExistsException extends RuntimeException{}
}
