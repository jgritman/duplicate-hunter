package com.helpscout.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.Processor;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.matchers.AbstractMatchListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import com.helpscout.dao.CustomerRepository;
import com.helpscout.dao.CustomerSimilarityRepository;
import com.helpscout.model.Customer;
import com.helpscout.model.CustomerSimilarity;

@Service
public class CustomerService {

    private Logger log = LoggerFactory.getLogger(CustomerService.class);
    private Processor dukeProcessor;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerSimilarityRepository customerSimilarityRepository;

    @PostConstruct
    public void init() throws IOException, SAXException {
        no.priv.garshol.duke.Configuration config = ConfigLoader.load("classpath:duke-customer.xml");
        dukeProcessor = new Processor(config);
        dukeProcessor.addMatchListener(new SimilarityMatchListener());
    }

    @PreDestroy
    public void destroy() {
        dukeProcessor.close();
    }

    public boolean exists(Long id) {
        return customerRepository.findOne(id) != null;
    }

    public Iterable<Customer> allCustomers() {
        return customerRepository.findAll();
    }

    public Customer addCustomer(Customer customer) {
        Customer inserted = customerRepository.save(customer);
        deduplicate(inserted);
        return inserted;
    }

    public Customer upsertCustomer(Long id, Customer customer) {
        customer.setId(id); // allow it to be blank in the body

        Customer updated = customerRepository.save(customer);

        // remove any previous duplicates with this record and rerun deduplication
        removeDuplicatesWithId(id);
        deduplicate(updated);
        return updated;
    }

    public void deleteCustomer(Long id) {
        removeDuplicatesWithId(id);
        customerRepository.delete(id);
    }

    public Iterable<CustomerSimilarity> getPotentialDuplicates() {
        return customerSimilarityRepository.findAll();
    }

    private void deduplicate(Customer customer) {
        RecordImpl record = new RecordImpl();
        record.addValue("id", customer.getId().toString());
        record.addValue("name", customer.getName());
        record.addValue("address1", customer.getAddress1());
        record.addValue("email", customer.getEmail());
        record.addValue("zip", customer.getZip());
        Collection<Record> batch = new ArrayList<Record>();
        batch.add(record);
        dukeProcessor.deduplicate(batch);
    }

    private void removeDuplicatesWithId(final Long id) {
        List<CustomerSimilarity> matches = customerSimilarityRepository.findByCustomer1IdOrCustomer2Id(id, id);
        if (!matches.isEmpty()) {
            customerSimilarityRepository.delete(matches);
        }
    }

    private class SimilarityMatchListener extends AbstractMatchListener {
        public void matches(Record r1, Record r2, double confidence) {
            Long customer1Id = Long.valueOf(r1.getValue("id"));
            Long customer2Id = Long.valueOf(r2.getValue("id"));

            Customer customer1 = customerRepository.findOne(customer1Id);
            Customer customer2 = customerRepository.findOne(customer2Id);

            log.info("Cust 1 " + customer1);
            log.info("Cust 2 " + customer2);

            if (customer1 != null && customer2 != null) {
                CustomerSimilarity newDup = new CustomerSimilarity();
                newDup.setCustomer1(customer1);
                newDup.setCustomer2(customer2);
                newDup.setSimilarity(new BigDecimal(confidence, new MathContext(4)));
                customerSimilarityRepository.save(newDup);
            }
        }
    }

    public Customer getCustomer(Long id) {
        return customerRepository.findOne(id);
    }

}
