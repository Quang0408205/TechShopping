package com.example.Tech.entity.payment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Banks whose credit cards can be used for an installment plan; the code is stored in installment_orders.card_bank_code. */
@Getter
@RequiredArgsConstructor
public enum InstallmentBank {

    VCB("Vietcombank"),
    TCB("Techcombank"),
    VPB("VPBank"),
    ACB("ACB"),
    MB("MB Bank"),
    BIDV("BIDV"),
    CTG("VietinBank"),
    STB("Sacombank"),
    TPB("TPBank"),
    HDB("HDBank");

    private final String displayName;
}
