package com.sotatek.order.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class ExternalStatus {

    @Getter
    @RequiredArgsConstructor
    public enum Member {
        ACTIVE("ACTIVE"),
        INACTIVE("INACTIVE"),
        SUSPENDED("SUSPENDED");

        private final String value;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Product {
        AVAILABLE("AVAILABLE"),
        OUT_OF_STOCK("OUT_OF_STOCK"),
        DISCONTINUED("DISCONTINUED");

        private final String value;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Payment {
        COMPLETED("COMPLETED"),
        FAILED("FAILED"),
        PENDING("PENDING"),
        REFUNDED("REFUNDED");

        private final String value;
    }
}
