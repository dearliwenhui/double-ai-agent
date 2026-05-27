package com.dave.ai.bi.helper.domain.rag.gateway;

import com.dave.ai.bi.helper.domain.rag.model.QueryIntent;

public interface QueryRewriter {

    QueryIntent rewrite(String userInput);
}
