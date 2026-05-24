package com.dave.ai.bi.helper.edges;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.apache.commons.lang3.StringUtils;

public class EvaluateEdge implements EdgeAction {
    @Override
    public String apply(OverAllState state) throws Exception {
        Integer evaluateCount = state.value("evaluateCount", 0);
        // 如果进行了5轮评估，直接结束
        if (evaluateCount > 5) {
            return StateGraph.END;
        }
        // 获取 EvaluateNode的评估结果
        String evaluateResultJson = state.value("evaluateResult", "");
        if (StringUtils.isBlank(evaluateResultJson)) {
            // 上一步说去到的SQL为null，不用重试
            return StateGraph.END;
        }
        JSONObject entries = JSONUtil.parseObj(evaluateResultJson);
        Boolean pass = entries.getBool("pass");
        if (!pass) {
            return "GenSQLNode";
        }

        return "ExecSqlAndCreateCsvNode";
    }
}
