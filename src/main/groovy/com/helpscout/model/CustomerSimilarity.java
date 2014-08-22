package com.helpscout.model;

import java.math.BigDecimal;

public class CustomerSimilarity {

    private Customer customer1;
    private Customer customer2;
    private BigDecimal similarity;

    public Customer getCustomer1() {
        return customer1;
    }

    public void setCustomer1(Customer customer1) {
        this.customer1 = customer1;
    }

    public Customer getCustomer2() {
        return customer2;
    }

    public void setCustomer2(Customer customer2) {
        this.customer2 = customer2;
    }

    public BigDecimal getSimilarity() {
        return similarity;
    }

    public void setSimilarity(BigDecimal similarity) {
        this.similarity = similarity;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((customer1 == null) ? 0 : customer1.hashCode());
        result = prime * result
                + ((customer2 == null) ? 0 : customer2.hashCode());
        result = prime * result
                + ((similarity == null) ? 0 : similarity.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CustomerSimilarity other = (CustomerSimilarity) obj;
        if (customer1 == null) {
            if (other.customer1 != null)
                return false;
        } else if (!customer1.equals(other.customer1))
            return false;
        if (customer2 == null) {
            if (other.customer2 != null)
                return false;
        } else if (!customer2.equals(other.customer2))
            return false;
        if (similarity == null) {
            if (other.similarity != null)
                return false;
        } else if (!similarity.equals(other.similarity))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CustomerSimilarity [customer1=" + customer1 + ", customer2="
                + customer2 + ", similarity=" + similarity + "]";
    }

}
