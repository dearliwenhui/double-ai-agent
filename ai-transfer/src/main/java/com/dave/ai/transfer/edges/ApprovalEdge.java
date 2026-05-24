package com.dave.ai.transfer.edges;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

public class ApprovalEdge implements EdgeAction {
    @Override
    public String apply(OverAllState state) throws Exception {
        String humanApprovalNextStep = state.value("humanApprovalNextStep", "");
        if("createInventoryTransferNode".equalsIgnoreCase(humanApprovalNextStep)){
            return "createInventoryTransferNode";
        }
        return StateGraph.END;
    }
}
