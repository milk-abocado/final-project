package com.example.finalproject.domain.orders.entity;

import com.example.finalproject.domain.menus.entity.Menus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "order_items")
public class OrderItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Orders order;

    @ManyToOne
    @JoinColumn(name = "menu_id", nullable = false)
    private Menus menu;

    @Column(nullable = false)
    private Integer quantity;
}
