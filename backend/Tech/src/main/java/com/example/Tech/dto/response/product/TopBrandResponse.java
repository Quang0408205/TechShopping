package com.example.Tech.dto.response.product;

/** Plan v8: a brand for the home page strip, with the number of its products on sale. */
public record TopBrandResponse(Integer id, String name, long productCount) {
}
