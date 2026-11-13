// CareerPilot AI — Production Frontend Application
const state = {
    token: localStorage.getItem('cp_jwt') || null,
    email: localStorage.getItem('cp_email') || null,
    isRegisterMode: false,
    resumes: [],
    applications: [],
    selectedAppId: null
};

const authSection = document.getElementById('authSection');
const dashboardSection = document.getElementById('dashboardSection');
const userBadge = document.getElementById('userBadge');
const userEmailDisplay = document.getElementById('userEmailDisplay');

function showToast(msg) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.classList.remove('hidden');
    setTimeout(() => toast.classList.add('hidden'), 3200);
}

async function apiFetch(path, options = {}) {
    const headers = options.headers ? { ...options.headers } : {};
    if (state.token) {
        headers['Authorization'] = `Bearer ${state.token}`;
    }
    if (!(options.body instanceof FormData) && !headers['Content-Type']) {
        headers['Content-Type'] = 'application/json';
    }
    const response = await fetch(path, { ...options, headers });
    if (response.status === 401) {
        logout();
        throw new Error('Please sign in to continue.');
    }
    if (!response.ok) {
        const errBody = await response.json().catch(() => ({}));
        throw new Error(errBody.detail || `Request failed (${response.status})`);
    }
    if (response.status === 204) return null;
    return response.json();
}

function setAuthSession(authResult) {
    state.token = authResult.accessToken;
    state.email = authResult.email;
    localStorage.setItem('cp_jwt', state.token);
    localStorage.setItem('cp_email', state.email);
    renderAppView();
}

function logout() {
    state.token = null;
    state.email = null;
    state.selectedAppId = null;
    localStorage.removeItem('cp_jwt');
    localStorage.removeItem('cp_email');
    authSection.classList.remove('hidden');
    dashboardSection.classList.add('hidden');
    userBadge.classList.add('hidden');
}

// Auth Mode Toggle
document.getElementById('tabLoginBtn').addEventListener('click', () => {
    state.isRegisterMode = false;
    document.getElementById('tabLoginBtn').classList.add('active');
    document.getElementById('tabRegisterBtn').classList.remove('active');
    document.getElementById('fullNameGroup').classList.add('hidden');
    document.getElementById('authSubmitBtn').textContent = 'Sign In';
});

document.getElementById('tabRegisterBtn').addEventListener('click', () => {
    state.isRegisterMode = true;
    document.getElementById('tabRegisterBtn').classList.add('active');
    document.getElementById('tabLoginBtn').classList.remove('active');
    document.getElementById('fullNameGroup').classList.remove('hidden');
    document.getElementById('authSubmitBtn').textContent = 'Create Account';
});

document.getElementById('authForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const errorBox = document.getElementById('authError');
    errorBox.classList.add('hidden');
    const email = document.getElementById('authEmail').value.trim();
    const password = document.getElementById('authPassword').value;
    const fullName = document.getElementById('authFullName').value.trim() || 'Candidate';

    try {
        const endpoint = state.isRegisterMode ? '/api/v1/auth/register' : '/api/v1/auth/login';
        const payload = state.isRegisterMode ? { email, password, fullName } : { email, password };
        const result = await apiFetch(endpoint, {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        setAuthSession(result);
        showToast('Welcome to CareerPilot AI!');
    } catch (err) {
        errorBox.textContent = err.message;
        errorBox.classList.remove('hidden');
    }
});

document.getElementById('logoutBtn').addEventListener('click', logout);

// Navigation Tabs
document.querySelectorAll('.nav-tab').forEach(tab => {
    tab.addEventListener('click', () => {
        document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.tab-view').forEach(v => v.classList.add('hidden'));
        tab.classList.add('active');
        document.getElementById(tab.dataset.target).classList.remove('hidden');
    });
});

async function renderAppView() {
    if (!state.token) {
        authSection.classList.remove('hidden');
        dashboardSection.classList.add('hidden');
        userBadge.classList.add('hidden');
        return;
    }

    authSection.classList.add('hidden');
    dashboardSection.classList.remove('hidden');
    userBadge.classList.remove('hidden');
    userEmailDisplay.textContent = state.email;

    try {
        await Promise.all([loadStats(), loadResumes(), loadApplications()]);
        if (state.selectedAppId) {
            await inspectApplication(state.selectedAppId);
        } else {
            document.getElementById('inspectorEmpty').classList.remove('hidden');
            document.getElementById('inspectorContent').classList.add('hidden');
            document.getElementById('deleteAppBtn').classList.add('hidden');
        }
    } catch (err) {
        console.error(err);
    }
}

async function loadStats() {
    const stats = await apiFetch('/api/v1/applications/stats');
    document.getElementById('kpiTotalApps').textContent = stats.totalApplications;
    document.getElementById('kpiConversion').textContent = `${stats.interviewConversionRatePercent}%`;
    document.getElementById('kpiInterviewing').textContent =
        (stats.byStatus && stats.byStatus.INTERVIEWING) ? stats.byStatus.INTERVIEWING : 0;
}

async function loadResumes() {
    state.resumes = await apiFetch('/api/v1/resumes');
    document.getElementById('kpiResumes').textContent = state.resumes.length;

    const appResumeSelect = document.getElementById('appResumeSelect');
    const ragResumeSelect = document.getElementById('ragResumeSelect');
    const reEvalResumeSelect = document.getElementById('reEvalResumeSelect');

    const optionsHtml = state.resumes.map(r =>
        `<option value="${r.id}">${r.versionName}</option>`
    ).join('');

    appResumeSelect.innerHTML = `<option value="">-- Select Resume --</option>` + optionsHtml;
    ragResumeSelect.innerHTML = `<option value="">-- Select Resume --</option>` + optionsHtml;
    reEvalResumeSelect.innerHTML = `<option value="">-- Choose Resume to Evaluate --</option>` + optionsHtml;

    if (state.resumes.length > 0) {
        appResumeSelect.value = state.resumes[0].id;
        ragResumeSelect.value = state.resumes[0].id;
        reEvalResumeSelect.value = state.resumes[0].id;
    }

    const resumesList = document.getElementById('resumesList');
    if (state.resumes.length === 0) {
        resumesList.innerHTML = `<div class="empty-state">No resumes saved yet. Upload a PDF or paste your resume on the left.</div>`;
        return;
    }
    resumesList.innerHTML = state.resumes.map(r => {
        const preview = r.rawContent
            ? (r.rawContent.length > 180 ? r.rawContent.substring(0, 180) + '...' : r.rawContent)
            : '';
        return `
            <div class="item-card">
                <div class="item-card-header">
                    <strong>${r.versionName}</strong>
                    <button class="btn btn-sm btn-outline" onclick="deleteResume('${r.id}')">Delete</button>
                </div>
                <p class="text-muted" style="font-size: 0.82rem; margin-top: 0.35rem; white-space: pre-line;">${preview}</p>
            </div>
        `;
    }).join('');
}

window.deleteResume = async function(resumeId) {
    await apiFetch(`/api/v1/resumes/${resumeId}`, { method: 'DELETE' });
    await loadResumes();
    showToast('Resume deleted.');
};

async function loadApplications() {
    const statusFilter = document.getElementById('statusFilterSelect').value;
    const query = statusFilter ? `?status=${statusFilter}` : '';
    state.applications = await apiFetch(`/api/v1/applications${query}`);

    const agentAppSelect = document.getElementById('agentAppSelect');
    agentAppSelect.innerHTML = `<option value="">-- All Applications --</option>` +
        state.applications.map(a => `<option value="${a.id}">${a.companyName} — ${a.roleTitle}</option>`).join('');

    if (state.selectedAppId && state.applications.some(a => a.id === state.selectedAppId)) {
        agentAppSelect.value = state.selectedAppId;
    } else if (state.applications.length > 0) {
        state.selectedAppId = state.applications[0].id;
        agentAppSelect.value = state.selectedAppId;
    } else {
        state.selectedAppId = null;
    }

    const listEl = document.getElementById('applicationsList');
    if (state.applications.length === 0) {
        listEl.innerHTML = `<div class="empty-state">No applications tracked yet. Click "+ Add Job Application" above to add your first role.</div>`;
        return;
    }

    listEl.innerHTML = state.applications.map(app => `
        <div class="item-card ${app.id === state.selectedAppId ? 'selected' : ''}" onclick="inspectApplication('${app.id}')">
            <div class="item-card-header">
                <strong>${app.companyName} — ${app.roleTitle}</strong>
                <span class="badge badge-${app.status}">${app.status}</span>
            </div>
            <div class="text-muted">
                ${app.workMode} ${app.salaryMin ? `• ${app.salaryMin.toLocaleString()}–${app.salaryMax.toLocaleString()} ${app.currency || 'USD'}` : ''}
            </div>
        </div>
    `).join('');
}

document.getElementById('statusFilterSelect').addEventListener('change', loadApplications);

// Create Job Application
document.getElementById('createAppForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const resumeId = document.getElementById('appResumeSelect').value || null;
    const payload = {
        resumeId,
        companyName: document.getElementById('appCompany').value.trim(),
        roleTitle: document.getElementById('appRole').value.trim(),
        workMode: document.getElementById('appWorkMode').value,
        salaryMin: parseInt(document.getElementById('appSalaryMin').value, 10) || null,
        salaryMax: parseInt(document.getElementById('appSalaryMax').value, 10) || null,
        currency: 'USD',
        jobDescription: document.getElementById('appJd').value.trim()
    };

    const created = await apiFetch('/api/v1/applications', {
        method: 'POST',
        body: JSON.stringify(payload)
    });
    state.selectedAppId = created.id;
    document.getElementById('appCompany').value = '';
    document.getElementById('appRole').value = '';
    document.getElementById('appSalaryMin').value = '';
    document.getElementById('appSalaryMax').value = '';
    document.getElementById('appJd').value = '';
    await renderAppView();
    showToast('Application saved!');
});

// Inspect Selected Application
window.inspectApplication = async function(appId) {
    state.selectedAppId = appId;
    document.getElementById('agentAppSelect').value = appId;

    const [app, assessments] = await Promise.all([
        apiFetch(`/api/v1/applications/${appId}`),
        apiFetch(`/api/v1/agent/assessments/${appId}`)
    ]);

    document.getElementById('inspectorEmpty').classList.add('hidden');
    document.getElementById('inspectorContent').classList.remove('hidden');
    document.getElementById('deleteAppBtn').classList.remove('hidden');

    document.getElementById('inspCompanyRole').textContent = `${app.companyName} — ${app.roleTitle}`;
    document.getElementById('inspMeta').textContent =
        `${app.workMode} ${app.salaryMin ? `• ${app.salaryMin.toLocaleString()} - ${app.salaryMax.toLocaleString()} ${app.currency || 'USD'}` : ''}`;
    document.getElementById('inspStatusSelect').value = app.status;

    if (assessments && assessments.length > 0) {
        const a = assessments[assessments.length - 1];
        document.getElementById('inspAiScoreBadge').textContent = `${a.matchScore}% Match`;
        document.getElementById('inspAiMatched').innerHTML = `<strong>Matched Skills:</strong> ${a.matchingSkills || 'None detected in resume'}`;
        document.getElementById('inspAiMissing').innerHTML = `<strong>Missing Skills:</strong> ${a.missingSkills || 'None'}`;
        document.getElementById('inspAiAdvice').textContent = a.tailoringSuggestions;
    } else {
        document.getElementById('inspAiScoreBadge').textContent = 'Not Evaluated';
        document.getElementById('inspAiMatched').textContent = 'Select a saved resume below and click "Run Match Analysis" to score your resume against this role.';
        document.getElementById('inspAiMissing').textContent = '';
        document.getElementById('inspAiAdvice').textContent = '';
    }

    const roundsEl = document.getElementById('inspRoundsList');
    if (!app.interviewRounds || app.interviewRounds.length === 0) {
        roundsEl.innerHTML = `<div class="text-muted" style="padding: 0.5rem 0;">No interview rounds scheduled yet.</div>`;
    } else {
        roundsEl.innerHTML = app.interviewRounds.map(r => `
            <div class="item-card" style="margin-bottom: 0.5rem; cursor: default;">
                <div class="item-card-header">
                    <strong>Round #${r.roundNumber}: ${r.roundType}</strong>
                    <select class="select-sm" onchange="updateRoundOutcome('${app.id}', '${r.id}', this.value)">
                        <option value="PENDING" ${r.outcome === 'PENDING' ? 'selected' : ''}>PENDING</option>
                        <option value="PASSED" ${r.outcome === 'PASSED' ? 'selected' : ''}>PASSED</option>
                        <option value="FAILED" ${r.outcome === 'FAILED' ? 'selected' : ''}>FAILED</option>
                    </select>
                </div>
                <p class="text-muted">${r.preparationNotes || ''}</p>
            </div>
        `).join('');
    }

    document.querySelectorAll('#applicationsList .item-card').forEach(card => {
        card.classList.toggle('selected', card.getAttribute('onclick').includes(appId));
    });
};

window.updateRoundOutcome = async function(appId, roundId, outcome) {
    await apiFetch(`/api/v1/applications/${appId}/interviews/${roundId}`, {
        method: 'PATCH',
        body: JSON.stringify({ outcome, questionsAsked: '' })
    });
    showToast(`Round outcome updated to ${outcome}`);
};

// Re-run Match Evaluation on Demand
document.getElementById('reEvalBtn').addEventListener('click', async () => {
    if (!state.selectedAppId) return;
    const resumeId = document.getElementById('reEvalResumeSelect').value;
    if (!resumeId) {
        alert('Please upload and select a resume first.');
        return;
    }
    await apiFetch(`/api/v1/agent/assessments/${state.selectedAppId}/evaluate?resumeId=${resumeId}`, {
        method: 'POST'
    });
    await inspectApplication(state.selectedAppId);
    showToast('Match analysis updated!');
});

// Delete Application
document.getElementById('deleteAppBtn').addEventListener('click', async () => {
    if (!state.selectedAppId) return;
    await apiFetch(`/api/v1/applications/${state.selectedAppId}`, { method: 'DELETE' });
    state.selectedAppId = null;
    await renderAppView();
    showToast('Application deleted.');
});

// Update Status Button
document.getElementById('updateStatusBtn').addEventListener('click', async () => {
    if (!state.selectedAppId) return;
    const status = document.getElementById('inspStatusSelect').value;
    await apiFetch(`/api/v1/applications/${state.selectedAppId}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status })
    });
    await renderAppView();
    showToast(`Status updated to ${status}`);
});

// Schedule Interview Round Form
document.getElementById('scheduleRoundForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!state.selectedAppId) return;
    const roundType = document.getElementById('roundTypeSelect').value;
    const preparationNotes = document.getElementById('roundNotesInput').value.trim();

    await apiFetch(`/api/v1/applications/${state.selectedAppId}/interviews`, {
        method: 'POST',
        body: JSON.stringify({
            roundType,
            scheduledAt: new Date(Date.now() + 86400000).toISOString(),
            preparationNotes
        })
    });
    document.getElementById('roundNotesInput').value = '';
    await renderAppView();
    showToast(`Added ${roundType} round`);
});

// Generate Tailored Interview Prep
document.getElementById('quickAiPrepBtn').addEventListener('click', async () => {
    if (!state.selectedAppId) return;
    const plan = await apiFetch(`/api/v1/agent/interview-prep/${state.selectedAppId}`, {
        method: 'POST',
        body: JSON.stringify({
            roundType: 'TECHNICAL',
            autoScheduleInTracker: false
        })
    });

    document.querySelector('[data-target="viewAgent"]').click();
    const prepOut = document.getElementById('prepPlanOutput');
    prepOut.className = 'ai-box';
    prepOut.innerHTML = `
        <h4>${plan.companyName} — ${plan.roleTitle}</h4>
        <p class="text-muted" style="margin: 0.5rem 0;"><strong>Recommended Technical Questions to Practice:</strong></p>
        <ul style="padding-left: 1.2rem; font-size: 0.88rem; margin-bottom: 0.75rem;">
            ${plan.likelyTechnicalQuestions.map(q => `<li>${q}</li>`).join('')}
        </ul>
        <p class="text-muted" style="margin: 0.5rem 0;"><strong>System Design &amp; Architecture Topics:</strong></p>
        <ul style="padding-left: 1.2rem; font-size: 0.88rem; margin-bottom: 0.75rem;">
            ${plan.systemDesignTopics.map(t => `<li>${t}</li>`).join('')}
        </ul>
        <div class="ai-advice">${plan.coachingAdvice}</div>
    `;
    showToast('Interview preparation guide ready!');
});

// Upload Text Resume
document.getElementById('uploadTextResumeForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const versionName = document.getElementById('resVersionName').value.trim();
    const rawContent = document.getElementById('resRawContent').value.trim();

    await apiFetch('/api/v1/resumes', {
        method: 'POST',
        body: JSON.stringify({ versionName, rawContent })
    });
    document.getElementById('resVersionName').value = '';
    document.getElementById('resRawContent').value = '';
    await loadResumes();
    showToast('Resume saved!');
});

// Upload PDF Resume
document.getElementById('uploadPdfResumeForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const versionName = document.getElementById('pdfVersionName').value.trim();
    const fileInput = document.getElementById('pdfFileInput');
    if (!fileInput.files.length) return;

    const formData = new FormData();
    formData.append('versionName', versionName);
    formData.append('file', fileInput.files[0]);

    await apiFetch('/api/v1/resumes/upload-pdf', {
        method: 'POST',
        body: formData
    });
    document.getElementById('pdfVersionName').value = '';
    fileInput.value = '';
    await loadResumes();
    showToast('PDF Resume uploaded and indexed!');
});

// Semantic Resume Search
document.getElementById('ragSearchForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const resumeId = document.getElementById('ragResumeSelect').value;
    const query = document.getElementById('ragQueryInput').value.trim();
    if (!resumeId || !query) return;

    const results = await apiFetch(`/api/v1/rag/search?resumeId=${resumeId}&query=${encodeURIComponent(query)}&topK=5`);
    const container = document.getElementById('ragResultsList');

    if (!results || results.length === 0) {
        container.innerHTML = `<div class="empty-state">No matching sections found.</div>`;
        return;
    }

    container.innerHTML = results.map((chunk, idx) => `
        <div class="item-card" style="cursor: default;">
            <div class="item-card-header">
                <strong>Match #${idx + 1}</strong>
                <span class="score-pill">${(chunk.similarityScore * 100).toFixed(1)}% Relevance</span>
            </div>
            <p style="font-size: 0.9rem; margin-top: 0.35rem;">${chunk.content}</p>
        </div>
    `).join('');
});

// AI Coach Chat
document.getElementById('agentChatForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const inputEl = document.getElementById('agentChatInput');
    const message = inputEl.value.trim();
    const applicationId = document.getElementById('agentAppSelect').value || null;
    if (!message) return;

    const chatLog = document.getElementById('chatLog');
    chatLog.innerHTML += `<div class="chat-bubble user">${message}</div>`;
    inputEl.value = '';

    const response = await apiFetch('/api/v1/agent/chat', {
        method: 'POST',
        body: JSON.stringify({ message, applicationId })
    });

    const toolsHtml = (response.executedTools || []).map(t =>
        `<div><span class="tool-pill">Action Completed: ${t.summary}</span></div>`
    ).join('');

    chatLog.innerHTML += `
        <div class="chat-bubble assistant">
            <strong>CareerPilot Coach:</strong><br/>
            ${response.reply}
            ${toolsHtml}
        </div>
    `;
    chatLog.scrollTop = chatLog.scrollHeight;

    if (response.executedTools && response.executedTools.length > 0) {
        await renderAppView();
        showToast('Interview round scheduled in your tracker!');
    }
});

renderAppView();
