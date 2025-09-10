package com.example.finalproject.domain.stores.entity;

import com.example.finalproject.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "stores")
public class Stores {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Users owner;

    private String name;
    private String address;
    private Integer minOrderPrice;
    private LocalTime opensAt;
    private LocalTime closesAt;
    private Long deliveryFee;
    private Boolean active = true;
}
