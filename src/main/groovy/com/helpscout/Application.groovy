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

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
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

    Map<UUID,Customer> emailsToCustomers = [:]

    /**
     * Not requested but for debugging purposes
     */
    @RequestMapping(value="/customers", method=RequestMethod.GET)
    def getAllCustomers() {
        emailsToCustomers.values()
    }

    @RequestMapping(value="/customers", method=RequestMethod.POST)
    def addCustomer(@RequestBody Customer customer) {
        UUID id = UUID.randomUUID()
        customer.id = id
        emailsToCustomers[id] = customer
        new ResponseEntity(customer, HttpStatus.CREATED)
    }

    @RequestMapping(value="/customers/{id}", method=RequestMethod.PUT)
    def updateCustomer(@PathVariable UUID id, @RequestBody Customer customer) {
        if (!emailsToCustomers[id]) {
            return new ResponseEntity(HttpStatus.NOT_FOUND)
        }
        customer.id = id // allow it to be blank in the body
        emailsToCustomers[id] = customer
        new ResponseEntity(customer, HttpStatus.OK)
    }

    @RequestMapping(value="/customers/{id}", method=RequestMethod.DELETE)
    def deleteCustomer(@PathVariable UUID id) {
        if (!emailsToCustomers[id]) {
            return new ResponseEntity(HttpStatus.NOT_FOUND)
        }
        emailsToCustomers.remove(id)
        new ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @RequestMapping(value="customerSimilarity", method=RequestMethod.GET)
    def getSimilarCustomers() {
         []
    }

    static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args)
    }

}
