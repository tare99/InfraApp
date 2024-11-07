package com.example.infraapp;

import org.springframework.boot.SpringApplication;

public class TestInfraAppApplication {

  public static void main(String[] args) {
    SpringApplication.from(InfraAppApplication::main).with(TestcontainersConfiguration.class)
        .run(args);
  }

}
