package com.dave.ai.transfer.service.biz.impl;

import com.dave.ai.transfer.service.biz.EmailService;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    @Value("${spring.mail.username}")
    private String from;

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private TemplateEngine templateEngine;

    @Override
    public void sendTemplateEmail(String to, String subject, Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null && !variables.isEmpty()) {
            variables.forEach(context::setVariable);
        }

        String html = templateEngine.process("EmailTemplate", context);

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject(subject);
            helper.setText(html, true);
            javaMailSender.send(mimeMessage);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send template email", ex);
        }
    }

}
