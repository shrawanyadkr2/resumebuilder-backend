package com.shrawan.resumebuilder.controller;

import com.shrawan.resumebuilder.service.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/email")
@Slf4j
public class EmailController {

    private final EmailService emailService;

    @PostMapping(value = "/send-resume",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> sendResumeByEmail(
            @RequestPart("recipientEmail") String recipientEmail,
            @RequestPart("subject") String subject,
            @RequestPart("message") String message,
            @RequestPart("pdfFile") MultipartFile pdfFile
    ) throws MessagingException, IOException {
        //1. validate the inputs
        Map<String , Object> response = new HashMap<>();
        if(Objects.isNull(recipientEmail) || Objects.isNull(pdfFile)){
            response.put("success", false);
            response.put("message","missing required fields");
            return ResponseEntity.badRequest().body(response);
        }

        //2. get the file data
        byte[] pdfBytes = pdfFile.getBytes();
        String originalFileName = pdfFile.getOriginalFilename();
        String filename = Objects.nonNull(originalFileName) ? originalFileName: "resume.pdf";

        //3. prepare the email content
        String emailSubject = Objects.nonNull(subject) ? subject:"Resume Application";
        String emailBody = Objects.nonNull(message) ? message :"please find mu resume attached. \n\n Best regards";

        //4. call the service method
        emailService.sendEmailWithAttachment(recipientEmail,emailSubject,emailBody,pdfBytes,filename);

        //return response
        response.put("success",true);
        response.put("message","Resume sent successfully to "+recipientEmail);
        return ResponseEntity.ok(response);

    }
}
