# 企业智能BI数据库表结构说明文档

## 文档元信息
- 项目名称：ai-helper
- 文档用途：用于 RAG 知识库构建与 AI-SQL-Agent 使用
- 数据库：MySQL
- 更新时间：2026-05-21

## 1. 文档目的
本文档用于描述企业智能 BI 系统涉及的数据库表结构、字段定义、主外键关系、业务口径及常用 SQL 关系，供后续 RAG 系统及 AI-SQL-Agent 使用。

## 2. 表清单
- dim_customer：客户维度表
- dim_date：时间维度表
- dim_product：商品维度表
- dim_store：门店维度表
- fact_inventory：库存事实表
- fact_sales：销售事实表

## 3. 表定义

### TABLE: dim_customer
#### 中文名称
客户维度表

#### 表类型
维度表

#### 表用途
存储客户基础属性信息，用于销售分析中的客户画像、客户地域分布、年龄分析及性别分析。

#### 主键
- customer_id

#### 唯一键
- 无

#### 字段定义
| 字段名 | 类型 | 是否主键 | 描述 |
|---|---|---|---|
| customer_id | bigint | YES | 客户ID |
| customer_name | varchar(255) | NO | 客户名称 |
| gender | varchar(10) | NO | 性别 |
| age | int | NO | 年龄 |
| city | varchar(100) | NO | 所在城市 |
| province | varchar(100) | NO | 所在省份 |

#### 关联关系
- fact_sales.customer_id -> dim_customer.customer_id

#### 业务口径
- customer_id 为客户主键
- customer_name 为客户名称
- gender 可用于性别分析
- age 可用于年龄层分析
- city、province 可用于客户地域分析

#### 常用查询场景
- 查询客户基础信息
- 按客户性别统计销售额
- 按客户年龄段统计销售数量
- 按客户地域分析销售分布

---

### TABLE: dim_date
#### 中文名称
时间维度表

#### 表类型
维度表

#### 表用途
存储标准日期维度信息，用于销售、库存等业务按年、季度、月、日进行统计分析。

#### 主键
- date_id

#### 唯一键
- 无

#### 字段定义
| 字段名 | 类型 | 是否主键 | 描述 |
|---|---|---|---|
| date_id | date | YES | 日期ID |
| year | int | NO | 年份 |
| quarter | int | NO | 季度 |
| month | int | NO | 月份 |
| day | int | NO | 日 |
| weekday | int | NO | 星期 |

#### 关联关系
- fact_inventory.date_id -> dim_date.date_id
- fact_sales.date_id -> dim_date.date_id

#### 业务口径
- date_id 为日期主键
- year 用于年度统计
- quarter 用于季度统计
- month 用于月度统计
- day 用于日度统计
- weekday 用于星期维度分析

#### 常用查询场景
- 按年统计销售额
- 按季度分析库存变化
- 按月统计销售趋势
- 按星期分析销售波动

---

### TABLE: dim_product
#### 中文名称
商品维度表

#### 表类型
维度表

#### 表用途
存储商品基础信息，用于销售分析、库存分析、分类分析、品牌分析及价格分析。

#### 主键
- product_id

#### 唯一键
- 无

#### 字段定义
| 字段名 | 类型 | 是否主键 | 描述 |
|---|---|---|---|
| product_id | bigint | YES | 商品唯一ID |
| product_name | varchar(255) | NO | 商品名称 |
| category_id | bigint | NO | 商品分类ID |
| category_name | varchar(255) | NO | 商品分类名称 |
| brand | varchar(255) | NO | 品牌 |
| cost_price | decimal(10,2) | NO | 成本价 |
| retail_price | decimal(10,2) | NO | 建议零售价 |

#### 关联关系
- fact_inventory.product_id -> dim_product.product_id
- fact_sales.product_id -> dim_product.product_id

#### 业务口径
- product_id 为商品主键
- category_id、category_name 用于分类分析
- brand 用于品牌分析
- cost_price 为成本价
- retail_price 为建议零售价

#### 常用查询场景
- 查询商品基础资料
- 按商品统计销售额
- 按商品分类统计库存数量
- 按品牌分析销售表现
- 分析销售价与成本价关系

---

### TABLE: dim_store
#### 中文名称
门店维度表

#### 表类型
维度表

#### 表用途
存储门店基础信息，用于门店销售分析、区域分析、库存分布分析及门店经营分析。

#### 主键
- store_id

#### 唯一键
- 无

#### 字段定义
| 字段名 | 类型 | 是否主键 | 描述 |
|---|---|---|---|
| store_id | bigint | YES | 门店唯一ID |
| store_name | varchar(255) | NO | 门店名称 |
| province | varchar(100) | NO | 所在省份 |
| city | varchar(100) | NO | 所在城市 |
| address | varchar(255) | NO | 门店详细地址 |
| open_date | date | NO | 开业日期 |

#### 关联关系
- fact_inventory.store_id -> dim_store.store_id
- fact_sales.store_id -> dim_store.store_id

#### 业务口径
- store_id 为门店主键
- store_name 为门店名称
- province、city 用于区域分析
- address 为门店详细地址
- open_date 为开业日期，可用于门店经营时长分析

#### 常用查询场景
- 查询门店基础资料
- 统计各门店销售额
- 统计各门店库存量
- 按省份或城市分析销售分布

---

### TABLE: fact_inventory
#### 中文名称
库存事实表

#### 表类型
事实表

#### 表用途
记录商品在门店维度、日期维度下的库存数据，用于库存分布分析、库存趋势分析及商品库存监控。

#### 主键
- inventory_id

#### 唯一键
- 无

#### 字段定义
| 字段名 | 类型 | 是否主键 | 描述 |
|---|---|---|---|
| inventory_id | bigint | YES | 库存记录ID |
| product_id | bigint | NO | 商品ID |
| store_id | bigint | NO | 门店ID |
| date_id | date | NO | 库存日期 |
| quantity | int | NO | 库存数量 |

#### 关联关系
- fact_inventory.product_id -> dim_product.product_id
- fact_inventory.store_id -> dim_store.store_id
- fact_inventory.date_id -> dim_date.date_id

#### 业务口径
- inventory_id 为库存事实主键
- quantity 表示指定日期、指定门店、指定商品的库存数量
- product_id 关联商品维度
- store_id 关联门店维度
- date_id 关联时间维度

#### 常用查询场景
- 查询指定日期各门店库存
- 按商品分析库存分布
- 按门店分析库存结构
- 按日期分析库存趋势

---

### TABLE: fact_sales
#### 中文名称
销售事实表

#### 表类型
事实表

#### 表用途
记录商品在门店、客户、日期等维度下的销售数据，用于销售额分析、销量分析、客户分析、折扣分析及多维经营分析。

#### 主键
- sales_id

#### 唯一键
- 无

#### 字段定义
| 字段名 | 类型 | 是否主键 | 描述 |
|---|---|---|---|
| sales_id | bigint | YES | 销售记录ID |
| product_id | bigint | NO | 商品ID |
| store_id | bigint | NO | 门店ID |
| customer_id | bigint | NO | 客户ID |
| date_id | date | NO | 销售日期 |
| quantity | int | NO | 销售数量 |
| sales_amount | decimal(10,2) | NO | 销售金额 |
| discount | decimal(10,2) | NO | 折扣金额 |

#### 关联关系
- fact_sales.product_id -> dim_product.product_id
- fact_sales.store_id -> dim_store.store_id
- fact_sales.customer_id -> dim_customer.customer_id
- fact_sales.date_id -> dim_date.date_id

#### 业务口径
- sales_id 为销售事实主键
- quantity 表示销售数量
- sales_amount 表示销售金额
- discount 表示折扣金额
- product_id 关联商品维度
- store_id 关联门店维度
- customer_id 关联客户维度
- date_id 关联时间维度

#### 常用查询场景
- 按商品统计销量和销售额
- 按门店统计销售业绩
- 按客户分析购买行为
- 按日期统计销售趋势
- 按折扣分析促销效果

## 4. 全局关系说明
- fact_sales 关联 dim_product、dim_store、dim_customer、dim_date
- fact_inventory 关联 dim_product、dim_store、dim_date
- 销售分析主线：fact_sales -> dim_product / dim_store / dim_customer / dim_date
- 库存分析主线：fact_inventory -> dim_product / dim_store / dim_date

## 5. AI-SQL-Agent 注意事项
- fact_sales.quantity 表示销售数量
- fact_inventory.quantity 表示库存数量
- fact_sales.sales_amount 表示销售金额
- fact_sales.discount 表示折扣金额，不等同于实收金额以外的其他费用
- fact_inventory 是按日期记录的库存事实表，可用于库存趋势分析
- 所有事实表查询都应优先关联对应维度表获取名称、分类、地域、时间等补充信息
- 生成 SQL 时应注意：
  - 商品名称来自 dim_product.product_name
  - 门店名称来自 dim_store.store_name
  - 客户名称来自 dim_customer.customer_name
  - 时间属性来自 dim_date

## 6. 常用 SQL 关系示例
- 销售表关联商品表：fact_sales.product_id = dim_product.product_id
- 销售表关联门店表：fact_sales.store_id = dim_store.store_id
- 销售表关联客户表：fact_sales.customer_id = dim_customer.customer_id
- 销售表关联时间表：fact_sales.date_id = dim_date.date_id
- 库存表关联商品表：fact_inventory.product_id = dim_product.product_id
- 库存表关联门店表：fact_inventory.store_id = dim_store.store_id
- 库存表关联时间表：fact_inventory.date_id = dim_date.date_id

## 7. BI 用途说明
这些表用于构建 BI 场景，如：
- 按门店、商品、日期进行销售分析
- 按客户维度进行用户画像分析
- GMV 统计
- 折扣效果分析
- 库存趋势分析
- 区域销售对比
- 热销商品分析
- 商品分类分析
