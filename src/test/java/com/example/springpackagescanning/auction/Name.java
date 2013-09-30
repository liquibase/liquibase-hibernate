package com.example.springpackagescanning.auction;

import javax.persistence.Embeddable;

/**
 * @author Gavin King
 */
@Embeddable
public class Name {
    private String firstName;
    private String lastName;
    private Character initial;

    public Name(String first, Character middle, String last) {
        firstName = first;
        initial = middle;
        lastName = last;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Character getInitial() {
        return initial;
    }

    public void setInitial(Character initial) {
        this.initial = initial;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer().append(firstName).append(' ');
        if (initial != null)
            buf.append(initial).append(' ');
        return buf.append(lastName).toString();
    }

}
