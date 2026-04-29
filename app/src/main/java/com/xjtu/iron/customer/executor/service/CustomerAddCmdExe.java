
package com.xjtu.iron.customer.executor.service;

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.exception.BizException;
import com.xjtu.iron.dto.CustomerAddCmd;
import com.xjtu.iron.dto.data.ErrorCode;
import org.springframework.stereotype.Component;



public interface CustomerAddCmdExe{
     Response execute(CustomerAddCmd cmd);
}
