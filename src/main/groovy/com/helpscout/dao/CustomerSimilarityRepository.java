package com.helpscout.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.helpscout.model.CustomerSimilarity;

public interface CustomerSimilarityRepository extends CrudRepository<CustomerSimilarity, Long> {

    List<CustomerSimilarity> findByCustomer1IdOrCustomer2Id(Long customer1Id, Long customer2Id);

}
