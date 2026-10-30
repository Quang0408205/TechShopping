package com.example.Tech.service.product;

import com.example.Tech.dto.response.product.FilterGroupResponse;
import com.example.Tech.dto.response.product.NeedResponse;

import java.util.List;

/** Normalised spec filters + needs of products. */
public interface ProductFilterService {

    /** Rebuilds the filter values of one product from its specifications and variants (same transaction as the caller). */
    void reindex(Long productId);

    /** Rebuilds every product; returns how many products were indexed. */
    int reindexAll();

    /** Groups + values (with product counts) of the products on sale in a category, needs included. */
    List<FilterGroupResponse> getFilters(Integer categoryId);

    List<NeedResponse> getNeeds(Integer categoryId);

    List<NeedResponse> getProductNeeds(Long productId);

    /** Replaces the needs of a product (each need must belong to the product's category). */
    List<NeedResponse> setProductNeeds(Long productId, List<Integer> needIds);
}
