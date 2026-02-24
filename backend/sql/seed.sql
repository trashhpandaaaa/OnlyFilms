-- OnlyFilms Seed Data
-- Run this after schema.sql to populate with sample data

USE onlyfilms;

-- Insert date dimension entries
INSERT INTO date_dim (full_date, day, month, quarter, year) VALUES
('2025-01-15', 15, 1, 1, 2025),
('2025-02-20', 20, 2, 1, 2025),
('2025-03-10', 10, 3, 1, 2025),
('2025-06-01', 1, 6, 2, 2025),
('2025-12-25', 25, 12, 4, 2025),
('2026-01-01', 1, 1, 1, 2026),
('2026-02-14', 14, 2, 1, 2026),
('2026-02-24', 24, 2, 1, 2026);

-- Insert genres
INSERT INTO genre (genre) VALUES
('Action'), ('Adventure'), ('Animation'), ('Comedy'), ('Crime'),
('Documentary'), ('Drama'), ('Family'), ('Fantasy'), ('History'),
('Horror'), ('Music'), ('Mystery'), ('Romance'), ('Science Fiction'),
('Thriller'), ('War'), ('Western');

-- Insert countries
INSERT INTO country (country_name) VALUES
('United States'), ('United Kingdom'), ('France'), ('Japan'),
('South Korea'), ('Germany'), ('Italy'), ('India'), ('Canada'), ('Australia');

-- Insert studios
INSERT INTO studio (studio_name) VALUES
('Warner Bros.'), ('Universal Pictures'), ('Paramount Pictures'),
('Walt Disney Pictures'), ('Columbia Pictures'), ('20th Century Studios'),
('Lionsgate'), ('A24'), ('Studio Ghibli'), ('New Line Cinema');

-- Insert persons (actors/directors)
INSERT INTO person (name, bio) VALUES
('Christopher Nolan', 'British-American filmmaker known for cerebral, often nonlinear storytelling.'),
('Steven Spielberg', 'American filmmaker, one of the most commercially successful directors in history.'),
('Quentin Tarantino', 'American filmmaker known for nonlinear storylines and stylized violence.'),
('Leonardo DiCaprio', 'American actor and producer known for his work in biopics and period films.'),
('Morgan Freeman', 'American actor and narrator known for his distinctive deep voice.'),
('Tim Robbins', 'American actor and filmmaker.'),
('Marlon Brando', 'American actor considered one of the greatest of all time.'),
('Al Pacino', 'American actor and filmmaker known for his intense performances.'),
('Hayao Miyazaki', 'Japanese animator, filmmaker, and co-founder of Studio Ghibli.'),
('Bong Joon-ho', 'South Korean filmmaker known for genre-mixing social thrillers.');

-- Insert sample films
INSERT INTO film (tmdb_id, film_title, release_year, synopsis, runtime_mins, poster_url, film_description) VALUES
(278, 'The Shawshank Redemption', 1994, 'Imprisoned in the 1940s for the double murder of his wife and her lover, upstanding banker Andy Dufresne begins a new life at the Shawshank prison.', 142, 'https://image.tmdb.org/t/p/w500/9cqNxx0GxF0bflZmeSMuL5tnGzr.jpg', 'A tale of hope and perseverance in prison.'),
(238, 'The Godfather', 1972, 'Spanning the years 1945 to 1955, a chronicle of the fictional Italian-American Corleone crime family.', 175, 'https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg', 'The aging patriarch transfers control to his reluctant son.'),
(155, 'The Dark Knight', 2008, 'Batman raises the stakes in his war on crime with the help of Lt. Jim Gordon and District Attorney Harvey Dent.', 152, 'https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg', 'A gritty superhero crime thriller.'),
(680, 'Pulp Fiction', 1994, 'A burger-loving hit man, his philosophical partner, a drug-addled gangsters moll and a washed-up boxer converge.', 154, 'https://image.tmdb.org/t/p/w500/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg', 'Interconnected crime stories in Los Angeles.'),
(27205, 'Inception', 2010, 'Cobb, a skilled thief who commits corporate espionage by infiltrating the subconscious of his targets.', 148, 'https://image.tmdb.org/t/p/w500/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg', 'A mind-bending heist within dreams.'),
(129, 'Spirited Away', 2001, 'A young girl becomes trapped in a strange new world of spirits when her parents undergo a mysterious transformation.', 125, 'https://image.tmdb.org/t/p/w500/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg', 'Studio Ghibli masterpiece about courage and identity.'),
(496243, 'Parasite', 2019, 'All unemployed, Ki-taeks family takes peculiar interest in the wealthy and glamorous Parks for their livelihood.', 132, 'https://image.tmdb.org/t/p/w500/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg', 'A dark comedy thriller about class divide.'),
(157336, 'Interstellar', 2014, 'Explorers make use of a newly discovered wormhole to surpass the limitations on human space travel.', 169, 'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg', 'An epic science fiction journey through space and time.');

-- Link films to genres
INSERT INTO film_genre (film_id, genre_id) VALUES
(1, 7), (1, 5),       -- Shawshank: Drama, Crime
(2, 7), (2, 5),       -- Godfather: Drama, Crime
(3, 1), (3, 5), (3, 7), (3, 16),  -- Dark Knight: Action, Crime, Drama, Thriller
(4, 5), (4, 16),      -- Pulp Fiction: Crime, Thriller
(5, 1), (5, 15), (5, 2),  -- Inception: Action, SciFi, Adventure
(6, 3), (6, 8), (6, 9),   -- Spirited Away: Animation, Family, Fantasy
(7, 7), (7, 16), (7, 4),  -- Parasite: Drama, Thriller, Comedy
(8, 2), (8, 7), (8, 15);  -- Interstellar: Adventure, Drama, SciFi

-- Link films to countries
INSERT INTO film_country (film_id, country_id) VALUES
(1, 1), (2, 1), (3, 1), (4, 1), (5, 1), (5, 2),  -- US/UK films
(6, 4),   -- Spirited Away: Japan
(7, 5),   -- Parasite: South Korea
(8, 1), (8, 2);  -- Interstellar: US, UK

-- Link films to studios
INSERT INTO film_studio (film_id, studio_id) VALUES
(1, 2),   -- Shawshank: Universal (approx)
(2, 3),   -- Godfather: Paramount
(3, 1),   -- Dark Knight: Warner Bros
(4, 7),   -- Pulp Fiction: Lionsgate (approx)
(5, 1),   -- Inception: Warner Bros
(6, 9),   -- Spirited Away: Studio Ghibli
(7, 5),   -- Parasite: N/A using Columbia
(8, 3);   -- Interstellar: Paramount

-- Link films to cast
INSERT INTO film_cast (film_id, person_id) VALUES
(1, 6), (1, 5),   -- Shawshank: Tim Robbins, Morgan Freeman
(2, 7), (2, 8),   -- Godfather: Brando, Pacino
(5, 4),            -- Inception: DiCaprio
(6, 9),            -- Spirited Away: (Miyazaki as representative)
(7, 10);           -- Parasite: (Bong Joon-ho as representative)

-- Link films to crew (directors)
INSERT INTO film_crew (film_id, person_id) VALUES
(3, 1),   -- Dark Knight: Nolan
(5, 1),   -- Inception: Nolan
(8, 1),   -- Interstellar: Nolan
(4, 3),   -- Pulp Fiction: Tarantino
(6, 9),   -- Spirited Away: Miyazaki
(7, 10);  -- Parasite: Bong Joon-ho

-- Create test user (password is 'password123' - BCrypt hash)
INSERT INTO users (email, password) VALUES
('test@example.com', '$2a$12$LJ3m4ysMVxaJCwJhMCf.N.bGv/MBHFIkHgHIPzvwb37dsxBmGBLEa');

-- Create date entry for today
INSERT INTO date_dim (full_date, day, month, quarter, year) VALUES
('2026-02-24', 24, 2, 1, 2026)
ON DUPLICATE KEY UPDATE date_id = date_id;

-- Create test profile
INSERT INTO profiles (user_id, display_name, bio, join_date_id) VALUES
(1, 'TestUser', 'Just a movie lover!', (SELECT date_id FROM date_dim WHERE full_date = '2026-02-24'));

SELECT 'Seed data inserted successfully!' AS status;
SELECT CONCAT(COUNT(*), ' genres inserted') AS genres FROM genre;
SELECT CONCAT(COUNT(*), ' films inserted') AS films FROM film;
SELECT CONCAT(COUNT(*), ' persons inserted') AS persons FROM person;
