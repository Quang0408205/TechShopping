package com.example.Tech.entity.aftersales;

/** return_requests.reason_type (DB CHECK chk_return_requests_reason_type). Every reason gets a full refund. */
public enum ReturnReasonType {
    DEFECTIVE,
    NOT_AS_DESCRIBED,
    CHANGED_MIND,
    OTHER
}
