package edu.cit.arnejo.dormshare.admin;

import edu.cit.arnejo.dormshare.shared.entity.UserEntity;
import edu.cit.arnejo.dormshare.shared.entity.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds an initial ADMIN user on first startup if no admin exists.
 * Default credentials: admin@dormshare.com / Admin@123
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Check if any admin exists
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(u -> "ADMIN".equals(u.getRole()));

        if (!adminExists) {
            // Only seed if no admin exists
            if (userRepository.findByEmail("admin@dormshare.com").isEmpty()) {
                UserEntity admin = new UserEntity();
                admin.setFirstName("System");
                admin.setLastName("Admin");
                admin.setEmail("admin@dormshare.com");
                admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setRole("ADMIN");
                admin.setIsActive(true);
                userRepository.save(admin);
                System.out.println("✅ Admin user seeded: admin@dormshare.com / Admin@123");
            }
        } else {
            System.out.println("✅ Admin user already exists — skipping seed.");
        }
    }
}
