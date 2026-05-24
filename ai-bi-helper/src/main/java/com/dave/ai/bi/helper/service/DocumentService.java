package com.dave.ai.bi.helper.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    void handleDocument(MultipartFile file);
}
