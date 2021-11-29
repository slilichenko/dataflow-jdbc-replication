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

package com.google.solutions.model;

import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "order_items")
public class OrderItem extends Timestamped {

  @Id
  @Column(name = "order_item_id")
  private String id;

  private String sku;

  private int quantity;

  @ManyToOne(optional = false)
  @JoinColumn(name = "order_id", nullable = false, updatable = false)
  private Order order;

  public OrderItem() {
    //  For JPA's use
  }

  public OrderItem(Order order, int quantity, String sku) {
    this.order = order;
    this.id = UUID.randomUUID().toString();
    this.quantity = quantity;
    this.sku = sku;
  }

  public String getId() {
    return id;
  }

  public String getSku() {
    return sku;
  }

  public int getQuantity() {
    return quantity;
  }

  public Order getOrder() {
    return order;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OrderItem orderItem = (OrderItem) o;

    return id.equals(orderItem.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
