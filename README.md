# OnlyFilms - Movie Review Platform

A movie review platform inspired by Letterboxd and MyAnimeList, built with Java (no Spring Boot) and MySQL.

## 🎬 Features

- **User Authentication** - Register, login with JWT tokens
- **Movie Database** - Browse movies synced from TMDB API
- **Reviews & Ratings** - Write reviews and rate movies (1-10)
- **Watchlist** - Track movies you want to watch
- **Watch History** - Log movies you've watched
- **Custom Lists** - Create and share movie lists
- **Social Features** - Follow users, activity feed

## 🛠️ Tech Stack

### Backend
- Java 17+ (No Spring Boot)
- Embedded Jetty Server
- MySQL Database (XAMPP)
- JWT Authentication
- BCrypt Password Hashing
- Gson for JSON
- TMDB API Integration

### Frontend
- Vanilla HTML/CSS/JavaScript
- Responsive dark theme design

## 📁 Project Structure

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

## 🚀 Getting Started

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

## 📡 API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login, returns JWT

### Movies
- `GET /api/movies` - List movies (paginated)
- `GET /api/movies/{id}` - Get movie details
- `GET /api/movies/search?q=` - Search movies

### Genres
- `GET /api/genres` - List all genres
- `GET /api/genres/{id}` - Get genre with movies

### Reviews
- `GET /api/reviews/movie/{id}` - Get movie reviews
- `POST /api/reviews` - Create review (auth required)
- `PUT /api/reviews/{id}` - Update review (auth required)
- `DELETE /api/reviews/{id}` - Delete review (auth required)

### Watchlist
- `GET /api/watchlist` - Get user's watchlist (auth required)
- `POST /api/watchlist/{movieId}` - Add to watchlist
- `DELETE /api/watchlist/{movieId}` - Remove from watchlist

### Users
- `GET /api/users/{id}` - Get user profile
- `GET /api/users/{id}/followers` - Get followers
- `GET /api/users/{id}/following` - Get following
- `POST /api/users/{id}/follow` - Follow user (auth required)

## 📄 License

MIT License
