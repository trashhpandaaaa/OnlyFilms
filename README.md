# OnlyFilms - Movie Review Platform

A movie review platform inspired by Letterboxd and MyAnimeList, built with Java and MySQL.

## Features

- **User Authentication** - Register, login with JWT tokens
- **Movie Database** - Browse movies synced from TMDB API
- **Reviews & Ratings** - Write reviews and rate movies (1-10)
- **Watchlist** - Track movies you want to watch
- **Watch History** - Log movies you've watched
- **Custom Lists** - Create and share movie lists
- **Social Features** - Follow users, activity feed

## Tech Stack

### Backend
- Java 17+
- Embedded Jetty Server
- MySQL Database (XAMPP)
- JWT Authentication
- BCrypt Password Hashing
- Gson for JSON
- TMDB API Integration

### Frontend
- Vanilla HTML/CSS/JavaScript
- Responsive dark theme design

## Project Structure

```
OnlyFilms/
├── backend/
│   ├── src/main/java/com/onlyfilms/
│   │   ├── config/         # Database configuration
│   │   ├── dao/            # Data Access Objects
│   │   ├── filter/         # Auth & CORS filters
│   │   ├── model/          # Entity classes
│   │   ├── service/        # Business logic
│   │   ├── servlet/        # REST API endpoints
│   │   ├── util/           # Utilities (JWT, JSON, etc.)
│   │   └── Main.java       # Entry point
│   ├── sql/                # Database schema & seed data
│   ├── pom.xml             # Maven dependencies
│   └── target/             # Build output
├── frontend/
│   ├── css/style.css       # Styling
│   ├── js/
│   │   ├── api.js          # API client
│   │   └── app.js          # Main application logic
│   └── index.html          # Main HTML
└── README.md
```

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.9+
- XAMPP (MySQL)
- TMDB API Key (optional, for syncing movies)

### Setup Database

1. Start XAMPP MySQL
2. Run the schema:
```bash
C:\xampp\mysql\bin\mysql.exe -u root < backend/sql/schema.sql
```

3. (Optional) Add seed data:
```bash
C:\xampp\mysql\bin\mysql.exe -u root < backend/sql/seed.sql
```

### Build & Run Backend

```bash
cd backend
mvn clean package -DskipTests
java -jar target/onlyfilms-backend-1.0-SNAPSHOT.jar
```

Server runs at: http://localhost:8080

### Sync Movies from TMDB

```bash
# Sync genres
curl -X POST http://localhost:8080/api/tmdb/sync-genres

# Sync popular movies
curl -X POST "http://localhost:8080/api/tmdb/sync-popular?pages=5"

# Sync top rated movies
curl -X POST "http://localhost:8080/api/tmdb/sync-top-rated?pages=5"
```

### Run Frontend

Open `frontend/index.html` in a browser, or serve with:
```bash
cd frontend
python -m http.server 3000
```


## API Endpoints

All endpoints are prefixed with `/api/`. Most endpoints return JSON. Endpoints requiring authentication expect a JWT in the `Authorization: Bearer <token>` header.

### Authentication
- `POST /api/auth/register` — Register a new user
- `POST /api/auth/login` — Login, returns JWT

### Movies
- `GET /api/movies` — List movies (paginated, supports `page` and `size` params)
- `GET /api/movies/{id}` — Get movie details by TMDB ID
- `GET /api/movies/search?q=` — Search movies by title

### Genres
- `GET /api/genres` — List all genres
- `GET /api/genres/{id}` — Get genre details and movies

### Reviews
- `GET /api/reviews/movie/{id}` — Get reviews for a movie
- `POST /api/reviews` — Create review (auth required)
- `PUT /api/reviews/{id}` — Update review (auth required)
- `DELETE /api/reviews/{id}` — Delete review (auth required)

### Watchlist
- `GET /api/watchlist` — Get your watchlist (auth required)
- `POST /api/watchlist/{movieId}` — Add movie to watchlist (auth required)
- `DELETE /api/watchlist/{movieId}` — Remove movie from watchlist (auth required)

### Users & Profiles
- `GET /api/users/{id}` — Get user profile by ID
- `GET /api/users/{id}/followers` — List followers
- `GET /api/users/{id}/following` — List following
- `POST /api/users/{id}/follow` — Follow/unfollow user (auth required)

### Lists
- `GET /api/lists` — List public lists (paginated)
- `GET /api/lists/{id}` — Get a list and its films (if public or owned)
- `GET /api/lists/profile/{profileId}` — Get all lists by a user
- `POST /api/lists` — Create a new list (auth required)
	- Body: `{ "listName": string, "listDescription"?: string, "isPublic"?: boolean }`
- `PUT /api/lists/{id}` — Update a list (auth required, owner only)
	- Body: `{ "listName"?: string, "listDescription"?: string, "isPublic"?: boolean }`
- `DELETE /api/lists/{id}` — Delete a list (auth required, owner only)
- `POST /api/lists/{id}/films` — Add a film to a list (auth required, owner only)
	- Body: `{ "tmdbId": number, "filmTitle": string, "releaseYear"?: number, "posterUrl"?: string }`
- `DELETE /api/lists/{id}/films/{tmdbId}` — Remove a film from a list (auth required, owner only)

#### Example: Add Film to List
```bash
curl -X POST http://localhost:8080/api/lists/5/films \
	-H "Authorization: Bearer <token>" \
	-H "Content-Type: application/json" \
	-d '{ "tmdbId": 603, "filmTitle": "The Matrix", "releaseYear": 1999, "posterUrl": "https://image.tmdb.org/t/p/w500/xyz.jpg" }'
```

### Favorites
- `GET /api/favorites` — Get your favorite films (auth required)
- `POST /api/favorites/{tmdbId}` — Add a film to favorites (auth required)
- `DELETE /api/favorites/{tmdbId}` — Remove a film from favorites (auth required)

### Activity Feed
- `GET /api/activity` — Get recent activity (auth required)

### Profile
- `GET /api/profile` — Get your profile (auth required)
- `PUT /api/profile` — Update profile (bio, avatar, etc., auth required)

### Health
- `GET /api/health` — Health check endpoint

---

## Usage Notes

- **Authentication:** Most write operations require a JWT. Pass it as `Authorization: Bearer <token>`.
- **Error Handling:** Errors return JSON with `success: false` and a `message` field.
- **CORS:** All endpoints support CORS for frontend integration.

## Contributing

Pull requests are welcome! Please open issues for bugs or feature requests.

## License

MIT License
