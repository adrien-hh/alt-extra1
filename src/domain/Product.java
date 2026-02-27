package domain;

public record Product(
    String id, String name, String category, double price, double weight, boolean taxable) {}
