/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.solutions;

import com.google.solutions.model.Customer;
import com.google.solutions.model.Order;
import com.google.solutions.model.Order.Status;
import com.google.solutions.model.OrderItem;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class DataGeneratorApplication {

  private static final Logger log = LoggerFactory.getLogger(DataGeneratorApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(DataGeneratorApplication.class, args);
  }

  @Bean
  public CommandLineRunner generator(CustomerRepository customerRepository,
      OrderRepository orderRepository, OrderItemRepository orderItemRepository,
      OrderService orderService) {
    return (args) -> {
      while (true) {
        runUpdates(customerRepository, orderRepository, orderService);
        printStats(customerRepository, orderRepository, orderItemRepository);
        Thread.sleep(5000);
      }
    };
  }

  private void printStats(CustomerRepository customerRepository, OrderRepository orderRepository,
      OrderItemRepository orderItemRepository) {
    long numberOfCustomers = customerRepository.count();
    long numberOfOrders = orderRepository.count();
    long numberOrOrderItems = orderItemRepository.count();

    log.info(String
        .format("Database state: %d customers, %d orders, %d order items.", numberOfCustomers,
            numberOfOrders, numberOrOrderItems));
  }

  private static Random random = new Random();
  private static String[] firstNames = {"Jack", "Jerry", "Joe", "Jasmin", "Judy", "Jennifer"};
  private static String[] lastNames = {"Smith", "Jackson", "Dodson", "Carlson", "Iverson"};
  private static List<Customer> recentCustomers = new ArrayList<>();
  private static List<Order> openOrders = new ArrayList<>();

  private void runUpdates(CustomerRepository customerRepository, OrderRepository orderRepository,
      OrderService orderService) {
    for (int i = 0; i < 10; i++) {
      Customer newCustomer = newCustomer();
      recentCustomers.add(newCustomer);
      customerRepository.save(newCustomer);
    }
    for (int i = 0; i < 20; i++) {
      Customer customer = recentCustomers.get(random.nextInt(recentCustomers.size()));
      Order order = newOrder(customer);

      Collection<OrderItem> orderItems = new ArrayList<>();
      for (int j = 0; j < random.nextInt(10) + 1; j++) {
        orderItems.add(newOrderItem(order));
      }
      openOrders.add(orderService.save(order, orderItems));
    }

    // Mark orders delivered or cancelled
    Date cutoffTime = new Date(Instant.now().minus(15, ChronoUnit.MINUTES).toEpochMilli());
    Iterator<Order> openOrdersIter = openOrders.listIterator();
    while (openOrdersIter.hasNext()) {
      Order order = openOrdersIter.next();
      if (order.getUpdated().after(cutoffTime)) {
        continue;
      }
      order.setStatus(random.nextBoolean() ? Status.fulfilled : Status.cancelled);
      orderRepository.save(order);
      openOrdersIter.remove();
    }

    // Remove older customers
    while (recentCustomers.size() > 1000) {
      recentCustomers.remove(0);
    }
  }

  private OrderItem newOrderItem(Order order) {
    return new OrderItem(order, random.nextInt(3) + 1, "SKU-" + random.nextInt(2000));
  }

  private Order newOrder(Customer customer) {
    return new Order(customer);
  }

  private Customer newCustomer() {
    return new Customer(UUID.randomUUID().toString(), firstNames[random.nextInt(firstNames.length)],
        lastNames[random.nextInt(lastNames.length)]);
  }
}