package com.learn.tradingmgmt.repository;

import com.learn.tradingmgmt.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
}
