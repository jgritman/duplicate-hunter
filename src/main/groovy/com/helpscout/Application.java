package com.helpscout;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.helpscout.model.Customer;
import com.helpscout.model.CustomerSimilarity;

@Configuration
@EnableAutoConfiguration
@RestController
public class Application {


    private Logger log = LoggerFactory.getLogger(Application.class);

    private final Map<UUID,Customer> idsToCustomers = Collections.synchronizedMap(new HashMap<UUID,Customer>());
    private final List<CustomerSimilarity> potentialDuplicates = Collections.synchronizedList(new ArrayList<CustomerSimilarity>());
    private Processor dukeProcessor;

    public static final void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

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

    /**
     * Not requested but for debugging purposes
     */
    @RequestMapping(value="/customers", method=RequestMethod.GET)
    public Collection<Customer> getAllCustomers() {
        return idsToCustomers.values();
    }

    @RequestMapping(value="/customers", method=RequestMethod.POST)
    public ResponseEntity<Customer> addCustomer(@RequestBody Customer customer) {
        UUID id = UUID.randomUUID();
        customer.setId(id);
        idsToCustomers.put(id, customer);
        deduplicate(customer);
        return new ResponseEntity<Customer>(customer, HttpStatus.CREATED);
    }

    @RequestMapping(value="/customers/{id}", method=RequestMethod.PUT)
    public ResponseEntity<Customer> updateCustomer(@PathVariable UUID id, @RequestBody Customer customer) {
        boolean exists = idsToCustomers.containsKey(id);
        customer.setId(id); // allow it to be blank in the body
        idsToCustomers.put(id, customer);
        removeDuplicatesWithId(id);
        deduplicate(customer);
        return new ResponseEntity<Customer>(customer, exists ? HttpStatus.OK : HttpStatus.CREATED);
    }

    @RequestMapping(value="/customers/{id}", method=RequestMethod.DELETE)
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        if (!idsToCustomers.containsKey(id)) {
            return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
        }
        idsToCustomers.remove(id);
        removeDuplicatesWithId(id);
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value="/potentialDuplicates", method=RequestMethod.GET)
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
                log.info("Comparing " + input + " with " + id);
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

