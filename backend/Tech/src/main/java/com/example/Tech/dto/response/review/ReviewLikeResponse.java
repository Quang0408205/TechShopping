package com.example.Tech.dto.response.review;

/** The like state of one review after PUT / DELETE /reviews/{id}/like. */
public record ReviewLikeResponse(long likeCount, boolean likedByMe) {
}
