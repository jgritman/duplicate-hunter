package com.helpscout

import groovy.transform.Canonical

@Canonical
class Customer {

    UUID id

    String firstName

    String lastName

    String email

    String address1

    String address2

    String city

    String state

    String zip

}
