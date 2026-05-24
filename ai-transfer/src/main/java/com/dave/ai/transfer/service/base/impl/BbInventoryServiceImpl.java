package com.dave.ai.transfer.service.base.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dave.ai.transfer.model.BbInventory;
import com.dave.ai.transfer.service.base.BbInventoryService;
import com.dave.ai.transfer.mapper.base.BbInventoryMapper;
import org.springframework.stereotype.Service;

/**
* @author Dave.Li
* @description 针对表【bb_inventory(库存表)】的数据库操作Service实现
* @createDate 2026-05-13 09:48:33
*/
@Service
public class BbInventoryServiceImpl extends ServiceImpl<BbInventoryMapper, BbInventory>
    implements BbInventoryService{

}




