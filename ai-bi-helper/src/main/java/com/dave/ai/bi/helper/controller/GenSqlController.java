package com.dave.ai.bi.helper.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.dave.common.domain.vo.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/genSql")
public class GenSqlController {

    @Resource
    private CompiledGraph graph;

    @GetMapping("/talk")
    public R<Map<String, Object>> talk(@RequestParam String userInput, @RequestParam String userId) {
        RunnableConfig build = RunnableConfig.builder().threadId("GenSql_"+userId).build();
        OverAllState state = graph.call(Map.of("userInput", userInput
                , "to", "dearliwenhui@qq.com"
                , "evaluateCount", 0
        ), build).get();
        Map<String, Object> data = state.data();
        return R.success(data);
    }
}
