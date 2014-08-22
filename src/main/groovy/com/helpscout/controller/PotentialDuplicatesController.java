package com.helpscout.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.helpscout.model.CustomerSimilarity;
import com.helpscout.service.CustomerService;

@RequestMapping("/potentialDuplicates")
@RestController
public class PotentialDuplicatesController {

    @Autowired
    CustomerService customerService;

    @RequestMapping(method=RequestMethod.GET)
    public Iterable<CustomerSimilarity> getPotentialDuplicates() {
        return customerService.getPotentialDuplicates();
    }
}
