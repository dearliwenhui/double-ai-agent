package com.dave.ai.transfer.nodes;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dave.ai.transfer.domain.vo.SaleRecordVo;
import com.dave.ai.transfer.model.BbInventory;
import com.dave.ai.transfer.service.base.BbInventoryService;
import com.dave.ai.transfer.service.biz.SaleRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
public class CollectSaleRecordNode implements NodeAction {

    private final SaleRecordService saleRecordService;
    private final BbInventoryService bbInventoryService;

    public CollectSaleRecordNode(SaleRecordService saleRecordService, BbInventoryService bbInventoryService) {
        this.saleRecordService = saleRecordService;
        this.bbInventoryService = bbInventoryService;
    }

    /**
     * 实际业务逻辑
     * 1. 从状态机获取商品ID
     * 2. 写SQL查询历史商品数据
     * 维度： 年份、季度、仓库、商品
     * 3. 写SQL查询商品剩余库存
     *
     * @param state
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String productId = state.value("productId", "");
        log.info("CollectSaleRecordNode start, productId: {}", productId);
        if (StringUtils.isBlank(productId)) {
            log.warn("CollectSaleRecordNode skip due to empty productId");
            return Map.of();
        }
        List<SaleRecordVo> saleRecordVos = saleRecordService.collectSaleRecordDataByProductId(Integer.valueOf(productId));
        // 将获取到的对象转换成json
        String saleRecordData = JSONUtil.toJsonStr(saleRecordVos);
        log.info("CollectSaleRecordNode success, record size: {}", saleRecordVos.size());

        List<BbInventory> bbInventories = bbInventoryService.list(
                Wrappers.lambdaQuery(BbInventory.class).eq(BbInventory::getProductId, productId)
        );
        String nowProductInventoryData = JSONUtil.toJsonStr(bbInventories);
        return Map.of("saleRecordData", saleRecordData,
                "nowProductInventoryData", nowProductInventoryData);
    }

}
