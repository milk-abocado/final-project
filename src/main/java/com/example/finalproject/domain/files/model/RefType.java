package com.example.finalproject.domain.files.model;

public enum RefType {
    STORE, MENU, REVIEW;

    public String key() { return name(); }        // "STORE" 등
    public String dir() { return name().toLowerCase(); } // "store" 등
}