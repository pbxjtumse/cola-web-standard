package com.xjtu.iron.customer.executor.service.impl;

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.exception.BizException;
import com.xjtu.iron.customer.executor.service.CustomerAddCmdExe;
import com.xjtu.iron.dto.CustomerAddCmd;
import com.xjtu.iron.dto.data.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class CustomerAddCmdExeImpl implements CustomerAddCmdExe {
    @Override
    public Response execute(CustomerAddCmd cmd) {
        if(cmd.getCustomerDTO().getCompanyName().equals("ConflictCompanyName")){
            throw new BizException(ErrorCode.B_CUSTOMER_companyNameConflict.getErrCode(), "公司名冲突");
        }
        return Response.buildSuccess();
    }
}
