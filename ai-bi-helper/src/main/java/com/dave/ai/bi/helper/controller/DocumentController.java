package com.dave.ai.bi.helper.controller;

import com.dave.ai.bi.helper.service.DocumentService;
import com.dave.common.domain.vo.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("document")
public class DocumentController {

    @Resource
    private DocumentService documentService;

    @PostMapping(value = "upload", consumes = "multipart/form-data")
    public R upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }
        documentService.handleDocument(file);

        return R.success();
    }

}
