package com.pet.admin;

import com.pet.system.mapper.UserMapper;
import com.pet.system.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@Profile("h2")
public class H2DataInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String hash = passwordEncoder.encode("123456");
        for (long id = 1; id <= 4; id++) {
            User u = userMapper.selectById(id);
            if (u != null) {
                u.setPassword_wsh(hash);
                u.setUpdated_at_wsh(LocalDateTime.now());
                userMapper.updateById(u);
            }
        }
    }
}
