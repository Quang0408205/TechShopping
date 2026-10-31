package com.example.Tech.service.mail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReservedAddressesTest {

    @Test
    void sampleAndTestDomainsAreReserved() {
        assertThat(ReservedAddresses.isReserved("mainguyen5014@example.test")).isTrue();
        assertThat(ReservedAddresses.isReserved("A@SHOP.TEST")).isTrue();
        assertThat(ReservedAddresses.isReserved("x@shop.invalid")).isTrue();
        assertThat(ReservedAddresses.isReserved("x@poy.example")).isTrue();
        assertThat(ReservedAddresses.isReserved("x@localhost")).isTrue();
    }

    @Test
    void realDomainsAreNot() {
        assertThat(ReservedAddresses.isReserved("someone@gmail.com")).isFalse();
        assertThat(ReservedAddresses.isReserved("hungdo@poyshop.vn")).isFalse();
        assertThat(ReservedAddresses.isReserved("a@testing.io")).isFalse();
        assertThat(ReservedAddresses.isReserved(null)).isFalse();
        assertThat(ReservedAddresses.isReserved("no-at-sign")).isFalse();
    }
}
