package com.xjtu.iron.api;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.xjtu.iron.dto.CustomerAddCmd;
import com.xjtu.iron.dto.CustomerListByNameQry;
import com.xjtu.iron.dto.data.CustomerDTO;

public interface CustomerServiceI {

    Response addCustomer(CustomerAddCmd customerAddCmd);

    MultiResponse<CustomerDTO> listByName(CustomerListByNameQry customerListByNameQry);
}
