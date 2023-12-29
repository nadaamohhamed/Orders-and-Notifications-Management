package app.models.Orders;

import app.models.Customer.Customer;
import app.models.Product.Product;

import java.util.ArrayList;

public class CompoundOrder extends Order{
    private ArrayList<Order> orders = new ArrayList<>();

    public CompoundOrder(Customer customer, ArrayList<Product> products, ArrayList<Order> orders) {
        super(customer, products);
        this.orders = orders;
    }

    public ArrayList<Order> getOrders() {
        return orders;
    }

    public void setOrders(ArrayList<Order> orders) {
        this.orders = orders;
    }
}
