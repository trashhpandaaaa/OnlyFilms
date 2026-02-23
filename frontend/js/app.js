/* ============================================================
   OnlyFilms — Application Logic
   ============================================================ */

// ─── State ──────────────────────────────────────────────────
const state = {
    currentSection: 'home',
    movies: { list: [], page: 1, hasMore: true, loading: false, query: '', sort: 'popularity.desc', year: '', rating: '' },
    genres: [],
    currentMovie: null,
    currentPerson: null,
    profileTab: 'watchlist',
    watchlist: new Set(),
    watched: new Set(),
};

const TMDB_IMG = 'https://image.tmdb.org/t/p/';
const POSTER_SIZE = 'w500';
const BACKDROP_SIZE = 'w1280';
const PROFILE_SIZE = 'w185';

// ─── Initialisation ─────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    initAuth();
    initNavbar();
    initSearch();
    initStarRating();
    initRevealObserver();
    populateYearFilter();
    loadHome();
});

// ─── Auth helpers ───────────────────────────────────────────
function initAuth() {
    if (API.Auth.isLoggedIn()) {
        const user = API.User.get();
        showLoggedIn(user);
        loadWatchlistState();
    }
}

function showLoggedIn(user) {
    document.getElementById('navAuth').classList.add('hidden');
    document.getElementById('navUser').classList.remove('hidden');
    if (user) {
        document.getElementById('navAvatar').textContent = (user.username || 'U')[0].toUpperCase();
        document.getElementById('userGreeting').textContent = user.username || 'Profile';
    }
    // Mobile
    const mobileAuth = document.getElementById('mobileAuthArea');
    if (mobileAuth) mobileAuth.innerHTML = `<button class="btn btn-ghost btn-full" onclick="navigateTo('profile')">My Profile</button><button class="btn btn-danger btn-full" onclick="logout()" style="margin-top:.5rem">Sign Out</button>`;
}

function showLoggedOut() {
    document.getElementById('navAuth').classList.remove('hidden');
    document.getElementById('navUser').classList.add('hidden');
    const mobileAuth = document.getElementById('mobileAuthArea');
    if (mobileAuth) mobileAuth.innerHTML = `<button class="btn btn-primary btn-full" onclick="showModal('login'); toggleMobileMenu()">Sign In</button>`;
}

async function loadWatchlistState() {
    try {
        const data = await API.Watchlist.get();
        state.watchlist.clear();
        state.watched.clear();
        if (Array.isArray(data)) {
            data.forEach(item => {
                state.watchlist.add(item.movieId || item.movie_id);
                if (item.watched) state.watched.add(item.movieId || item.movie_id);
            });
        }
    } catch { /* not logged in or error */ }
}

// ─── Navigation ─────────────────────────────────────────────
function navigateTo(section, data) {
    // hide all sections
    document.querySelectorAll('.page-section').forEach(s => s.classList.add('hidden'));
    // show target
    const el = document.getElementById(`${section}Section`);
    if (el) {
        el.classList.remove('hidden');
        el.classList.add('page-section');
    }
    // nav link active state
    document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
    const activeLink = document.querySelector(`.nav-link[data-section="${section}"]`);
    if (activeLink) activeLink.classList.add('active');

    state.currentSection = section;
    window.scrollTo({ top: 0, behavior: 'smooth' });

    switch (section) {
        case 'home': loadHome(); break;
        case 'movies': loadMovies(true); break;
        case 'genres': loadGenres(); break;
        case 'movieDetail': loadMovieDetail(data); break;
        case 'person': loadPersonDetail(data); break;
        case 'profile': loadProfile(); break;
    }
}

function showCategory(category) {
    state.movies.query = '';
    state.movies.sort = 'popularity.desc';
    state.movies.year = '';
    state.movies.rating = '';
    const titles = { 'now-playing': 'Now Playing', 'upcoming': 'Coming Soon', 'top-rated': 'Top Rated' };
    document.getElementById('moviesTitle').textContent = titles[category] || 'Browse Films';
    navigateTo('movies', { category });
}

// ─── Navbar ─────────────────────────────────────────────────
function initNavbar() {
    let lastScroll = 0;
    window.addEventListener('scroll', () => {
        const navbar = document.getElementById('navbar');
        navbar.classList.toggle('scrolled', window.scrollY > 30);
        lastScroll = window.scrollY;
    }, { passive: true });
}

function toggleMobileMenu() {
    const menu = document.getElementById('mobileMenu');
    menu.classList.toggle('hidden');
}

// ─── Search ─────────────────────────────────────────────────
function initSearch() {
    const globalInput = document.getElementById('globalSearch');
    const filterInput = document.getElementById('searchInput');
    let debounce;

    const doSearch = (q) => {
        state.movies.query = q;
        state.movies.page = 1;
        state.movies.hasMore = true;
        if (state.currentSection !== 'movies') navigateTo('movies');
        else loadMovies(true);
    };

    if (globalInput) globalInput.addEventListener('input', e => {
        clearTimeout(debounce);
        debounce = setTimeout(() => doSearch(e.target.value.trim()), 400);
    });
    if (filterInput) filterInput.addEventListener('input', e => {
        clearTimeout(debounce);
        debounce = setTimeout(() => {
            state.movies.query = e.target.value.trim();
            state.movies.page = 1;
            state.movies.hasMore = true;
            loadMovies(true);
        }, 400);
    });
}

// ─── Filters ────────────────────────────────────────────────
function populateYearFilter() {
    const sel = document.getElementById('yearSelect');
    if (!sel) return;
    const currentYear = new Date().getFullYear();
    for (let y = currentYear; y >= 1950; y--) {
        const opt = document.createElement('option');
        opt.value = y; opt.textContent = y;
        sel.appendChild(opt);
    }
}

function applyFilters() {
    state.movies.sort = document.getElementById('sortSelect')?.value || 'popularity.desc';
    state.movies.year = document.getElementById('yearSelect')?.value || '';
    state.movies.rating = document.getElementById('ratingSelect')?.value || '';
    state.movies.page = 1;
    state.movies.hasMore = true;
    loadMovies(true);
}

function resetFilters() {
    document.getElementById('sortSelect').value = 'popularity.desc';
    document.getElementById('yearSelect').value = '';
    document.getElementById('ratingSelect').value = '';
    document.getElementById('searchInput').value = '';
    state.movies.query = '';
    state.movies.sort = 'popularity.desc';
    state.movies.year = '';
    state.movies.rating = '';
    state.movies.page = 1;
    state.movies.hasMore = true;
    loadMovies(true);
}

// ─── Home ───────────────────────────────────────────────────
async function loadHome() {
    loadHomeRow('topMoviesGrid', () => API.Movies.getTopRated(12));
    loadHomeRow('nowPlayingGrid', () => API.Movies.getNowPlaying(1), 8);
    loadHomeRow('upcomingGrid', () => API.Movies.getUpcoming(1), 8);
}

async function loadHomeRow(gridId, fetcher, limit) {
    const grid = document.getElementById(gridId);
    if (!grid) return;
    grid.innerHTML = skeletonCards(limit || 12);
    try {
        let data = await fetcher();
        let movies = Array.isArray(data) ? data : data?.results || data?.movies || [];
        if (limit) movies = movies.slice(0, limit);
        grid.innerHTML = movies.length ? movies.map(m => createMovieCard(m)).join('') : '<p class="empty-state">No films found.</p>';
        updateRevealObserver();
    } catch (err) {
        grid.innerHTML = `<p class="error-state">Could not load films. <button onclick="loadHome()">Retry</button></p>`;
    }
}

// ─── Movies Page ────────────────────────────────────────────
async function loadMovies(reset) {
    if (state.movies.loading) return;
    const grid = document.getElementById('moviesGrid');
    const loadBtn = document.getElementById('loadMoreBtn');
    const spinner = document.getElementById('loadMoreSpinner');
    if (!grid) return;

    if (reset) {
        state.movies.page = 1;
        state.movies.hasMore = true;
        grid.innerHTML = skeletonCards(20);
    }
    state.movies.loading = true;
    if (loadBtn) loadBtn.classList.add('hidden');
    if (spinner) spinner.classList.remove('hidden');

    try {
        let data;
        if (state.movies.query) {
            data = await API.Movies.search(state.movies.query, state.movies.page);
        } else {
            data = await API.Movies.discover({
                page: state.movies.page,
                sort: state.movies.sort,
                year: state.movies.year,
                ratingGte: state.movies.rating ? (parseFloat(state.movies.rating) * 2).toString() : undefined,
            });
        }
        let movies = Array.isArray(data) ? data : data?.results || data?.movies || [];
        state.movies.hasMore = movies.length >= 20;

        const html = movies.map(m => createMovieCard(m)).join('');
        if (reset) grid.innerHTML = html || '<p class="empty-state">No films match your filters.</p>';
        else grid.insertAdjacentHTML('beforeend', html);
    } catch (err) {
        if (reset) grid.innerHTML = `<p class="error-state">Failed to load films. <button onclick="loadMovies(true)">Retry</button></p>`;
    } finally {
        state.movies.loading = false;
        if (spinner) spinner.classList.add('hidden');
        if (loadBtn) loadBtn.classList.toggle('hidden', !state.movies.hasMore);
    }
}

function loadMoreMovies() {
    state.movies.page++;
    loadMovies(false);
}

// ─── Movie Card ─────────────────────────────────────────────
function createMovieCard(movie) {
    const id = movie.id || movie.tmdbId || movie.tmdb_id;
    const title = escapeHtml(movie.title || 'Untitled');
    const year = movie.release_date ? movie.release_date.substring(0, 4) : (movie.releaseDate ? movie.releaseDate.substring(0, 4) : '');
    const rating = movie.vote_average ?? movie.voteAverage ?? movie.rating ?? 0;
    const stars = (rating / 2).toFixed(1);
    const posterPath = movie.poster_path || movie.posterPath;
    const poster = posterPath ? `<img class="movie-poster" src="${TMDB_IMG}${POSTER_SIZE}${posterPath}" alt="${title}" loading="lazy">` : `<div class="poster-placeholder"><svg viewBox="0 0 24 24" fill="currentColor"><path d="M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z"/></svg></div>`;

    const isInWatchlist = state.watchlist.has(id);
    const isWatched = state.watched.has(id);

    return `
    <div class="movie-card" onclick="navigateTo('movieDetail', ${id})">
        <div class="movie-poster-wrap">
            ${poster}
            <div class="movie-overlay">
                <div class="overlay-actions">
                    <button class="overlay-btn ${isInWatchlist ? 'active' : ''}" onclick="event.stopPropagation(); toggleWatchlist(${id})" title="Watchlist">
                        <svg viewBox="0 0 24 24" fill="${isInWatchlist ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/></svg>
                    </button>
                    <button class="overlay-btn ${isWatched ? 'active' : ''}" onclick="event.stopPropagation(); toggleWatched(${id})" title="Watched">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                    </button>
                </div>
                <div></div>
            </div>
        </div>
        <div class="movie-info">
            <div class="movie-title" title="${title}">${title}</div>
            <div class="movie-meta">
                <span>${year}</span>
                <span class="movie-rating">
                    <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
                    ${stars}
                </span>
            </div>
        </div>
    </div>`;
}

function skeletonCards(n) {
    return Array.from({ length: n }, () => `
        <div class="movie-card" style="pointer-events:none">
            <div class="skeleton-poster"></div>
            <div class="movie-info"><div class="skeleton-text"></div><div class="skeleton-text short"></div></div>
        </div>`).join('');
}

// ─── Movie Detail ───────────────────────────────────────────
async function loadMovieDetail(movieId) {
    const container = document.getElementById('movieDetail');
    if (!container) return;
    container.innerHTML = `<div class="section-container"><div class="loading-state"><div class="spinner"><div class="dot"></div><div class="dot"></div><div class="dot"></div></div></div></div>`;
    state.currentMovie = movieId;

    try {
        const movie = await API.Movies.getById(movieId);
        const backdropPath = movie.backdrop_path || movie.backdropPath;
        const posterPath = movie.poster_path || movie.posterPath;
        const rating = movie.vote_average ?? movie.voteAverage ?? 0;
        const genres = movie.genres || [];
        const runtime = movie.runtime || 0;
        const cast = movie.credits?.cast || movie.cast || [];
        const crew = movie.credits?.crew || movie.crew || [];
        const director = crew.find(c => c.job === 'Director');
        const similar = movie.similar?.results || movie.similar || [];

        container.innerHTML = `
        <div class="movie-detail-page">
            ${backdropPath ? `<div class="movie-backdrop" style="background-image:url('${TMDB_IMG}${BACKDROP_SIZE}${backdropPath}')"></div>` : ''}
            <div class="movie-detail-content">
                <div>
                    ${posterPath ? `<img class="movie-detail-poster" src="${TMDB_IMG}${POSTER_SIZE}${posterPath}" alt="${escapeHtml(movie.title)}">` : '<div class="poster-placeholder" style="aspect-ratio:2/3;width:100%;border-radius:var(--radius)"><svg viewBox="0 0 24 24" fill="currentColor" width="80" height="80"><path d="M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z"/></svg></div>'}
                </div>
                <div class="movie-detail-info">
                    <h1>${escapeHtml(movie.title)}${movie.release_date || movie.releaseDate ? ` <span style="color:var(--text-muted);font-weight:400;font-size:1.25rem">(${(movie.release_date || movie.releaseDate || '').substring(0, 4)})</span>` : ''}</h1>
                    ${movie.tagline ? `<p class="movie-tagline">"${escapeHtml(movie.tagline)}"</p>` : ''}
                    <div class="movie-detail-meta">
                        <span class="meta-item rating">
                            <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
                            ${(rating / 2).toFixed(1)} / 5
                        </span>
                        ${runtime ? `<span class="meta-item"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>${formatRuntime(runtime)}</span>` : ''}
                        ${movie.release_date || movie.releaseDate ? `<span class="meta-item"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>${formatDate(movie.release_date || movie.releaseDate)}</span>` : ''}
                    </div>
                    <div class="movie-detail-genres">${genres.map(g => `<span class="genre-tag" onclick="event.stopPropagation(); browseGenre(${g.id},'${escapeHtml(g.name)}')">${escapeHtml(g.name)}</span>`).join('')}</div>
                    ${movie.overview ? `<p class="movie-overview">${escapeHtml(movie.overview)}</p>` : ''}
                    ${director ? `<p class="movie-credits"><strong>Director:</strong> <a href="#" class="person-link" onclick="event.preventDefault();navigateTo('person',${director.id})">${escapeHtml(director.name)}</a></p>` : ''}
                    ${cast.length ? `<p class="movie-credits"><strong>Cast:</strong> ${cast.slice(0, 8).map(c => `<a href="#" class="person-link" onclick="event.preventDefault();navigateTo('person',${c.id})">${escapeHtml(c.name)}</a>`).join(', ')}</p>` : ''}
                    <div class="movie-actions">
                        <button class="btn ${state.watchlist.has(movieId) ? 'btn-primary' : 'btn-outline'}" onclick="toggleWatchlist(${movieId})">
                            <svg viewBox="0 0 24 24" fill="${state.watchlist.has(movieId) ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/></svg>
                            ${state.watchlist.has(movieId) ? 'In Watchlist' : 'Add to Watchlist'}
                        </button>
                        <button class="btn ${state.watched.has(movieId) ? 'btn-primary' : 'btn-outline'}" onclick="toggleWatched(${movieId})">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                            ${state.watched.has(movieId) ? 'Watched' : 'Mark Watched'}
                        </button>
                        <button class="btn btn-outline" onclick="openReviewModal(${movieId}, '${escapeAttr(movie.title)}')">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                            Write Review
                        </button>
                    </div>
                    <div class="reviews-section" id="reviewsSection">
                        <div class="reviews-header">
                            <h2>Reviews</h2>
                        </div>
                        <div id="reviewsList"><div class="loading-state">Loading reviews…</div></div>
                    </div>
                    ${similar.length ? `
                    <div class="similar-section">
                        <h3>More Like This</h3>
                        <div class="movies-grid compact">${similar.slice(0, 6).map(m => createMovieCard(m)).join('')}</div>
                    </div>` : ''}
                </div>
            </div>
        </div>`;

        loadReviews(movieId);
    } catch (err) {
        container.innerHTML = `<div class="section-container"><p class="error-state">Failed to load film details. <button onclick="loadMovieDetail(${movieId})">Retry</button></p></div>`;
    }
}

async function loadReviews(movieId) {
    const list = document.getElementById('reviewsList');
    if (!list) return;
    try {
        const data = await API.Reviews.getByMovie(movieId);
        const reviews = Array.isArray(data) ? data : data?.reviews || [];
        if (!reviews.length) {
            list.innerHTML = '<p class="empty-state">No reviews yet. Be the first to share your thoughts!</p>';
            return;
        }
        list.innerHTML = reviews.map(r => createReviewCard(r)).join('');
    } catch {
        list.innerHTML = '<p class="empty-state">No reviews yet.</p>';
    }
}

function createReviewCard(review) {
    const user = API.User.get();
    const isOwner = user && (user.id === review.userId || user.id === review.user_id);
    const rating = review.rating || 0;
    const author = review.username || review.user?.username || 'Anonymous';
    const initial = author[0].toUpperCase();
    const stars = Array.from({ length: 5 }, (_, i) => `<svg viewBox="0 0 24 24" fill="currentColor" class="${i < rating ? 'filled' : ''}"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>`).join('');

    return `
    <div class="review-card">
        <div class="review-header">
            <div class="review-author">
                <div class="review-avatar">${initial}</div>
                <div>
                    <div class="review-author-name">${escapeHtml(author)}</div>
                    <div class="review-date">${formatDate(review.createdAt || review.created_at)}</div>
                </div>
            </div>
            <div class="review-stars">${stars}</div>
        </div>
        ${review.content ? `<p class="review-content">${escapeHtml(review.content)}</p>` : ''}
        <div class="review-actions">
            <button class="review-action-btn ${review.liked ? 'liked' : ''}" onclick="likeReview(${review.id})">
                <svg viewBox="0 0 24 24" fill="${review.liked ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                <span>${review.likes || 0}</span>
            </button>
            ${isOwner ? `<button class="review-action-btn delete" onclick="deleteReview(${review.id})"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>Delete</button>` : ''}
        </div>
    </div>`;
}

async function likeReview(reviewId) {
    if (!requireAuth()) return;
    try {
        await API.Reviews.like(reviewId);
        loadReviews(state.currentMovie);
    } catch { showToast('Could not like review'); }
}

async function deleteReview(reviewId) {
    if (!confirm('Delete this review?')) return;
    try {
        await API.Reviews.delete(reviewId);
        showToast('Review deleted');
        loadReviews(state.currentMovie);
    } catch { showToast('Could not delete review'); }
}

// ─── Person Detail ──────────────────────────────────────────
async function loadPersonDetail(personId) {
    const container = document.getElementById('personDetail');
    if (!container) return;
    container.innerHTML = `<div class="loading-state"><div class="spinner"><div class="dot"></div><div class="dot"></div><div class="dot"></div></div></div>`;
    state.currentPerson = personId;

    try {
        const person = await API.Person.getById(personId);
        const profilePath = person.profile_path || person.profilePath;
        const credits = person.movie_credits?.cast || person.credits?.cast || person.filmography || [];

        container.innerHTML = `
        <div class="person-header">
            <div class="person-photo">
                ${profilePath ? `<img src="${TMDB_IMG}${PROFILE_SIZE}${profilePath}" alt="${escapeHtml(person.name)}">` : `<div class="person-placeholder"><svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg></div>`}
            </div>
            <div class="person-info">
                <h1>${escapeHtml(person.name)}</h1>
                ${person.known_for_department ? `<p class="person-known-for">${escapeHtml(person.known_for_department)}</p>` : ''}
                ${person.birthday ? `<p class="person-meta"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>Born ${formatDate(person.birthday)}${person.place_of_birth ? ` · ${escapeHtml(person.place_of_birth)}` : ''}</p>` : ''}
                ${person.biography ? `<p class="person-bio">${escapeHtml(person.biography).substring(0, 600)}${person.biography.length > 600 ? '…' : ''}</p>` : ''}
            </div>
        </div>
        ${credits.length ? `
        <div class="filmography">
            <h2>Known For</h2>
            <div class="movies-grid compact">${credits.slice(0, 12).map(m => createMovieCard(m)).join('')}</div>
        </div>` : ''}`;
    } catch {
        container.innerHTML = `<p class="error-state">Failed to load person details. <button onclick="loadPersonDetail(${personId})">Retry</button></p>`;
    }
}

// ─── Genres ─────────────────────────────────────────────────
const genreColors = ['#00AFF0','#0095d0','#33bff3','#66cff6','#00AFF0','#0088cc','#00c4ff','#0095d0','#33bff3','#00AFF0','#0077b3','#00AFF0','#0095d0','#33bff3','#66cff6','#00AFF0','#0088cc','#00c4ff','#0095d0'];

async function loadGenres() {
    const grid = document.getElementById('genresGrid');
    if (!grid) return;
    if (state.genres.length) { renderGenres(grid); return; }
    grid.innerHTML = Array.from({ length: 12 }, () => '<div class="genre-card" style="opacity:.3"><div class="skeleton-text large"></div></div>').join('');
    try {
        const data = await API.Genres.getAll();
        state.genres = Array.isArray(data) ? data : data?.genres || [];
        renderGenres(grid);
    } catch {
        grid.innerHTML = '<p class="error-state">Failed to load genres. <button onclick="loadGenres()">Retry</button></p>';
    }
}

function renderGenres(grid) {
    grid.innerHTML = state.genres.map((g, i) => `
        <div class="genre-card" style="--genre-color:${genreColors[i % genreColors.length]}" onclick="browseGenre(${g.id},'${escapeAttr(g.name)}')">
            <div class="genre-name">${escapeHtml(g.name)}</div>
        </div>`).join('');
}

function browseGenre(genreId, genreName) {
    document.getElementById('moviesTitle').textContent = genreName || 'Films';
    state.movies.query = '';
    state.movies.genre = genreId;
    state.movies.page = 1;
    state.movies.hasMore = true;
    navigateTo('movies', { genre: genreId });
}

// ─── Profile ────────────────────────────────────────────────
async function loadProfile() {
    if (!requireAuth()) return;
    const container = document.getElementById('profileContainer');
    if (!container) return;
    const user = API.User.get();
    if (!user) return;

    container.innerHTML = `
    <div class="profile-header">
        <div class="profile-avatar">${(user.username || 'U')[0].toUpperCase()}</div>
        <div class="profile-info">
            <h1>${escapeHtml(user.username)}</h1>
            ${user.email ? `<p class="profile-email">${escapeHtml(user.email)}</p>` : ''}
            <div class="profile-stats">
                <div><div class="stat-val" id="statWatchlist">-</div><div class="stat-label">Watchlist</div></div>
                <div><div class="stat-val" id="statWatched">-</div><div class="stat-label">Watched</div></div>
                <div><div class="stat-val" id="statReviews">-</div><div class="stat-label">Reviews</div></div>
            </div>
        </div>
    </div>
    <div class="profile-tabs">
        <button class="tab-btn active" onclick="switchProfileTab('watchlist',this)">Watchlist</button>
        <button class="tab-btn" onclick="switchProfileTab('reviews',this)">Reviews</button>
    </div>
    <div class="profile-tab-content" id="profileTabContent"><div class="loading-state">Loading…</div></div>`;

    loadProfileTab('watchlist');
    loadProfileStats(user.id);
}

async function loadProfileStats(userId) {
    try {
        await loadWatchlistState();
        document.getElementById('statWatchlist').textContent = state.watchlist.size;
        document.getElementById('statWatched').textContent = state.watched.size;
    } catch { }
    try {
        const stats = await API.Users.getStats(userId);
        if (stats?.reviewCount !== undefined) document.getElementById('statReviews').textContent = stats.reviewCount;
    } catch { }
}

function switchProfileTab(tab, btn) {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');
    state.profileTab = tab;
    loadProfileTab(tab);
}

async function loadProfileTab(tab) {
    const content = document.getElementById('profileTabContent');
    if (!content) return;
    content.innerHTML = '<div class="loading-state"><div class="spinner"><div class="dot"></div><div class="dot"></div><div class="dot"></div></div></div>';

    try {
        if (tab === 'watchlist') {
            const data = await API.Watchlist.get();
            const items = Array.isArray(data) ? data : [];
            if (!items.length) { content.innerHTML = '<p class="empty-state">Your watchlist is empty. Start adding films!</p>'; return; }
            content.innerHTML = `<div class="movies-grid">${items.map(item => {
                const m = item.movie || item;
                return createMovieCard({
                    id: m.movieId || m.movie_id || m.tmdbId || m.tmdb_id || m.id,
                    title: m.title,
                    poster_path: m.poster_path || m.posterPath,
                    vote_average: m.vote_average || m.voteAverage || m.rating || 0,
                    release_date: m.release_date || m.releaseDate || ''
                });
            }).join('')}</div>`;
        } else if (tab === 'reviews') {
            const user = API.User.get();
            // Try getting user profile which may have reviews
            const profile = await API.Users.getProfile(user.id);
            const reviews = profile?.reviews || [];
            if (!reviews.length) { content.innerHTML = '<p class="empty-state">You haven\'t written any reviews yet.</p>'; return; }
            content.innerHTML = reviews.map(r => createReviewCard(r)).join('');
        }
    } catch {
        content.innerHTML = '<p class="error-state">Failed to load data.</p>';
    }
}

// ─── Watchlist / Watched ────────────────────────────────────
async function toggleWatchlist(movieId) {
    if (!requireAuth()) return;
    try {
        if (state.watchlist.has(movieId)) {
            await API.Watchlist.remove(movieId);
            state.watchlist.delete(movieId);
            showToast('Removed from watchlist');
        } else {
            await API.Watchlist.add(movieId);
            state.watchlist.add(movieId);
            showToast('Added to watchlist');
        }
        // Refresh detail if on that page
        if (state.currentSection === 'movieDetail' && state.currentMovie === movieId) loadMovieDetail(movieId);
    } catch (err) { showToast(err.message || 'Could not update watchlist'); }
}

async function toggleWatched(movieId) {
    if (!requireAuth()) return;
    try {
        if (state.watched.has(movieId)) {
            state.watched.delete(movieId);
            showToast('Unmarked as watched');
        } else {
            await API.Watchlist.markWatched(movieId);
            state.watched.add(movieId);
            if (!state.watchlist.has(movieId)) state.watchlist.add(movieId);
            showToast('Marked as watched');
        }
        if (state.currentSection === 'movieDetail' && state.currentMovie === movieId) loadMovieDetail(movieId);
    } catch (err) { showToast(err.message || 'Could not update'); }
}

// ─── Modals ─────────────────────────────────────────────────
function showModal(name) {
    document.getElementById(`${name}Modal`)?.classList.remove('hidden');
    document.body.style.overflow = 'hidden';
}
function closeModal(name) {
    document.getElementById(`${name}Modal`)?.classList.add('hidden');
    document.body.style.overflow = '';
}
function switchModal(from, to) {
    closeModal(from);
    setTimeout(() => showModal(to), 150);
}

function openReviewModal(movieId, movieTitle) {
    if (!requireAuth()) return;
    document.getElementById('reviewMovieId').value = movieId;
    document.getElementById('reviewMovieTitle').textContent = movieTitle;
    document.getElementById('reviewRating').value = 0;
    document.getElementById('reviewContent').value = '';
    document.querySelectorAll('#starRating .star').forEach(s => s.classList.remove('active'));
    document.getElementById('ratingText').textContent = 'Select a rating';
    document.getElementById('reviewError')?.classList.add('hidden');
    showModal('review');
}

// ─── Auth Handlers ──────────────────────────────────────────
async function handleLogin(e) {
    e.preventDefault();
    const errEl = document.getElementById('loginError');
    errEl.classList.add('hidden');
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;
    try {
        const data = await API.Auth.login(username, password);
        closeModal('login');
        showLoggedIn(data.user || API.User.get());
        loadWatchlistState();
        showToast('Welcome back!');
        if (state.currentSection === 'home') loadHome();
    } catch (err) {
        errEl.textContent = err.message || 'Login failed';
        errEl.classList.remove('hidden');
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const errEl = document.getElementById('registerError');
    errEl.classList.add('hidden');
    const username = document.getElementById('registerUsername').value.trim();
    const email = document.getElementById('registerEmail').value.trim();
    const password = document.getElementById('registerPassword').value;
    try {
        const data = await API.Auth.register(username, email, password);
        closeModal('register');
        showLoggedIn(data.user || API.User.get());
        showToast('Account created! Welcome to OnlyFilms.');
    } catch (err) {
        errEl.textContent = err.message || 'Registration failed';
        errEl.classList.remove('hidden');
    }
}

async function handleReview(e) {
    e.preventDefault();
    const errEl = document.getElementById('reviewError');
    errEl.classList.add('hidden');
    const movieId = parseInt(document.getElementById('reviewMovieId').value);
    const rating = parseInt(document.getElementById('reviewRating').value);
    const content = document.getElementById('reviewContent').value.trim();
    if (!rating) {
        errEl.textContent = 'Please select a star rating';
        errEl.classList.remove('hidden');
        return;
    }
    try {
        await API.Reviews.create(movieId, rating, content);
        closeModal('review');
        showToast('Review submitted!');
        if (state.currentSection === 'movieDetail' && state.currentMovie === movieId) loadReviews(movieId);
    } catch (err) {
        errEl.textContent = err.message || 'Failed to submit review';
        errEl.classList.remove('hidden');
    }
}

function logout() {
    API.Auth.logout();
    state.watchlist.clear();
    state.watched.clear();
    showLoggedOut();
    showToast('Signed out');
    navigateTo('home');
}

function requireAuth() {
    if (!API.Auth.isLoggedIn()) {
        showModal('login');
        return false;
    }
    return true;
}

// ─── Star Rating ────────────────────────────────────────────
function initStarRating() {
    const labels = ['Terrible', 'Bad', 'OK', 'Good', 'Excellent'];
    document.querySelectorAll('#starRating .star').forEach(star => {
        star.addEventListener('click', () => {
            const val = parseInt(star.dataset.rating);
            document.getElementById('reviewRating').value = val;
            document.getElementById('ratingText').textContent = `${labels[val - 1]} (${val}/5)`;
            document.querySelectorAll('#starRating .star').forEach(s => {
                s.classList.toggle('active', parseInt(s.dataset.rating) <= val);
            });
        });
        star.addEventListener('mouseenter', () => {
            const val = parseInt(star.dataset.rating);
            document.querySelectorAll('#starRating .star').forEach(s => {
                s.classList.toggle('active', parseInt(s.dataset.rating) <= val);
            });
        });
    });
    document.getElementById('starRating')?.addEventListener('mouseleave', () => {
        const val = parseInt(document.getElementById('reviewRating').value);
        document.querySelectorAll('#starRating .star').forEach(s => {
            s.classList.toggle('active', parseInt(s.dataset.rating) <= val);
        });
    });
}

// ─── Scroll Reveal ──────────────────────────────────────────
let revealObserver;
function initRevealObserver() {
    revealObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
                revealObserver.unobserve(entry.target);
            }
        });
    }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' });
    updateRevealObserver();
}
function updateRevealObserver() {
    if (!revealObserver) return;
    document.querySelectorAll('.reveal:not(.visible)').forEach(el => revealObserver.observe(el));
}

// ─── Toast ──────────────────────────────────────────────────
let toastTimeout;
function showToast(message) {
    const toast = document.getElementById('toast');
    const msg = document.getElementById('toastMessage');
    if (!toast || !msg) return;
    msg.textContent = message;
    toast.classList.remove('hidden');
    clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => toast.classList.add('hidden'), 3000);
}

// ─── Utilities ──────────────────────────────────────────────
function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
function escapeAttr(str) {
    return escapeHtml(str).replace(/'/g, '&#39;');
}
function formatDate(dateStr) {
    if (!dateStr) return '';
    try { return new Date(dateStr).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' }); }
    catch { return dateStr; }
}
function formatRuntime(mins) {
    if (!mins) return '';
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    return h ? `${h}h ${m}m` : `${m}m`;
}
