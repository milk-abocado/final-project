package com.example.finalproject.domain.menus.entity;


import com.example.finalproject.domain.stores.entity.Stores;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "menus")
public class Menus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Stores store;

    private String name;
    private int price;

    @Enumerated(EnumType.STRING)
    private MenuStatus status;

    public enum MenuStatus {
        ACTIVE, DELETED, SOLD_OUT
    }
}
