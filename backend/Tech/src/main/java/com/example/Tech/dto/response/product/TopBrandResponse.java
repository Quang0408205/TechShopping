package com.example.Tech.dto.response.product;

/** A brand for the home page strip, with the number of its products on sale. */
public record TopBrandResponse(Integer id, String name, long productCount) {
}
