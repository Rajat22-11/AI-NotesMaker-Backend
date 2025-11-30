package com.contentprocessor.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for file upload response.
 * Returns essential information about the uploaded file.
 *
 * Example response:
 * {
 *   "fileId": "672bf8a5e4b0c1234567890a",
 *   "fileName": "lecture_recording.mp4",
 *   "fileSize": 15728640,
 *   "contentType": "video/mp4",
 *   "uploadedAt": "2025-10-25T10:30:00"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    /**
     * MongoDB ID of the saved FileMetadata
     */
    private String fileId;

    /**
     * Original filename
     */
    private String fileName;

    /**
     * File size in bytes
     */
    private Long fileSize;

    /**
     * MIME type
     */
    private String contentType;

    /**
     * Local file path or cloud URL (depending on storage type)
     */
    private String filePath;

    /**
     * When the file was uploaded
     */
    private LocalDateTime uploadedAt;
}