package com.dave.ai.transfer.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.Map;

public class HumanApprovalNode implements NodeAction {


    /**
     * 在到达这个节点之前会进行阻塞，等待人类决策，获取决策结果，再决定nextStep
     * @param state
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Map<String, Object> data = state.humanFeedback().data();
        Boolean approval = (Boolean) data.getOrDefault("approval", false);
        String nextStep = StateGraph.END;

        if (approval) {
            nextStep = "createInventoryTransferNode";
        }
        return Map.of("humanApprovalNextStep", nextStep);
    }

}
