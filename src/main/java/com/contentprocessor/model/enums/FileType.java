package com.contentprocessor.model.enums;

/**
 * Enum representing the type of uploaded file.
 * Used for validation and routing to appropriate processors.
 */
public enum FileType {
    /**
     * Video files (mp4, avi, mov, mkv, etc.)
     */
    VIDEO,

    /**
     * Audio files (mp3, wav, aac, ogg, etc.)
     */
    AUDIO,

    /**
     * Document files (pdf, txt)
     */
    DOCUMENT
}