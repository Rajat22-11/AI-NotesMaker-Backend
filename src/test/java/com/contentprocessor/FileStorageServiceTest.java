package com.contentprocessor;

import com.contentprocessor.exception.FileStorageException;
import com.contentprocessor.exception.ResourceNotFoundException;
import com.contentprocessor.model.entities.FileMetadata;
import com.contentprocessor.model.entities.User;
import com.contentprocessor.model.enums.FileType;
import com.contentprocessor.repository.FileMetadataRepository;
import com.contentprocessor.service.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FileStorageService.
 *
 * Testing Strategy:
 * - Use @TempDir for temporary file storage
 * - Mock FileMetadataRepository
 * - Test both success and failure scenarios
 */
class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private FileMetadataRepository fileMetadataRepository;

    @TempDir
    Path tempDir; // JUnit automatically creates and cleans up temp directory

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create mocks
        fileMetadataRepository = mock(FileMetadataRepository.class);

        // Initialize service with temp directory
        fileStorageService = new FileStorageService(
                tempDir.toString(),
                fileMetadataRepository
        );

        // Create test user
        testUser = User.builder()
                .id("user123")
                .email("test@example.com")
                .name("Test User")
                .build();
    }

    /**
     * Test successful file storage
     */
    @Test
    void storeFile_Success() {
        // Arrange
        String fileName = "test-video.mp4";
        String content = "This is test video content";
        MultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "video/mp4",
                content.getBytes()
        );

        FileMetadata savedMetadata = FileMetadata.builder()
                .id("file123")
                .originalFileName(fileName)
                .storedFileName("uuid_" + fileName)
                .fileSize((long) content.length())
                .build();

        when(fileMetadataRepository.save(any(FileMetadata.class)))
                .thenReturn(savedMetadata);

        // Act
        FileMetadata result = fileStorageService.storeFile(file, testUser, FileType.VIDEO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("file123");
        assertThat(result.getOriginalFileName()).isEqualTo(fileName);

        // Verify repository was called
        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
        verify(fileMetadataRepository).save(captor.capture());

        FileMetadata captured = captor.getValue();
        assertThat(captured.getOriginalFileName()).isEqualTo(fileName);
        assertThat(captured.getFileType()).isEqualTo(FileType.VIDEO);
        assertThat(captured.getUploadedBy()).isEqualTo(testUser);
        assertThat(captured.getFileSize()).isEqualTo(content.length());

        // Verify file exists on disk
        Path uploadedFile = tempDir.resolve(captured.getStoredFileName());
        assertThat(uploadedFile).exists();
    }

    /**
     * Test file storage with invalid filename (path traversal attack)
     */
    @Test
    void storeFile_InvalidFileName_ThrowsException() {
        // Arrange - malicious filename
        MultipartFile file = new MockMultipartFile(
                "file",
                "../../../etc/passwd", // Path traversal attempt
                "text/plain",
                "malicious content".getBytes()
        );

        // Act & Assert
        assertThatThrownBy(() ->
                fileStorageService.storeFile(file, testUser, FileType.DOCUMENT)
        )
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("invalid path sequence");

        // Verify no file was saved to repository
        verify(fileMetadataRepository, never()).save(any());
    }

    /**
     * Test loading file as resource
     */
    @Test
    void loadFileAsResource_Success() throws IOException {
        // Arrange - create a file on disk
        String storedFileName = "test_file.mp4";
        Path filePath = tempDir.resolve(storedFileName);
        Files.write(filePath, "test content".getBytes());

        FileMetadata metadata = FileMetadata.builder()
                .id("file123")
                .storedFileName(storedFileName)
                .localFilePath(filePath.toString())
                .build();

        when(fileMetadataRepository.findById("file123"))
                .thenReturn(Optional.of(metadata));

        // Act
        Resource resource = fileStorageService.loadFileAsResource("file123");

        // Assert
        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
        assertThat(resource.getFilename()).isEqualTo(storedFileName);
    }

    /**
     * Test loading non-existent file
     */
    @Test
    void loadFileAsResource_FileNotFound_ThrowsException() {
        // Arrange
        when(fileMetadataRepository.findById("nonexistent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                fileStorageService.loadFileAsResource("nonexistent")
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File not found");
    }

    /**
     * Test loading file that exists in DB but not on disk
     */
    @Test
    void loadFileAsResource_FileOnDiskMissing_ThrowsException() {
        // Arrange
        FileMetadata metadata = FileMetadata.builder()
                .id("file123")
                .storedFileName("missing_file.mp4")
                .localFilePath(tempDir.resolve("missing_file.mp4").toString())
                .build();

        when(fileMetadataRepository.findById("file123"))
                .thenReturn(Optional.of(metadata));

        // Act & Assert
        assertThatThrownBy(() ->
                fileStorageService.loadFileAsResource("file123")
        )
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("not found or not readable");
    }

    /**
     * Test deleting a file
     */
    @Test
    void deleteFile_Success() throws IOException {
        // Arrange - create a file on disk
        String storedFileName = "to_delete.mp4";
        Path filePath = tempDir.resolve(storedFileName);
        Files.write(filePath, "test content".getBytes());

        FileMetadata metadata = FileMetadata.builder()
                .id("file123")
                .storedFileName(storedFileName)
                .localFilePath(filePath.toString())
                .build();

        when(fileMetadataRepository.findById("file123"))
                .thenReturn(Optional.of(metadata));

        // Verify file exists before deletion
        assertThat(filePath).exists();

        // Act
        fileStorageService.deleteFile("file123");

        // Assert - file should be deleted from disk
        assertThat(filePath).doesNotExist();

        // Assert - metadata should be deleted from repository
        verify(fileMetadataRepository).delete(metadata);
    }

    /**
     * Test marking file as processed
     */
    @Test
    void markFileAsProcessed_Success() {
        // Arrange
        FileMetadata metadata = FileMetadata.builder()
                .id("file123")
                .processed(false)
                .build();

        when(fileMetadataRepository.findById("file123"))
                .thenReturn(Optional.of(metadata));
        when(fileMetadataRepository.save(any(FileMetadata.class)))
                .thenReturn(metadata);

        // Act
        fileStorageService.markFileAsProcessed("file123");

        // Assert
        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
        verify(fileMetadataRepository).save(captor.capture());

        assertThat(captor.getValue().isProcessed()).isTrue();
    }

    /**
     * Test getting file path
     */
    @Test
    void getFilePath_LocalStorage_ReturnsCorrectPath() {
        // Arrange
        String expectedPath = tempDir.resolve("test.mp4").toString();
        FileMetadata metadata = FileMetadata.builder()
                .id("file123")
                .localFilePath(expectedPath)
                .build();

        when(fileMetadataRepository.findById("file123"))
                .thenReturn(Optional.of(metadata));

        // Act
        String result = fileStorageService.getFilePath("file123");

        // Assert
        assertThat(result).isEqualTo(expectedPath);
    }

    /**
     * Test getting file path for cloud-stored file
     */
    @Test
    void getFilePath_CloudStorage_ReturnsCloudUrl() {
        // Arrange
        String cloudUrl = "https://res.cloudinary.com/demo/video/upload/v1234/test.mp4";
        FileMetadata metadata = FileMetadata.builder()
                .id("file123")
                .cloudinaryUrl(cloudUrl)
                .localFilePath(null)
                .build();

        when(fileMetadataRepository.findById("file123"))
                .thenReturn(Optional.of(metadata));

        // Act
        String result = fileStorageService.getFilePath("file123");

        // Assert
        assertThat(result).isEqualTo(cloudUrl);
    }
}