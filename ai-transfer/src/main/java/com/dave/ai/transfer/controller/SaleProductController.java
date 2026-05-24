package com.dave.ai.transfer.controller;

import cn.hutool.core.util.IdUtil;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.dave.ai.transfer.domain.param.ProductSaleParam;
import com.dave.ai.transfer.service.biz.ProductSaleService;
import com.dave.common.domain.vo.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/saleProduct")
public class SaleProductController {

    @Resource
    private ProductSaleService productSaleService;

    @PostMapping("/sale")
    public R sale(@RequestBody ProductSaleParam productSaleParam) {
        productSaleService.sale(productSaleParam);

        return R.success();

    }

    @GetMapping("approval")
    public R approval(@RequestParam Boolean approval,
                      @RequestParam String executionId) {
        productSaleService.approval(approval, executionId);
        return R.success();
    }
}
