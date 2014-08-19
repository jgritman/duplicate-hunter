package com.helpscout

import groovy.transform.Canonical

@Canonical
class Customer {

    UUID id

    String name

    String email

    String address1

    String address2

    String city

    String state

    String zip

}
