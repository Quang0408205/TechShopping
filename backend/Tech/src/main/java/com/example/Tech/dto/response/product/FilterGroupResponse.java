package com.example.Tech.dto.response.product;

import java.util.List;

/** A group of filter values for the product list of one category (plan v5 stage 9). */
public record FilterGroupResponse(String key, String title, List<FilterValueResponse> values) {

    /** value = what to send back as filter / need; label = what to show (the need name, else the value itself). */
    public record FilterValueResponse(String value, String label, long count) {
    }
}
