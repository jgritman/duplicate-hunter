package com.helpscout.dao;

import org.springframework.data.repository.CrudRepository;

import com.helpscout.model.Customer;

public interface CustomerRepository extends CrudRepository<Customer, Long> {

}
