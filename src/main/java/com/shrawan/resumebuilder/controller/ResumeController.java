package com.shrawan.resumebuilder.controller;

import com.shrawan.resumebuilder.document.Resume;
import com.shrawan.resumebuilder.dto.CreateResumeRequest;
import com.shrawan.resumebuilder.service.FileUploadeService;
import com.shrawan.resumebuilder.service.ResumeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.shrawan.resumebuilder.util.AppConstants.*;

@RestController
@RequestMapping(RESUME)
@RequiredArgsConstructor
@Slf4j
public class ResumeController {

    private final ResumeService resumeService;
    private final FileUploadeService fileUploadeService;


    @PostMapping
    public ResponseEntity<?> createResume(@Valid @RequestBody CreateResumeRequest request,
                                          Authentication authentication) {
        //1. call the service method

        Resume newResume = resumeService.createResume(request,authentication.getPrincipal());
        //2. return the response
        return ResponseEntity.status(HttpStatus.CREATED).body(newResume);


    }

    @GetMapping
    public ResponseEntity<?> getUserResume(Authentication authentication){
        //1. call the service method
        List<Resume> resumes = resumeService.getUserResume(authentication.getPrincipal());
        //2. return the response
        return ResponseEntity.ok(resumes);


    }

    @GetMapping(ID)
    public ResponseEntity<?> getResumeById(@PathVariable String id,
                                           Authentication authentication){
        //1. call the service method
        Resume existingResume = resumeService.getResumeById(id, authentication.getPrincipal());
        //2. return the response
        return ResponseEntity.ok(existingResume);

    }

    @PutMapping(ID)
    public ResponseEntity<?> updateResume(@PathVariable String id,
                                          @RequestBody Resume updatedData,
                                          Authentication authentication){
        //1. call the service method
        Resume updatedResume = resumeService.updateResume(id,updatedData,authentication.getPrincipal());
        //2. return response
        return ResponseEntity.ok(updatedResume);


    }

    @PostMapping(UPLOAD_IMAGES)
    public ResponseEntity<?> uploadResumeImages(@PathVariable String id, @RequestPart(value = "thumbnail" , required = false)MultipartFile thumbnail,
                                                @RequestPart(value = "profileImage", required = false)MultipartFile profileImage,
                                                Authentication authentication) throws IOException {
        //1. Call the service method
        Map<String,String> response = fileUploadeService.uploadResumeImages(id,authentication.getPrincipal(),thumbnail,profileImage);

        //2. return the response
        return ResponseEntity.ok(response);



    }

    @DeleteMapping(ID)
    public ResponseEntity<?> deleteResume(@PathVariable String id,
                                          Authentication authentication){
        //1. call the service method
        resumeService.deleteResume(id,authentication.getPrincipal());
        //2. return response
        return ResponseEntity.ok(Map.of("message", "Resume deleted successfully"));

    }
}















































