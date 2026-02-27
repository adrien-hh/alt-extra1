package domain;

public record Customer(
    String id, String name, String level, String shippingZone, String currency) {}
