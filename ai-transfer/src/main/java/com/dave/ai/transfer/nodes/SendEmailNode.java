package com.dave.ai.transfer.nodes;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.dave.ai.transfer.service.biz.EmailService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendEmailNode implements NodeAction {
    private final EmailService emailService;
    private final String approvalBaseUrl;

    public SendEmailNode(EmailService emailService, String approvalBaseUrl) {
        this.emailService = emailService;
        this.approvalBaseUrl = approvalBaseUrl;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String inventoryTransferJsonStr = state.value("inventoryTransferJsonStr", "");
        String executionId = state.value("executionId", "");
        if (StringUtils.isBlank(inventoryTransferJsonStr)) {
            return Map.of();
        }

        JSONObject entries = JSONUtil.parseObj(inventoryTransferJsonStr);
        String sourceWarehouseId = safeString(entries.get("sourceWarehouseId"));
        String targetWarehouseId = safeString(entries.get("targetWarehouseId"));
        String transferDate = entries.getStr("transferDate", "");
        String comment = entries.getStr("comment", "");

        Map<String, Object> variables = new HashMap<>();
        variables.put("recipientName", entries.getStr("createdBy", "AI智能助手"));
        variables.put("orderNo", entries.getStr("orderNo", ""));
        variables.put("sourceWarehouseId", sourceWarehouseId);
        variables.put("targetWarehouseId", targetWarehouseId);
        variables.put("sourceWarehouseName", entries.getStr("sourceWarehouseName", "仓库ID " + sourceWarehouseId));
        variables.put("targetWarehouseName", entries.getStr("targetWarehouseName", "仓库ID " + targetWarehouseId));
        variables.put("transferDate", transferDate);
        variables.put("comment", comment);
        variables.put("adoptLink", buildApprovalLink(true, executionId));
        variables.put("rejectLink", buildApprovalLink(false, executionId));
        variables.put("itemList", normalizeItemList(entries.getJSONArray("items")));

        emailService.sendTemplateEmail("dearliwenhui@qq.com", "AI智能库存调拨单通知", variables);
        return Map.of();
    }

    private String buildApprovalLink(boolean approval, String executionId) {
        if (StringUtils.isBlank(approvalBaseUrl)) {
            return "";
        }
        return approvalBaseUrl + "?approval=" + approval + "&executionId=" + executionId;
    }

    private List<Map<String, Object>> normalizeItemList(JSONArray items) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return result;
        }

        for (Object itemObject : items) {
            JSONObject item = itemObject instanceof JSONObject
                    ? (JSONObject) itemObject
                    : JSONUtil.parseObj(itemObject);

            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("productId", item.get("productId"));
            itemMap.put("productCode", item.getStr("productCode", ""));
            itemMap.put("productName", item.getStr("productName", ""));
            itemMap.put("spec", item.getStr("spec", ""));
            itemMap.put("unit", item.getStr("unit", ""));
            itemMap.put("transferQuantity", item.get("transferQuantity"));
            itemMap.put("actualQuantity", item.get("actualQuantity"));
            itemMap.put("remark", item.getStr("remark", ""));
            result.add(itemMap);
        }

        return result;
    }

    private String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
