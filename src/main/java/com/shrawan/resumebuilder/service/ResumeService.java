package com.shrawan.resumebuilder.service;


import com.shrawan.resumebuilder.document.Resume;
import com.shrawan.resumebuilder.dto.AuthResponse;
import com.shrawan.resumebuilder.dto.CreateResumeRequest;
import com.shrawan.resumebuilder.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {
    private final ResumeRepository resumeRepository;
    private final AuthService authService;

    public Resume createResume(CreateResumeRequest request, Object principalObject ) {
        //1. create the resume object
        Resume newResume = new Resume();
        //2. get the current profile
        AuthResponse response = authService.getProfile(principalObject);

        //3. update the resume object
        newResume.setUserId(response.getId());
        newResume.setTitle(request.getTitle());
        //4. set the default data for the resume
        setDefaultResumeData(newResume);
        //5. save the resume data
        return resumeRepository.save(newResume);

    }

    private void setDefaultResumeData(Resume newResume) {
        newResume.setProfileInfo(new Resume.ProfileInfo());
        newResume.setContactInfo(new Resume.ContactInfo());
        newResume.setWorkExperience(new ArrayList<>());
        newResume.setEducation(new ArrayList<>());
        newResume.setSkills(new ArrayList<>());
        newResume.setProjects(new ArrayList<>());
        newResume.setCertifications(new ArrayList<>());
        newResume.setLanguages(new ArrayList<>());
        newResume.setInterests(new ArrayList<>());
    }

    public List<Resume> getUserResume(Object principal) {
        //1. get the current profile
        AuthResponse response =authService.getProfile(principal);
        //2. call the repository find method
        List<Resume> resumes = resumeRepository.findByUserIdOrderByUpdatedAtDesc( response.getId());
        //3. return response
        return resumes;
    }

    public Resume getResumeById(String resumeId,  Object principal) {
        //1. get the current profile
        AuthResponse response = authService.getProfile(principal);
        //2. call the repo finder method
        Resume existingResume = resumeRepository.findByUserIdAndId(response.getId(),resumeId)
                .orElseThrow(()->new RuntimeException("Resume not found"));

        //3. return the result
        return existingResume;

    }

    public Resume updateResume(String resumeId, Resume updatedData,  Object principal) {
        //1. get the current profile
        AuthResponse response = authService.getProfile(principal);
        //2. call the repository finder method
        Resume existingResume =  resumeRepository.findByUserIdAndId(response.getId(),resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
        //3. update the new data
        existingResume.setTitle(updatedData.getTitle());
        existingResume.setThumbnailLink(updatedData.getThumbnailLink());
        existingResume.setTemplate(updatedData.getTemplate());
        existingResume.setProfileInfo(updatedData.getProfileInfo());
        existingResume.setContactInfo(updatedData.getContactInfo());
        existingResume.setWorkExperience(updatedData.getWorkExperience());
        existingResume.setEducation(updatedData.getEducation());
        existingResume.setSkills(updatedData.getSkills());
        existingResume.setProjects(updatedData.getProjects());
        existingResume.setCertifications(updatedData.getCertifications());
        existingResume.setLanguages(updatedData.getLanguages());
        existingResume.setInterests(updatedData.getInterests());

        //3. update the details into db
        resumeRepository.save(existingResume);
        //4. return result

        return existingResume;
    }

    public void deleteResume(String resumeId,  Object principal) {

        //get the current profile
        AuthResponse response = authService.getProfile(principal);
        //call the repo finder method
        Resume existingResume = resumeRepository.findByUserIdAndId(response.getId(),resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        resumeRepository.delete(existingResume);

    }
}
