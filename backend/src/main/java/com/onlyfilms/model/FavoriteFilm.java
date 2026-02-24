package com.onlyfilms.model;

public class FavoriteFilm {
    private int favoriteFilmId;
    private int profileId;
    private int filmId;
    private int sortOrder;

    // Transient display fields
    private String filmTitle;
    private String posterUrl;
    private Integer tmdbId;

    public FavoriteFilm() {}

    public int getFavoriteFilmId() { return favoriteFilmId; }
    public void setFavoriteFilmId(int favoriteFilmId) { this.favoriteFilmId = favoriteFilmId; }

    public int getProfileId() { return profileId; }
    public void setProfileId(int profileId) { this.profileId = profileId; }

    public int getFilmId() { return filmId; }
    public void setFilmId(int filmId) { this.filmId = filmId; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public String getFilmTitle() { return filmTitle; }
    public void setFilmTitle(String filmTitle) { this.filmTitle = filmTitle; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public Integer getTmdbId() { return tmdbId; }
    public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }
}
