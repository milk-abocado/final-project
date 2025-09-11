package com.example.finalproject.domain.stores.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1) 필드/제약코드별 커스텀 문구 매핑
    //    키 규칙 우선순위: "field.constraint" > "field" > "constraint"
    //    constraint 예: NotNull, NotBlank, Min, Positive, PositiveOrZero 등
    private static final Map<String, String> OVERRIDES = Map.ofEntries(
            // StoresRequest 전용 예시
            Map.entry("name.NotBlank",        "가게 이름은 필수입니다."),
            Map.entry("name.NotNull",         "가게 이름은 필수입니다."),
            Map.entry("address.NotBlank",     "주소는 필수입니다."),
            Map.entry("address.NotNull",      "주소는 필수입니다."),
            Map.entry("minOrderPrice.Min",    "최소 주문 금액은 1원 이상이어야 합니다."),
            Map.entry("minOrderPrice.Positive","최소 주문 금액은 1원 이상이어야 합니다."),
            Map.entry("opensAt.NotNull",      "영업 시작 시간은 필수입니다."),
            Map.entry("closesAt.NotNull",     "영업 종료 시간은 필수입니다."),

            // 공통 fallback
            Map.entry("NotNull",              "필수 값이 누락되었습니다."),
            Map.entry("NotBlank",             "필수 값이 누락되었습니다."),
            Map.entry("Min",                  "값이 최소 허용 범위 미만입니다."),
            Map.entry("Positive",             "값은 1 이상이어야 합니다.")
    );

    // 2) 비즈니스 예외는 그대로 포맷 유지
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handle(ApiException e) {
        return ResponseEntity.status(e.getCode().status)
                .body(Map.of(
                        "error", e.getCode().name(),
                        "message", e.getMessage()
                ));
    }

    // 3) Bean Validation 예외를 전부 커스텀 문구로 치환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        var fieldErrors = e.getBindingResult().getFieldErrors();

        // 여러 필드 에러를 배열로 반환 (원하면 첫 번째만 리턴하도록 바꿔도 됨)
        List<Map<String, Object>> messages = new ArrayList<>();

        for (FieldError fe : fieldErrors) {
            String field = fe.getField();          // ex) "minOrderPrice"
            String code  = fe.getCode();           // ex) "Min", "NotBlank", ...
            Object bad   = fe.getRejectedValue();  // 사용자가 보낸 값

            // 키 우선순위로 커스텀 메시지 선택
            String msg = OVERRIDES.get(field + "." + code);
            if (msg == null) msg = OVERRIDES.get(field);
            if (msg == null) msg = OVERRIDES.get(code);
            if (msg == null) msg = "잘못된 요청입니다."; // 최종 fallback

            messages.add(Map.of(
                    "field", field,
                    "message", msg,
                    "rejectedValue", bad
            ));
        }

        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION",
                "messages", messages
        ));
    }
}
