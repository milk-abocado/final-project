package com.example.finalproject.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(com.example.finalproject.config.TokenProperties.class)
public class PropertiesConfig { }