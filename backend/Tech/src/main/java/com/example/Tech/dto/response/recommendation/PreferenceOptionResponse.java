package com.example.Tech.dto.response.recommendation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** One category of the questionnaire with the brands on sale in it and its needs. */
public record PreferenceOptionResponse(

        Integer id,

        String name,

        String slug,

        @Schema(description = "Brands with products on sale in this category, most products first")
        List<Choice> brands,

        @Schema(description = "Needs of this category (gaming, photography...); empty when the category has none")
        List<Choice> needs
) {

    public record Choice(Integer id, String name, long productCount) {
    }
}
