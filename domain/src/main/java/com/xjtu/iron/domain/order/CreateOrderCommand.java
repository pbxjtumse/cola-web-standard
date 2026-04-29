package com.xjtu.iron.domain.order;

import lombok.*;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class CreateOrderCommand {
    private String userId;
    private String skuId;
}
