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

import com.google.solutions.model.Order;
import com.google.solutions.model.OrderItem;
import java.util.Collection;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;

  public OrderServiceImpl(OrderRepository orderRepository,
      OrderItemRepository orderItemRepository) {
    this.orderRepository = orderRepository;
    this.orderItemRepository = orderItemRepository;
  }

  @Override
  @Transactional
  public Order save(Order order, Collection<OrderItem> items) {
    Order result = orderRepository.save(order);
    orderItemRepository.saveAll(items);
    return result;
  }
}
