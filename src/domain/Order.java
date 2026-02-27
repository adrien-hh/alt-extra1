package domain;

public record Order(
    String id,
    String customerId,
    String productId,
    int qty,
    double unitPrice,
    String date,
    String promoCode,
    String time) {}
