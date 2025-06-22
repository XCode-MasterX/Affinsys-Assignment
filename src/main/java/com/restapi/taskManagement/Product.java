package com.restapi.taskManagement;

public class Product {
    private int id;
    private String name;
    private float price;
    private String description;

    public String getName() { return name; }
    public Product setName(String name) { this.name = name; return this; }

    public int getId() { return id; }
    public Product setId(int id) { this.id = id; return this; }

    public float getPrice() { return price; }
    public Product setPrice(float price) { this.price = price; return this; }

    public String getDescription() { return description; }
    public Product setDescription(String desc) { this.description = desc; return this; }

    public String toString() {
        return String.format("%d, %s, %f, %s", id, name, price, description);
    }
}