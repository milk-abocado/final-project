package com.example.finalproject.domain.reviews.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReviewsCommentsResponse {
    private Long commentId;
    private Long reviewId;
    private Long storeId;
    private Long ownerId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
