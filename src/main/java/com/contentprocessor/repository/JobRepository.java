package com.contentprocessor.repository;

import com.contentprocessor.model.entities.Job;
import com.contentprocessor.model.entities.User;
import com.contentprocessor.model.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends MongoRepository<Job, String> {
    List<Job> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Job> findByStatus(JobStatus status);
    Optional<Job> findByIdAndUserId(String id, String userId);

    // pageable variants used by service and tests
    Page<Job> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<Job> findByUserAndStatusOrderByCreatedAtDesc(User user, JobStatus status, Pageable pageable);
}