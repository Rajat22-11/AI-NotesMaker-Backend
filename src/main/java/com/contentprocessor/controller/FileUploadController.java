package com.contentprocessor.controller;

import com.contentprocessor.exception.BadRequestException;
import com.contentprocessor.model.dto.response.ApiResponse;
import com.contentprocessor.model.dto.response.FileUploadResponse;
import com.contentprocessor.model.entities.FileMetadata;
import com.contentprocessor.model.entities.User;
import com.contentprocessor.model.enums.FileType;
import com.contentprocessor.security.oauth2.UserPrincipal;
import com.contentprocessor.service.storage.FileStorageService;
import com.contentprocessor.utils.FileValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private final FileStorageService fileStorageService;

    public FileUploadController(FileStorageService fileStorageService){
        this.fileStorageService = fileStorageService;
    }

    /**
     * Upload a video file
     * POST /api/files/upload/video
     *
     * Example using cURL:
     * curl -X POST http://localhost:8080/api/files/upload/video \
     *   -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     *   -F "file=@/path/to/video.mp4"
     */

    @PostMapping("/upload/video")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadVideoFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) throws BadRequestException {
        logger.info("Received video file upload request from user: {}", userPrincipal.getEmail());

        FileValidator.validateVideoFile(file);

        User user = userPrincipal.getUser();
        FileMetadata fileMetadata = fileStorageService.storeFile(file, user, FileType.VIDEO);

        FileUploadResponse response = buildFileUploadResponse(fileMetadata);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Video File Uploaded Successfully", response));
    }

    @PostMapping("/upload/audio")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadAudioFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) throws BadRequestException {
        logger.info("Received audio file upload request from user: {}", userPrincipal.getEmail());

        FileValidator.validateAudioFile(file);

        User user = userPrincipal.getUser();
        FileMetadata fileMetadata = fileStorageService.storeFile(file, user, FileType.AUDIO);

        FileUploadResponse response = buildFileUploadResponse(fileMetadata);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Audio File Uploaded Successfully", response));
    }

    @PostMapping("/upload/document")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadDocumentFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) throws BadRequestException {
        logger.info("Received document file upload request from user: {}", userPrincipal.getEmail());

        FileValidator.validateDocumentFile(file);

        User user = userPrincipal.getUser();
        FileMetadata fileMetadata = fileStorageService.storeFile(file, user, FileType.DOCUMENT);

        FileUploadResponse response = buildFileUploadResponse(fileMetadata);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Document File Uploaded Successfully", response));
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId, @AuthenticationPrincipal UserPrincipal userPrincipal){
        logger.info("File download request for fileID : {} by user: {}", fileId, userPrincipal.getEmail());

        Resource resource = fileStorageService.loadFileAsResource(fileId);

        FileMetadata fileMetadata = fileStorageService.getFileMetadata(fileId);

        if(!fileMetadata.getUploadedBy().getId().equals(userPrincipal.getUser().getId())){
            return ResponseEntity.<Resource>status(HttpStatus.FORBIDDEN).build();
        }

        String contentType = fileMetadata.getContentType();
        if(contentType == null){
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileMetadata.getOriginalFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileUploadResponse>> getFileMetadata(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        logger.info("File metadata request for fileId: {} by user: {}", fileId, userPrincipal.getEmail());

        FileMetadata fileMetadata = fileStorageService.getFileMetadata(fileId);

        // Verify user owns the file
        if (!fileMetadata.getUploadedBy().getId().equals(userPrincipal.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        FileUploadResponse response = buildFileUploadResponse(fileMetadata);

        return ResponseEntity.ok(ApiResponse.success("File metadata retrieved successfully", response));
    }

    /**
     * Delete a file by ID
     * DELETE /api/files/{fileId}
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        logger.info("File delete request for fileId: {} by user: {}", fileId, userPrincipal.getEmail());

        FileMetadata fileMetadata = fileStorageService.getFileMetadata(fileId);

        // Verify user owns the file
        if (!fileMetadata.getUploadedBy().getId().equals(userPrincipal.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        fileStorageService.deleteFile(fileId);

        return ResponseEntity.ok(ApiResponse.success("File deleted successfully"));
    }

    /**
     * Helper method to build FileUploadResponse from FileMetadata
     */
    private FileUploadResponse buildFileUploadResponse(FileMetadata fileMetadata) {
        return FileUploadResponse.builder()
                .fileId(fileMetadata.getId())
                .fileName(fileMetadata.getOriginalFileName())
                .fileSize(fileMetadata.getFileSize())
                .contentType(fileMetadata.getContentType())
                .filePath(fileMetadata.getFilePath())
                .uploadedAt(fileMetadata.getCreatedAt())
                .build();
    }
}
