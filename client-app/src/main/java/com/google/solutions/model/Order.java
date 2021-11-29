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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "orders")
public class Order extends Timestamped {

  public enum Status {open, fulfilled, cancelled}

  @Id
  @Column(name = "order_id")
  private String id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "customer_id", nullable = false, updatable = false)
  private Customer customer;

  @Enumerated(value = EnumType.STRING)
  private Status status;

  public String getId() {
    return id;
  }

  public Customer getCustomer() {
    return customer;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  protected Order() {
    //  For JPA's use
  }

  public Order(Customer customer) {
    this.customer = customer;
    this.status = Status.open;
    this.id = UUID.randomUUID().toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Order order = (Order) o;

    return id.equals(order.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
