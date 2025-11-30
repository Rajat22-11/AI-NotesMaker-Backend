package com.contentprocessor.repository;

import com.contentprocessor.model.entities.FileMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetadataRepository extends MongoRepository<FileMetadata, String> {
    // query by the DBRef user's id
    List<FileMetadata> findByUploadedById(String userId);
    void deleteByIdAndUploadedById(String id, String userId);
}