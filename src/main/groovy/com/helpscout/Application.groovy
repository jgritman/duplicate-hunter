/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.helpscout

import groovy.transform.Canonical

import java.util.Collections.SynchronizedList

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

    Map<UUID,Customer> idsToCustomers = [:]
    final List<CustomerSimilarity> similarities = [].asSynchronized()
    Processor dukeProcessor

    @PostConstruct
    def init() {
        def config = ConfigLoader.load('classpath:duke-customer.xml')
        dukeProcessor = new Processor(config)
        dukeProcessor.addMatchListener(new SimilarityMatchListener(similarities))
    }

    @PreDestroy
    def destroy() {
        dukeProcessor.close()
    }

    /**
     * Not requested but for debugging purposes
     */
    @RequestMapping(value="/customers", method=RequestMethod.GET)
    def getAllCustomers() {
        idsToCustomers.values()
    }

    @RequestMapping(value="/customers", method=RequestMethod.POST)
    def addCustomer(@RequestBody Customer customer) {
        UUID id = UUID.randomUUID()
        customer.id = id
        idsToCustomers[id] = customer
        deduplicate(customer)
        new ResponseEntity(customer, HttpStatus.CREATED)
    }

    @RequestMapping(value="/customers/{id}", method=RequestMethod.PUT)
    def updateCustomer(@PathVariable UUID id, @RequestBody Customer customer) {
        if (!idsToCustomers[id]) {
            return new ResponseEntity(HttpStatus.NOT_FOUND)
        }
        customer.id = id // allow it to be blank in the body
        idsToCustomers[id] = customer
        new ResponseEntity(customer, HttpStatus.OK)
    }

    @RequestMapping(value="/customers/{id}", method=RequestMethod.DELETE)
    def deleteCustomer(@PathVariable UUID id) {
        if (!idsToCustomers[id]) {
            return new ResponseEntity(HttpStatus.NOT_FOUND)
        }
        idsToCustomers.remove(id)
        new ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @RequestMapping(value="customerSimilarity", method=RequestMethod.GET)
    def getSimilarCustomers() {
        similarities
    }

    private deduplicate(Customer customer) {
        RecordImpl record = new RecordImpl()
        record.addValue('id', customer.id.toString())
        record.addValue('name', customer.name)
        record.addValue('address1', customer.address1)
        record.addValue('email', customer.email)
        record.addValue('zip', customer.zip)
        dukeProcessor.deduplicate([record])
    }

    static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args)
    }

}

@Canonical
class SimilarityMatchListener extends AbstractMatchListener {

    Collection similarities

    void matches(Record r1, Record r2, double confidence) {
        def newDup = new CustomerSimilarity(
            UUID.fromString(r1.getValue('id')),
            UUID.fromString(r2.getValue('id')),
            new BigDecimal(confidence))
        similarities << newDup
        println newDup
    }

}
