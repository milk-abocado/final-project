package com.example.finalproject.domain.orders.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderOptionsResponse {
    private String optionGroupName;
    private String choiceName;
    private Integer extraPrice;
}
