package com.onlyfilms.model;

public class Film {
    private int filmId;
    private Integer tmdbId;
    private String filmTitle;
    private Integer releaseYear;
    private String synopsis;
    private int runtimeMins;
    private String posterUrl;
    private String filmDescription;

    // Transient computed fields
    private Double averageRating;
    private int reviewCount;
    private int watchedCount;
    private int favoriteCount;

    public Film() {}

    public int getFilmId() { return filmId; }
    public void setFilmId(int filmId) { this.filmId = filmId; }

    public Integer getTmdbId() { return tmdbId; }
    public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }

    public String getFilmTitle() { return filmTitle; }
    public void setFilmTitle(String filmTitle) { this.filmTitle = filmTitle; }

    public Integer getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }

    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }

    public int getRuntimeMins() { return runtimeMins; }
    public void setRuntimeMins(int runtimeMins) { this.runtimeMins = runtimeMins; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getFilmDescription() { return filmDescription; }
    public void setFilmDescription(String filmDescription) { this.filmDescription = filmDescription; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public int getWatchedCount() { return watchedCount; }
    public void setWatchedCount(int watchedCount) { this.watchedCount = watchedCount; }

    public int getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }
}
