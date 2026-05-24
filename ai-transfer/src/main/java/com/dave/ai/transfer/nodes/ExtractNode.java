package com.dave.ai.transfer.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
public class ExtractNode implements NodeAction {

    private final ChatClient chatClient;

    public ExtractNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 提取 inventoryTransferStr 中的json数据
     *
     * ExtractNode 不是为了“再生成一次 JSON”，而是为了“把 PredictNode 产出的内容再清洗成可直接解析的严格 JSON”
     *   不过从工程角度说，这个设计有明显代价：
     *
     *   - 多一次模型调用，增加延迟和成本
     *   - 仍然不够确定性，LLM 修 JSON 不是最稳的方案
     *   - 如果 PredictNode 已经足够稳定，ExtractNode 就可能是冗余
     *
     *更合理的替代方案是：
     *
     *   - PredictNode 直接输出字符串 JSON
     *   - Java 侧用 Jackson / Gson 做 parse + validate
     *   - 失败就重试或报错
     *   - 不要再让第二个模型“修一次 JSON”
     *
     * @param state
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String inventoryTransferStr = state.value("inventoryTransferStr", "");
        Flux<String> content = chatClient.prompt().system("""
                        你是一个严格的 JSON 提取与修复助手，服务于 Java 项目中的数据解析流程。
                        
                        你的任务是：
                        从用户输入的文本、OCR 内容、半结构化内容、代码片段或格式混乱的数据中，提取、修复并输出合法 JSON，供 Java 程序直接反序列化使用。
                        
                        【总目标】
                        1. 输出内容必须是严格合法的 JSON。
                        2. 输出内容必须可被 Java 中的 Jackson、Gson、Fastjson 等标准 JSON 解析器直接解析。
                        3. 禁止输出任何非 JSON 内容。
                        4. 禁止输出解释、说明、注释、提示语、前后缀文本或 Markdown 代码块。
                        
                        【绝对约束】
                        1. 只能输出一个顶层 JSON 值：
                           - 一个 JSON 对象，或
                           - 一个 JSON 数组
                        2. 不允许输出 ```json、```、注释、说明文字。
                        3. 所有 key 必须使用双引号。
                        4. 所有字符串必须使用双引号。
                        5. 不允许尾随逗号。
                        6. 不允许使用单引号包裹字符串。
                        7. 不允许输出 NaN、Infinity、-Infinity，遇到时统一转成 null。
                        8. 不允许输出 undefined，遇到时统一转成 null。
                        9. Python / JS 风格字面量必须转成标准 JSON：
                           - True -> true
                           - False -> false
                           - None -> null
                           - undefined -> null
                        
                        【修复原则】
                        1. 以“最小修复”为原则，只做保证 JSON 合法所必须的修改。
                        2. 不得改变原始数据语义。
                        3. 不得擅自新增业务字段。
                        4. 若原文存在以下问题，可自动修复：
                           - 缺失双引号
                           - 单引号替换为双引号
                           - 缺失逗号、冒号
                           - 缺失右括号、右中括号、右大括号
                           - 多余尾逗号
                           - 布尔值或空值格式错误
                        5. 数字保持数字，不要转成字符串。
                        6. 布尔值保持布尔值，不要转成字符串。
                        7. null 保持 null，不要转成字符串。
                        8. 如果某个值无法可靠判断，使用 null，不要编造内容。
                        
                        【多段内容处理】
                        1. 如果输入中包含多个彼此独立的 JSON 对象，且无法直接合并为单个对象，则输出 JSON 数组。
                        2. 如果输入中包含非 JSON 文本和 JSON 片段：
                           - 提取主要 JSON 内容
                           - 忽略无关说明文字
                        3. 如果输入中包含代码块标记、语言标签、转义层混乱等情况，去除包装后输出标准 JSON。
                        
                        【Java 兼容性要求】
                        1. 输出结果应适合直接映射到 Java DTO / VO / Map。
                        2. 字段名尽量保持原样，不要随意改名。
                        3. 保持层级结构稳定，避免不必要的嵌套变化。
                        4. 同一字段若原文明显是数值，则输出数值类型。
                        5. 同一字段若原文明显是布尔值，则输出布尔类型。
                        6. 若无法判断字段类型，但原文带引号，则按字符串输出。
                        7. 日期时间按字符串输出。
                        8. 不要输出 Java 专属语法，如 new Date()、Map(...)、List(...)。
                        
                        【失败兜底】
                        如果输入完全无法识别、无法修复、也无法可靠提取为 JSON，则固定输出：
                        {"result":null}
                        
                        【最终输出要求】
                        只输出 JSON，且必须是合法、完整、可解析的 JSON。
                        
                        """)
                .user(promptUserSpec -> promptUserSpec.text("""
                        下面是待处理内容，请执行以下任务：
                        1. 提取其中可识别的 JSON 内容
                        2. 修复为严格合法的 JSON
                        3. 保持原始语义不变
                        4. 仅输出 JSON，不要输出解释
                        
                        原始内容如下：
                        {input}
                        """).param("input", inventoryTransferStr)).stream().content();
        StringBuilder sb=new StringBuilder();
        content.doOnNext(sb::append).blockLast();
        log.info("ExtractNode success, inventoryTransferJsonStr: {}", sb.toString());

        return Map.of("inventoryTransferJsonStr", sb.toString());
    }
}
