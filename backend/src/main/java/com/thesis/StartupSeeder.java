package com.thesis;

import com.thesis.domain.RoleEntity;
import com.thesis.domain.UserEntity;
import com.thesis.repo.RoleRepository;
import com.thesis.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class StartupSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StartupSeeder(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        List<String> roleNames = Arrays.asList("ADMIN", "OPERATOR", "VIEWER");
        for (String roleName : roleNames) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> {
                        RoleEntity role = new RoleEntity();
                        role.setName(roleName);
                        return roleRepository.save(role);
                    });
        }

        UserEntity admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = new UserEntity();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));

            // 赋予所有角色（方便演示）
            for (String roleName : roleNames) {
                RoleEntity role = roleRepository.findByName(roleName).orElseThrow();
                admin.getRoles().add(role);
            }

            userRepository.save(admin);
        }
    }
}

