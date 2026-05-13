package com.lab;

public class Customer {

    private int id;          // unique customer identifier
    private String name;     // customer's name
    private String address;  // customer's address
    private double balance;  // current account balance

    // Constructor matching the lab spec
    public Customer(int id, String name, String address, double balance) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.balance = balance;
    }

    // Convenience constructor (no balance — defaults to 0.0)
    public Customer(int id, String name, String address) {
        this(id, name, address, 0.0);
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "Customer{id=" + id +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", balance=" + balance + '}';
    }
}
