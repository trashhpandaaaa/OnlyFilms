package com.onlyfilms.model;

import java.util.List;

public class FilmList {
    private int listId;
    private int profileId;
    private String listName;
    private String listDescription;
    private Integer createdDateId;
    private boolean isPublic;

    // Transient display fields
    private String displayName;
    private String profilePic;
    private String createdDate;
    private int filmCount;
    private List<Film> films; // films in this list

    public FilmList() {}

    public int getListId() { return listId; }
    public void setListId(int listId) { this.listId = listId; }

    public int getProfileId() { return profileId; }
    public void setProfileId(int profileId) { this.profileId = profileId; }

    public String getListName() { return listName; }
    public void setListName(String listName) { this.listName = listName; }

    public String getListDescription() { return listDescription; }
    public void setListDescription(String listDescription) { this.listDescription = listDescription; }

    public Integer getCreatedDateId() { return createdDateId; }
    public void setCreatedDateId(Integer createdDateId) { this.createdDateId = createdDateId; }

    public boolean getIsPublic() { return isPublic; }
    public void setIsPublic(boolean isPublic) { this.isPublic = isPublic; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

    public int getFilmCount() { return filmCount; }
    public void setFilmCount(int filmCount) { this.filmCount = filmCount; }

    public List<Film> getFilms() { return films; }
    public void setFilms(List<Film> films) { this.films = films; }
}
