package com.xjtu.iron.web;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.xjtu.iron.api.CustomerServiceI;
import com.xjtu.iron.domain.order.CreateOrderCommand;
import com.xjtu.iron.domain.order.Order;
import com.xjtu.iron.domain.order.OrderDomainService;
import com.xjtu.iron.dto.CustomerAddCmd;
import com.xjtu.iron.dto.CustomerListByNameQry;
import com.xjtu.iron.dto.data.CustomerDTO;
import com.xjtu.iron.order.OrderApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class CustomerController {

    @Autowired
    private CustomerServiceI customerService;
    @Autowired
    private OrderApplicationService orderApplicationService;

    @GetMapping(value = "/helloworld")
    public String helloWorld(){
        return "Hello, welcome to COLA world!";
    }

    @GetMapping(value = "/customer")
    public MultiResponse<CustomerDTO> listCustomerByName(@RequestParam(required = false) String name){
        CustomerListByNameQry customerListByNameQry = new CustomerListByNameQry();
        customerListByNameQry.setName(name);
        return customerService.listByName(customerListByNameQry);
    }

    @PostMapping(value = "/customer")
    public Response addCustomer(@RequestBody CustomerAddCmd customerAddCmd){
        return customerService.addCustomer(customerAddCmd);
    }

    @PostMapping("/create")
    public OrderDTO create(@RequestBody CreateOrderRequest req) throws Exception {
        CreateOrderCommand cmd = req.totoCommand();
        Order order = orderApplicationService.createOrder(cmd);
        return OrderDTO.from(order);
    }
}
