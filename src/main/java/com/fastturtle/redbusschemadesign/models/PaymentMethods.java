package com.fastturtle.redbusschemadesign.models;

public enum PaymentMethods {
    CASH("CASH"),
    DEBIT_CARD("DEBIT_CARD"),
    CREDIT_CARD("CREDIT_CARD"),
    WALLET("WALLET");

    private final String value;

    PaymentMethods(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
