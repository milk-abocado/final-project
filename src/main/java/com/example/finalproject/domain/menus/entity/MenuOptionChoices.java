package com.example.finalproject.domain.menus.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "menu_option_choices")
public class MenuOptionChoices {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private MenuOptions group;

    private String choiceName;
    private Integer extraPrice;
}
