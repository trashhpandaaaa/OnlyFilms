-- OnlyFilms Database Schema
-- Based on Letterboxd ERD
-- Run this file to create the database and all tables

CREATE DATABASE IF NOT EXISTS onlyfilms;
USE onlyfilms;

-- Drop existing tables in reverse dependency order
DROP TABLE IF EXISTS list_profile;
DROP TABLE IF EXISTS `list`;
DROP TABLE IF EXISTS favorite_film_list;
DROP TABLE IF EXISTS `like`;
DROP TABLE IF EXISTS comment;
DROP TABLE IF EXISTS activity;
DROP TABLE IF EXISTS follows_list;
DROP TABLE IF EXISTS film_crew;
DROP TABLE IF EXISTS film_cast;
DROP TABLE IF EXISTS film_studio;
DROP TABLE IF EXISTS film_country;
DROP TABLE IF EXISTS film_genre;
DROP TABLE IF EXISTS person;
DROP TABLE IF EXISTS studio;
DROP TABLE IF EXISTS country;
DROP TABLE IF EXISTS genre;
DROP TABLE IF EXISTS date_dim;
DROP TABLE IF EXISTS film;
DROP TABLE IF EXISTS profiles;
DROP TABLE IF EXISTS users;

-- ============================================================
-- SECTION: What users can do (Blue)
-- ============================================================

-- Users table (authentication only)
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

-- Date dimension table (shared across sections)
CREATE TABLE date_dim (
    date_id INT AUTO_INCREMENT PRIMARY KEY,
    full_date DATE NOT NULL UNIQUE,
    day INT NOT NULL,
    month INT NOT NULL,
    quarter INT NOT NULL,
    year INT NOT NULL
);

-- Profiles table (user profile details)
CREATE TABLE profiles (
    profile_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    display_name VARCHAR(100),
    bio TEXT,
    favorite_movie VARCHAR(255),
    profile_pic MEDIUMTEXT,
    join_date_id INT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (join_date_id) REFERENCES date_dim(date_id)
);

-- Follows table
CREATE TABLE follows_list (
    follower_id INT NOT NULL,
    following_id INT NOT NULL,
    PRIMARY KEY (follower_id, following_id),
    FOREIGN KEY (follower_id) REFERENCES profiles(profile_id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES profiles(profile_id) ON DELETE CASCADE
);

-- ============================================================
-- SECTION: Film characteristics (Pink)
-- ============================================================

-- Film table
CREATE TABLE film (
    film_id INT AUTO_INCREMENT PRIMARY KEY,
    tmdb_id INT UNIQUE,
    film_title VARCHAR(255) NOT NULL,
    release_year INT,
    synopsis TEXT,
    runtime_mins INT DEFAULT 0,
    poster_url VARCHAR(500),
    film_description TEXT
);

-- Genre table
CREATE TABLE genre (
    genre_id INT AUTO_INCREMENT PRIMARY KEY,
    genre VARCHAR(50) NOT NULL UNIQUE
);

-- Country table
CREATE TABLE country (
    country_id INT AUTO_INCREMENT PRIMARY KEY,
    country_name VARCHAR(100) NOT NULL UNIQUE
);

-- Studio table
CREATE TABLE studio (
    studio_id INT AUTO_INCREMENT PRIMARY KEY,
    studio_name VARCHAR(200) NOT NULL UNIQUE
);

-- Person table (actors, directors, crew)
CREATE TABLE person (
    person_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    bio TEXT
);

-- Junction: Film <-> Genre (M:N)
CREATE TABLE film_genre (
    film_id INT NOT NULL,
    genre_id INT NOT NULL,
    PRIMARY KEY (film_id, genre_id),
    FOREIGN KEY (film_id) REFERENCES film(film_id) ON DELETE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genre(genre_id) ON DELETE CASCADE
);

-- Junction: Film <-> Country (M:N)
CREATE TABLE film_country (
    film_id INT NOT NULL,
    country_id INT NOT NULL,
    PRIMARY KEY (film_id, country_id),
    FOREIGN KEY (film_id) REFERENCES film(film_id) ON DELETE CASCADE,
    FOREIGN KEY (country_id) REFERENCES country(country_id) ON DELETE CASCADE
);

-- Junction: Film <-> Studio (M:N)
CREATE TABLE film_studio (
    film_id INT NOT NULL,
    studio_id INT NOT NULL,
    PRIMARY KEY (film_id, studio_id),
    FOREIGN KEY (film_id) REFERENCES film(film_id) ON DELETE CASCADE,
    FOREIGN KEY (studio_id) REFERENCES studio(studio_id) ON DELETE CASCADE
);

-- Film Cast (who acted in the film)
CREATE TABLE film_cast (
    cast_id INT AUTO_INCREMENT PRIMARY KEY,
    film_id INT NOT NULL,
    person_id INT NOT NULL,
    FOREIGN KEY (film_id) REFERENCES film(film_id) ON DELETE CASCADE,
    FOREIGN KEY (person_id) REFERENCES person(person_id) ON DELETE CASCADE
);

-- Film Crew (who worked on the film)
CREATE TABLE film_crew (
    crew_id INT AUTO_INCREMENT PRIMARY KEY,
    film_id INT NOT NULL,
    person_id INT NOT NULL,
    FOREIGN KEY (film_id) REFERENCES film(film_id) ON DELETE CASCADE,
    FOREIGN KEY (person_id) REFERENCES person(person_id) ON DELETE CASCADE
);

-- ============================================================
-- SECTION: Where films are involved (Green)
-- ============================================================

-- Activity (reviews + watch logs combined)
CREATE TABLE activity (
    activity_id INT AUTO_INCREMENT PRIMARY KEY,
    profile_id INT NOT NULL,
    film_id INT NOT NULL,
    review_date_id INT,
    watched_date_id INT,
    rating DECIMAL(3,1),
    watched_status VARCHAR(50),
    review_description TEXT,
    FOREIGN KEY (profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE,
    FOREIGN KEY (film_id) REFERENCES film(film_id) ON DELETE CASCADE,
    FOREIGN KEY (review_date_id) REFERENCES date_dim(date_id),
    FOREIGN KEY (watched_date_id) REFERENCES date_dim(date_id)
);

-- Favorite Film List (user's favorite/watchlist films)
CREATE TABLE favorite_film_list (
    favorite_film_id INT AUTO_INCREMENT PRIMARY KEY,
    profile_id INT NOT NULL,
    film_id INT NOT NULL,
    sort_order INT DEFAULT 0,
    FOREIGN KEY (profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE,
    FOREIGN KEY (film_id) REFERENCES film(film_id) ON DELETE CASCADE,
    UNIQUE KEY unique_fav (profile_id, film_id)
);

-- List (user-created film lists)
CREATE TABLE `list` (
    list_id INT AUTO_INCREMENT PRIMARY KEY,
    profile_id INT NOT NULL,
    list_name VARCHAR(100) NOT NULL,
    list_description TEXT,
    created_date_id INT,
    is_public BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE,
    FOREIGN KEY (created_date_id) REFERENCES date_dim(date_id)
);

-- List-Profile junction (films in a list)
CREATE TABLE list_profile (
    list_id INT NOT NULL,
    film_id INT NOT NULL,
    PRIMARY KEY (list_id, film_id),
    FOREIGN KEY (list_id) REFERENCES `list`(list_id) ON DELETE CASCADE,
    FOREIGN KEY (film_id) REFERENCES film(film_id) ON DELETE CASCADE
);

-- Comment (on activities)
CREATE TABLE comment (
    comment_id INT AUTO_INCREMENT PRIMARY KEY,
    activity_id INT NOT NULL,
    profile_id INT NOT NULL,
    comment_content TEXT NOT NULL,
    created_id INT,
    FOREIGN KEY (activity_id) REFERENCES activity(activity_id) ON DELETE CASCADE,
    FOREIGN KEY (profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE,
    FOREIGN KEY (created_id) REFERENCES date_dim(date_id)
);

-- Like (on activities)
CREATE TABLE `like` (
    like_id INT AUTO_INCREMENT PRIMARY KEY,
    activity_id INT NOT NULL,
    profile_id INT NOT NULL,
    FOREIGN KEY (activity_id) REFERENCES activity(activity_id) ON DELETE CASCADE,
    FOREIGN KEY (profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE,
    UNIQUE KEY unique_like (activity_id, profile_id)
);

-- ============================================================
-- INDEXES for performance
-- ============================================================

CREATE INDEX idx_film_tmdb ON film(tmdb_id);
CREATE INDEX idx_activity_profile ON activity(profile_id);
CREATE INDEX idx_activity_film ON activity(film_id);
CREATE INDEX idx_comment_activity ON comment(activity_id);
CREATE INDEX idx_like_activity ON `like`(activity_id);
CREATE INDEX idx_favorite_profile ON favorite_film_list(profile_id);
CREATE INDEX idx_follows_follower ON follows_list(follower_id);
CREATE INDEX idx_follows_following ON follows_list(following_id);

SELECT 'Database schema created successfully!' AS status;
