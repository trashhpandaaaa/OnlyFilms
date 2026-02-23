-- OnlyFilms Seed Data
-- Run this after schema.sql to populate with sample data

USE onlyfilms;

-- Insert genres
INSERT INTO genres (name, slug) VALUES
('Action', 'action'),
('Adventure', 'adventure'),
('Animation', 'animation'),
('Comedy', 'comedy'),
('Crime', 'crime'),
('Documentary', 'documentary'),
('Drama', 'drama'),
('Family', 'family'),
('Fantasy', 'fantasy'),
('History', 'history'),
('Horror', 'horror'),
('Music', 'music'),
('Mystery', 'mystery'),
('Romance', 'romance'),
('Science Fiction', 'science-fiction'),
('Thriller', 'thriller'),
('War', 'war'),
('Western', 'western');

-- Insert sample movies
INSERT INTO movies (title, original_title, overview, poster_url, backdrop_url, release_date, runtime, language, average_rating, rating_count) VALUES
('The Shawshank Redemption', 'The Shawshank Redemption', 'Imprisoned in the 1940s for the double murder of his wife and her lover, upstanding banker Andy Dufresne begins a new life at the Shawshank prison, where he puts his accounting skills to work for an pointedly corrupt warden.', 'https://image.tmdb.org/t/p/w500/9cqNxx0GxF0bflZmeSMuL5tnGzr.jpg', 'https://image.tmdb.org/t/p/original/kXfqcdQKsToO0OUXHcrrNCHDBzO.jpg', '1994-09-23', 142, 'en', 4.70, 24500),
('The Godfather', 'The Godfather', 'Spanning the years 1945 to 1955, a chronicle of the fictional Italian-American Corleone crime family. When organized crime family patriarch, Vito Corleone barely survives an attempt on his life, his youngest son, Michael steps in to take care of the would-be killers.', 'https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg', 'https://image.tmdb.org/t/p/original/tmU7GeKVybMWFButWEGl2M4GeiP.jpg', '1972-03-14', 175, 'en', 4.65, 18200),
('The Dark Knight', 'The Dark Knight', 'Batman raises the stakes in his war on crime. With the help of Lt. Jim Gordon and District Attorney Harvey Dent, Batman sets out to dismantle the remaining criminal organizations that plague the streets.', 'https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg', 'https://image.tmdb.org/t/p/original/nMKdUUepR0i5zn0y1T4CsSB5chy.jpg', '2008-07-16', 152, 'en', 4.55, 29800),
('Pulp Fiction', 'Pulp Fiction', 'A burger-loving hit man, his philosophical partner, a drug-addled gangster''s moll and a washed-up boxer converge in this sprawling, comedic crime caper.', 'https://image.tmdb.org/t/p/w500/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg', 'https://image.tmdb.org/t/p/original/suaEOtk1N1sgg2MTM7oZd2cfVp3.jpg', '1994-09-10', 154, 'en', 4.50, 25600),
('Inception', 'Inception', 'Cobb, a skilled thief who commits corporate espionage by infiltrating the subconscious of his targets is offered a chance to regain his old life as payment for a task considered to be impossible: inception.', 'https://image.tmdb.org/t/p/w500/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg', 'https://image.tmdb.org/t/p/original/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg', '2010-07-15', 148, 'en', 4.45, 33400),
('Forrest Gump', 'Forrest Gump', 'A man with a low IQ has accomplished great things in his life and been present during significant historic events—in each case, far exceeding what anyone imagined he could do.', 'https://image.tmdb.org/t/p/w500/arw2vcBveWOVZr6pxd9XTd1TdQa.jpg', 'https://image.tmdb.org/t/p/original/3h1JZGDhZ8nzxdgvkxha0qBqi05.jpg', '1994-06-23', 142, 'en', 4.50, 24100),
('The Matrix', 'The Matrix', 'Set in the 22nd century, The Matrix tells the story of a computer hacker who joins a group of underground insurgents fighting the vast and powerful computers who now rule the earth.', 'https://image.tmdb.org/t/p/w500/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg', 'https://image.tmdb.org/t/p/original/fNG7i7RqMErkcqhohV2a6cV1Ehy.jpg', '1999-03-30', 136, 'en', 4.40, 23700),
('Interstellar', 'Interstellar', 'The adventures of a group of explorers who make use of a newly discovered wormhole to surpass the limitations on human space travel and conquer the vast distances involved in an interstellar voyage.', 'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg', 'https://image.tmdb.org/t/p/original/xJHokMbljvjADYdit5fK5VQsXEG.jpg', '2014-11-05', 169, 'en', 4.45, 31200),
('Spirited Away', 'Sen to Chihiro no Kamikakushi', 'A young girl, Chihiro, becomes trapped in a strange new world of spirits. When her parents undergo a mysterious transformation, she must call upon the courage she never knew she had to free her family.', 'https://image.tmdb.org/t/p/w500/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg', 'https://image.tmdb.org/t/p/original/6oaL4DP75yABrd5EbC4H2zq5ghc.jpg', '2001-07-20', 125, 'ja', 4.55, 14300),
('Parasite', 'Gisaengchung', 'All unemployed, Ki-taek''s family takes peculiar interest in the wealthy and glamorous Parks for their livelihood until they get entangled in an unexpected incident.', 'https://image.tmdb.org/t/p/w500/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg', 'https://image.tmdb.org/t/p/original/TU9NIjwzjoKPwQHoHshkFcQUCG.jpg', '2019-05-30', 132, 'ko', 4.50, 15800),
('Your Name', 'Kimi no Na wa', 'High schoolers Mitsuha and Taki are complete strangers living separate lives. But one night, they suddenly switch places. Mitsuha wakes up in Taki''s body, and he in hers.', 'https://image.tmdb.org/t/p/w500/q719jXXEzOoYaps6babgKnONONX.jpg', 'https://image.tmdb.org/t/p/original/dIWwZW7dJJtqC6CgWzYkNVKIUm8.jpg', '2016-08-26', 106, 'ja', 4.50, 10200),
('Fight Club', 'Fight Club', 'A ticking-Loss insomnia-Loss insomniac and a slippery soap salesman channel primal male aggression into a shocking new form of therapy. Their concept catches on, with underground fight clubs forming in every town.', 'https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg', 'https://image.tmdb.org/t/p/original/hZkgoQYus5vegHoetLkCJzb17zJ.jpg', '1999-10-15', 139, 'en', 4.45, 26500),
('Goodfellas', 'Goodfellas', 'The true story of Henry Hill, a half-Irish, half-Sicilian Brooklyn kid who is adopted by neighbourhood gangsters at an early age and climbs the ranks of a Mafia family under the guidance of Jimmy Conway.', 'https://image.tmdb.org/t/p/w500/aKuFiU82s5ISJpGZp7YkIr3kCUd.jpg', 'https://image.tmdb.org/t/p/original/sw7mordbZxgITU877yTpZCud90M.jpg', '1990-09-12', 145, 'en', 4.40, 11700),
('The Lord of the Rings: The Fellowship of the Ring', 'The Lord of the Rings: The Fellowship of the Ring', 'Young hobbit Frodo Baggins, after inheriting a mysterious ring from his uncle Bilbo, must leave his home in order to keep it from falling into the hands of its evil creator.', 'https://image.tmdb.org/t/p/w500/6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg', 'https://image.tmdb.org/t/p/original/x2RS3uTcsJJ9IfjNPcgDmukoEcQ.jpg', '2001-12-18', 178, 'en', 4.45, 22100),
('Whiplash', 'Whiplash', 'Under the direction of a ruthless instructor, a talented young drummer begins to pursue perfection at any cost, even his humanity.', 'https://image.tmdb.org/t/p/w500/7fn624j5lj3xTme2SgiLCeuedmO.jpg', 'https://image.tmdb.org/t/p/original/fRGxZuo7jJUWQsVg9PREb98Aclp.jpg', '2014-10-10', 106, 'en', 4.40, 12400);

-- Link movies to genres (movie_genres)
-- The Shawshank Redemption - Drama, Crime
INSERT INTO movie_genres (movie_id, genre_id) VALUES (1, 7), (1, 5);
-- The Godfather - Drama, Crime
INSERT INTO movie_genres (movie_id, genre_id) VALUES (2, 7), (2, 5);
-- The Dark Knight - Action, Crime, Drama, Thriller
INSERT INTO movie_genres (movie_id, genre_id) VALUES (3, 1), (3, 5), (3, 7), (3, 16);
-- Pulp Fiction - Crime, Thriller
INSERT INTO movie_genres (movie_id, genre_id) VALUES (4, 5), (4, 16);
-- Inception - Action, Science Fiction, Adventure
INSERT INTO movie_genres (movie_id, genre_id) VALUES (5, 1), (5, 15), (5, 2);
-- Forrest Gump - Drama, Comedy, Romance
INSERT INTO movie_genres (movie_id, genre_id) VALUES (6, 7), (6, 4), (6, 14);
-- The Matrix - Action, Science Fiction
INSERT INTO movie_genres (movie_id, genre_id) VALUES (7, 1), (7, 15);
-- Interstellar - Adventure, Drama, Science Fiction
INSERT INTO movie_genres (movie_id, genre_id) VALUES (8, 2), (8, 7), (8, 15);
-- Spirited Away - Animation, Family, Fantasy
INSERT INTO movie_genres (movie_id, genre_id) VALUES (9, 3), (9, 8), (9, 9);
-- Parasite - Drama, Thriller, Comedy
INSERT INTO movie_genres (movie_id, genre_id) VALUES (10, 7), (10, 16), (10, 4);
-- Your Name - Animation, Romance, Drama
INSERT INTO movie_genres (movie_id, genre_id) VALUES (11, 3), (11, 14), (11, 7);
-- Fight Club - Drama, Thriller
INSERT INTO movie_genres (movie_id, genre_id) VALUES (12, 7), (12, 16);
-- Goodfellas - Drama, Crime
INSERT INTO movie_genres (movie_id, genre_id) VALUES (13, 7), (13, 5);
-- LOTR Fellowship - Adventure, Fantasy, Action
INSERT INTO movie_genres (movie_id, genre_id) VALUES (14, 2), (14, 9), (14, 1);
-- Whiplash - Drama, Music
INSERT INTO movie_genres (movie_id, genre_id) VALUES (15, 7), (15, 12);

-- Create a test user (password is 'password123')
INSERT INTO users (username, email, password_hash, display_name, bio) VALUES
('testuser', 'test@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.r8s6P.3bL9.r7xK6nW', 'Test User', 'Just a movie lover!');

SELECT 'Seed data inserted successfully!' AS status;
SELECT CONCAT(COUNT(*), ' genres inserted') AS genres FROM genres;
SELECT CONCAT(COUNT(*), ' movies inserted') AS movies FROM movies;
SELECT CONCAT(COUNT(*), ' movie-genre links inserted') AS links FROM movie_genres;
