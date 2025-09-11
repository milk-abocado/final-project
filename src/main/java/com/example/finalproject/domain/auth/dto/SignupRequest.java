package com.example.finalproject.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequest {
    @Email @NotBlank private String email;
    @NotBlank private String password;
    @NotBlank private String name;
    @NotBlank private String nickname; }
