package com.onlyfilms.model;

public class Person {
    private int personId;
    private String name;
    private String bio;

    public Person() {}

    public Person(int personId, String name, String bio) {
        this.personId = personId;
        this.name = name;
        this.bio = bio;
    }

    public int getPersonId() { return personId; }
    public void setPersonId(int personId) { this.personId = personId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
