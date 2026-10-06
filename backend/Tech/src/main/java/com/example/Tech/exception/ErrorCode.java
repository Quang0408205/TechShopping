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
    PRODUCT_IMAGE_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "The product already has the maximum number of images"),
    LAST_PRODUCT_IMAGE(HttpStatus.CONFLICT, "The last image of a product cannot be deleted"),
    PRIMARY_IMAGE_REQUIRED(HttpStatus.CONFLICT, "Set another image as primary instead of unsetting the primary image"),

    // Image upload
    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "Only JPEG, PNG or WebP images are accepted"),
    IMAGE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "Image must be at most 5 MB"),

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
    VARIANT_ATTRIBUTE_CONFLICT(HttpStatus.CONFLICT, "Variant already has a value for this attribute"),

    // Authentication / authorization
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Token is invalid or expired"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid login or password"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Account is disabled"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Role not found"),
    CUSTOMER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "Customer profile not found"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "Email is already registered"),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "Username is already taken"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "Current password is incorrect"),

    // Admin user management
    CANNOT_MODIFY_OWN_ACCOUNT(HttpStatus.CONFLICT, "Administrators cannot deactivate, delete or demote their own account"),
    LAST_ADMIN(HttpStatus.CONFLICT, "The last active administrator cannot be removed"),
    USER_DELETED(HttpStatus.CONFLICT, "User has been deleted"),

    // Cart
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Product variant is not in the cart"),
    PRODUCT_NOT_AVAILABLE(HttpStatus.CONFLICT, "Product is not available for purchase"),
    CART_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "The cart already holds the maximum number of different items"),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order not found"),
    CART_EMPTY(HttpStatus.CONFLICT, "The cart is empty"),
    INVALID_ORDER_STATUS(HttpStatus.CONFLICT, "The order status does not allow this change"),

    // Promotion
    PROMOTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy chương trình khuyến mãi"),
    INVALID_PROMOTION_DATE_RANGE(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải sau ngày bắt đầu"),
    INVALID_PROMOTION_DISCOUNT(HttpStatus.BAD_REQUEST, "Mức giảm giá không hợp lệ"),
    PROMOTION_PRODUCT_OVERLAP(HttpStatus.CONFLICT,
            "Sản phẩm đã thuộc một chương trình khuyến mãi khác đang bật trong khoảng thời gian này"),

    // Payment / installment
    INSTALLMENT_NOT_ELIGIBLE(HttpStatus.BAD_REQUEST, "Đơn hàng từ 3.000.000đ trở lên mới được trả góp"),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Đơn hàng không có khoản thanh toán này"),
    INVALID_PAYMENT_STATUS(HttpStatus.CONFLICT, "Trạng thái thanh toán không cho phép thao tác này"),
    PAYMENT_REQUIRED(HttpStatus.CONFLICT, "Đơn chuyển khoản chưa được xác nhận đã nhận tiền"),
    INSTALLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng trả góp"),
    INVALID_INSTALLMENT_STATUS(HttpStatus.CONFLICT, "Trạng thái hợp đồng trả góp không cho phép thao tác này"),
    INSTALLMENT_NOT_APPROVED(HttpStatus.CONFLICT, "Hợp đồng trả góp chưa được duyệt"),
    INSTALLMENT_PERIOD_OUT_OF_ORDER(HttpStatus.CONFLICT, "Phải ghi nhận kỳ trả góp sớm nhất chưa thanh toán"),

    // Store
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy chi nhánh"),
    STORE_IN_USE(HttpStatus.CONFLICT,
            "Chi nhánh đã có nhân viên, tồn kho hoặc đơn hàng nên không xoá được; hãy tạm đóng chi nhánh"),

    // Employee
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ nhân viên"),
    EMPLOYEE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Tài khoản này đã có hồ sơ nhân viên"),
    DUPLICATE_EMPLOYEE_CODE(HttpStatus.CONFLICT, "Mã nhân viên đã được dùng"),
    NO_ACTIVE_STORE_ASSIGNMENT(HttpStatus.FORBIDDEN, "Tài khoản của bạn chưa được gán vào chi nhánh nào"),

    // Inventory
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "Chi nhánh xử lý không đủ hàng để xác nhận đơn"),
    ORDER_STORE_MISSING(HttpStatus.CONFLICT, "Đơn chưa có chi nhánh xử lý; ADMIN cần gán chi nhánh trước");

    private final HttpStatus status;
    private final String defaultMessage;
}
