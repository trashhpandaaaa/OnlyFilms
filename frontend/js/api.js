/**
 * OnlyFilms API Client
 * Handles all communication with the backend API
 * Updated for Letterboxd ERD schema
 */

const API_BASE_URL = 'http://localhost:8080/api';

// Token management
const TokenManager = {
    get: () => localStorage.getItem('onlyfilms_token'),
    set: (token) => localStorage.setItem('onlyfilms_token', token),
    remove: () => localStorage.removeItem('onlyfilms_token'),
    getAuthHeader: () => {
        const token = TokenManager.get();
        return token ? { 'Authorization': `Bearer ${token}` } : {};
    }
};

// User management (stores profile info locally)
const UserManager = {
    get: () => {
        const user = localStorage.getItem('onlyfilms_user');
        return user ? JSON.parse(user) : null;
    },
    set: (user) => localStorage.setItem('onlyfilms_user', JSON.stringify(user)),
    remove: () => localStorage.removeItem('onlyfilms_user')
};

// API request helper
async function apiRequest(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const config = {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...TokenManager.getAuthHeader(),
            ...options.headers
        }
    };

    try {
        const response = await fetch(url, config);
        const json = await response.json();
        
        if (!response.ok || !json.success) {
            throw new Error(json.message || 'Request failed');
        }
        
        return json.data !== undefined ? json.data : json;
    } catch (error) {
        console.error(`API Error [${endpoint}]:`, error);
        throw error;
    }
}

// ===== Auth API =====
const AuthAPI = {
    async register(email, password, displayName) {
        const data = await apiRequest('/auth/register', {
            method: 'POST',
            body: JSON.stringify({ email, password, displayName })
        });
        
        if (data.token) {
            TokenManager.set(data.token);
            UserManager.set({
                userId: data.userId,
                profileId: data.profileId,
                email: data.email,
                displayName: data.displayName
            });
        }
        
        return data;
    },

    async login(email, password) {
        const data = await apiRequest('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
        
        if (data.token) {
            TokenManager.set(data.token);
            UserManager.set({
                userId: data.userId,
                profileId: data.profileId,
                email: data.email,
                displayName: data.displayName
            });
        }
        
        return data;
    },

    logout() {
        TokenManager.remove();
        UserManager.remove();
    },

    isLoggedIn() {
        return !!TokenManager.get();
    }
};

// ===== Movies API (TMDB proxy) =====
const MoviesAPI = {
    async getPopular(page = 1) {
        return apiRequest(`/movies/popular?page=${page}`);
    },

    async getTopRated(page = 1) {
        return apiRequest(`/movies/top-rated?page=${page}`);
    },

    async getNowPlaying(page = 1) {
        return apiRequest(`/movies/now-playing?page=${page}`);
    },

    async getUpcoming(page = 1) {
        return apiRequest(`/movies/upcoming?page=${page}`);
    },

    async getTrending(timeWindow = 'week', page = 1) {
        return apiRequest(`/movies/trending/${timeWindow}?page=${page}`);
    },

    async search(query, page = 1) {
        return apiRequest(`/movies/search?q=${encodeURIComponent(query)}&page=${page}`);
    },

    async getById(id) {
        return apiRequest(`/movies/${id}`);
    },

    async getByGenre(genreId, page = 1) {
        return apiRequest(`/movies/genre/${genreId}?page=${page}`);
    }
};

// ===== Genres API =====
const GenresAPI = {
    async getAll() {
        return apiRequest('/genres');
    }
};

// ===== Activity API (reviews + watch logs) =====
const ActivityAPI = {
    async getRecent(limit = 20, offset = 0) {
        return apiRequest(`/activity/recent?limit=${limit}&offset=${offset}`);
    },

    async getFeed(limit = 20, offset = 0) {
        return apiRequest(`/activity/feed?limit=${limit}&offset=${offset}`);
    },

    async getByProfile(profileId, limit = 20, offset = 0) {
        return apiRequest(`/activity/profile/${profileId}?limit=${limit}&offset=${offset}`);
    },

    async getByFilm(filmId, limit = 20, offset = 0) {
        return apiRequest(`/activity/film/${filmId}?limit=${limit}&offset=${offset}`);
    },

    async getById(activityId) {
        return apiRequest(`/activity/${activityId}`);
    },

    async create(tmdbId, filmTitle, releaseYear, posterUrl, rating, watchedStatus, reviewDescription) {
        return apiRequest('/activity', {
            method: 'POST',
            body: JSON.stringify({ tmdbId, filmTitle, releaseYear, posterUrl, rating, watchedStatus, reviewDescription })
        });
    },

    async update(activityId, data) {
        return apiRequest(`/activity/${activityId}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },

    async delete(activityId) {
        return apiRequest(`/activity/${activityId}`, {
            method: 'DELETE'
        });
    }
};

// ===== Comments API =====
const CommentsAPI = {
    async getByActivity(activityId) {
        return apiRequest(`/comments/activity/${activityId}`);
    },

    async create(activityId, commentContent) {
        return apiRequest('/comments', {
            method: 'POST',
            body: JSON.stringify({ activityId, commentContent })
        });
    },

    async delete(commentId) {
        return apiRequest(`/comments/${commentId}`, {
            method: 'DELETE'
        });
    }
};

// ===== Likes API =====
const LikesAPI = {
    async toggle(activityId) {
        return apiRequest('/likes/toggle', {
            method: 'POST',
            body: JSON.stringify({ activityId })
        });
    },

    async getByActivity(activityId) {
        return apiRequest(`/likes/activity/${activityId}`);
    }
};

// ===== Favorites API =====
const FavoritesAPI = {
    async getByProfile(profileId) {
        return apiRequest(`/favorites/${profileId}`);
    },

    async add(tmdbId, filmTitle, releaseYear, posterUrl) {
        return apiRequest('/favorites', {
            method: 'POST',
            body: JSON.stringify({ tmdbId, filmTitle, releaseYear, posterUrl })
        });
    },

    async remove(filmId) {
        return apiRequest(`/favorites/${filmId}`, {
            method: 'DELETE'
        });
    },

    async check(filmId) {
        return apiRequest(`/favorites/check/${filmId}`);
    }
};

// ===== Lists API =====
const ListsAPI = {
    async getPublic(limit = 20, offset = 0) {
        return apiRequest(`/lists?limit=${limit}&offset=${offset}`);
    },

    async getByProfile(profileId) {
        return apiRequest(`/lists/profile/${profileId}`);
    },

    async getById(listId) {
        return apiRequest(`/lists/${listId}`);
    },

    async create(listName, listDescription, isPublic = true) {
        return apiRequest('/lists', {
            method: 'POST',
            body: JSON.stringify({ listName, listDescription, isPublic })
        });
    },

    async update(listId, data) {
        return apiRequest(`/lists/${listId}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },

    async delete(listId) {
        return apiRequest(`/lists/${listId}`, {
            method: 'DELETE'
        });
    },

    async addFilm(listId, tmdbId, filmTitle, releaseYear, posterUrl) {
        const parsedTmdbId = Number(tmdbId);
        const parsedReleaseYear = releaseYear === '' || releaseYear == null ? null : Number(releaseYear);
        return apiRequest(`/lists/${listId}/films`, {
            method: 'POST',
            body: JSON.stringify({
                tmdbId: parsedTmdbId,
                filmTitle,
                releaseYear: Number.isFinite(parsedReleaseYear) ? parsedReleaseYear : null,
                posterUrl: posterUrl || null
            })
        });
    },

    async removeFilm(listId, filmId) {
        return apiRequest(`/lists/${listId}/films/${filmId}`, {
            method: 'DELETE'
        });
    }
};

// ===== Users API =====
const UsersAPI = {
    async getMe() {
        return apiRequest('/users/me');
    },

    async getProfile(profileId) {
        return apiRequest(`/users/${profileId}`);
    },

    async updateProfile(data) {
        return apiRequest('/users/me', {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },

    async search(query) {
        return apiRequest(`/users/search?q=${encodeURIComponent(query)}`);
    }
};

// ===== Follows API =====
const FollowsAPI = {
    async follow(profileId) {
        return apiRequest(`/follows/${profileId}`, {
            method: 'POST'
        });
    },

    async unfollow(profileId) {
        return apiRequest(`/follows/${profileId}`, {
            method: 'DELETE'
        });
    },

    async getFollowers(profileId) {
        return apiRequest(`/follows/${profileId}/followers`);
    },

    async getFollowing(profileId) {
        return apiRequest(`/follows/${profileId}/following`);
    },

    async getStatus(profileId) {
        return apiRequest(`/follows/${profileId}`);
    }
};

// ===== Person API =====
const PersonAPI = {
    async getById(id) {
        return apiRequest(`/tmdb/person/${id}`);
    }
};

// Export for use in other files
window.API = {
    Auth: AuthAPI,
    Movies: MoviesAPI,
    Genres: GenresAPI,
    Activity: ActivityAPI,
    Comments: CommentsAPI,
    Likes: LikesAPI,
    Favorites: FavoritesAPI,
    Lists: ListsAPI,
    Users: UsersAPI,
    Follows: FollowsAPI,
    Person: PersonAPI,
    Token: TokenManager,
    User: UserManager
};
