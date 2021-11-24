package com.google.solutions;

import com.google.solutions.model.Customer;
import org.springframework.data.repository.CrudRepository;

public interface Repository extends CrudRepository<Customer, String> {

}
