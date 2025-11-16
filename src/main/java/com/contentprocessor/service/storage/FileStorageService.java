package com.contentprocessor.service.storage;

import com.contentprocessor.exception.FileStorageException;
import com.contentprocessor.exception.ResourceNotFoundException;
import com.contentprocessor.model.entities.FileMetadata;
import com.contentprocessor.model.entities.User;
import com.contentprocessor.model.enums.FileType;
import com.contentprocessor.repository.FileMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service for handling file storage operations.
 * Currently stores files locally on disk (Phase 3).
 * In Phase 6, this will be updated to use cloud storage (Cloudinary).
 */
@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final Path fileStorageLocation;
    private final FileMetadataRepository fileMetadataRepository;

    /**
     * Constructor initializes the file storage directory
     * The upload directory is read from application.yml
     */
    public FileStorageService(
            @Value("${file.upload-dir:uploads}") String uploadDir,
            FileMetadataRepository fileMetadataRepository) {

        this.fileMetadataRepository = fileMetadataRepository;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            // Create the upload directory if it doesn't exist
            Files.createDirectories(this.fileStorageLocation);
            logger.info("File storage location initialized: {}", this.fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the directory for file uploads", ex);
        }
    }

    /**
     * Stores an uploaded file to disk and saves metadata to the database
     *
     * @param file The uploaded file
     * @param user The user uploading the file
     * @param fileType The type of file (VIDEO, AUDIO, DOCUMENT)
     * @return FileMetadata entity with file information
     */
    public FileMetadata storeFile(MultipartFile file, User user, FileType fileType) {
        // Normalize file name
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the filename contains invalid characters
            if (originalFileName.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence: " + originalFileName);
            }

            // Generate a unique filename to avoid collisions
            // Format: UUID_originalFileName
            String storedFileName = UUID.randomUUID().toString() + "_" + originalFileName;

            // Resolve the target location
            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);

            // Copy file to the target location (replacing existing file if any)
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            logger.info("File stored successfully: {}", storedFileName);

            // Create and save FileMetadata entity
            FileMetadata fileMetadata = FileMetadata.builder()
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .localFilePath(targetLocation.toString())
                    .fileType(fileType)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .uploadedBy(user)
                    .processed(false)
                    .build();

            return fileMetadataRepository.save(fileMetadata);

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file: " + originalFileName, ex);
        }
    }

    /**
     * Loads a file as a Resource
     *
     * @param fileId The MongoDB ID of the FileMetadata
     * @return Resource representing the file
     */
    public Resource loadFileAsResource(String fileId) {
        try {
            // Retrieve file metadata from database
            FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                    .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

            // For local storage, use localFilePath
            if (fileMetadata.isLocalStored()) {
                Path filePath = Paths.get(fileMetadata.getLocalFilePath()).normalize();
                Resource resource = new UrlResource(filePath.toUri());

                if (resource.exists() && resource.isReadable()) {
                    return resource;
                } else {
                    throw new FileStorageException("File not found or not readable: " + fileMetadata.getStoredFileName());
                }
            } else {
                // For cloud storage (Phase 6)
                throw new FileStorageException("Cloud file download not yet implemented");
            }
        } catch (MalformedURLException ex) {
            throw new FileStorageException("File not found with id: " + fileId, ex);
        }
    }

    /**
     * Gets the file path for a given file ID
     * Useful for processing services that need direct file access
     *
     * @param fileId The MongoDB ID of the FileMetadata
     * @return The file path as a string
     */
    public String getFilePath(String fileId) {
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        return fileMetadata.getFilePath(); // Uses helper method that returns appropriate path
    }

    /**
     * Deletes a file from storage and removes its metadata
     *
     * @param fileId The MongoDB ID of the FileMetadata
     */
    public void deleteFile(String fileId) {
        try {
            // Retrieve file metadata
            FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                    .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

            // Delete the physical file (if stored locally)
            if (fileMetadata.isLocalStored()) {
                Path filePath = Paths.get(fileMetadata.getLocalFilePath()).normalize();
                Files.deleteIfExists(filePath);
                logger.info("Local file deleted: {}", fileMetadata.getStoredFileName());
            }

            // For cloud storage deletion (Phase 6)
            if (fileMetadata.isCloudStored()) {
                // Will implement: cloudinaryService.deleteFile(fileMetadata.getCloudinaryPublicId());
                logger.info("Cloud file deletion not yet implemented");
            }

            // Delete the metadata from database
            fileMetadataRepository.delete(fileMetadata);
            logger.info("File metadata deleted: {}", fileId);

        } catch (IOException ex) {
            throw new FileStorageException("Could not delete file with id: " + fileId, ex);
        }
    }

    /**
     * Gets FileMetadata by ID
     *
     * @param fileId The MongoDB ID
     * @return FileMetadata entity
     */
    public FileMetadata getFileMetadata(String fileId) {
        return fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));
    }

    /**
     * Marks a file as processed
     * Called after successful processing (transcription, etc.)
     *
     * @param fileId The MongoDB ID of the FileMetadata
     */
    public void markFileAsProcessed(String fileId) {
        FileMetadata fileMetadata = getFileMetadata(fileId);
        fileMetadata.setProcessed(true);
        fileMetadataRepository.save(fileMetadata);
        logger.info("File marked as processed: {}", fileId);
    }
}