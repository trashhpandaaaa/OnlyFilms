// Add to List Modal logic
async function openAddToListModal(movieId, movieTitle, movieYear, posterUrl) {
    if (!requireAuth()) return;
    const select = document.getElementById('addToListSelect');
    const statusDiv = document.getElementById('addToListStatus');
    const confirmBtn = document.getElementById('addToListConfirm');
    if (!select || !statusDiv || !confirmBtn) return;

    statusDiv.textContent = '';
    select.innerHTML = '<option>Loading...</option>';
    select.disabled = true;
    confirmBtn.disabled = true;
    showModal('addToList');

    // Fetch user lists
    try {
        const user = API.User.get();
        const profileId = user?.profileId || user?.profile_id;
        if (!profileId) throw new Error('Not logged in');

        const data = await API.Lists.getByProfile(profileId);
        const lists = Array.isArray(data) ? data : [];
        select.innerHTML = '';
        let validCount = 0;

        lists.forEach(list => {
            const listId = list.listId || list.list_id;
            if (!listId) return;
            const opt = document.createElement('option');
            opt.value = String(listId);
            opt.textContent = list.listName || list.list_name || `List ${listId}`;
            select.appendChild(opt);
            validCount += 1;
        });

        if (!validCount) {
            select.innerHTML = '<option disabled>No lists found</option>';
            select.disabled = true;
            confirmBtn.disabled = true;
        } else {
            select.disabled = false;
            confirmBtn.disabled = false;
        }
    } catch (err) {
        select.innerHTML = '<option disabled>Error loading lists</option>';
        select.disabled = true;
        confirmBtn.disabled = true;
        statusDiv.textContent = err.message || 'Could not load lists.';
    }

    // Confirm button logic
    confirmBtn.onclick = async function() {
        const listId = Number(select.value);
        if (!Number.isInteger(listId) || listId <= 0) {
            statusDiv.textContent = 'Select a valid list.';
            return;
        }

        confirmBtn.disabled = true;
        statusDiv.textContent = 'Adding...';

        const parsedYear = movieYear === '' || movieYear == null ? null : Number(movieYear);
        const releaseYear = Number.isFinite(parsedYear) ? parsedYear : null;

        try {
            await API.Lists.addFilm(listId, movieId, movieTitle, releaseYear, posterUrl || null);
            statusDiv.textContent = 'Movie added to list!';
            showToast('Movie added to list');
            setTimeout(() => closeAddToListModal(), 500);
        } catch (err) {
            statusDiv.textContent = err.message || 'Failed to add movie.';
        } finally {
            confirmBtn.disabled = false;
        }
    };
}

function closeAddToListModal() {
    closeModal('addToList');
}
/* ============================================================
   OnlyFilms — Application Logic (Letterboxd ERD)
   ============================================================ */

// ─── State ──────────────────────────────────────────────────
const state = {
    currentSection: 'home',
    movies: { list: [], page: 1, hasMore: true, loading: false, query: '', sort: 'popularity.desc', year: '', rating: '' },
    genres: [],
    currentMovie: null,
    currentPerson: null,
    profileTab: 'favorites',
    favorites: new Set(),
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
        API.Users.getMe().then(profile => {
            API.User.set(profile);
            showLoggedIn(profile);
            loadFavoritesState();
        });
    }
}

function showLoggedIn(user) {
    document.getElementById('navAuth').classList.add('hidden');
    document.getElementById('navUser').classList.remove('hidden');
    const hero = document.getElementById('heroSection');
    if (hero) hero.classList.add('hidden');
    if (user) {
        const navAvatar = document.getElementById('navAvatar');
        if (user.profilePic) {
            navAvatar.innerHTML = `<img class="nav-avatar-img" src="${user.profilePic}" alt="Profile Picture">`;
        } else {
            navAvatar.textContent = (user.displayName || 'U')[0].toUpperCase();
        }
        document.getElementById('userGreeting').textContent = user.displayName || 'Profile';
    }
    // Mobile
    const mobileAuth = document.getElementById('mobileAuthArea');
    if (mobileAuth) mobileAuth.innerHTML = `<button class="btn btn-ghost btn-full" onclick="navigateTo('profile')">My Profile</button><button class="btn btn-danger btn-full" onclick="logout()" style="margin-top:.5rem">Sign Out</button>`;
}

function showLoggedOut() {
    document.getElementById('navAuth').classList.remove('hidden');
    document.getElementById('navUser').classList.add('hidden');
    const hero = document.getElementById('heroSection');
    if (hero) hero.classList.remove('hidden');
    const mobileAuth = document.getElementById('mobileAuthArea');
    if (mobileAuth) mobileAuth.innerHTML = `<button class="btn btn-primary btn-full" onclick="showModal('login'); toggleMobileMenu()">Sign In</button>`;
}

async function loadFavoritesState() {
    try {
        const user = API.User.get();
        if (!user) return;
        const data = await API.Favorites.getByProfile(user.profileId);
        state.favorites.clear();
        if (Array.isArray(data)) {
            data.forEach(item => {
                if (item.tmdbId || item.tmdb_id) state.favorites.add(item.tmdbId || item.tmdb_id);
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
        case 'userProfile': loadUserProfile(data); break;
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
        // Also search users and show dropdown
        searchUsers(q);
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

// ─── User Search ────────────────────────────────────────────
async function searchUsers(query) {
    const dropdown = document.getElementById('userSearchDropdown');
    if (!query || query.length < 2) {
        if (dropdown) dropdown.classList.add('hidden');
        return;
    }
    try {
        const users = await API.Users.search(query);
        const results = Array.isArray(users) ? users : [];
        if (!dropdown) {
            // Create dropdown dynamically
            const wrap = document.querySelector('.nav-search');
            if (!wrap) return;
            const dd = document.createElement('div');
            dd.id = 'userSearchDropdown';
            dd.className = 'user-search-dropdown hidden';
            wrap.appendChild(dd);
            return searchUsers(query); // re-run now that dropdown exists
        }
        if (!results.length) {
            dropdown.classList.add('hidden');
            return;
        }
        dropdown.innerHTML = `<div class="usd-header">People</div>` + results.slice(0, 5).map(u => `
            <div class="usd-item" onclick="navigateTo('userProfile', ${u.profileId}); document.getElementById('userSearchDropdown').classList.add('hidden')">
                <div class="usd-avatar">${(u.displayName || 'U')[0].toUpperCase()}</div>
                <span class="usd-name">${escapeHtml(u.displayName)}</span>
            </div>`).join('');
        dropdown.classList.remove('hidden');
    } catch { /* ignore search errors */ }
}

// Close user search dropdown when clicking outside
document.addEventListener('click', (e) => {
    const dd = document.getElementById('userSearchDropdown');
    if (dd && !e.target.closest('.nav-search')) dd.classList.add('hidden');
});

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
    loadHomeRow('topMoviesGrid', () => API.Movies.getTopRated(), 12);
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
        } else if (state.movies.genre) {
            data = await API.Movies.getByGenre(state.movies.genre, state.movies.page);
        } else {
            data = await API.Movies.getPopular(state.movies.page);
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
    const title = escapeHtml(movie.title || movie.filmTitle || 'Untitled');
    const year = movie.release_date ? movie.release_date.substring(0, 4) : (movie.releaseDate ? movie.releaseDate.substring(0, 4) : (movie.releaseYear || ''));
    const ratingRaw = movie.vote_average ?? movie.voteAverage ?? movie.rating ?? 0;
    const stars = movie.averageRating != null ? movie.averageRating.toFixed(1) : (ratingRaw / 2).toFixed(1);
    const posterPath = movie.poster_path || movie.posterPath;
    const posterUrl = movie.posterUrl;
    const poster = posterUrl ? `<img class="movie-poster" src="${posterUrl}" alt="${title}" loading="lazy">` : (posterPath ? `<img class="movie-poster" src="${TMDB_IMG}${POSTER_SIZE}${posterPath}" alt="${title}" loading="lazy">` : `<div class="poster-placeholder"><svg viewBox="0 0 24 24" fill="currentColor"><path d="M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z"/></svg></div>`);

    const isFav = state.favorites.has(id);

    return `
    <div class="movie-card" onclick="navigateTo('movieDetail', ${id})">
        <div class="movie-poster-wrap">
            ${poster}
            <div class="movie-overlay">
                <div class="overlay-actions">
                    <button class="overlay-btn ${isFav ? 'active' : ''}" onclick="event.stopPropagation(); toggleFavorite(${id}, '${escapeAttr(title)}', '${year}', '${escapeAttr(posterUrl || (posterPath ? TMDB_IMG + POSTER_SIZE + posterPath : ''))}')" title="Favorite">
                        <svg viewBox="0 0 24 24" fill="${isFav ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
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
        const backdropUrl = movie.backdropUrl;
        const posterPath = movie.poster_path || movie.posterPath;
        const posterUrl = movie.posterUrl;
        const rating = movie.averageRating != null ? (movie.averageRating * 2) : (movie.vote_average ?? movie.voteAverage ?? 0);
        const genres = movie.genres || [];
        const runtime = movie.runtime || 0;
        const cast = movie.credits?.cast || movie.cast || [];
        const crew = movie.credits?.crew || movie.crew || [];
        const director = crew.find(c => c.job === 'Director');
        const similar = movie.similar?.results || movie.similar || [];
        const isFav = state.favorites.has(movieId);
        const fullPosterUrl = posterUrl || (posterPath ? TMDB_IMG + POSTER_SIZE + posterPath : '');

        container.innerHTML = `
        <div class="movie-detail-page">
            ${backdropUrl || backdropPath ? `<div class="movie-backdrop" style="background-image:url('${backdropUrl || (TMDB_IMG + BACKDROP_SIZE + backdropPath)}')"></div>` : ''}
            <div class="movie-detail-content">
                <div>
                    ${posterUrl || posterPath ? `<img class="movie-detail-poster" src="${posterUrl || (TMDB_IMG + POSTER_SIZE + posterPath)}" alt="${escapeHtml(movie.title)}">` : '<div class="poster-placeholder" style="aspect-ratio:2/3;width:100%;border-radius:var(--radius)"><svg viewBox="0 0 24 24" fill="currentColor" width="80" height="80"><path d="M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z"/></svg></div>'}
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
                        <button class="btn ${isFav ? 'btn-primary' : 'btn-outline'}" onclick="toggleFavorite(${movieId}, '${escapeAttr(movie.title)}', '${(movie.release_date || movie.releaseDate || '').substring(0, 4)}', '${escapeAttr(fullPosterUrl)}')">
                            <svg viewBox="0 0 24 24" fill="${isFav ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                            ${isFav ? 'Favorited' : 'Add to Favorites'}
                        </button>
                        <button class="btn btn-outline" onclick="openReviewModal(${movieId}, '${escapeAttr(movie.title)}', '${(movie.release_date || movie.releaseDate || '').substring(0, 4)}', '${escapeAttr(fullPosterUrl)}')">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                            Log Activity
                        </button>
                        <button class="btn btn-outline" onclick="openAddToListModal(${movieId}, '${escapeAttr(movie.title)}', '${(movie.release_date || movie.releaseDate || '').substring(0, 4)}', '${escapeAttr(fullPosterUrl)}')">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M4 6h16M4 12h16M4 18h7"/></svg>
                            Add to List
                        </button>
                    </div>
                    <div class="reviews-section" id="reviewsSection">
                        <div class="reviews-header">
                            <h2>Activity</h2>
                        </div>
                        <div id="reviewsList"><div class="loading-state">Loading activity…</div></div>
                    </div>
                    ${similar.length ? `
                    <div class="similar-section">
                        <h3>More Like This</h3>
                        <div class="movies-grid compact">${similar.slice(0, 6).map(m => createMovieCard(m)).join('')}</div>
                    </div>` : ''}
                </div>
            </div>
        </div>`;

        loadActivities(movieId);
    } catch (err) {
        container.innerHTML = `<div class="section-container"><p class="error-state">Failed to load film details. <button onclick="loadMovieDetail(${movieId})">Retry</button></p></div>`;
    }
}

async function loadActivities(tmdbId) {
    const list = document.getElementById('reviewsList');
    if (!list) return;
    try {
        const data = await API.Activity.getByFilm(tmdbId);
        const activities = Array.isArray(data) ? data : [];
        if (!activities.length) {
            list.innerHTML = '<p class="empty-state">No activity yet. Be the first to log this film!</p>';
            return;
        }
        list.innerHTML = activities.map(a => createActivityCard(a)).join('');
    } catch {
        list.innerHTML = '<p class="empty-state">No activity yet.</p>';
    }
}

function createActivityCard(activity) {
    const user = API.User.get();
    const isOwner = user && (user.profileId === activity.profileId);
    const rating = activity.rating || 0;
    const author = activity.displayName || 'Anonymous';
    const authorProfileId = activity.profileId;
    const initial = author[0].toUpperCase();
    let starsHtml = '';
    for (let i = 1; i <= 5; i++) {
        starsHtml += `<span class="star-icon${i <= rating ? ' filled' : ''}">★</span>`;
    }
    const watchedStatus = activity.watchedStatus || activity.watched_status || '';
    const statusLabel = watchedStatus ? watchedStatus.replace(/_/g, ' ') : '';
    const statusClass = watchedStatus ? watchedStatus.toLowerCase().replace(/_/g, '-') : '';

    return `
    <div class="activity-card">
        <div class="activity-card-inner">
            <div class="activity-avatar" onclick="event.stopPropagation(); navigateTo('userProfile', ${authorProfileId})" style="cursor:pointer">${initial}</div>
            <div class="activity-body">
                <div class="activity-meta">
                    <a href="#" class="activity-author" onclick="event.preventDefault(); event.stopPropagation(); navigateTo('userProfile', ${authorProfileId})">${escapeHtml(author)}</a>
                    ${statusLabel ? `<span class="activity-status activity-status--${statusClass}">${statusLabel}</span>` : ''}
                    <span class="activity-dot">·</span>
                    <span class="activity-date">${formatDate(activity.reviewDate || activity.watchedDate)}</span>
                </div>
                ${rating ? `<div class="activity-rating">${starsHtml}</div>` : ''}
                ${activity.reviewDescription ? `<p class="activity-review">${escapeHtml(activity.reviewDescription)}</p>` : ''}
                <div class="activity-actions">
                    <button class="activity-action${activity.likedByCurrentUser ? ' active' : ''}" onclick="toggleActivityLike(${activity.activityId})">
                        <svg viewBox="0 0 24 24" fill="${activity.likedByCurrentUser ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                        <span>${activity.likeCount || 0}</span>
                    </button>
                    <button class="activity-action" onclick="toggleComments(${activity.activityId})">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                        <span>${activity.commentCount || 0}</span>
                    </button>
                    ${isOwner ? `<button class="activity-action activity-action--delete" onclick="deleteActivity(${activity.activityId})">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                    </button>` : ''}
                </div>
            </div>
        </div>
        <div class="activity-comments hidden" id="comments-${activity.activityId}"></div>
    </div>`;
}

async function toggleActivityLike(activityId) {
    if (!requireAuth()) return;
    try {
        await API.Likes.toggle(activityId);
        if (state.currentMovie) loadActivities(state.currentMovie);
    } catch { showToast('Could not update like'); }
}

async function toggleComments(activityId) {
    const container = document.getElementById(`comments-${activityId}`);
    if (!container) return;
    if (!container.classList.contains('hidden')) {
        container.classList.add('hidden');
        return;
    }
    container.classList.remove('hidden');
    container.innerHTML = '<div class="loading-state">Loading comments…</div>';
    try {
        const comments = await API.Comments.getByActivity(activityId);
        const list = Array.isArray(comments) ? comments : [];
        const user = API.User.get();
        const commentForm = API.Auth.isLoggedIn() ? `
            <div class="comment-form">
                <div class="comment-form-avatar">${user ? user.displayName[0].toUpperCase() : '?'}</div>
                <input type="text" class="comment-input" id="commentInput-${activityId}" placeholder="Add a comment...">
                <button class="comment-submit" onclick="postComment(${activityId})">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
                </button>
            </div>` : '';
        container.innerHTML = `
            ${list.map(c => {
                const cInitial = (c.displayName || 'A')[0].toUpperCase();
                return `
                <div class="comment-item">
                    <div class="comment-avatar" onclick="navigateTo('userProfile', ${c.profileId})" style="cursor:pointer">${cInitial}</div>
                    <div class="comment-body">
                        <a href="#" class="comment-author" onclick="event.preventDefault(); navigateTo('userProfile', ${c.profileId})">${escapeHtml(c.displayName || 'Anonymous')}</a>
                        <span class="comment-text">${escapeHtml(c.commentContent)}</span>
                    </div>
                    ${user && user.profileId === c.profileId ? `<button class="comment-delete" onclick="deleteComment(${c.commentId}, ${activityId})">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                    </button>` : ''}
                </div>`;
            }).join('') || '<p class="comment-empty">No comments yet — be the first!</p>'}
            ${commentForm}`;
    } catch {
        container.innerHTML = '<p class="comment-empty">Could not load comments.</p>';
    }
}

async function postComment(activityId) {
    const input = document.getElementById(`commentInput-${activityId}`);
    if (!input || !input.value.trim()) return;
    try {
        await API.Comments.create(activityId, input.value.trim());
        toggleComments(activityId);
        toggleComments(activityId);
        if (state.currentMovie) loadActivities(state.currentMovie);
    } catch { showToast('Could not post comment'); }
}

async function deleteComment(commentId, activityId) {
    if (!confirm('Delete this comment?')) return;
    try {
        await API.Comments.delete(commentId);
        toggleComments(activityId);
        toggleComments(activityId);
        if (state.currentMovie) loadActivities(state.currentMovie);
    } catch { showToast('Could not delete comment'); }
}

async function deleteActivity(activityId) {
    if (!confirm('Delete this activity?')) return;
    try {
        await API.Activity.delete(activityId);
        showToast('Activity deleted');
        if (state.currentMovie) loadActivities(state.currentMovie);
    } catch { showToast('Could not delete activity'); }
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

    try {
        const profile = await API.Users.getMe();
        const initial = (profile.displayName || user.displayName || 'U')[0].toUpperCase();
        const avatarHtml = profile.profilePic ? `<img class="profile-avatar-img" src="${profile.profilePic}" alt="Profile Picture">` : `<div class="profile-avatar">${initial}</div>`;

        container.innerHTML = `
        <div class="profile-card">
            <div class="profile-banner"></div>
            <div class="profile-card-body profile-card-flex">
                <div class="profile-avatar-block">
                    ${avatarHtml}
                </div>
                <div class="profile-info-block">
                    <div class="profile-name-block">
                        <h1>${escapeHtml(profile.displayName || user.displayName)}</h1>
                        ${profile.email ? `<p class="profile-email">${escapeHtml(profile.email)}</p>` : ''}
                        ${profile.bio ? `<p class="profile-bio">${escapeHtml(profile.bio)}</p>` : ''}
                    </div>
                    <div class="profile-actions-row">
                        <button class="btn btn-outline" onclick="openEditProfileModal()">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                            Edit Profile
                        </button>
                    </div>
                </div>
            </div>
            <div class="profile-stats-grid">
                <div class="stat-item"><div class="stat-val">${profile.reviewCount || 0}</div><div class="stat-label">Reviews</div></div>
                <div class="stat-item"><div class="stat-val">${profile.watchedCount || 0}</div><div class="stat-label">Watched</div></div>
                <div class="stat-item"><div class="stat-val">${profile.favoriteCount || 0}</div><div class="stat-label">Favorites</div></div>
                <div class="stat-item"><div class="stat-val">${profile.listCount || 0}</div><div class="stat-label">Lists</div></div>
                <div class="stat-item"><div class="stat-val">${profile.followersCount || 0}</div><div class="stat-label">Followers</div></div>
                <div class="stat-item"><div class="stat-val">${profile.followingCount || 0}</div><div class="stat-label">Following</div></div>
            </div>
        </div>
        <div class="profile-tabs">
            <button class="tab-btn active" onclick="switchProfileTab('favorites',this)">Favorites</button>
            <button class="tab-btn" onclick="switchProfileTab('activity',this)">Activity</button>
            <button class="tab-btn" onclick="switchProfileTab('lists',this)">Lists</button>
        </div>
        <div class="profile-tab-content" id="profileTabContent"><div class="loading-state">Loading…</div></div>`;

        loadProfileTab('favorites');
    } catch {
        container.innerHTML = `<p class="error-state">Failed to load profile. <button onclick="loadProfile()">Retry</button></p>`;
    }
}

function switchProfileTab(tab, btn) {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');
    loadProfileTab(tab);
}

async function loadProfileTab(tab) {
    const content = document.getElementById('profileTabContent');
    if (!content) return;
    content.innerHTML = '<div class="loading-state"><div class="spinner"><div class="dot"></div><div class="dot"></div><div class="dot"></div></div></div>';
    const user = API.User.get();
    if (!user) return;

    try {
        if (tab === 'favorites') {
            const data = await API.Favorites.getByProfile(user.profileId);
            const items = Array.isArray(data) ? data : [];
            if (!items.length) { content.innerHTML = '<p class="empty-state">No favorite films yet. Start adding some!</p>'; return; }
            content.innerHTML = `<div class="movies-grid">${items.map(item => createMovieCard({
                id: item.tmdbId || item.tmdb_id,
                title: item.filmTitle || item.film_title,
                posterUrl: item.posterUrl || item.poster_url,
                releaseYear: item.releaseYear || item.release_year || '',
                release_date: ''
            })).join('')}</div>`;
        } else if (tab === 'activity') {
            const data = await API.Activity.getByProfile(user.profileId);
            const activities = Array.isArray(data) ? data : [];
            if (!activities.length) { content.innerHTML = '<p class="empty-state">No activity yet. Start logging films!</p>'; return; }
            content.innerHTML = activities.map(a => {
                const filmTitle = a.filmTitle || '';
                const filmHeader = filmTitle ? `<div class="activity-film-header" onclick="navigateTo('movieDetail', ${a.tmdbId || 0})"><strong>${escapeHtml(filmTitle)}</strong></div>` : '';
                return filmHeader + createActivityCard(a);
            }).join('');
        } else if (tab === 'lists') {
            const data = await API.Lists.getByProfile(user.profileId);
            const lists = Array.isArray(data) ? data : [];
            let html = '';
            html += `<button class="btn btn-primary btn-create-list" onclick="openCreateListModal()">Create New List</button>`;
            if (!lists.length) {
                html += '<p class="empty-state">No lists yet. Create one!</p>';
            } else {
                html += lists.map(l => `
                    <div class="list-card list-card-clickable" onclick="openListDetail(${Number(l.listId || l.list_id || 0)})">
                        <h3>${escapeHtml(l.listName || l.list_name)}</h3>
                        ${l.listDescription ? `<p>${escapeHtml(l.listDescription)}</p>` : ''}
                        <div class="list-meta">
                            <span>${l.filmCount || 0} films</span>
                            <span>${l.isPublic || l.is_public ? 'Public' : 'Private'}</span>
                        </div>
                    </div>`).join('');
            }
            content.innerHTML = html;
        }
    } catch {
        content.innerHTML = '<p class="error-state">Failed to load data.</p>';
    }
}

// ─── User Profile (other users) ─────────────────────────────
async function loadUserProfile(profileId) {
    const container = document.getElementById('userProfileContainer');
    if (!container) return;
    container.innerHTML = '<div class="loading-state"><div class="spinner"><div class="dot"></div><div class="dot"></div><div class="dot"></div></div></div>';

    const currentUser = API.User.get();
    // If viewing own profile, redirect
    if (currentUser && currentUser.profileId === profileId) {
        navigateTo('profile');
        return;
    }

    try {
        const profile = await API.Users.getProfile(profileId);
        const isLoggedIn = API.Auth.isLoggedIn();
        const isFollowing = profile.isFollowing || false;

        const uInitial = (profile.displayName || 'U')[0].toUpperCase();
        const userProfilePic = profile.profilePic || profile.profile_pic;
        const avatarHtml = userProfilePic
            ? `<img class="profile-avatar-img" src="${userProfilePic}" alt="Profile Picture">`
            : `<div class="profile-avatar">${uInitial}</div>`;

        container.innerHTML = `
        <div class="profile-card">
            <div class="profile-banner"></div>
            <div class="profile-card-body profile-card-flex">
                <div class="profile-avatar-block">
                    ${avatarHtml}
                </div>
                <div class="profile-info-block profile-identity">
                    <div class="profile-name-block">
                        <div class="profile-name-row">
                            <h1>${escapeHtml(profile.displayName || 'User')}</h1>
                            ${profile.isFollowedBy ? '<span class="follows-you-badge">Follows you</span>' : ''}
                        </div>
                        ${profile.bio ? `<p class="profile-bio">${escapeHtml(profile.bio)}</p>` : ''}
                    </div>
                    <div class="profile-actions-row">
                        ${isLoggedIn ? `<button class="btn ${isFollowing ? 'btn-outline' : 'btn-primary'} btn-follow" id="followBtn" onclick="toggleFollow(${profileId})">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/>${isFollowing ? '<polyline points="17 11 19 13 23 9"/>' : '<line x1="20" y1="11" x2="20" y2="17"/><line x1="17" y1="14" x2="23" y2="14"/>'}</svg>
                            ${isFollowing ? 'Following' : 'Follow'}
                        </button>` : ''}
                    </div>
                </div>
            </div>
            <div class="profile-card-body">
                <div class="profile-stats-grid">
                    <div class="stat-item"><div class="stat-val">${profile.reviewCount || 0}</div><div class="stat-label">Reviews</div></div>
                    <div class="stat-item"><div class="stat-val">${profile.watchedCount || 0}</div><div class="stat-label">Watched</div></div>
                    <div class="stat-item"><div class="stat-val">${profile.favoriteCount || 0}</div><div class="stat-label">Favorites</div></div>
                    <div class="stat-item"><div class="stat-val">${profile.listCount || 0}</div><div class="stat-label">Lists</div></div>
                    <div class="stat-item" id="userFollowerCount"><div class="stat-val">${profile.followersCount || 0}</div><div class="stat-label">Followers</div></div>
                    <div class="stat-item"><div class="stat-val">${profile.followingCount || 0}</div><div class="stat-label">Following</div></div>
                </div>
            </div>
        </div>
        <div class="profile-tabs">
            <button class="tab-btn active" onclick="switchUserProfileTab('favorites',this, ${profileId})">Favorites</button>
            <button class="tab-btn" onclick="switchUserProfileTab('activity',this, ${profileId})">Activity</button>
            <button class="tab-btn" onclick="switchUserProfileTab('lists',this, ${profileId})">Lists</button>
        </div>
        <div class="profile-tab-content" id="userProfileTabContent"><div class="loading-state">Loading…</div></div>`;

        loadUserProfileTab('favorites', profileId);
    } catch {
        container.innerHTML = `<p class="error-state">Failed to load profile. <button onclick="loadUserProfile(${profileId})">Retry</button></p>`;
    }
}

function switchUserProfileTab(tab, btn, profileId) {
    document.querySelectorAll('#userProfileSection .tab-btn').forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');
    loadUserProfileTab(tab, profileId);
}

async function loadUserProfileTab(tab, profileId) {
    const content = document.getElementById('userProfileTabContent');
    if (!content) return;
    content.innerHTML = '<div class="loading-state"><div class="spinner"><div class="dot"></div><div class="dot"></div><div class="dot"></div></div></div>';

    try {
        if (tab === 'favorites') {
            const data = await API.Favorites.getByProfile(profileId);
            const items = Array.isArray(data) ? data : [];
            if (!items.length) { content.innerHTML = '<p class="empty-state">No favorite films yet.</p>'; return; }
            content.innerHTML = `<div class="movies-grid">${items.map(item => createMovieCard({
                id: item.tmdbId || item.tmdb_id,
                title: item.filmTitle || item.film_title,
                posterUrl: item.posterUrl || item.poster_url,
                releaseYear: item.releaseYear || item.release_year || '',
                release_date: ''
            })).join('')}</div>`;
        } else if (tab === 'activity') {
            const data = await API.Activity.getByProfile(profileId);
            const activities = Array.isArray(data) ? data : [];
            if (!activities.length) { content.innerHTML = '<p class="empty-state">No activity yet.</p>'; return; }
            content.innerHTML = activities.map(a => {
                const filmTitle = a.filmTitle || '';
                const filmHeader = filmTitle ? `<div class="activity-film-header" onclick="navigateTo('movieDetail', ${a.tmdbId || 0})"><strong>${escapeHtml(filmTitle)}</strong></div>` : '';
                return filmHeader + createActivityCard(a);
            }).join('');
        } else if (tab === 'lists') {
            const data = await API.Lists.getByProfile(profileId);
            const lists = Array.isArray(data) ? data : [];
            if (!lists.length) {
                content.innerHTML = '<p class="empty-state">No lists yet.</p>';
            } else {
                content.innerHTML = `<div class="lists-grid">${lists.map(l => `
                    <div class="list-card list-card-clickable" onclick="openListDetail(${Number(l.listId || l.list_id || 0)})">
                        <h3>${escapeHtml(l.listName || l.list_name)}</h3>
                        ${l.listDescription ? `<p>${escapeHtml(l.listDescription)}</p>` : ''}
                        <div class="list-meta">
                            <span>${l.filmCount || 0} films</span>
                            <span>${l.isPublic || l.is_public ? 'Public' : 'Private'}</span>
                        </div>
                    </div>
                `).join('')}</div>`;
            }
        }
    } catch {
        content.innerHTML = '<p class="error-state">Failed to load data.</p>';
    }
}

async function toggleFollow(profileId) {
    if (!requireAuth()) return;
    const btn = document.getElementById('followBtn');
    if (!btn) return;
    const isCurrentlyFollowing = btn.classList.contains('btn-outline');

    try {
        if (isCurrentlyFollowing) {
            await API.Follows.unfollow(profileId);
            btn.classList.remove('btn-outline');
            btn.classList.add('btn-primary');
            btn.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="11" x2="20" y2="17"/><line x1="17" y1="14" x2="23" y2="14"/></svg> Follow`;
            showToast('Unfollowed');
        } else {
            await API.Follows.follow(profileId);
            btn.classList.remove('btn-primary');
            btn.classList.add('btn-outline');
            btn.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><polyline points="17 11 19 13 23 9"/></svg> Following`;
            showToast('Following!');
        }
        // Update follower count
        const status = await API.Follows.getStatus(profileId);
        const countEl = document.querySelector('#userFollowerCount .stat-val');
        if (countEl && status) countEl.textContent = status.followerCount || 0;
    } catch { showToast('Could not update follow status'); }
}

// ─── Favorites ──────────────────────────────────────────────
async function toggleFavorite(tmdbId, filmTitle, releaseYear, posterUrl) {
    if (!requireAuth()) return;
    try {
        if (state.favorites.has(tmdbId)) {
            await API.Favorites.remove(tmdbId);
            state.favorites.delete(tmdbId);
            showToast('Removed from favorites');
        } else {
            await API.Favorites.add(tmdbId, filmTitle, releaseYear, posterUrl);
            state.favorites.add(tmdbId);
            showToast('Added to favorites');
        }
        if (state.currentSection === 'movieDetail' && state.currentMovie === tmdbId) loadMovieDetail(tmdbId);
    } catch (err) { showToast(err.message || 'Could not update favorites'); }
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

function openReviewModal(movieId, movieTitle, releaseYear, posterUrl) {
    if (!requireAuth()) return;
    document.getElementById('reviewMovieId').value = movieId;
    document.getElementById('reviewMovieTitle').textContent = movieTitle;
    document.getElementById('reviewMovieId').dataset.title = movieTitle;
    document.getElementById('reviewMovieId').dataset.year = releaseYear || '';
    document.getElementById('reviewMovieId').dataset.poster = posterUrl || '';
    document.getElementById('reviewRating').value = 0;
    document.getElementById('reviewContent').value = '';
    document.querySelectorAll('#starRating .star').forEach(s => s.classList.remove('active'));
    document.getElementById('ratingText').textContent = 'Select a rating';
    document.getElementById('reviewError')?.classList.add('hidden');
    const statusSel = document.getElementById('watchedStatus');
    if (statusSel) statusSel.value = 'WATCHED';
    showModal('review');
}

async function openEditProfileModal() {
    if (!requireAuth()) return;
    try {
        const profile = await API.Users.getMe();
        document.getElementById('editDisplayName').value = profile.displayName || '';
        document.getElementById('editBio').value = profile.bio || '';
        document.getElementById('editFavoriteMovie').value = profile.favoriteMovie || '';
        document.getElementById('editProfilePic').value = profile.profilePic || '';
        document.getElementById('editProfileError')?.classList.add('hidden');
        showModal('editProfile');
    } catch { showToast('Could not load profile data'); }
}

async function handleEditProfile(e) {
    e.preventDefault();
    const errEl = document.getElementById('editProfileError');
    errEl.classList.add('hidden');
    const data = {
        displayName: document.getElementById('editDisplayName').value.trim(),
        bio: document.getElementById('editBio').value.trim(),
        favoriteMovie: document.getElementById('editFavoriteMovie').value.trim(),
        profilePic: document.getElementById('editProfilePic').value.trim()
    };
    if (!data.displayName) {
        errEl.textContent = 'Display name is required';
        errEl.classList.remove('hidden');
        return;
    }
    try {
        await API.Users.updateProfile(data);
        closeModal('editProfile');
        showToast('Profile updated!');
        // Update nav display
        const user = API.User.get();
        if (user) {
            user.displayName = data.displayName;
            user.profilePic = data.profilePic;
            API.User.set(user);
            const navAvatar = document.getElementById('navAvatar');
            if (user.profilePic) {
                navAvatar.innerHTML = `<img class="nav-avatar-img" src="${user.profilePic}" alt="Profile Picture">`;
            } else {
                navAvatar.textContent = data.displayName[0].toUpperCase();
            }
            document.getElementById('userGreeting').textContent = data.displayName;
        }
        loadProfile();
    } catch (err) {
        errEl.textContent = err.message || 'Failed to update profile';
        errEl.classList.remove('hidden');
    }
}

// ─── Auth Handlers ──────────────────────────────────────────
async function handleLogin(e) {
    e.preventDefault();
    const errEl = document.getElementById('loginError');
    errEl.classList.add('hidden');
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;
    try {
        await API.Auth.login(email, password);
        closeModal('login');
        const profile = await API.Users.getMe();
        API.User.set(profile);
        showLoggedIn(profile);
        loadFavoritesState();
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
    const displayName = document.getElementById('registerDisplayName').value.trim();
    const email = document.getElementById('registerEmail').value.trim();
    const password = document.getElementById('registerPassword').value;
    try {
        await API.Auth.register(email, password, displayName);
        closeModal('register');
        showLoggedIn(API.User.get());
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
    const movieIdEl = document.getElementById('reviewMovieId');
    const tmdbId = parseInt(movieIdEl.value);
    const filmTitle = movieIdEl.dataset.title || '';
    const releaseYear = movieIdEl.dataset.year || '';
    const posterUrl = movieIdEl.dataset.poster || '';
    const rating = parseInt(document.getElementById('reviewRating').value);
    const reviewDescription = document.getElementById('reviewContent').value.trim();
    const watchedStatus = document.getElementById('watchedStatus')?.value || 'WATCHED';

    if (!rating) {
        errEl.textContent = 'Please select a star rating';
        errEl.classList.remove('hidden');
        return;
    }
    try {
        await API.Activity.create(tmdbId, filmTitle, releaseYear, posterUrl, rating, watchedStatus, reviewDescription);
        closeModal('review');
        showToast('Activity logged!');
        if (state.currentSection === 'movieDetail' && state.currentMovie === tmdbId) loadActivities(tmdbId);
    } catch (err) {
        errEl.textContent = err.message || 'Failed to log activity';
        errEl.classList.remove('hidden');
    }
}

function logout() {
    API.Auth.logout();
    state.favorites.clear();
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
// Create List Modal logic
function openCreateListModal() {
    document.getElementById('createListModal').classList.remove('hidden');
    document.body.style.overflow = 'hidden';
}
function closeCreateListModal() {
    document.getElementById('createListModal').classList.add('hidden');
    document.body.style.overflow = '';
}
async function handleCreateList(e) {
    e.preventDefault();
    const errEl = document.getElementById('createListError');
    errEl.classList.add('hidden');
    const listName = document.getElementById('createListName').value.trim();
    const listDescription = document.getElementById('createListDescription').value.trim();
    const isPublic = document.getElementById('createListPublic').checked;
    if (!listName) {
        errEl.textContent = 'List name is required';
        errEl.classList.remove('hidden');
        return;
    }
    try {
        await API.Lists.create(listName, listDescription, isPublic);
        closeCreateListModal();
        showToast('List created!');
        loadProfileTab('lists');
    } catch (err) {
        errEl.textContent = err.message || 'Failed to create list';
        errEl.classList.remove('hidden');
    }
}

async function openListDetail(listId) {
    if (!Number.isInteger(listId) || listId <= 0) {
        showToast('Invalid list');
        return;
    }

    const titleEl = document.getElementById('listDetailTitle');
    const metaEl = document.getElementById('listDetailMeta');
    const descEl = document.getElementById('listDetailDescription');
    const filmsEl = document.getElementById('listDetailFilms');
    if (!titleEl || !metaEl || !descEl || !filmsEl) return;

    showModal('listDetail');
    titleEl.textContent = 'Loading list...';
    metaEl.textContent = '';
    descEl.textContent = '';
    filmsEl.innerHTML = '<div class="loading-state"><div class="spinner"><div class="dot"></div><div class="dot"></div><div class="dot"></div></div></div>';

    try {
        const list = await API.Lists.getById(listId);
        const name = list.listName || list.list_name || `List ${listId}`;
        const filmCount = list.filmCount || (Array.isArray(list.films) ? list.films.length : 0);
        const privacy = (list.isPublic || list.is_public) ? 'Public' : 'Private';
        const owner = list.displayName || list.display_name || '';

        titleEl.textContent = name;
        metaEl.textContent = `${filmCount} films · ${privacy}${owner ? ` · by ${owner}` : ''}`;
        descEl.textContent = list.listDescription || list.list_description || '';

        const films = Array.isArray(list.films) ? list.films : [];
        if (!films.length) {
            filmsEl.innerHTML = '<p class="empty-state">No movies in this list yet.</p>';
            return;
        }

        filmsEl.innerHTML = `<div class="movies-grid">${films.map(f => createMovieCard({
            id: f.tmdbId || f.tmdb_id || f.filmId || f.film_id,
            title: f.filmTitle || f.film_title || f.title || 'Untitled',
            posterUrl: f.posterUrl || f.poster_url || '',
            releaseYear: f.releaseYear || f.release_year || '',
            release_date: ''
        })).join('')}</div>`;
    } catch (err) {
        filmsEl.innerHTML = '<p class="error-state">Could not open this list.</p>';
        showToast(err.message || 'Could not open list');
    }
}
// Profile picture upload handler
function handleProfilePicUpload(event) {
    const file = event.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = function(e) {
        document.getElementById('editProfilePic').value = e.target.result;
    };
    reader.readAsDataURL(file);
}
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
