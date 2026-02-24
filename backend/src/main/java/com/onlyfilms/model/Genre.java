package com.onlyfilms.model;

public class Genre {
    private int genreId;
    private String genre;

    public Genre() {}

    public Genre(int genreId, String genre) {
        this.genreId = genreId;
        this.genre = genre;
    }

    public int getGenreId() { return genreId; }
    public void setGenreId(int genreId) { this.genreId = genreId; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
}
