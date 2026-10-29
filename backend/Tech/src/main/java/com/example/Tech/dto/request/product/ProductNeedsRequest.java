package com.example.Tech.dto.request.product;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ProductNeedsRequest(@NotNull(message = "needIds is required (may be empty)") List<Integer> needIds) {
}
