package com.helpscout

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.IntegrationTest
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration

import com.helpscout.model.Customer;

import spock.lang.Specification

@ContextConfiguration(loader = SpringApplicationContextLoader.class, classes=Application)
@WebAppConfiguration
@IntegrationTest
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class CustomerApplicationSpec extends Specification {

    @Autowired
    Application app

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
        app.addCustomer(MORRIS)
        app.addCustomer(MORRIS)

        then:
        app.potentialDuplicates.size() == 1
        app.potentialDuplicates[0].similarity > 0.98
    }

    def 'should not flag different addresses'() {
        when:
        app.addCustomer(MORRIS)
        app.addCustomer(RICHARD)

        then:
        app.potentialDuplicates.size() == 0
    }

    def 'should match almost identical records'() {
        when:
        def alterMorris = new Customer(name: "M Hartwick",
                email: "MorrisBHartwick@teleworm.us",
                address1: "3767 Hillside St #1",
                zip: "85226")
        app.addCustomer(MORRIS)
        app.addCustomer(alterMorris)

        then:
        app.potentialDuplicates.size() == 1
        app.potentialDuplicates[0].similarity > 0.85
    }

    def 'should flag when update duplicates the record'() {
        when:
        def customerResult = app.addCustomer(MORRIS)
        app.addCustomer(RICHARD)

        then:
        app.potentialDuplicates.size() == 0

        when:
        app.updateCustomer(customerResult.body.id, RICHARD)

        then:
        app.potentialDuplicates.size() == 1
    }

    def 'should unflag when update unduplicates the record'() {
        when:
        def customerResult = app.addCustomer(MORRIS)
        app.addCustomer(MORRIS)

        then:
        app.potentialDuplicates.size() == 1

        when:
        app.updateCustomer(customerResult.body.id, RICHARD)

        then:
        app.potentialDuplicates.size() == 0
    }

    def 'should only flag once when update reduplicates the record'() {
        when:
        def customerResult = app.addCustomer(MORRIS)
        app.addCustomer(MORRIS)

        then:
        app.potentialDuplicates.size() == 1

        when:
        app.updateCustomer(customerResult.body.id, MORRIS)

        then:
        app.potentialDuplicates.size() == 1
    }

    def 'should unflag when record deleted'() {
        when:
        def customerResult = app.addCustomer(MORRIS)
        app.addCustomer(MORRIS)

        then:
        app.potentialDuplicates.size() == 1

        when:
        app.deleteCustomer(customerResult.body.id)

        then:
        app.potentialDuplicates.size() == 0
    }

    def 'should not flag deleted record'() {
        when:
        def customerResult = app.addCustomer(MORRIS)
        app.deleteCustomer(customerResult.body.id)
        app.addCustomer(MORRIS)

        then:
        app.potentialDuplicates.size() == 0
    }


}
