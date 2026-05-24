package com.dave.ai.bi.helper.service.impl;

import com.dave.ai.bi.helper.service.DocumentService;
import com.dave.ai.bi.helper.spliter.BiSchemaMarkdownSplitter;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final int VECTOR_STORE_BATCH_SIZE = 64;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private BiSchemaMarkdownSplitter biSchemaMarkdownSplitter;

    /**
     * <pre>
     * 业务逻辑
     * 1. 读取文档 : https://docs.spring.io/spring-ai/reference/1.0/api/etl-pipeline.html
     *      使用 tika：https://docs.spring.io/spring-ai/reference/1.0/api/etl-pipeline.html#_tika_docx_pptx_html
     * 2. 进行分文档拆分，拆成不同的块
     * 3. 借助向量大模型，将文本块转成向量
     * 4. 入库
     * </pre>
     *
     * @param file 上传的文档文件
     */
    @Override
    public void handleDocument(MultipartFile file) {
        // 先用 Tika 读取上传文档，再交给自定义 splitter 按 schema 语义拆分。
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(file.getResource());
        List<Document> documents = tikaDocumentReader.get();
        List<Document> splitDocuments = biSchemaMarkdownSplitter.apply(documents);

        // 向量化接口单次请求条数有限，入库前按固定批次拆开提交。
        addDocumentsInBatch(splitDocuments);
    }

    private void addDocumentsInBatch(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        // 每批控制在模型允许的上限内，避免 embedding 接口返回 413。
        for (int index = 0; index < documents.size(); index += VECTOR_STORE_BATCH_SIZE) {
            int end = Math.min(index + VECTOR_STORE_BATCH_SIZE, documents.size());
            vectorStore.add(documents.subList(index, end));
        }
    }
}
