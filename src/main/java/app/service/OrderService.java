package app.service;

import app.models.Customer.Customer;
import app.models.Notification.NotificationSubject;
import app.models.Orders.*;
import app.repos.CustomerRepo;
import app.repos.OrderRepo;
import app.repos.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Random;

@Service
public class OrderService {
    private OrderRepo orderRepo;
    private final NotificationTemplateService notificationService;
    private final CustomerRepo customerRepo;
    private final ProductRepo productRepo;
    private final int PRECONFIGURED_TIME = 120; // Default value is 120 seconds - 2 minutes

    @Autowired
    public OrderService(OrderRepo orderRepo, NotificationTemplateService notificationService, CustomerRepo customerRepo, ProductRepo productRepo) {
        this.orderRepo = orderRepo;
        this.notificationService = notificationService;
        this.customerRepo = customerRepo;
        this.productRepo = productRepo;
    }

    public Order findOrderById(int id) {
        return orderRepo.findByID(id);
    }

    public ArrayList<Order> getOrders() {
        return orderRepo.getAll();
    }

    public void addSimpleOrder(SimpleOrder order) {
        ProcessOrder orderProcessor = new ProcessSimpleOrder(this, productRepo);
        orderProcessor.processOrder(order);
        if (order.getStatus() == OrderStatus.INVALID) {
            return;
        }
        notificationService.generateNotification(NotificationSubject.ORDER_PLACEMENT, order);
        orderRepo.add(order);
    }
    public void addCompoundOrder(CompoundOrder order) {
        ProcessOrder orderProcessor = new ProcessCompoundOrder(this, productRepo);
        orderProcessor.processOrder(order);
        if (order.getStatus() == OrderStatus.INVALID) {
            return;
        }
        notificationService.generateNotification(NotificationSubject.ORDER_PLACEMENT, order);
        orderRepo.add(order);
    }

    public void shipOrder(int id) {
        // create el notification subject shipOrder
        Order order = orderRepo.findByID(id);
        // check if order is placed before shipping
        if(!(order.getStatus() == OrderStatus.PLACED))
            return;
        notificationService.generateNotification(NotificationSubject.ORDER_SHIPMENT, order);
        orderRepo.updateStatus(OrderStatus.SHIPPED, id);
        // once the order is shipped, start a timer for preconfigured time
        new Thread(new Runnable() {
            // run in new thread so the request thread doesn't wait for preconfigured time and stops
            @Override
            public void run() {
                try {
                    Thread.sleep(PRECONFIGURED_TIME * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                order.setPreconfiguredTimeFinished(true);
            }
        }).start();
    }
    public void cancelPlacement(int id) {
        Order order = orderRepo.findByID(id);
        if(!(order.getStatus() == OrderStatus.PLACED))
            return;
        notificationService.generateNotification(NotificationSubject.PLACEMENT_CANCELLATION, order);
        orderRepo.delete(id);
    }
    public void cancelShipment(int id) {
        Order order = orderRepo.findByID(id);
        if(!(order.getStatus() == OrderStatus.SHIPPED))
            return;
        // cancel only if preconfigured time isn't finished yet
       if(!order.isPreconfiguredTimeFinished()) {
           notificationService.generateNotification(NotificationSubject.SHIPMENT_CANCELLATION, order);
           orderRepo.updateStatus(OrderStatus.PLACED, id);
       }
    }
    public boolean orderExists(int id) {
        return orderRepo.findByID(id) != null;
    }
    public boolean userExists(int id) {
        // check if user exists
        return customerRepo.findByID(id) != null;
    }
    public boolean hasMoney(Customer customer, double amount) {
        return customer.getBalance() >= amount;
    }
    public int generateID(){
        Random random = new Random();
        int value = random.nextInt(100000 - 1);
        while(orderExists(value)){
            value = random.nextInt(100000 - 1);
        }
        return value;
    }
    public Customer getCustomer(String customerUsername){
        orderRepo = new OrderRepo();
        return customerRepo.findByUsername(customerUsername);
    }

}
