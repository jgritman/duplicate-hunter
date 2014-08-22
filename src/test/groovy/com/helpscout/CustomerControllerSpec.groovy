package com.helpscout

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.IntegrationTest
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration

import spock.lang.Specification

import com.helpscout.controller.CustomerController
import com.helpscout.model.Customer

@ContextConfiguration(loader = SpringApplicationContextLoader.class, classes=Application)
@WebAppConfiguration
@IntegrationTest
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class CustomerControllerSpec extends Specification {

    @Autowired
    CustomerController customerController

    static final MORRIS = new Customer(name: "Morris B. Hartwick",
                email: "MorrisBHartwick@teleworm.us",
                address1: "3776 Hillside Street",
                zip: "85226")
    static final RICHARD = new Customer(name: "Richard R. Callahan",
                email: "RichardRCallahan@armyspy.com",
                address1: "871 Melody Lane",
                zip: "23872")


    def 'should flag exact duplicate addresses'() {
        when:
        customerController.addCustomer(MORRIS)
        customerController.addCustomer(MORRIS)

        then:
        customerController.potentialDuplicates.size() == 1
        customerController.potentialDuplicates[0].similarity > 0.98
    }

    def 'should not flag different addresses'() {
        when:
        customerController.addCustomer(MORRIS)
        customerController.addCustomer(RICHARD)

        then:
        customerController.potentialDuplicates.size() == 0
    }

    def 'should match almost identical records'() {
        when:
        def alterMorris = new Customer(name: "M Hartwick",
                email: "MorrisBHartwick@teleworm.us",
                address1: "3767 Hillside St #1",
                zip: "85226")
        customerController.addCustomer(MORRIS)
        customerController.addCustomer(alterMorris)

        then:
        customerController.potentialDuplicates.size() == 1
        customerController.potentialDuplicates[0].similarity > 0.85
    }

    def 'should flag when update duplicates the record'() {
        when:
        def customerResult = customerController.addCustomer(MORRIS)
        customerController.addCustomer(RICHARD)

        then:
        customerController.potentialDuplicates.size() == 0

        when:
        customerController.updateCustomer(customerResult.body.id, RICHARD)

        then:
        customerController.potentialDuplicates.size() == 1
    }

    def 'should unflag when update unduplicates the record'() {
        when:
        def customerResult = customerController.addCustomer(MORRIS)
        customerController.addCustomer(MORRIS)

        then:
        customerController.potentialDuplicates.size() == 1

        when:
        customerController.updateCustomer(customerResult.body.id, RICHARD)

        then:
        customerController.potentialDuplicates.size() == 0
    }

    def 'should only flag once when update reduplicates the record'() {
        when:
        def customerResult = customerController.addCustomer(MORRIS)
        customerController.addCustomer(MORRIS)

        then:
        customerController.potentialDuplicates.size() == 1

        when:
        customerController.updateCustomer(customerResult.body.id, MORRIS)

        then:
        customerController.potentialDuplicates.size() == 1
    }

    def 'should unflag when record deleted'() {
        when:
        def customerResult = customerController.addCustomer(MORRIS)
        customerController.addCustomer(MORRIS)

        then:
        customerController.potentialDuplicates.size() == 1

        when:
        customerController.deleteCustomer(customerResult.body.id)

        then:
        customerController.potentialDuplicates.size() == 0
    }

    def 'should not flag deleted record'() {
        when:
        def customerResult = customerController.addCustomer(MORRIS)
        customerController.deleteCustomer(customerResult.body.id)
        customerController.addCustomer(MORRIS)

        then:
        customerController.potentialDuplicates.size() == 0
    }

}
