package app.models.Orders;

public abstract class ProcessOrder {
    public final void processOrder(Order order) {
        // Template method - Skeleton
        calculateProductFees(order);
        calculateShippingFees(order);
        calculateTotalFees(order);
    }
    public abstract void calculateProductFees(Order order);
    public abstract void calculateShippingFees(Order order);
    public final void calculateTotalFees(Order order) {
        order.setTotalPrice(order.productsFees + order.shippingFees);
    }
}
