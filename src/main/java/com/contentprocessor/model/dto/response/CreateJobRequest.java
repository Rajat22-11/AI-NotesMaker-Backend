package com.contentprocessor.model.dto.response;

import com.contentprocessor.model.enums.SourceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new processing job.
 *
 * The client can provide either:
 * - A fileId (reference to an already uploaded file)
 * - A URL (for YouTube videos or other web content)
 * - Direct text content
 *
 * Example request for uploaded file:
 * {
 *   "sourceType": "VIDEO_FILE",
 *   "fileId": "672bf8a5e4b0c1234567890a"
 * }
 *
 * Example request for YouTube:
 * {
 *   "sourceType": "YOUTUBE",
 *   "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
 * }
 *
 * Example request for text:
 * {
 *   "sourceType": "TEXT",
 *   "textContent": "This is some text to process..."
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateJobRequest {

    /**
     * Type of content being processed
     * Required field
     */
    @NotNull(message = "Source type is required")
    private SourceType sourceType;

    /**
     * Reference to an uploaded file (MongoDB ID)
     * Used for VIDEO_FILE, AUDIO_FILE, PDF_FILE
     */
    private String fileId;

    /**
     * URL for web-based content
     * Used for YOUTUBE, WEB_URL
     */
    private String url;

    /**
     * Direct text content
     * Used for TEXT source type
     */
    private String textContent;

    /**
     * Optional: Custom title for the job
     */
    private String title;

    /**
     * Optional: Additional notes or context
     */
    private String notes;
}