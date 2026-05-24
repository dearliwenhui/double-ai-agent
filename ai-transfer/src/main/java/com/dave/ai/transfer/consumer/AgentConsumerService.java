package com.dave.ai.transfer.consumer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.dave.ai.transfer.constants.KeyPrefixConstant;
import com.dave.ai.transfer.constants.TopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AgentConsumerService {

    @Resource
    private CompiledGraph graph;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @KafkaListener(topics = TopicConstant.PRODUCT_SALE_TOPIC, groupId = "agent-group")
    public void consumeProductSale(List<String> messages, Acknowledgment acknowledgment) throws Exception {
        if (messages == null || messages.isEmpty()) {
            log.warn("Ignore empty product sale batch");
            acknowledgment.acknowledge();
            return;
        }

        try {
            for (String message : messages) {
                if (message == null || message.isBlank()) {
                    log.warn("Ignore empty product sale message");
                    continue;
                }

                JSONObject jsonObject = JSONUtil.parseObj(message);
                String productId = jsonObject.getStr("productId");
                String warehouseId = jsonObject.getStr("warehouseId");
                Integer quantity = jsonObject.getInt("quantity");
                Date saleDate = jsonObject.getDate("saleDate");
                String key = KeyPrefixConstant.PRODUCT_SALE_ONLY_KEY + ":" + productId + ":" + warehouseId + ":" + saleDate;
                Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(key, message, Duration.ofDays(1));
                if (Boolean.FALSE.equals(locked)) {
                    log.info("Ignore duplicated product sale event, key: {}", key);
                    continue;
                }

                String executionId = IdUtil.simpleUUID();
                RunnableConfig runnableConfig = RunnableConfig.builder()
                        .threadId(executionId)
                        .build();

                log.info("Received product sale event, productId: {}, executionId: {}", productId, executionId);
                OverAllState overAllState = graph.call(Map.of(
                        "productId", productId,
                        "executionId", executionId
                ), runnableConfig).get();
                log.info("Product sale event processed, executionId: {}, stateKeys: {}", executionId, overAllState.data().keySet());
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Product sale batch failed, productIds: {}", messages, e);
            throw e;
        }
    }
}
