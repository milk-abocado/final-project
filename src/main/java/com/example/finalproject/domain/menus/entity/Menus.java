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

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MenuStatus status; // ACTIVE, DELETED, SOLD_OUT

    public enum MenuStatus {
        ACTIVE, DELETED, SOLD_OUT
    }
}
