/**
 * OnlyFilms API Client
 * Handles all communication with the backend API
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

// User management
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
        
        // Backend wraps response in {success, message, data}
        return json.data !== undefined ? json.data : json;
    } catch (error) {
        console.error(`API Error [${endpoint}]:`, error);
        throw error;
    }
}

// ===== Auth API =====
const AuthAPI = {
    async register(username, email, password) {
        const data = await apiRequest('/auth/register', {
            method: 'POST',
            body: JSON.stringify({ username, email, password })
        });
        
        if (data.token) {
            TokenManager.set(data.token);
            UserManager.set(data.user);
        }
        
        return data;
    },

    async login(username, password) {
        const data = await apiRequest('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });
        
        if (data.token) {
            TokenManager.set(data.token);
            UserManager.set(data.user);
        }
        
        return data;
    },

    async getMe() {
        return apiRequest('/auth/me');
    },

    logout() {
        TokenManager.remove();
        UserManager.remove();
    },

    isLoggedIn() {
        return !!TokenManager.get();
    }
};

// ===== Movies API =====
const MoviesAPI = {
    async getAll(page = 1, pageSize = 20) {
        return apiRequest(`/movies?page=${page}&pageSize=${pageSize}`);
    },

    async getById(id) {
        return apiRequest(`/movies/${id}`);
    },

    async search(query, page = 1, pageSize = 20) {
        return apiRequest(`/movies?search=${encodeURIComponent(query)}&page=${page}&pageSize=${pageSize}`);
    },

    async getByGenre(genreId, page = 1, pageSize = 20) {
        return apiRequest(`/movies?genre=${genreId}&page=${page}&pageSize=${pageSize}`);
    },

    async getByCategory(category, page = 1) {
        return apiRequest(`/movies?category=${category}&page=${page}`);
    },
    
    async getNowPlaying(page = 1) {
        return apiRequest(`/movies/now-playing?page=${page}`);
    },
    
    async getUpcoming(page = 1) {
        return apiRequest(`/movies/upcoming?page=${page}`);
    },
    
    async discover(options = {}) {
        const params = new URLSearchParams();
        params.append('page', options.page || 1);
        if (options.sort) params.append('sort', options.sort);
        if (options.year) params.append('year', options.year);
        if (options.yearGte) params.append('yearGte', options.yearGte);
        if (options.yearLte) params.append('yearLte', options.yearLte);
        if (options.ratingGte) params.append('ratingGte', options.ratingGte);
        if (options.genre) params.append('genre', options.genre);
        return apiRequest(`/movies?${params.toString()}`);
    },

    async getTopRated(limit = 10) {
        return apiRequest(`/movies/top?limit=${limit}`);
    }
};

// ===== Genres API =====
const GenresAPI = {
    async getAll() {
        return apiRequest('/genres');
    },

    async getById(id) {
        return apiRequest(`/genres/${id}`);
    }
};

// ===== Reviews API =====
const ReviewsAPI = {
    async getByMovie(movieId, page = 1, pageSize = 10) {
        return apiRequest(`/reviews/movie/${movieId}?page=${page}&pageSize=${pageSize}`);
    },

    async create(movieId, rating, content) {
        return apiRequest('/reviews', {
            method: 'POST',
            body: JSON.stringify({ movieId, rating, content })
        });
    },

    async update(reviewId, rating, content) {
        return apiRequest(`/reviews/${reviewId}`, {
            method: 'PUT',
            body: JSON.stringify({ rating, content })
        });
    },

    async delete(reviewId) {
        return apiRequest(`/reviews/${reviewId}`, {
            method: 'DELETE'
        });
    },

    async like(reviewId) {
        return apiRequest(`/reviews/${reviewId}/like`, {
            method: 'POST'
        });
    },

    async unlike(reviewId) {
        return apiRequest(`/reviews/${reviewId}/like`, {
            method: 'DELETE'
        });
    }
};

// ===== Watchlist API =====
const WatchlistAPI = {
    async get() {
        return apiRequest('/watchlist');
    },

    async add(movieId) {
        return apiRequest('/watchlist', {
            method: 'POST',
            body: JSON.stringify({ movieId })
        });
    },

    async remove(movieId) {
        return apiRequest(`/watchlist/${movieId}`, {
            method: 'DELETE'
        });
    },

    async markWatched(movieId) {
        return apiRequest(`/watchlist/${movieId}/watched`, {
            method: 'POST'
        });
    }
};

// ===== Users API =====
const UsersAPI = {
    async getProfile(userId) {
        return apiRequest(`/users/${userId}`);
    },

    async updateProfile(data) {
        return apiRequest('/users/profile', {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },
    
    async getStats(userId) {
        return apiRequest(`/users/${userId}/stats`);
    },

    async follow(userId) {
        return apiRequest(`/users/${userId}/follow`, {
            method: 'POST'
        });
    },

    async unfollow(userId) {
        return apiRequest(`/users/${userId}/follow`, {
            method: 'DELETE'
        });
    }
};

// ===== Person API =====
const PersonAPI = {
    async getById(id) {
        return apiRequest(`/person/${id}`);
    }
};

// Export for use in other files
window.API = {
    Auth: AuthAPI,
    Movies: MoviesAPI,
    Genres: GenresAPI,
    Reviews: ReviewsAPI,
    Watchlist: WatchlistAPI,
    Users: UsersAPI,
    Person: PersonAPI,
    Token: TokenManager,
    User: UserManager
};
