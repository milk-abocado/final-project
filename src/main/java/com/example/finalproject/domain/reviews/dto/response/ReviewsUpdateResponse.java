package com.example.finalproject.domain.reviews.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * ReviewsUpdateResponse
 * -------------------------------------------------
 * - 리뷰 수정 시 응답용 DTO
 * - 수정 전(old) / 수정 후(new) 상태를 함께 전달
 */
@Getter
@Setter
public class ReviewsUpdateResponse {

    // 수정 전 상태
    private Integer oldRating;
    private String oldContent;
    private LocalDateTime oldUpdatedAt;

    // 수정 후 상태
    private Integer newRating;
    private String newContent;
    private LocalDateTime newUpdatedAt;

    public ReviewsUpdateResponse(Integer oldRating, String oldContent, LocalDateTime oldUpdatedAt,
                                 Integer newRating, String newContent, LocalDateTime newUpdatedAt) {
        this.oldRating = oldRating;
        this.oldContent = oldContent;
        this.oldUpdatedAt = oldUpdatedAt;
        this.newRating = newRating;
        this.newContent = newContent;
        this.newUpdatedAt = newUpdatedAt;
    }
}
