package com.dave.ai.bi.helper.service.impl;

import com.dave.ai.bi.helper.service.EmailService;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class EmailServiceImpl implements EmailService {

    @Value("${spring.mail.username}")
    private String from;

    @Resource
    private JavaMailSender javaMailSender;

    private static final String DEFAULT_TEXT_SUBJECT = "BI Helper Notification";
    private static final String DEFAULT_ATTACHMENT_SUBJECT = "BI Helper Attachment";


    @Override
    public void sendEmail(String to, String content) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject(DEFAULT_TEXT_SUBJECT);
            helper.setText(content);
            javaMailSender.send(mimeMessage);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send email", ex);
        }
    }

    @Override
    public void sendEmail(String to, File file) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject(DEFAULT_ATTACHMENT_SUBJECT);
            helper.setText("Please check the attached file.");
            FileSystemResource fileSystemResource = new FileSystemResource(file);
            helper.addAttachment(file.getName(), fileSystemResource);
            javaMailSender.send(mimeMessage);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send email with attachment", ex);
        }
    }


}
