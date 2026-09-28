const $ = (id) => document.getElementById(id);

const state = {
  user: null,
  categories: []
};

const api = async (url, options = {}) => {
  const config = { ...options, headers: { ...(options.body ? { "Content-Type": "application/json" } : {}), ...(options.headers || {}) } };
  const response = await fetch(url, config);
  const text = await response.text();
  let data = {};
  try { data = text ? JSON.parse(text) : {}; } catch { data = { message: text || "Request failed" }; }
  if (!response.ok) {
    const validation = data.validationErrors ? Object.values(data.validationErrors).join(" ") : "";
    throw new Error(validation || data.message || `Request failed (${response.status})`);
  }
  return data;
};

const escapeHtml = (value = "") => String(value)
  .replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
  .replaceAll('"', "&quot;").replaceAll("'", "&#039;");

const today = () => new Date().toISOString().slice(0, 10);

function toast(message, type = "success") {
  const el = $("toast");
  el.textContent = message;
  el.className = `toast show ${type}`;
  window.clearTimeout(toast.timer);
  toast.timer = window.setTimeout(() => { el.className = "toast"; }, 3200);
}

function setLoading(targetId, message = "Loading...") {
  $(targetId).innerHTML = `<div class="loading">${escapeHtml(message)}</div>`;
}

function fillCategorySelect(id, includeAll = false) {
  const el = $(id);
  if (!el) return;
  const selected = el.value;
  const first = includeAll ? '<option value="">All categories</option>' : '<option value="">Select category</option>';
  el.innerHTML = first + state.categories.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join("");
  if ([...el.options].some(o => o.value === selected)) el.value = selected;
}

function showPage(page, updateUrl = true) {
  if ((page === "admin" || page === "categories") && !canSeeAdmin()) page = "dashboard";
  if (page === "report-found" && !canSeeStaff()) page = "dashboard";
  document.querySelectorAll(".page").forEach(p => p.classList.remove("active"));
  const target = $(
    page === "report-lost" ? "page-report-lost" :
    page === "report-found" ? "page-report-found" : `page-${page}`
  );
  if (!target) return;
  target.classList.add("active");
  document.querySelectorAll(".nav-btn").forEach(btn => btn.classList.toggle("active", btn.dataset.page === page));
  const titleMap = { dashboard: "Dashboard", lost: "Lost Reports", found: "Found Items", matches: "Possible Matches", "report-lost": "Report Lost Item", "report-found": "Report Found Item", admin: "Admin Dashboard", categories: "Categories" };
  $("pageTitle").textContent = titleMap[page] || "Dashboard";
  if (updateUrl) history.replaceState({}, "", `#${page}`);

  const loaders = {
    dashboard: loadDashboard,
    lost: loadLost,
    found: loadFound,
    matches: loadMatches,
    admin: loadAdmin,
    categories: loadCategories
  };
  if (loaders[page]) loaders[page]().catch(handleError);
}

function canSeeStaff() {
  return state.user && ["STAFF", "ADMIN"].includes(state.user.role);
}
function canSeeAdmin() {
  return state.user?.role === "ADMIN";
}

function applyRoleVisibility() {
  document.querySelectorAll(".staff-only").forEach(el => el.classList.toggle("hidden", !canSeeStaff()));
  document.querySelectorAll(".admin-only").forEach(el => el.classList.toggle("hidden", !canSeeAdmin()));
  $("adminMini").classList.toggle("hidden", !canSeeAdmin());
}

function handleError(error) {
  console.error(error);
  toast(error.message || "Something went wrong", "error");
}

async function loadSession() {
  try {
    state.user = await api("/api/auth/me");
    enterApp();
  } catch {
    enterAuth();
  }
}

function enterAuth() {
  $("authView").classList.remove("hidden");
  $("appView").classList.add("hidden");
}

async function enterApp() {
  $("authView").classList.add("hidden");
  $("appView").classList.remove("hidden");
  $("userName").textContent = state.user.fullName;
  $("userRole").textContent = state.user.role;
  $("userAvatar").textContent = state.user.fullName?.trim()?.charAt(0)?.toUpperCase() || "U";
  $("welcomeName").textContent = state.user.fullName;
  $("welcomeRole").textContent = state.user.role;
  applyRoleVisibility();
  try {
    state.categories = await api("/api/categories");
    ["lostCategory", "foundCategory", "lostCategoryFilter", "foundCategoryFilter"].forEach(id => fillCategorySelect(id, id.endsWith("Filter")));
    $("lostDate").value = today();
    $("foundDate").value = today();
    showPage(location.hash.slice(1) || "dashboard", false);
  } catch (error) {
    handleError(error);
  }
}

async function loadDashboard() {
  setLoading("dashboardLost");
  setLoading("dashboardFound");
  const [lost, found, matches] = await Promise.all([
    api("/api/lost-reports?keyword=").catch(() => []),
    api("/api/found-items?status=AVAILABLE").catch(() => []),
    api("/api/matches").catch(() => [])
  ]);
  const myLost = state.user?.role === "ADMIN" ? lost : lost.filter(x => x.reportingUserId === state.user.userId || x.reportingUserId === state.user.id);
  const openLost = lost.filter(x => x.status === "OPEN").length;
  $("myLostCount").textContent = myLost.length;
  $("openLostCount").textContent = openLost;
  $("availableFoundCount").textContent = found.length;
  $("matchCount").textContent = matches.length;
  $("dashboardLost").innerHTML = lost.slice(0, 5).map(item => `<div class="item-card"><strong>${escapeHtml(item.itemName)} <span class="status ${item.status}">${item.status}</span></strong><small>${escapeHtml(item.categoryName)} · ${escapeHtml(item.location)} · ${escapeHtml(item.dateLost)}</small></div>`).join("") || '<div class="empty">No lost reports yet.</div>';
  $("dashboardFound").innerHTML = found.slice(0, 5).map(item => `<div class="item-card"><strong>${escapeHtml(item.itemName)} <span class="status ${item.status}">${item.status}</span></strong><small>${escapeHtml(item.categoryName)} · ${escapeHtml(item.location)} · ${escapeHtml(item.dateFound)}</small></div>`).join("") || '<div class="empty">No available found items.</div>';
  if (canSeeAdmin()) await loadAdminMini();
}

async function loadLost() {
  setLoading("lostTableWrap");
  const params = new URLSearchParams();
  const category = $("lostCategoryFilter").value;
  if (category) params.set("categoryId", category);
  if ($("lostLocationFilter").value.trim()) params.set("location", $("lostLocationFilter").value.trim());
  if ($("lostKeywordFilter").value.trim()) params.set("keyword", $("lostKeywordFilter").value.trim());
  if ($("lostStatusFilter").value) params.set("status", $("lostStatusFilter").value);
  const rows = await api(`/api/lost-reports?${params}`);
  const mineOnly = state.user?.role === "STUDENT" ? rows.filter(x => x.reportingUserId === state.user.userId || x.reportingUserId === state.user.id) : rows;
  $("lostTableWrap").innerHTML = mineOnly.length ? `<table class="data-table"><thead><tr><th>ID</th><th>Item</th><th>Category</th><th>Location</th><th>Date</th><th>Status</th><th>Case</th><th>Reporter</th></tr></thead><tbody>${mineOnly.map(x => `<tr><td>#${x.id}</td><td><strong>${escapeHtml(x.itemName)}</strong><br><small>${escapeHtml(x.description)}</small></td><td>${escapeHtml(x.categoryName)}</td><td>${escapeHtml(x.location)}</td><td>${escapeHtml(x.dateLost)}</td><td><span class="status ${x.status}">${x.status}</span></td><td>${escapeHtml(x.caseState || "PENDING")}</td><td>${escapeHtml(x.reportingUserName)}</td></tr>`).join("")}</tbody></table>` : '<div class="empty">No lost reports match the filters.</div>';
}

async function loadFound() {
  setLoading("foundTableWrap");
  const params = new URLSearchParams();
  const category = $("foundCategoryFilter").value;
  if (category) params.set("categoryId", category);
  if ($("foundLocationFilter").value.trim()) params.set("location", $("foundLocationFilter").value.trim());
  if ($("foundKeywordFilter").value.trim()) params.set("keyword", $("foundKeywordFilter").value.trim());
  if ($("foundStatusFilter").value) params.set("status", $("foundStatusFilter").value);
  const rows = await api(`/api/found-items?${params}`);
  const contactHeader = canSeeStaff() ? "<th>Finder phone</th>" : "";
  $("foundTableWrap").innerHTML = rows.length ? `<table class="data-table"><thead><tr><th>ID</th><th>Item</th><th>Category</th><th>Location</th><th>Date</th>${contactHeader}<th>Status</th><th>Resolution</th><th>Action</th></tr></thead><tbody>${rows.map(x => {
    const canEdit = canSeeAdmin() || x.reportingStaffId === state.user.userId || x.reportingStaffId === state.user.id;
    let actions = "";
    if (canEdit && x.status === "MATCHED") actions += `<button class="tiny" data-action="return" data-id="${x.id}">Return</button>`;
    const contactCell = canSeeStaff() ? `<td>${escapeHtml(x.finderPhoneNumber || "—")}</td>` : "";
    return `<tr><td>#${x.id}</td><td><strong>${escapeHtml(x.itemName)}</strong><br><small>${escapeHtml(x.description)}</small></td><td>${escapeHtml(x.categoryName)}</td><td>${escapeHtml(x.location)}</td><td>${escapeHtml(x.dateFound)}</td>${contactCell}<td><span class="status ${x.status}">${x.status}</span></td><td>${escapeHtml(x.caseState || "PENDING")}</td><td><div class="action-row">${actions || "—"}</div></td></tr>`;
  }).join("")}</tbody></table>` : '<div class="empty">No found items match the filters.</div>';
}

async function loadMatches() {
  setLoading("matchTableWrap");
  const [suggested, confirmed] = await Promise.all([api("/api/matches"), api("/api/matches/confirmed")]);
  const matches = [...suggested, ...confirmed];
  $("matchTableWrap").innerHTML = matches.length ? `<table class="data-table"><thead><tr><th>Lost item</th><th>Found item</th><th>Category</th><th>Lost date</th><th>Found date</th><th>Locations</th><th>Contact</th><th>Match</th><th>Action</th></tr></thead><tbody>${matches.map(x => {
    let action = canSeeStaff() ? `<button class="tiny" data-action="confirm-match" data-lost="${x.lostReportId}" data-found="${x.foundItemId}">Confirm Match</button>` : "Staff/Admin only";
    if (x.foundStatus !== "AVAILABLE") {
      action = x.caseState === "CLOSED"
        ? "Closed"
        : x.foundStatus === "RETURNED"
          ? `<button class="tiny" data-action="resolve-match" data-state="CLOSED" data-lost="${x.lostReportId}">Close Case</button>`
          : `<div class="action-row"><button class="tiny" data-action="resolve-match" data-state="MATCHED" data-lost="${x.lostReportId}">Matched</button><button class="tiny" data-action="resolve-match" data-state="PENDING" data-lost="${x.lostReportId}">Still Pending</button><button class="tiny" data-action="resolve-match" data-state="RETURNED" data-lost="${x.lostReportId}">Mark Returned</button></div>`;
    }
    return `<tr><td>#${x.lostReportId} ${escapeHtml(x.lostItemName)}<br><small>${escapeHtml(x.lostStatus)} / ${escapeHtml(x.caseState)}</small></td><td>#${x.foundItemId} ${escapeHtml(x.foundItemName)}<br><small>${escapeHtml(x.foundStatus)}</small></td><td>${escapeHtml(x.category)}</td><td>${escapeHtml(x.lostDate)}</td><td>${escapeHtml(x.foundDate)}</td><td>${escapeHtml(x.lostLocation)} → ${escapeHtml(x.foundLocation)}</td><td>${escapeHtml(x.foundPhoneNumber || "—")}</td><td><strong>${x.matchScore}%</strong><br><small>${escapeHtml(x.matchReason)}</small></td><td>${action}</td></tr>`;
  }).join("")}</tbody></table>` : '<div class="empty">No possible matches yet. Create an OPEN lost report and an AVAILABLE found item with the same category.</div>';
}

async function loadAdmin() {
  if (!canSeeAdmin()) return;
  const d = await api("/api/admin/dashboard");
  renderAdminGrid("adminGrid", d);
}
async function loadAdminMini() {
  if (!canSeeAdmin()) return;
  const d = await api("/api/admin/dashboard");
  renderAdminGrid("adminMiniGrid", d);
}
function renderAdminGrid(id, d) {
  const cards = [
    ["Total Users", d.totalUsers], ["Lost Reports", d.totalLostReports], ["Found Items", d.totalFoundItems],
    ["Open Lost", d.openLostReports], ["Available Found", d.availableFoundItems], ["Matched Lost", d.matchedLostReports],
    ["Claimed Found", d.claimedFoundItems], ["Returned Found", d.returnedFoundItems], ["Returned Lost", d.returnedLostReports]
  ];
  $(id).innerHTML = cards.map(([label, value]) => `<div class="stat-card"><span>${label}</span><strong>${value}</strong></div>`).join("");
}

async function loadCategories() {
  state.categories = await api("/api/categories");
  $("categoryList").innerHTML = state.categories.map(c => `<span class="chip">#${c.id} · ${escapeHtml(c.name)}</span>`).join("") || '<div class="empty">No categories.</div>';
  ["lostCategory", "foundCategory", "lostCategoryFilter", "foundCategoryFilter"].forEach(id => fillCategorySelect(id, id.endsWith("Filter")));
}

async function submitJson(formId, url, body, successMessage, after) {
  try {
    await api(url, { method: "POST", body: JSON.stringify(body) });
    $(formId).reset();
    toast(successMessage);
    if (after) await after();
  } catch (error) {
    handleError(error);
  }
}

function bindEvents() {
  document.querySelectorAll("[data-auth]").forEach(btn => btn.addEventListener("click", () => {
    document.querySelectorAll("[data-auth]").forEach(x => x.classList.remove("active"));
    btn.classList.add("active");
    $("loginForm").classList.toggle("hidden", btn.dataset.auth !== "login");
    $("registerForm").classList.toggle("hidden", btn.dataset.auth !== "register");
  }));

  document.querySelectorAll(".nav-btn").forEach(btn => btn.addEventListener("click", () => showPage(btn.dataset.page)));
  document.querySelectorAll("[data-page-jump]").forEach(btn => btn.addEventListener("click", () => showPage(btn.dataset.pageJump)));
  $("reportLostTop").addEventListener("click", () => showPage("report-lost"));
  $("lostRefresh").addEventListener("click", () => loadLost().catch(handleError));
  $("foundRefresh").addEventListener("click", () => loadFound().catch(handleError));
  $("matchRefresh").addEventListener("click", () => loadMatches().catch(handleError));
  $("adminRefresh").addEventListener("click", () => loadAdmin().catch(handleError));
  $("lostFilterBtn").addEventListener("click", () => loadLost().catch(handleError));
  $("foundFilterBtn").addEventListener("click", () => loadFound().catch(handleError));

  $("loginForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      state.user = await api("/api/auth/login", { method: "POST", body: JSON.stringify({ username: $("loginUsername").value.trim(), password: $("loginPassword").value }) });
      toast("Login successful");
      await enterApp();
    } catch (error) { handleError(error); }
  });

  $("registerForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      state.user = await api("/api/auth/register", { method: "POST", body: JSON.stringify({ fullName: $("regFullName").value.trim(), username: $("regUsername").value.trim(), email: $("regEmail").value.trim(), password: $("regPassword").value }) });
      toast("Account created");
      await enterApp();
    } catch (error) { handleError(error); }
  });

  $("logoutBtn").addEventListener("click", async () => {
    try { await api("/api/auth/logout", { method: "POST" }); } catch (error) { console.warn(error); }
    state.user = null;
    enterAuth();
    toast("Logged out");
  });

  $("lostForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    await submitJson("lostForm", "/api/lost-reports", {
      categoryId: Number($("lostCategory").value),
      itemName: $("lostItemName").value.trim(),
      description: $("lostDescription").value.trim(),
      location: $("lostLocation").value.trim(),
      dateLost: $("lostDate").value
    }, "Lost report created", async () => { await loadDashboard(); showPage("lost"); });
  });

  $("foundForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    await submitJson("foundForm", "/api/found-items", {
      categoryId: Number($("foundCategory").value),
      itemName: $("foundItemName").value.trim(),
      description: $("foundDescription").value.trim(),
      location: $("foundLocation").value.trim(),
      dateFound: $("foundDate").value,
      finderPhoneNumber: $("foundPhone").value.trim()
    }, "Found item saved", async () => { await loadDashboard(); showPage("found"); });
  });

  $("categoryForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    await submitJson("categoryForm", "/api/categories", { name: $("newCategoryName").value.trim() }, "Category added", loadCategories);
  });

  document.addEventListener("click", async (event) => {
    const action = event.target.dataset.action;
    if (!action) return;
    try {
      if (action === "match" || action === "return") {
        await api(`/api/found-items/${event.target.dataset.id}/status`, { method: "PATCH", body: JSON.stringify({ status: action === "match" ? "MATCHED" : "RETURNED" }) });
        toast(action === "match" ? "Item matched" : "Item returned");
        await Promise.all([loadFound(), loadDashboard(), loadAdmin()]);
      }
      if (action === "confirm-match") {
        await api("/api/matches/confirm", { method: "POST", body: JSON.stringify({ lostReportId: Number(event.target.dataset.lost), foundItemId: Number(event.target.dataset.found) }) });
        toast("Match confirmed");
        await Promise.all([loadMatches(), loadLost(), loadFound(), loadDashboard(), loadAdmin()]);
      }
      if (action === "resolve-match") {
        await api(`/api/matches/${event.target.dataset.lost}/resolution`, { method: "PATCH", body: JSON.stringify({ resolution: event.target.dataset.state }) });
        toast(event.target.dataset.state === "CLOSED" ? "Case closed" : `Case marked ${event.target.dataset.state.toLowerCase()}`);
        await Promise.all([loadMatches(), loadLost(), loadFound(), loadDashboard(), loadAdmin()]);
      }
    } catch (error) { handleError(error); }
  });
}

bindEvents();
loadSession();
