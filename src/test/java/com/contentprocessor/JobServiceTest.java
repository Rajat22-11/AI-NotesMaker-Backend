package com.contentprocessor;

import com.contentprocessor.exception.BadRequestException;
import com.contentprocessor.exception.ResourceNotFoundException;
import com.contentprocessor.model.dto.request.CreateJobRequest;
import com.contentprocessor.model.dto.response.JobResponse;
import com.contentprocessor.model.entities.FileMetadata;
import com.contentprocessor.model.entities.Job;
import com.contentprocessor.model.entities.User;
import com.contentprocessor.model.enums.JobStatus;
import com.contentprocessor.model.enums.SourceType;
import com.contentprocessor.repository.FileMetadataRepository;
import com.contentprocessor.repository.JobRepository;
import com.contentprocessor.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JobService.
 * Tests business logic and validation.
 */
class JobServiceTest {

    private JobService jobService;
    private JobRepository jobRepository;
    private FileMetadataRepository fileMetadataRepository;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        jobRepository = mock(JobRepository.class);
        fileMetadataRepository = mock(FileMetadataRepository.class);
        jobService = new JobService(jobRepository, fileMetadataRepository);

        // Create test users
        testUser = User.builder()
                .id("user123")
                .email("test@example.com")
                .build();

        otherUser = User.builder()
                .id("user456")
                .email("other@example.com")
                .build();
    }

    /**
     * Test creating a VIDEO_FILE job successfully
     */
    @Test
    void createJob_VideoFile_Success() {
        // Arrange
        FileMetadata fileMetadata = FileMetadata.builder()
                .id("file123")
                .originalFileName("lecture.mp4")
                .uploadedBy(testUser)
                .build();

        CreateJobRequest request = CreateJobRequest.builder()
                .sourceType(SourceType.VIDEO_FILE)
                .fileId("file123")
                .title("Test Job")
                .build();

        Job savedJob = Job.builder()
                .id("job123")
                .user(testUser)
                .fileMetadata(fileMetadata)
                .sourceType(SourceType.VIDEO_FILE)
                .status(JobStatus.PENDING)
                .title("Test Job")
                .progress(0)
                .build();

        when(fileMetadataRepository.findById("file123"))
                .thenReturn(Optional.of(fileMetadata));
        when(jobRepository.save(any(Job.class)))
                .thenReturn(savedJob);

        // Act
        JobResponse result = jobService.createJob(request, testUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("job123");
        assertThat(result.getSourceType()).isEqualTo(SourceType.VIDEO_FILE);
        assertThat(result.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(result.getFileId()).isEqualTo("file123");

        // Verify job was saved
        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());

        Job captured = captor.getValue();
        assertThat(captured.getUser()).isEqualTo(testUser);
        assertThat(captured.getFileMetadata()).isEqualTo(fileMetadata);
        assertThat(captured.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(captured.getProgress()).isEqualTo(0);
    }

    /**
     * Test creating VIDEO_FILE job without fileId fails
     */
    @Test
    void createJob_VideoFileWithoutFileId_ThrowsException() {
        // Arrange
        CreateJobRequest request = CreateJobRequest.builder()
                .sourceType(SourceType.VIDEO_FILE)
                // Missing fileId!
                .title("Test Job")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> jobService.createJob(request, testUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("requires a fileId");

        verify(jobRepository, never()).save(any());
    }

    /**
     * Test creating job with non-existent file fails
     */
    @Test
    void createJob_FileNotFound_ThrowsException() {
        // Arrange
        CreateJobRequest request = CreateJobRequest.builder()
                .sourceType(SourceType.VIDEO_FILE)
                .fileId("nonexistent")
                .build();

        when(fileMetadataRepository.findById("nonexistent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobService.createJob(request, testUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File not found");
    }

    /**
     * Test creating job with someone else's file fails
     */
    @Test
    void createJob_FileOwnedByOtherUser_ThrowsException() {
        // Arrange
        FileMetadata fileMetadata = FileMetadata.builder()
                .id("file123")
                .uploadedBy(otherUser) // Different user!
                .build();

        CreateJobRequest request = CreateJobRequest.builder()
                .sourceType(SourceType.VIDEO_FILE)
                .fileId("file123")
                .build();

        when(fileMetadataRepository.findById("file123"))
                .thenReturn(Optional.of(fileMetadata));

        // Act & Assert
        assertThatThrownBy(() -> jobService.createJob(request, testUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("don't have permission");

        verify(jobRepository, never()).save(any());
    }

    /**
     * Test creating YOUTUBE job successfully
     */
    @Test
    void createJob_YouTube_Success() {
        // Arrange
        CreateJobRequest request = CreateJobRequest.builder()
                .sourceType(SourceType.YOUTUBE)
                .url("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                .title("Test Video")
                .build();

        Job savedJob = Job.builder()
                .id("job123")
                .user(testUser)
                .sourceType(SourceType.YOUTUBE)
                .url("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                .status(JobStatus.PENDING)
                .progress(0)
                .build();

        when(jobRepository.save(any(Job.class)))
                .thenReturn(savedJob);

        // Act
        JobResponse result = jobService.createJob(request, testUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSourceType()).isEqualTo(SourceType.YOUTUBE);
        assertThat(result.getUrl()).isEqualTo("https://www.youtube.com/watch?v=dQw4w9WgXcQ");

        verify(jobRepository).save(any(Job.class));
    }

    /**
     * Test creating YOUTUBE job without URL fails
     */
    @Test
    void createJob_YouTubeWithoutUrl_ThrowsException() {
        // Arrange
        CreateJobRequest request = CreateJobRequest.builder()
                .sourceType(SourceType.YOUTUBE)
                // Missing URL!
                .build();

        // Act & Assert
        assertThatThrownBy(() -> jobService.createJob(request, testUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("requires a URL");
    }

    /**
     * Test creating YOUTUBE job with invalid URL format fails
     */
    @Test
    void createJob_YouTubeWithInvalidUrl_ThrowsException() {
        // Arrange
        CreateJobRequest request = CreateJobRequest.builder()
                .sourceType(SourceType.YOUTUBE)
                .url("not-a-valid-url") // Missing http://
                .build();

        // Act & Assert
        assertThatThrownBy(() -> jobService.createJob(request, testUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid URL format");
    }

    /**
     * Test creating TEXT job successfully
     */
    @Test
    void createJob_Text_Success() {
        // Arrange
        CreateJobRequest request = CreateJobRequest.builder()
                .sourceType(SourceType.TEXT)
                .textContent("This is some text to process")
                .title("Text Job")
                .build();

        Job savedJob = Job.builder()
                .id("job123")
                .user(testUser)
                .sourceType(SourceType.TEXT)
                .textContent("This is some text to process")
                .status(JobStatus.PENDING)
                .build();

        when(jobRepository.save(any(Job.class)))
                .thenReturn(savedJob);

        // Act
        JobResponse result = jobService.createJob(request, testUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSourceType()).isEqualTo(SourceType.TEXT);
    }

    /**
     * Test getting job by ID successfully
     */
    @Test
    void getJobById_Success() {
        // Arrange
        Job job = Job.builder()
                .id("job123")
                .user(testUser)
                .sourceType(SourceType.VIDEO_FILE)
                .status(JobStatus.PENDING)
                .build();

        when(jobRepository.findById("job123"))
                .thenReturn(Optional.of(job));

        // Act
        JobResponse result = jobService.getJobById("job123", testUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("job123");
    }

    /**
     * Test getting non-existent job fails
     */
    @Test
    void getJobById_NotFound_ThrowsException() {
        // Arrange
        when(jobRepository.findById("nonexistent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobService.getJobById("nonexistent", testUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Job not found");
    }

    /**
     * Test getting someone else's job fails
     */
    @Test
    void getJobById_OtherUsersJob_ThrowsException() {
        // Arrange
        Job job = Job.builder()
                .id("job123")
                .user(otherUser) // Different user!
                .build();

        when(jobRepository.findById("job123"))
                .thenReturn(Optional.of(job));

        // Act & Assert
        assertThatThrownBy(() -> jobService.getJobById("job123", testUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("don't have permission");
    }

    /**
     * Test getting user's jobs with pagination
     */
    @Test
    void getUserJobs_Success() {
        // Arrange
        Job job1 = Job.builder().id("job1").user(testUser).build();
        Job job2 = Job.builder().id("job2").user(testUser).build();

        Page<Job> jobPage = new PageImpl<>(List.of(job1, job2));
        Pageable pageable = PageRequest.of(0, 10);

        when(jobRepository.findByUserOrderByCreatedAtDesc(testUser, pageable))
                .thenReturn(jobPage);

        // Act
        Page<JobResponse> result = jobService.getUserJobs(testUser, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo("job1");
        assertThat(result.getContent().get(1).getId()).isEqualTo("job2");
    }

    /**
     * Test getting jobs by status
     */
    @Test
    void getUserJobsByStatus_Success() {
        // Arrange
        Job job = Job.builder()
                .id("job1")
                .user(testUser)
                .status(JobStatus.PENDING)
                .build();

        Page<Job> jobPage = new PageImpl<>(List.of(job));
        Pageable pageable = PageRequest.of(0, 10);

        when(jobRepository.findByUserAndStatusOrderByCreatedAtDesc(
                testUser, JobStatus.PENDING, pageable))
                .thenReturn(jobPage);

        // Act
        Page<JobResponse> result = jobService.getUserJobsByStatus(
                testUser, JobStatus.PENDING, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus())
                .isEqualTo(JobStatus.PENDING);
    }

    /**
     * Test updating job status
     */
    @Test
    void updateJobStatus_Success() {
        // Arrange
        Job job = Job.builder()
                .id("job123")
                .status(JobStatus.PENDING)
                .progress(0)
                .build();

        when(jobRepository.findById("job123"))
                .thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class)))
                .thenReturn(job);

        // Act
        jobService.updateJobStatus("job123", JobStatus.PROCESSING, 50);

        // Assert
        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());

        Job captured = captor.getValue();
        assertThat(captured.getStatus()).isEqualTo(JobStatus.PROCESSING);
        assertThat(captured.getProgress()).isEqualTo(50);
    }

    /**
     * Test marking job as failed
     */
    @Test
    void markJobAsFailed_Success() {
        // Arrange
        Job job = Job.builder()
                .id("job123")
                .status(JobStatus.PROCESSING)
                .build();

        when(jobRepository.findById("job123"))
                .thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class)))
                .thenReturn(job);

        // Act
        jobService.markJobAsFailed("job123", "Processing error");

        // Assert
        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());

        Job captured = captor.getValue();
        assertThat(captured.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(captured.getErrorMessage()).isEqualTo("Processing error");
        assertThat(captured.getProgress()).isEqualTo(0);
    }

    /**
     * Test deleting job successfully
     */
    @Test
    void deleteJob_Success() {
        // Arrange
        Job job = Job.builder()
                .id("job123")
                .user(testUser)
                .build();

        when(jobRepository.findById("job123"))
                .thenReturn(Optional.of(job));

        // Act
        jobService.deleteJob("job123", testUser);

        // Assert
        verify(jobRepository).delete(job);
    }

    /**
     * Test deleting someone else's job fails
     */
    @Test
    void deleteJob_OtherUsersJob_ThrowsException() {
        // Arrange
        Job job = Job.builder()
                .id("job123")
                .user(otherUser) // Different user!
                .build();

        when(jobRepository.findById("job123"))
                .thenReturn(Optional.of(job));

        // Act & Assert
        assertThatThrownBy(() -> jobService.deleteJob("job123", testUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("don't have permission");

        verify(jobRepository, never()).delete(any());
    }
}