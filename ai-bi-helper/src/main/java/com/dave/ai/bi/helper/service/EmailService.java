package com.dave.ai.bi.helper.service;

import java.io.File;

public interface EmailService {

    void sendEmail(String to, String content);

    void sendEmail(String to, File file);


}
