package com.helpscout

import groovy.transform.Canonical;

@Canonical
class CustomerSimilarity {

    UUID customerId1
    UUID customerId2
    BigDecimal similarity

}
