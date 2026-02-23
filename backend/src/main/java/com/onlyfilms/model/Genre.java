package com.onlyfilms.model;

public class Genre {
    private int id;
    private String name;
    private String slug;
    
    public Genre() {}
    
    public Genre(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
}
