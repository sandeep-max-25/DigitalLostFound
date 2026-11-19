
package com.example;

public class Item {

    private int id;
    private String name;
    private String type;
    private String category;
    private String location;
    private String contact;
    private boolean returned;

    public Item(int id, String name, String type,
                String category, String location, String contact) {

        this.id = id;
        this.name = name;
        this.type = type;
        this.category = category;
        this.location = location;
        this.contact = contact;
        this.returned = false;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getLocation() {
        return location;
    }

    public String getContact() {
        return contact;
    }

    public boolean isReturned() {
        return returned;
    }

    public void setReturned(boolean returned) {
        this.returned = returned;
    }
}
