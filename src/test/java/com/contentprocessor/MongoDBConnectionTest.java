package com.contentprocessor;

import com.contentprocessor.model.entities.User;
import com.contentprocessor.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MongoDBConnectionTest {

    @Autowired
    private UserRepository userRepository;

    private static final String TEST_EMAIL = "test.user@example.com";
    private static final String TEST_MICROSOFT_ID = "test-ms-id-123";

    @Test
    @Order(1)
    void testSaveUser() {
        System.out.println("🚀 Running testSaveUser...");

        // Check if user already exists
        if (userRepository.existsByEmail(TEST_EMAIL)) {
            System.out.println("ℹ️ User already exists, skipping insert.");
            return;
        }

        User testUser = User.builder()
                .email(TEST_EMAIL)
                .microsoftId(TEST_MICROSOFT_ID)
                .createdAt(Instant.now())
                .build();

        User savedUser = userRepository.save(testUser);
        assertNotNull(savedUser.getId());
        assertEquals(TEST_EMAIL, savedUser.getEmail());
        System.out.println("✅ testSaveUser passed.");
    }

    @Test
    @Order(2)
    void testFindByEmail() {
        System.out.println("🔍 Running testFindByEmail...");
        Optional<User> found = userRepository.findByEmail(TEST_EMAIL);
        assertTrue(found.isPresent(), "User should exist in DB");
        assertEquals(TEST_EMAIL, found.get().getEmail());
        System.out.println("✅ testFindByEmail passed.");
    }

    @Test
    @Order(3)
    void testFindByMicrosoftId() {
        System.out.println("🔍 Running testFindByMicrosoftId...");
        Optional<User> found = userRepository.findByMicrosoftId(TEST_MICROSOFT_ID);
        assertTrue(found.isPresent(), "User should exist in DB");
        assertEquals(TEST_MICROSOFT_ID, found.get().getMicrosoftId());
        System.out.println("✅ testFindByMicrosoftId passed.");
    }

    @Test
    @Order(4)
    void testExistsByEmail() {
        System.out.println("🔍 Running testExistsByEmail...");
        boolean exists = userRepository.existsByEmail(TEST_EMAIL);
        assertTrue(exists, "User should exist in DB");
        System.out.println("✅ testExistsByEmail passed.");
    }

    @Test
    @Order(5)
    void testDeleteById() {
        System.out.println("🗑️ Running testDeleteById...");

        Optional<User> found = userRepository.findByEmail(TEST_EMAIL);
        if (found.isPresent()) {
            userRepository.deleteById(found.get().getId());
            assertFalse(userRepository.existsByEmail(TEST_EMAIL));
            System.out.println("✅ testDeleteById passed.");
        } else {
            System.out.println("⚠️ User not found, nothing to delete.");
        }
    }
}
