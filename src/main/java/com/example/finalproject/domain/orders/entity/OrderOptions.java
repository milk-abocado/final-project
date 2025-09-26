package com.example.finalproject.domain.orders.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "order_options")
public class OrderOptions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItems orderItem;

    private String optionGroupName;
    private String choiceName;

    private Integer extraPrice;
}
