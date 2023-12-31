package app.models.Orders;

import app.models.Customer.Customer;
import app.models.Product.Product;
import app.repos.ProductRepo;
import app.service.OrderService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

public class ProcessCompoundOrder extends ProcessOrder {


    public ProcessCompoundOrder(OrderService orderService, ProductRepo productRepo) {
        super(orderService, productRepo);
    }

    @Override
    public void assignOrderID(Order order) {
        int id = orderService.generateID();
        order.setOrderID(id++);
        for (SimpleOrder ord : ((CompoundOrder) order).getOrders()) {
            ord.setOrderID(id++);
        }
    }

    @Override
    public void calculateProductFees(Order order) {
        CompoundOrder compoundOrder = (CompoundOrder) order;
        double totalProductsFees = 0;
        for (SimpleOrder o : compoundOrder.getOrders()) {
            double productsFees = 0;
            for (Product p : o.getProducts()) {
                Product repoProduct = productRepo.findByID(p.getProductID());
                if (repoProduct == null)
                    continue;
                productsFees += repoProduct.getPrice() * p.getQuantity();
            }
            o.setProductsFees(productsFees);
            totalProductsFees += productsFees;
        }
        order.setProductsFees(totalProductsFees);
    }

    @Override
    public void calculateShippingFees(Order order) {
        // should be calculated based on customer's city (random for now)
        double totalShippingFees = 0;
        Random random = new Random();
        double shippingFees = random.nextInt(50 - 11);
        CompoundOrder compoundOrder = (CompoundOrder) order;
        for (Order o : compoundOrder.getOrders()) {
            o.setShippingFees(shippingFees);
            o.setTotalPrice(o.getProductsFees() + shippingFees);
            totalShippingFees += shippingFees;
        }
        order.setShippingFees(totalShippingFees);
    }

    @Override
    public void validateOrder(Order ord) {
//              pID, bought quantity
        HashMap<Integer, Integer> boughtProducts = new HashMap<>();
        CompoundOrder order = (CompoundOrder) ord;

        // if order's status is returned as invalid, don't add it
        order.setStatus(OrderStatus.INVALID);
        String city = null;
        for (SimpleOrder o : order.getOrders()) {
            Customer customer = orderService.getCustomer(o.getCustomerUsername());
            if (city == null && customer != null)
                city = customer.getShippingAddress().getCity();
            if (customer == null || !Objects.equals(city, customer.getShippingAddress().getCity())
                || !orderService.hasMoney(customer, o.getTotalPrice())) {

                return;
            }
            for (Product p : o.getProducts()) {
                boughtProducts.putIfAbsent(p.getProductID(), 0);
                boughtProducts.put(p.getProductID(), boughtProducts.get(p.getProductID()) + p.getQuantity());
            }
        }

        // check that each product in the order exists and that it has enough quantity
        for (int pID : boughtProducts.keySet()) {
            Product repoProduct = productRepo.findByID(pID);
            if (repoProduct == null || repoProduct.getQuantity() - boughtProducts.get(pID) < 0) {
                return;
            }
        }
        // update product repo quantities
        for (int pID : boughtProducts.keySet()) {
            Product repoProduct = productRepo.findByID(pID);
            repoProduct.setQuantity(repoProduct.getQuantity() - boughtProducts.get(pID));
            if (repoProduct.getQuantity() == 0) {
                productRepo.delete(repoProduct.getProductID());
            }
        }
        // update customer balance
        order.deductProductsFees(orderService);
        // update order status
        order.setStatus(OrderStatus.PLACED);
    }
}
