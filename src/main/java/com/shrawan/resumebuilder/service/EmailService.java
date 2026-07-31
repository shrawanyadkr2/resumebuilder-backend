package com.shrawan.resumebuilder.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${spring.mail.properties.mail.smtp.from:${spring.mail.username:shrawan29yadav@gmail.com}}")
    private String fromEmail;

    @Value("${brevo.api.key:${spring.mail.password:}}")
    private String brevoApiKey;

    private final JavaMailSender mailSender;

    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        log.info("inside the EmailService - sendHtmlEmail(): to={}, subject={}", to, subject);

        // 1. Try Brevo HTTP REST API (Port 443 - Bypasses all Railway firewall blocks)
        if (sendViaBrevoApi(to, subject, htmlContent)) {
            return;
        }

        // 2. Fallback to SMTP JavaMailSender
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent successfully via SMTP to {}", to);
        } catch (Exception ex) {
            log.error("Failed to send email via SMTP to {}. Error: {}", to, ex.getMessage(), ex);
            throw ex;
        }
    }

    private boolean sendViaBrevoApi(String to, String subject, String htmlContent) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            return false;
        }
        try {
            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("accept", "application/json");
            conn.setRequestProperty("api-key", brevoApiKey.trim());
            conn.setRequestProperty("content-type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);

            String escapedHtml = htmlContent.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
            String jsonPayload = String.format(
                "{\"sender\":{\"name\":\"ResumeBuilder PRO\",\"email\":\"%s\"},\"to\":[{\"email\":\"%s\"}],\"subject\":\"%s\",\"htmlContent\":\"%s\"}",
                fromEmail, to, subject, escapedHtml
            );

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                log.info("Email sent successfully via Brevo HTTP REST API (Port 443) to {}", to);
                return true;
            } else {
                if (conn.getErrorStream() != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder errResp = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) errResp.append(line);
                        log.warn("Brevo HTTP API returned status {}: {}", responseCode, errResp.toString());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Brevo HTTP API attempt failed: {}", ex.getMessage());
        }
        return false;
    }

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
