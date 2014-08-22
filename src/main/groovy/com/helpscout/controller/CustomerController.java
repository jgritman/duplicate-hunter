package com.helpscout.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.helpscout.model.Customer;
import com.helpscout.model.CustomerSimilarity;
import com.helpscout.service.CustomerService;

@RestController
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    /**
     * Not requested but for debugging purposes
     */
    @RequestMapping(value="/customers", method=RequestMethod.GET)
    public Iterable<Customer> getAllCustomers() {
        return customerService.allCustomers();
    }

    @RequestMapping(value="/customers", method=RequestMethod.POST)
    public ResponseEntity<Customer> addCustomer(@RequestBody Customer customer) {
        Customer inserted = customerService.addCustomer(customer);
        return new ResponseEntity<Customer>(inserted, HttpStatus.CREATED);
    }

    @RequestMapping(value="/customers/{id}", method=RequestMethod.PUT)
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        boolean exists = customerService.exists(id);
        Customer updated = customerService.upsertCustomer(id, customer);
        return new ResponseEntity<Customer>(updated, exists ? HttpStatus.OK : HttpStatus.CREATED);
    }

    @RequestMapping(value="/customers/{id}", method=RequestMethod.DELETE)
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        if (!customerService.exists(id)) {
            return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
        }
        customerService.deleteCustomer(id);
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value="/potentialDuplicates", method=RequestMethod.GET)
    public Iterable<CustomerSimilarity> getPotentialDuplicates() {
        return customerService.getPotentialDuplicates();
    }

}
