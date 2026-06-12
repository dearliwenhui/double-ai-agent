# AI BI Helper 架构图生成说明

本文档用于将 `ai-bi-helper` 项目的架构和核心流程描述给 GPT、DALL-E、Midjourney、可视化绘图模型或其他架构图生成工具，用于生成系统架构图、NL2SQL 流程图、Agent 工作流图和数据/RAG 架构图。

## 项目架构摘要

`ai-bi-helper` 是一个基于 Spring Boot 和 Spring AI 的 BI SQL Copilot / NL2SQL 项目。

核心能力是：用户输入业务问题，系统通过 RAG 检索数据库 schema，上下文喂给大模型生成 MySQL SQL，再由大模型审核 SQL，最后支持预览、导出 CSV、邮件发送。

主要组成：

- 前端：`src/main/resources/static/sql-chat/index.html`，静态 SQL Copilot 工作台。
- 后端：Spring Boot，端口 `8081`。
- AI 能力：Spring AI `ChatClient`，OpenAI-compatible 接口。
- RAG：Schema 文档切分、向量检索、规则重排、关系图解析、上下文组装。
- 向量库：Milvus，存储 schema chunks。
- 数据库：MySQL，包含业务星型模型和 SQL 会话表。
- 工作流：Alibaba Spring AI Graph，生成 SQL -> 审核 -> 执行 SQL -> 导出 CSV -> 发邮件。
- 安全：只允许 `SELECT / WITH`，拦截 `INSERT / UPDATE / DELETE / DROP / ALTER` 等危险 SQL。

注意：生成架构图时不要展示配置文件中的数据库密码、API Key、邮箱密码、Token 等敏感信息。

## 提示词 1：总体系统架构图

```text
请生成一张 16:9 的企业级系统架构图，中文标签，风格清晰、现代、扁平化，不要卡通风格。

系统名称：AI BI Helper / SQL Copilot

架构从左到右分为 5 层：

1. 用户层
- BI 用户
- 浏览器中的 SQL Copilot 工作台

2. 接入层
- Spring Boot REST API，端口 8081
- /sql-chat：会话、消息、SQL 预览、CSV 导出
- /document/upload：上传 schema 文档
- /genSql/talk：图工作流入口
- SSE 流式事件：/sql-chat/message/stream

3. 应用服务层
- SqlChatApplicationService：编排对话式 SQL 生成流程
- SqlChatSessionService：会话和消息管理
- SqlGenerationService：生成 SQL
- SqlEvaluationService：审核 SQL
- SqlPreviewService：预览前 20 行
- CsvExportService：导出 CSV
- SqlSafetyGuardService：只读 SQL 安全校验

4. AI 与 RAG 层
- ChatClient / OpenAI-compatible LLM：问题改写、SQL 生成、SQL 审核
- SchemaRetrievalPipeline：schema 检索流水线
- QueryRewriter：把自然语言问题改写成检索意图
- VectorStore / Milvus：schema 向量检索
- SchemaGraphResolver：解析事实表、维度表、JOIN 路径
- RuleBasedChunkReranker：规则重排
- SchemaContextAssembler：组装最终 schema 上下文

5. 数据与外部系统层
- MySQL：业务数据表 fact_sales、fact_inventory、dim_product、dim_store、dim_customer、dim_date
- MySQL：sql_chat_session、sql_chat_message
- schema.md：数据库结构说明文档
- output CSV：查询结果文件
- SMTP Mail：邮件发送 CSV 附件

请用箭头表示调用链：
用户 -> 静态页面 -> REST API -> 应用服务层 -> RAG 检索 -> LLM 生成 SQL -> LLM 审核 -> MySQL 预览/查询 -> CSV 导出 -> 邮件发送。
突出显示“只读 SQL 安全校验”和“RAG schema 上下文增强”两个关键能力。
```

## 提示词 2：NL2SQL 核心流程图

```text
请生成一张中文流程图，展示 AI BI Helper 的自然语言生成 SQL 流程，横向布局，简洁专业。

流程节点如下：

1. 用户输入业务问题
例如：按城市统计最近一个月销售额

2. QueryRewriter
调用大模型，将问题解析为结构化检索意图：
topics、metrics、dimensions、filters、needTime、needRelation、queryMode

3. SchemaRetrievalPipeline
并行检索四类 schema chunk：
- table overview
- field definition
- table relation
- business rules

4. Milvus 向量库
根据改写后的查询进行相似度检索

5. SchemaGraphResolver
选择主事实表，例如 fact_sales 或 fact_inventory
解析维度表，例如 dim_product、dim_store、dim_customer、dim_date
解析 JOIN 路径

6. RuleBasedChunkReranker
根据主题、指标、维度、时间字段、关系路径进行重排

7. SchemaContextAssembler
组装最小可用 schema 上下文：
主事实表、候选维表、必需字段、JOIN 路径、业务规则、SQL 示例

8. SqlGenerationService
调用 LLM，根据用户问题和 schema 上下文生成 MySQL SELECT SQL

9. SqlEvaluationService
调用 LLM 审核 SQL：
意图匹配、MySQL 兼容、安全性、性能、是否幻觉字段

10. SqlSafetyGuardService
执行前再次校验：
只允许 SELECT / WITH
禁止 INSERT、UPDATE、DELETE、DROP、ALTER、TRUNCATE 等

11. MySQL 执行预览
包装为 SELECT * FROM (...) LIMIT 20

12. 用户确认后导出 CSV

请在图中用不同颜色区分：
- 蓝色：应用服务
- 紫色：大模型调用
- 绿色：RAG / 向量检索
- 橙色：安全审核
- 灰色：数据库与文件输出
```

## 提示词 3：图工作流架构图

```text
请生成一张中文的 AI Agent 图工作流架构图，标题为“AI BI Helper Graph Workflow”。

使用节点和有向边展示：

START
-> GenSQLNode
-> EvaluateNode
-> 条件判断 EvaluateEdge

EvaluateEdge 逻辑：
- 如果 evaluateResult.pass = false 且 evaluateCount <= 5，回到 GenSQLNode 重新生成 SQL
- 如果 evaluateResult 为空或 evaluateCount > 5，流程结束 END
- 如果 pass = true，进入 ExecSqlAndCreateCsvNode

ExecSqlAndCreateCsvNode
-> SendEmailNode
-> END

节点说明：
GenSQLNode：调用 RAG 检索 schema 上下文，调用 LLM 生成 MySQL SQL
EvaluateNode：调用 LLM 审核 SQL 安全性、正确性、MySQL 兼容性
ExecSqlAndCreateCsvNode：使用 JdbcTemplate 查询 MySQL，生成 CSV 文件
SendEmailNode：通过 SMTP 邮件发送 CSV 附件

图中额外显示外部依赖：
- ChatClient / LLM 连接 GenSQLNode 和 EvaluateNode
- SchemaRetrievalPipeline / Milvus 连接 GenSQLNode 和 EvaluateNode
- MySQL 连接 ExecSqlAndCreateCsvNode
- output/query-result-*.csv 连接 ExecSqlAndCreateCsvNode
- SMTP Mail 连接 SendEmailNode

请突出“失败自动重试生成 SQL，最多 5 次审核循环”的闭环。
```

## 提示词 4：数据模型 / RAG 知识库图

```text
请生成一张中文数据架构图，展示 AI BI Helper 的 BI 数据模型和 RAG schema 知识库。

左侧：业务 MySQL 星型模型
事实表：
- fact_sales：销售事实表，字段包括 sales_id、product_id、store_id、customer_id、date_id、quantity、sales_amount、discount
- fact_inventory：库存事实表，字段包括 inventory_id、product_id、store_id、date_id、quantity

维度表：
- dim_product：商品维度，product_id、product_name、category_name、brand、cost_price、retail_price
- dim_store：门店维度，store_id、store_name、province、city、address、open_date
- dim_customer：客户维度，customer_id、customer_name、gender、age、city、province
- dim_date：时间维度，date_id、year、quarter、month、day、weekday

关系：
fact_sales.product_id -> dim_product.product_id
fact_sales.store_id -> dim_store.store_id
fact_sales.customer_id -> dim_customer.customer_id
fact_sales.date_id -> dim_date.date_id
fact_inventory.product_id -> dim_product.product_id
fact_inventory.store_id -> dim_store.store_id
fact_inventory.date_id -> dim_date.date_id

右侧：RAG 知识库构建
schema.md / 上传文档
-> TikaDocumentReader
-> BiSchemaMarkdownSplitter
-> schema chunks：
table_overview、table_field_batch、table_relation、business_rules、sql_relation_example
-> SchemaDocumentRepository 内存元数据注册表
-> Milvus VectorStore 向量库

中间：NL2SQL 使用这些 schema 信息生成 SQL。
请用箭头连接“schema 文档”到“向量库”和“元数据注册表”，再连接到“SQL 生成服务”。
```

## 推荐使用方式

建议优先生成两张图：

1. 总体系统架构图：使用“提示词 1”，适合放在技术方案、项目介绍、答辩 PPT。
2. NL2SQL 核心流程图：使用“提示词 2”，适合解释 AI 生成 SQL 的核心链路。

如果需要解释 Agent 自动化执行和重试闭环，再使用“提示词 3”。

如果需要解释数据表关系、schema 文档如何进入 RAG 知识库，再使用“提示词 4”。
