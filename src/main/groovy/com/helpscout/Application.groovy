package com.helpscout

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

import no.priv.garshol.duke.ConfigLoader
import no.priv.garshol.duke.Processor
import no.priv.garshol.duke.Record
import no.priv.garshol.duke.RecordImpl
import no.priv.garshol.duke.matchers.AbstractMatchListener

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@Configuration
@EnableAutoConfiguration
@RestController
class Application {

    Map<UUID,Customer> idsToCustomers = [:].asSynchronized()
    final List<CustomerSimilarity> potentialDuplicates = [].asSynchronized()
    Processor dukeProcessor

    static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args)
    }

    @PostConstruct
    def init() {
        def config = ConfigLoader.load('classpath:duke-customer.xml')
        dukeProcessor = new Processor(config)
        dukeProcessor.addMatchListener(new SimilarityMatchListener())
    }

    @PreDestroy
    def destroy() {
        dukeProcessor.close()
    }

    /**
     * Not requested but for debugging purposes
     */
    @RequestMapping(value="/customers", method=RequestMethod.GET)
    List<Customer> getAllCustomers() {
        idsToCustomers.values()
    }

    @RequestMapping(value="/customers", method=RequestMethod.POST)
    ResponseEntity<Customer> addCustomer(@RequestBody Customer customer) {
        UUID id = UUID.randomUUID()
        customer.id = id
        idsToCustomers[id] = customer
        deduplicate(customer)
        new ResponseEntity(customer, HttpStatus.CREATED)
    }

    @RequestMapping(value="/customers/{id}", method=RequestMethod.PUT)
    ResponseEntity<Customer> updateCustomer(@PathVariable UUID id, @RequestBody Customer customer) {
        if (!idsToCustomers[id]) {
            return new ResponseEntity(HttpStatus.NOT_FOUND)
        }
        customer.id = id // allow it to be blank in the body
        idsToCustomers[id] = customer
        removeDuplicatesWithId(id)
        deduplicate(customer)
        new ResponseEntity(customer, HttpStatus.OK)
    }

    @RequestMapping(value="/customers/{id}", method=RequestMethod.DELETE)
    ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        if (!idsToCustomers[id]) {
            return new ResponseEntity(HttpStatus.NOT_FOUND)
        }
        idsToCustomers.remove(id)
        removeDuplicatesWithId(id)
        new ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @RequestMapping(value="/potentialDuplicates", method=RequestMethod.GET)
    List<CustomerSimilarity> getPotentialDuplicates() {
        potentialDuplicates
    }

    private void deduplicate(Customer customer) {
        RecordImpl record = new RecordImpl()
        record.with {
            addValue('id', customer.id.toString())
            addValue('name', customer.name)
            addValue('address1', customer.address1)
            addValue('email', customer.email)
            addValue('zip', customer.zip)
        }
        dukeProcessor.deduplicate([record])
    }

    private void removeDuplicatesWithId(UUID id) {
        potentialDuplicates.removeAll { it.customerId1 == id || it.customerId2 == id }
    }

    private class SimilarityMatchListener extends AbstractMatchListener {
        void matches(Record r1, Record r2, double confidence) {
            def customer1Id = UUID.fromString(r1.getValue('id'))
            def customer2Id = UUID.fromString(r2.getValue('id'))
            if (idsToCustomers[customer1Id] && idsToCustomers[customer2Id]) {
                def newDup = new CustomerSimilarity(customer1Id, customer2Id,
                        new BigDecimal(confidence, ))
                potentialDuplicates << newDup
            }
        }
    }
}

