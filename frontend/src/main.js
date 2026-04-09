const $ = (sel) => document.querySelector(sel);

async function runSearch(ev) {
  ev.preventDefault();
  $("p#search-error").textContent = "";
  const fd = new FormData(ev.target);
  const params = new URLSearchParams();
  for (const [k, v] of fd.entries()) {
    if (v !== "" && v != null) params.set(k, v);
  }
  const url = "/api/search?" + params.toString();
  try {
    const r = await fetch(url);
    const text = await r.text();
    if (!r.ok) {
      let detail = text;
      try {
        const j = JSON.parse(text);
        if (j.message) detail = j.message;
        else if (j.error) detail = typeof j.error === "string" ? j.error : JSON.stringify(j);
      } catch {
        /* use raw text */
      }
      throw new Error(`${r.status} ${r.statusText}: ${detail}`);
    }
    const data = JSON.parse(text);
    $("p#search-summary").textContent =
      `query: ${data.querySummary ?? "—"} · total: ${data.totalResults} · page ${data.page} / ${data.totalPages}`;
    const tbody = $("#search-results");
    tbody.innerHTML = "";
    for (const b of data.results ?? []) {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${escapeHtml(String(b.bookId ?? ""))}</td>
        <td>${escapeHtml(b.title ?? "")}</td>
        <td>${escapeHtml(b.author ?? "")}</td>
        <td>${escapeHtml(String(b.publishYear ?? ""))}</td>
        <td>${escapeHtml(String(b.availableCopies ?? ""))}</td>
        <td>${escapeHtml(String(b.status ?? ""))}</td>`;
      tbody.appendChild(tr);
    }
  } catch (e) {
    $("p#search-error").textContent = String(e.message);
  }
}

async function runBorrow(ev) {
  ev.preventDefault();
  $("#borrow-error").textContent = "";
  const fd = new FormData(ev.target);
  const body = {
    userId: Number(fd.get("userId")),
    bookId: Number(fd.get("bookId")),
  };
  try {
    const res = await fetch("/api/borrows", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    const text = await res.text();
    $("#borrow-out").textContent = text;
    if (!res.ok) throw new Error(res.status + " " + text);
  } catch (e) {
    $("#borrow-error").textContent = String(e.message);
  }
}

function escapeHtml(s) {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

$("#search-form").addEventListener("submit", runSearch);
$("#borrow-form").addEventListener("submit", runBorrow);
