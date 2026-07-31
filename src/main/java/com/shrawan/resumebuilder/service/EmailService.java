package com.shrawan.resumebuilder.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${spring.mail.properties.mail.smtp.from}")
    private String fromEmail;

    private final JavaMailSender mailSender;

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        log.info("inside the EmailService - sendHtmlEmail(): {}, {}, {}", to, subject, htmlContent);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    @Async
    public void sendEmailWithAttachment(String to, String subject, String body, byte[] attachment, String filename) throws MessagingException {
        log.info("inside the EmailService - sendEmailWithAttachment(): to={}, subject={}", to, subject);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);

        if (attachment != null && attachment.length > 0) {
            String attachmentName = (filename != null && !filename.isBlank()) ? filename : "resume.pdf";
            helper.addAttachment(attachmentName, new ByteArrayResource(attachment));
        }
        mailSender.send(message);
    }
}
