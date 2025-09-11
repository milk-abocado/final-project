package com.example.finalproject.domain.menus.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "menu_options")
public class MenuOptions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "menu_id")
    private Menus menu;

    private String optionsName;
    private Integer minSelect;
    private Integer maxSelect;
    private Boolean isRequired;
}
