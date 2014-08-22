package com.helpscout.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.Processor;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.matchers.AbstractMatchListener;

import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.helpscout.model.Customer;
import com.helpscout.model.CustomerSimilarity;

@Service
public class CustomerService {

    private final Map<UUID,Customer> idsToCustomers = Collections.synchronizedMap(new HashMap<UUID,Customer>());
    private final List<CustomerSimilarity> potentialDuplicates = Collections.synchronizedList(new ArrayList<CustomerSimilarity>());
    private Processor dukeProcessor;

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

    public boolean exists(UUID id) {
        return idsToCustomers.containsKey(id);
    }

    public Collection<Customer> allCustomers() {
        return idsToCustomers.values();
    }

    public Customer addCustomer(Customer customer) {
        UUID id = UUID.randomUUID();
        customer.setId(id);
        idsToCustomers.put(id, customer);
        deduplicate(customer);
        return customer;
    }

    public Customer upsertCustomer(UUID id, Customer customer) {
        customer.setId(id); // allow it to be blank in the body
        idsToCustomers.put(id, customer);
        removeDuplicatesWithId(id);
        deduplicate(customer);
        return customer;
    }

    public void deleteCustomer(UUID id) {
        idsToCustomers.remove(id);
        removeDuplicatesWithId(id);
    }

    public List<CustomerSimilarity> getPotentialDuplicates() {
        return potentialDuplicates;
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

    private void removeDuplicatesWithId(final UUID id) {
        Iterables.removeIf(potentialDuplicates, new Predicate<CustomerSimilarity>() {
            @Override
            public boolean apply(CustomerSimilarity input) {
                return id.equals(input.getCustomer1().getId()) || id.equals(input.getCustomer2().getId());
            }
        });
    }

    private class SimilarityMatchListener extends AbstractMatchListener {
        public void matches(Record r1, Record r2, double confidence) {
            UUID customer1Id = UUID.fromString(r1.getValue("id"));
            UUID customer2Id = UUID.fromString(r2.getValue("id"));

            Customer customer1 = idsToCustomers.get(customer1Id);
            Customer customer2 = idsToCustomers.get(customer2Id);;
            if (customer1 != null && customer2 != null) {
                CustomerSimilarity newDup = new CustomerSimilarity();
                newDup.setCustomer1(customer1);
                newDup.setCustomer2(customer2);
                newDup.setSimilarity(new BigDecimal(confidence, new MathContext(4)));
                potentialDuplicates.add(newDup);
            }
        }
    }

}
