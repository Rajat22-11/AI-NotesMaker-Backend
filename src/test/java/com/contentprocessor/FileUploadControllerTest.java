package com.contentprocessor;

import com.contentprocessor.model.entities.FileMetadata;
import com.contentprocessor.model.entities.User;
import com.contentprocessor.model.enums.FileType;
import com.contentprocessor.repository.FileMetadataRepository;
import com.contentprocessor.repository.UserRepository;
import com.contentprocessor.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FileUploadController.
 * Tests the full HTTP request/response cycle including security.
 *
 * Uses SpringBootTest, AutoConfigureMockMvc and ActiveProfiles("test").
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    private String jwtToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean database
        fileMetadataRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .email("test@example.com")
                .microsoftId("ms123")
                .build();
        testUser = userRepository.save(testUser);

        // Generate JWT token
        jwtToken = jwtTokenProvider.generateTokenFromUserId(testUser.getId());
    }

    /**
     * Test uploading video file successfully
     */
    @Test
    void uploadVideo_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-video.mp4",
                "video/mp4",
                "test video content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/files/upload/video")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Video file uploaded successfully"))
                .andExpect(jsonPath("$.data.fileName").value("test-video.mp4"))
                .andExpect(jsonPath("$.data.contentType").value("video/mp4"))
                .andExpect(jsonPath("$.data.fileId").exists());

        // Verify file was saved to database
        assertThat(fileMetadataRepository.count()).isEqualTo(1);
        FileMetadata saved = fileMetadataRepository.findAll().get(0);
        assertThat(saved.getOriginalFileName()).isEqualTo("test-video.mp4");
        assertThat(saved.getFileType()).isEqualTo(FileType.VIDEO);
        assertThat(saved.getUploadedBy().getId()).isEqualTo(testUser.getId());
    }

    /**
     * Test uploading audio file successfully
     */
    @Test
    void uploadAudio_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-audio.mp3",
                "audio/mpeg",
                "test audio content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/files/upload/audio")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("test-audio.mp3"))
                .andExpect(jsonPath("$.data.contentType").value("audio/mpeg"));

        assertThat(fileMetadataRepository.count()).isEqualTo(1);
    }

    /**
     * Test uploading document file successfully
     */
    @Test
    void uploadDocument_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "test pdf content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/files/upload/document")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("test-document.pdf"));

        assertThat(fileMetadataRepository.count()).isEqualTo(1);
    }

    /**
     * Test upload without authentication fails
     */
    @Test
    void uploadVideo_NoAuth_Returns401() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.mp4",
                "video/mp4",
                "content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/files/upload/video")
                        .file(file))
                // No Authorization header
                .andExpect(status().isUnauthorized());

        // Verify no file was saved
        assertThat(fileMetadataRepository.count()).isEqualTo(0);
    }

    /**
     * Test upload with invalid file type fails
     */
    @Test
    void uploadVideo_InvalidFileType_Returns400() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain", // Wrong type for video!
                "content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/files/upload/video")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid video file type")));

        // Verify no file was saved
        assertThat(fileMetadataRepository.count()).isEqualTo(0);
    }

    /**
     * Test upload with malicious filename fails
     */
    @Test
    void uploadVideo_MaliciousFilename_Returns400() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../../etc/passwd", // Path traversal attempt
                "video/mp4",
                "content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/files/upload/video")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("invalid characters")));
    }

    /**
     * Test getting file metadata
     */
    @Test
    void getFileMetadata_Success() throws Exception {
        // Arrange - save a file first
        FileMetadata file = FileMetadata.builder()
                .originalFileName("test.mp4")
                .storedFileName("uuid_test.mp4")
                .fileType(FileType.VIDEO)
                .fileSize(1024L)
                .contentType("video/mp4")
                .uploadedBy(testUser)
                .build();
        file = fileMetadataRepository.save(file);

        // Act & Assert
        mockMvc.perform(get("/api/files/" + file.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileId").value(file.getId()))
                .andExpect(jsonPath("$.data.fileName").value("test.mp4"));
    }

    /**
     * Test deleting file successfully
     */
    @Test
    void deleteFile_Success() throws Exception {
        // Arrange - save a file first
        FileMetadata file = FileMetadata.builder()
                .originalFileName("test.mp4")
                .storedFileName("uuid_test.mp4")
                .localFilePath("/tmp/uuid_test.mp4")
                .uploadedBy(testUser)
                .build();
        file = fileMetadataRepository.save(file);

        assertThat(fileMetadataRepository.count()).isEqualTo(1);

        // Act & Assert
        mockMvc.perform(delete("/api/files/" + file.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("File deleted successfully"));

        // Verify file was deleted
        assertThat(fileMetadataRepository.count()).isEqualTo(0);
    }

    /**
     * Test accessing another user's file fails
     */
    @Test
    void getFileMetadata_OtherUsersFile_Returns403() throws Exception {
        // Arrange - create another user and their file
        User otherUser = User.builder()
                .email("other@example.com")
                .microsoftId("ms456")
                .build();
        otherUser = userRepository.save(otherUser);

        FileMetadata file = FileMetadata.builder()
                .originalFileName("test.mp4")
                .uploadedBy(otherUser) // Owned by other user
                .build();
        file = fileMetadataRepository.save(file);

        // Act & Assert
        mockMvc.perform(get("/api/files/" + file.getId())
                        .header("Authorization", "Bearer " + jwtToken)) // testUser's token
                .andExpect(status().isForbidden());
    }
}