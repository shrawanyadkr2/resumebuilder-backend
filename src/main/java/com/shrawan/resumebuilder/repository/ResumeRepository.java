package com.shrawan.resumebuilder.repository;

import com.shrawan.resumebuilder.document.Resume;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends MongoRepository<Resume, String > {
    List<Resume> findByUserIdOrderByUpdatedAtDesc(String userId);
    Optional<Resume> findByUserIdAndId(String userId, String id);


}
