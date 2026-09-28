// Run this after MySQL is configured and the Spring Boot app is running at http://localhost:8080.
// Command: node docs/smoke-test.mjs

const base = process.env.BASE_URL || "http://localhost:8080";

function assert(condition, message) {
  if (!condition) throw new Error(`FAIL: ${message}`);
}

function cookieFrom(response) {
  const raw = response.headers.get("set-cookie");
  if (!raw) return "";
  return raw.split(";")[0];
}

async function request(path, options = {}, cookie = "") {
  const headers = { ...(options.body ? { "Content-Type": "application/json" } : {}), ...(options.headers || {}) };
  if (cookie) headers.Cookie = cookie;
  const response = await fetch(base + path, { ...options, headers });
  const text = await response.text();
  let data = {};
  try { data = text ? JSON.parse(text) : {}; } catch { data = { message: text }; }
  return { response, data, cookie: cookieFrom(response) || cookie };
}

try {
  // Admin login
  let r = await request("/api/auth/login", { method: "POST", body: JSON.stringify({ username: "admin", password: "admin123" }) });
  assert(r.response.status === 200, "admin login");
  const adminCookie = r.cookie;

  // Staff login
  r = await request("/api/auth/login", { method: "POST", body: JSON.stringify({ username: "staff1", password: "staff123" }) });
  assert(r.response.status === 200, "staff login");
  const staffCookie = r.cookie;

  // Student registration creates a real DB-backed user.
  const username = `student_${Date.now()}`;
  r = await request("/api/auth/register", {
    method: "POST",
    body: JSON.stringify({ username, password: "student123", fullName: "Smoke Test Student", email: `${username}@test.local` })
  });
  assert(r.response.status === 201, "student registration");
  const studentCookie = r.cookie;

  // Category list is database-backed and seeded by the application.
  r = await request("/api/categories");
  assert(r.response.status === 200 && r.data.length > 0, "category list");
  const charger = r.data.find(x => x.name === "Charger") || r.data[0];

  // Student creates an actual lost report.
  r = await request("/api/lost-reports", {
    method: "POST",
    body: JSON.stringify({ categoryId: charger.id, itemName: "White USB Charger", description: "USB-C phone charger", location: "Library", dateLost: new Date().toISOString().slice(0, 10) })
  }, studentCookie);
  assert(r.response.status === 201, "lost report creation");
  const lostId = r.data.id;

  // Student must not create a found item.
  r = await request("/api/found-items", {
    method: "POST",
    body: JSON.stringify({ categoryId: charger.id, itemName: "White USB Charger", description: "USB-C phone charger", location: "Library", dateFound: new Date().toISOString().slice(0, 10) })
  }, studentCookie);
  assert(r.response.status === 403, "student blocked from found-item creation");

  // Staff creates a matching found item.
  r = await request("/api/found-items", {
    method: "POST",
    body: JSON.stringify({ categoryId: charger.id, itemName: "White USB Charger", description: "USB-C charger", location: "Library", dateFound: new Date().toISOString().slice(0, 10) })
  }, staffCookie);
  assert(r.response.status === 201, "staff found-item creation");
  const foundId = r.data.id;

  // Matching must come from current DB records.
  r = await request(`/api/matches?lostId=${lostId}`);
  assert(r.response.status === 200 && r.data.some(x => x.foundItemId === foundId), "database-backed match generation");

  // Confirming the match should CLAIM the found item and MATCH the lost report.
  r = await request("/api/matches/confirm", {
    method: "POST",
    body: JSON.stringify({ lostReportId: lostId, foundItemId: foundId })
  }, staffCookie);
  assert(r.response.status === 200, "confirm match");

  r = await request(`/api/found-items/${foundId}`);
  assert(r.data.status === "CLAIMED", "match changes found status to CLAIMED");
  r = await request(`/api/lost-reports/${lostId}`);
  assert(r.data.status === "MATCHED", "match changes lost status to MATCHED");

  // Return the item; the linked lost report should become RETURNED.
  r = await request(`/api/found-items/${foundId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status: "RETURNED" })
  }, staffCookie);
  assert(r.response.status === 200, "return status transition");
  r = await request(`/api/lost-reports/${lostId}`);
  assert(r.data.status === "RETURNED", "linked lost report becomes RETURNED");

  // Invalid transition must be rejected without silently changing the DB.
  r = await request("/api/found-items", {
    method: "POST",
    body: JSON.stringify({ categoryId: charger.id, itemName: "Second Charger", description: "Demo invalid transition", location: "Lab", dateFound: new Date().toISOString().slice(0, 10) })
  }, staffCookie);
  assert(r.response.status === 201, "second found-item creation");
  const invalidId = r.data.id;
  r = await request(`/api/found-items/${invalidId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status: "RETURNED" })
  }, staffCookie);
  assert(r.response.status === 400, "AVAILABLE to RETURNED rejected");
  r = await request(`/api/found-items/${invalidId}`);
  assert(r.data.status === "AVAILABLE", "invalid transition does not change DB status");

  // Admin-only dashboard must expose DB counts.
  r = await request("/api/admin/dashboard", {}, adminCookie);
  assert(r.response.status === 200 && typeof r.data.totalUsers === "number", "admin dashboard");

  console.log("PASS: CampusFind smoke test completed successfully.");
} catch (error) {
  console.error(error.message);
  process.exitCode = 1;
}
