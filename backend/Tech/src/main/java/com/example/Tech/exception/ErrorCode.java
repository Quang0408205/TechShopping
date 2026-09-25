package com.example.Tech.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Invalid request data"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request"),
    RESOURCE_IN_USE(HttpStatus.CONFLICT, "Resource is referenced by other data"),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "Data integrity violation"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Category not found"),
    DUPLICATE_CATEGORY(HttpStatus.CONFLICT, "Category already exists"),
    INVALID_CATEGORY_PARENT(HttpStatus.BAD_REQUEST, "Invalid parent category"),

    // Brand
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "Brand not found"),
    DUPLICATE_BRAND(HttpStatus.CONFLICT, "Brand already exists"),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
    DUPLICATE_PRODUCT(HttpStatus.CONFLICT, "Product already exists"),
    INVALID_PRODUCT_DATA(HttpStatus.BAD_REQUEST, "Invalid product data"),

    // Product variant
    PRODUCT_VARIANT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product variant not found"),
    DUPLICATE_PRODUCT_VARIANT(HttpStatus.CONFLICT, "Product variant already exists"),
    INVALID_PRODUCT_VARIANT_DATA(HttpStatus.BAD_REQUEST, "Invalid product variant data"),

    // Product image
    PRODUCT_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Product image not found"),

    // Product specification
    PRODUCT_SPECIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Product specification not found"),

    // Attribute
    ATTRIBUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "Attribute not found"),
    DUPLICATE_ATTRIBUTE(HttpStatus.CONFLICT, "Attribute already exists"),
    ATTRIBUTE_VALUE_NOT_FOUND(HttpStatus.NOT_FOUND, "Attribute value not found"),
    DUPLICATE_ATTRIBUTE_VALUE(HttpStatus.CONFLICT, "Attribute value already exists"),

    // Variant attribute value
    VARIANT_ATTRIBUTE_VALUE_NOT_FOUND(HttpStatus.NOT_FOUND, "Attribute value is not assigned to the variant"),
    DUPLICATE_VARIANT_ATTRIBUTE_VALUE(HttpStatus.CONFLICT, "Attribute value is already assigned to the variant"),
    VARIANT_ATTRIBUTE_CONFLICT(HttpStatus.CONFLICT, "Variant already has a value for this attribute");

    private final HttpStatus status;
    private final String defaultMessage;
}
