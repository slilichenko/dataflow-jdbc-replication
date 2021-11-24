package com.google.solutions;

import com.google.solutions.model.Customer;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DataGeneratorApplication {

  private static final Logger log = LoggerFactory.getLogger(DataGeneratorApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(DataGeneratorApplication.class, args);
  }

  @Bean
  public CommandLineRunner generator(Repository repository) {
    return (args) -> {
      // save a few customers
      repository.save(new Customer(UUID.randomUUID().toString(), "Jack", "Bauer"));
      repository.save(new Customer(UUID.randomUUID().toString(), "Chloe", "O'Brian"));
      repository.save(new Customer(UUID.randomUUID().toString(), "Kim", "Bauer"));
      repository.save(new Customer(UUID.randomUUID().toString(), "David", "Palmer"));
      repository.save(new Customer(UUID.randomUUID().toString(), "Michelle", "Dessler"));
    };
  }
}