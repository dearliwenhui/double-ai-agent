package com.dave.ai.transfer.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
public class PredictNode implements NodeAction {

    private final ChatClient chatClient;

    public PredictNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String productId = state.value("productId", "");
        String saleRecordData = state.value("saleRecordData", "");
        String nowProductInventoryData = state.value("nowProductInventoryData", "");
        String inventoryOrderData = state.value("inventoryOrderData", "");

        StringBuilder sb=new StringBuilder();
        chatClient.prompt()
                .system("""
                        你是库存调拨决策引擎，也是一名专业的供应链分析师和库存管理专家。
                        你的任务是根据用户提供的商品销售数据、库存数据、仓库数据、在途订单数据和日期信息，
                        分析销售趋势、预测未来需求、评估库存平衡情况，并生成一份科学合理的库存调拨单。
                        
                        你必须完成以下分析：
                        1. 分析销售趋势、季节性规律、促销影响、同比/环比变化；
                        2. 预测未来需求，并评估预测的不确定性；
                        3. 计算安全库存水平；
                        4. 识别库存过剩仓与库存短缺仓；
                        5. 生成最优仓间调拨建议；
                        6. 若数据不足，允许做最小必要假设，但必须在 comment 字段中明确说明；
                        7. 禁止凭空编造输入中不存在的业务事实。
                        
                        你必须严格遵守以下输出规则：
                        1. 最终输出只能是 JSON；
                        2. 不允许输出 Markdown，不允许输出代码块，不允许输出 JSON 之外的任何文字；
                        3. 一次只允许输出一个调拨单对象；
                        4. 只允许一个 sourceWarehouseId 和一个 targetWarehouseId；
                        5. 如果多个商品从同一源仓调往同一目标仓，可以放在 items 数组中；
                        6. 若没有合理调拨建议，仍需输出合法 JSON，items 返回空数组；
                        7. comment 字段必须输出适合邮件/Thymeleaf 模板直接展示的简洁摘要型 HTML，不得输出完整推理链或冗长报告。只允许使用 div、p、strong、br、ul、li 标签，内容控制在 3~6 个要点内，必须包含调拨建议、核心原因、风险提示或假设说明；
                           comment 中禁止出现 style、script、table、img、a、h1-h6 等标签，只允许使用以下 HTML 标签：div、p、strong、br、ul、li；
                        8. transferQuantity 必须是明确数值，禁止输出公式、范围或模糊描述；
                        9. actualQuantity 固定为 0；
                        10. status 固定为 0；
                        11. createdBy 固定为 "AI智能助手"；
                        12. transferType 固定为 1；
                        13. transferDate 格式必须为 yyyy-MM-dd；
                        14. 顶层字段必须且只能包含：
                        sourceWarehouseId,sourceWarehouseCode,sourceWarehouseName, targetWarehouseId,targetWarehouseCode,targetWarehouseName, status, createdBy, transferType, transferDate, comment, items。
                        
                        输出 JSON 结构如下：
                        {
                          "sourceWarehouseId": 1,
                          "sourceWarehouseCode": "WH001",
                          "sourceWarehouseName": "华北仓",
                          "targetWarehouseId": 2,
                          "targetWarehouseCode": "WH002",
                          "targetWarehouseName": "华东仓",
                          "status": 0,
                          "createdBy": "AI智能助手",
                          "transferType": 1,
                          "transferDate": "2026-05-14",
                          "comment": "<div><p><strong>调拨建议：</strong>从华北仓(WH001)向华东仓(WH002)调拨智能手机(P001) <strong>80</strong> 件。</p><p><strong>核心原因：</strong></p><ul><li><strong>需求旺盛：</strong>华东仓历史销量极高（2025 Q1达1034件），当前库存129件，相对于其高销量的历史表现，库存水位偏低，存在缺货风险。</li><li><strong>库存充裕：</strong>华北仓当前库存262件，且近期（2025 Q4）销量为518件，显示其备货充足，有能力支持调出。</li><li><strong>平衡策略：</strong>华南仓库存160件，近期销量327件，虽也有一定压力，但华东仓作为传统高销区，优先保障其供应以最大化销售机会。</li></ul><p><strong>风险提示/假设：</strong></p><ul><li>假设未来季度销售趋势与历史高峰期（如2025 Q1华东、2025 Q4华北）保持正相关或季节性回升。</li><li>在途订单多为2024-2025年数据，对当前2026年5月的即时库存影响已结算或不再计入，故主要依据现有库存与历史销售能力判断。</li></ul></div>",
                          "items": [
                            {
                              "productId": 11,
                              "transferQuantity": 100,
                              "actualQuantity": 0,
                              "remark": "商品调拨备注"
                            }
                          ]
                        }
                        
                        最终回复时，只返回合法 JSON。
                        如果输出的内容不是合法 JSON，则视为任务失败，请重新修正后只输出合法 JSON。
                        """)
                .user(promptUserSpec -> promptUserSpec.text(
                        """
                                请基于以下数据生成库存调拨建议，并按 system 要求只返回合法 JSON。
                                商品ID: {productId}
                                历史销售数据: {saleRecordData}
                                当前库存数据: {nowProductInventoryData}
                                在途/采购订单数据: {inventoryOrderData}
                                当前日期: {currentDate}
                                
                                要求：
                                    1. 分析销量趋势和库存风险；
                                    2. 结合在途数据判断是否建议调拨；
                                    3. 若建议调拨，只输出一份调拨单；
                                    4. 若不建议调拨，items 返回空数组；
                                    5. 所有理由、假设和风险写入 comment；
                                    6. 不要编造不存在的数据。
                                """
                ).params(Map.of("productId", productId
                        , "saleRecordData", saleRecordData
                        , "nowProductInventoryData", nowProductInventoryData
                        , "inventoryOrderData", inventoryOrderData
                        , "currentDate", LocalDateTime.now())))
                .stream().content().doOnNext(sb::append).blockLast();
        String content = sb.toString().trim();
        log.info("PredictNode success, productId: {}, prediction:  {}", productId, content);
/*            String content = chatClient.prompt()
                .user(promptUserSpec -> promptUserSpec.text(
                        "User input: {content} Generate a transfer recommendation."
                ).param("content", productId + saleRecordData + nowProductInventoryData + inventoryOrderData))
                .call().content();*/

        return Map.of("inventoryTransferStr", content);
    }
}
