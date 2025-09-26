package com.example.finalproject.domain.users;

/**
 * 사용자 권한(Role) Enum
 * - USER  : 일반 사용자 (주문, 가게 조회, 리뷰 작성 등 기본 기능 사용)
 * - OWNER : 가게 사장님 (자신의 가게 등록/수정/삭제, 메뉴 관리, 리뷰 관리 등)
 * - ADMIN : 관리자 (시스템 전체 관리, 사용자/가게 제재 및 모니터링 등)
 */
public enum UserRole { USER, OWNER, ADMIN }
