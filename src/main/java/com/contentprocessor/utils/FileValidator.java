package com.contentprocessor.utils;

import com.contentprocessor.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

public class FileValidator {

    //500MB in bytes
    private static final long MAX_FILE_SIZE = 500 * 1024 * 1024L;

    // Allowed video file types
    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            "video/mp4",
            "video/mpeg",
            "video/quicktime",
            "video/x-msvideo",
            "video/x-matroska"
    );

    // Allowed audio file types
    private static final List<String> ALLOWED_AUDIO_TYPES = Arrays.asList(
            "audio/mpeg",
            "audio/mp3",
            "audio/wav",
            "audio/x-wav",
            "audio/ogg",
            "audio/mp4",
            "audio/aac"
    );

    // Allowed document file types
    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf",
            "text/plain"
    );


    //Validate not emptyq
    public static void validateFileExists(MultipartFile file) throws BadRequestException {
        if(file == null || file.isEmpty()){
            throw new BadRequestException("File is Required and cannot be empty");
        }
    }

    public static void validateFileSize(MultipartFile file) throws BadRequestException {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException(
                    String.format("File size exceeds maximum allowed size of %d MB",
                            MAX_FILE_SIZE / (1024 * 1024))
            );
        }
    }

    public static void validateFileName(MultipartFile file) throws BadRequestException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new BadRequestException("File must have a valid name");
        }

        // Check for potentially dangerous characters
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new BadRequestException("File name contains invalid characters");
        }
    }

    public static void validateVideoFile(MultipartFile file) throws BadRequestException {
        validateFileExists(file);
        validateFileSize(file);
        validateFileName(file);

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_VIDEO_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Invalid video file type. Allowed types: " + String.join(", ", ALLOWED_VIDEO_TYPES)
            );
        }
    }

    /**
     * Validates audio file type
     */
    public static void validateAudioFile(MultipartFile file) throws BadRequestException {
        validateFileExists(file);
        validateFileSize(file);
        validateFileName(file);

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AUDIO_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Invalid audio file type. Allowed types: " + String.join(", ", ALLOWED_AUDIO_TYPES)
            );
        }
    }

    /**
     * Validates document file type (PDF or text)
     */
    public static void validateDocumentFile(MultipartFile file) throws BadRequestException {
        validateFileExists(file);
        validateFileSize(file);
        validateFileName(file);

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_DOCUMENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Invalid document file type. Allowed types: " + String.join(", ", ALLOWED_DOCUMENT_TYPES)
            );
        }
    }

    /**
     * Generic file validation (checks basic requirements)
     */
    public static void validateFile(MultipartFile file) throws BadRequestException {
        validateFileExists(file);
        validateFileSize(file);
        validateFileName(file);
    }
}
