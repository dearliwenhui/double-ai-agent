package com.dave.ai.bi.helper.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.dave.ai.bi.helper.service.EmailService;

import java.io.File;
import java.util.Map;
import java.util.Optional;

public class SendEmailNode implements NodeAction {

    private final EmailService emailService;

    public SendEmailNode(EmailService emailService) {
        this.emailService = emailService;
    }


    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Optional<String> csvFilePath = state.value("csvFilePath");
        String to = state.value("to", "");
        if (csvFilePath.isEmpty() || csvFilePath.get().isBlank()) {
            emailService.sendEmail(to, "查询数据不存在");

        } else {
            emailService.sendEmail(to, new File(csvFilePath.get()));
        }

        return Map.of();
    }
}
