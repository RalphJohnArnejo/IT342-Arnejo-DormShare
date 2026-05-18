package edu.cit.arnejo.dormshare.admin;

import edu.cit.arnejo.dormshare.shared.entity.UserEntity;
import edu.cit.arnejo.dormshare.shared.entity.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds an initial ADMIN user on first startup if no admin exists.
 * Also handles the is_active column migration for existing databases.
 * Default credentials: admin@dormshare.com / Admin@123
 */
@Component
@Order(1)
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Step 1: Ensure the is_active column exists with a default for existing rows
        try {
            entityManager.createNativeQuery(
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active boolean DEFAULT true"
            ).executeUpdate();
            entityManager.createNativeQuery(
                "UPDATE users SET is_active = true WHERE is_active IS NULL"
            ).executeUpdate();
            System.out.println("✅ Database migration: is_active column ready.");
        } catch (Exception e) {
            System.out.println("ℹ️ is_active column migration skipped (may already exist): " + e.getMessage());
        }

        // Step 2: Check if any admin exists
        try {
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
        } catch (Exception e) {
            System.err.println("⚠️ Admin seeder error: " + e.getMessage());
        }
    }
}
