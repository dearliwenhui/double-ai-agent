package com.dave.ai.transfer.service.base.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dave.ai.transfer.model.BbTransferOrder;
import com.dave.ai.transfer.service.base.BbTransferOrderService;
import com.dave.ai.transfer.mapper.base.BbTransferOrderMapper;
import org.springframework.stereotype.Service;

/**
* @author Dave.Li
* @description 针对表【bb_transfer_order(调拨单主表)】的数据库操作Service实现
* @createDate 2026-05-13 09:48:33
*/
@Service
public class BbTransferOrderServiceImpl extends ServiceImpl<BbTransferOrderMapper, BbTransferOrder>
    implements BbTransferOrderService{

}




