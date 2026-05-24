package com.dave.ai.transfer.service.biz;

import java.util.Map;

public interface EmailService {

    void sendTemplateEmail(String to, String subject, Map<String, Object> variables);

}
