package domain;

public record Order(
    String id,
    String customer_id,
    String product_id,
    int qty,
    double unit_price,
    String date,
    String promo_code,
    String time) {}
