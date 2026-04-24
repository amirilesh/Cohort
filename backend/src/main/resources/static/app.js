/* ============================================
   Cohort — Frontend Application Logic
   ============================================ */

const API_BASE = '';  // Same origin — Ktor serves both API and static files

// ---- State ----
let currentPaper = null;  // Paper we're generating a study card for
let lastQuery = '';

// ---- DOM Elements ----
const searchForm      = document.getElementById('search-form');
const searchInput     = document.getElementById('search-input');
const btnSearch       = document.getElementById('btn-search');
const heroSection     = document.getElementById('hero-section');
const resultsSection  = document.getElementById('results-section');
const resultsList     = document.getElementById('results-list');
const resultsTitle    = document.getElementById('results-title');
const resultsCount    = document.getElementById('results-count');
const studycardSection = document.getElementById('studycard-section');
const loadingContainer = document.getElementById('loading-container');
const errorContainer   = document.getElementById('error-container');
const errorReason      = document.getElementById('error-reason');
const studycardContent = document.getElementById('studycard-content');
const btnBack          = document.getElementById('btn-back');
const btnRetry         = document.getElementById('btn-retry');
const tabSearch        = document.getElementById('tab-search');
const tabHistory       = document.getElementById('tab-history');
const viewSearch       = document.getElementById('view-search');
const viewHistory      = document.getElementById('view-history');
const historyList      = document.getElementById('history-list');
const historyEmpty     = document.getElementById('history-empty');
const logoHome         = document.getElementById('logo-home');

// ---- Tab Switching ----
tabSearch.addEventListener('click', () => switchTab('search'));
tabHistory.addEventListener('click', () => {
    switchTab('history');
    loadHistory();
});

logoHome.addEventListener('click', () => {
    switchTab('search');
    resetToHome();
});

function switchTab(tab) {
    tabSearch.classList.toggle('active', tab === 'search');
    tabHistory.classList.toggle('active', tab === 'history');
    viewSearch.style.display = tab === 'search' ? '' : 'none';
    viewHistory.style.display = tab === 'history' ? '' : 'none';
}

function resetToHome() {
    heroSection.classList.remove('compact');
    resultsSection.style.display = 'none';
    studycardSection.style.display = 'none';
    searchInput.value = '';
    searchInput.focus();
}

// ---- Search ----
searchForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const query = searchInput.value.trim();
    if (!query) return;

    lastQuery = query;
    btnSearch.disabled = true;
    btnSearch.textContent = 'Searching...';

    try {
        const resp = await fetch(`${API_BASE}/search?q=${encodeURIComponent(query)}`);
        const data = await resp.json();

        showResults(data);
    } catch (err) {
        console.error('Search failed:', err);
        resultsList.innerHTML = '<p style="color: var(--text-muted); text-align:center; padding:40px;">Search failed. Make sure the backend is running.</p>';
        resultsSection.style.display = '';
    } finally {
        btnSearch.disabled = false;
        btnSearch.textContent = 'Search';
    }
});

function showResults(data) {
    // Compact the hero
    heroSection.classList.add('compact');
    studycardSection.style.display = 'none';

    resultsTitle.textContent = `Results for "${data.query}"`;
    resultsCount.textContent = `${data.results.length} of ${data.totalCount} papers`;

    resultsList.innerHTML = '';

    if (data.results.length === 0) {
        resultsList.innerHTML = `
            <div style="text-align:center; padding:60px 20px; color: var(--text-muted);">
                <span style="font-size:48px; display:block; margin-bottom:16px;">🔍</span>
                <p>No open-access papers found for this query.<br>Try different keywords.</p>
            </div>
        `;
        resultsSection.style.display = '';
        return;
    }

    data.results.forEach((paper, index) => {
        const card = createPaperCard(paper, index);
        resultsList.appendChild(card);
    });

    resultsSection.style.display = '';
}

function createPaperCard(paper, index) {
    const card = document.createElement('div');
    card.className = 'paper-card';
    card.style.animationDelay = `${index * 0.05}s`;

    const shortDoi = paper.doi
        ? paper.doi.replace('https://doi.org/', '')
        : 'No DOI';

    card.innerHTML = `
        <div class="paper-title">${escapeHtml(paper.title)}</div>
        <div class="paper-meta">
            <span class="paper-year">📅 ${paper.year || 'Unknown'}</span>
            <span class="paper-doi">DOI: ${escapeHtml(shortDoi)}</span>
        </div>
        <button class="btn-generate" data-doi="${escapeAttr(paper.doi || '')}" data-title="${escapeAttr(paper.title)}">
            ✨ Generate Study Card
        </button>
    `;

    card.querySelector('.btn-generate').addEventListener('click', () => {
        generateStudyCard(paper);
    });

    return card;
}

// ---- Study Card Generation ----
async function generateStudyCard(paper) {
    currentPaper = paper;

    // Show study card section, hide results
    resultsSection.style.display = 'none';
    studycardSection.style.display = '';

    // Show loading
    loadingContainer.style.display = '';
    errorContainer.style.display = 'none';
    studycardContent.style.display = 'none';

    // Animate loading steps
    startLoadingAnimation();

    try {
        let url;
        if (paper.doi) {
            const cleanDoi = paper.doi.replace('https://doi.org/', '');
            url = `${API_BASE}/studycard?doi=${encodeURIComponent(cleanDoi)}`;
        } else if (paper.oaUrl) {
            url = `${API_BASE}/studycard?url=${encodeURIComponent(paper.oaUrl)}`;
        } else {
            throw new Error('No DOI or URL available');
        }

        const resp = await fetch(url);
        const data = await resp.json();

        if (data.success) {
            showStudyCard(paper, data);
        } else {
            showError(friendlyReason(data.reason));
        }
    } catch (err) {
        console.error('Study card generation failed:', err);
        showError('Connection failed. Make sure the backend is running.');
    }
}

function startLoadingAnimation() {
    const steps = ['step-find', 'step-extract', 'step-generate'];
    const delays = [0, 2500, 5000];

    // Reset all steps
    steps.forEach(id => {
        const step = document.getElementById(id);
        step.className = 'loading-step';
        step.querySelector('.step-icon').className = 'step-icon';
    });

    // Activate first step
    const firstStep = document.getElementById(steps[0]);
    firstStep.classList.add('active');
    firstStep.querySelector('.step-icon').classList.add('spinner');

    // Progressively activate steps
    steps.forEach((id, i) => {
        if (i === 0) return;
        setTimeout(() => {
            // Mark previous as done
            const prevStep = document.getElementById(steps[i - 1]);
            prevStep.classList.remove('active');
            prevStep.classList.add('done');
            prevStep.querySelector('.step-icon').classList.remove('spinner');

            // Activate current
            const currentStep = document.getElementById(id);
            currentStep.classList.add('active');
            currentStep.querySelector('.step-icon').classList.add('spinner');
        }, delays[i]);
    });
}

function showStudyCard(paper, data) {
    loadingContainer.style.display = 'none';
    errorContainer.style.display = 'none';
    studycardContent.style.display = '';

    // Title
    document.getElementById('sc-paper-title').textContent = paper.title;

    // Source badge
    const badge = document.getElementById('sc-source-badge');
    const source = (data.source || 'llm').toLowerCase();
    badge.textContent = `Source: ${source.toUpperCase()}`;
    badge.className = 'source-badge ' + (source === 'cached' ? 'cached' : source === 'fallback' ? 'fallback' : 'llm');

    // TL;DR
    document.getElementById('sc-tldr').textContent = data.tldr || 'No summary available.';

    // Study Design
    document.getElementById('sc-design').textContent = data.studyDesign || 'Not specified.';

    // Key Findings
    const findingsList = document.getElementById('sc-findings');
    findingsList.innerHTML = '';
    if (data.keyFindings && data.keyFindings.length > 0) {
        data.keyFindings.forEach(finding => {
            const li = document.createElement('li');
            li.textContent = finding;
            findingsList.appendChild(li);
        });
    } else {
        const li = document.createElement('li');
        li.textContent = 'No key findings extracted.';
        findingsList.appendChild(li);
    }

    // Limitations
    document.getElementById('sc-limitations').textContent = data.limitations || 'No limitations listed.';

    // DOI link
    const doiLink = document.getElementById('sc-doi-link');
    if (paper.doi) {
        const doiUrl = paper.doi.startsWith('http') ? paper.doi : `https://doi.org/${paper.doi}`;
        doiLink.href = doiUrl;
        doiLink.style.display = '';
    } else {
        doiLink.style.display = 'none';
    }

    // Scroll to top
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function showError(reason) {
    loadingContainer.style.display = 'none';
    studycardContent.style.display = 'none';
    errorContainer.style.display = '';
    errorReason.textContent = `Reason: ${reason}`;
}

function friendlyReason(reason) {
    const reasons = {
        'no_open_access_pdf': 'No accessible PDF available for this paper.',
        'pdf_download_failed': 'Could not download the PDF file.',
        'llm_generation_failed': 'AI generation failed. Please try again.',
        'openalex_not_found': 'Paper not found in OpenAlex database.',
        'missing_doi_or_url': 'No DOI or URL provided.',
    };
    return reasons[reason] || reason || 'Unknown error.';
}

// ---- Back & Retry ----
btnBack.addEventListener('click', () => {
    studycardSection.style.display = 'none';
    resultsSection.style.display = '';
});

btnRetry.addEventListener('click', () => {
    if (currentPaper) {
        generateStudyCard(currentPaper);
    }
});

// ---- History ----
async function loadHistory() {
    try {
        const resp = await fetch(`${API_BASE}/studycards/recent`);
        const data = await resp.json();

        historyList.innerHTML = '';

        if (!data || data.length === 0) {
            historyEmpty.style.display = '';
            historyList.style.display = 'none';
            return;
        }

        historyEmpty.style.display = 'none';
        historyList.style.display = '';

        data.forEach((card, index) => {
            const el = document.createElement('div');
            el.className = 'history-card';
            el.style.animationDelay = `${index * 0.05}s`;

            const source = (card.generationSource || 'llm').toLowerCase();
            const sourceClass = source === 'cached' ? 'cached' : source === 'fallback' ? 'fallback' : 'llm';
            const sourceColor = source === 'cached'
                ? 'background: rgba(34,197,94,0.15); color: #22c55e;'
                : source === 'fallback'
                    ? 'background: rgba(245,158,11,0.15); color: #f59e0b;'
                    : 'background: rgba(124,92,255,0.15); color: #7c5cff;';

            const date = new Date(card.createdAt).toLocaleDateString('en-US', {
                month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
            });

            el.innerHTML = `
                <div class="history-card-title">${escapeHtml(card.title || card.studyDesign || 'Study Card')}</div>
                <div class="history-card-tldr">${escapeHtml(card.tldr || '')}</div>
                <div class="history-card-meta">
                    <span class="history-card-source" style="${sourceColor}">${source.toUpperCase()}</span>
                    <span class="history-card-date">${date}</span>
                </div>
            `;

            historyList.appendChild(el);
        });
    } catch (err) {
        console.error('Failed to load history:', err);
        historyList.innerHTML = '<p style="color: var(--text-muted); text-align:center; padding:40px;">Could not load history.</p>';
    }
}

// ---- Utilities ----
function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function escapeAttr(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/'/g, '&#39;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

// ---- Init ----
searchInput.focus();
