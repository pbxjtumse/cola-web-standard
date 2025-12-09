package com.xjtu.iron.customer;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.catchlog.CatchAndLog;
import com.xjtu.iron.api.CustomerServiceI;
import com.xjtu.iron.dto.CustomerAddCmd;
import com.xjtu.iron.dto.CustomerListByNameQry;
import com.xjtu.iron.dto.data.CustomerDTO;
import org.springframework.stereotype.Service;

import com.xjtu.iron.customer.executor.service.CustomerAddCmdExe;
import com.xjtu.iron.customer.executor.query.CustomerListByNameQryExe;

import javax.annotation.Resource;


@Service
@CatchAndLog
public class CustomerServiceImpl implements CustomerServiceI {

    @Resource
    private CustomerAddCmdExe customerAddCmdExe;

    @Resource
    private CustomerListByNameQryExe customerListByNameQryExe;

    public Response addCustomer(CustomerAddCmd customerAddCmd) {
        return customerAddCmdExe.execute(customerAddCmd);
    }

    @Override
    public MultiResponse<CustomerDTO> listByName(CustomerListByNameQry customerListByNameQry) {
        return customerListByNameQryExe.execute(customerListByNameQry);
    }

}