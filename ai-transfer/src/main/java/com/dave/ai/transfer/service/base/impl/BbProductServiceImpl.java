package com.dave.ai.transfer.service.base.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dave.ai.transfer.model.BbProduct;
import com.dave.ai.transfer.service.base.BbProductService;
import com.dave.ai.transfer.mapper.base.BbProductMapper;
import org.springframework.stereotype.Service;

/**
* @author Dave.Li
* @description 针对表【bb_product(商品信息表)】的数据库操作Service实现
* @createDate 2026-05-13 09:48:33
*/
@Service
public class BbProductServiceImpl extends ServiceImpl<BbProductMapper, BbProduct>
    implements BbProductService{

}




