<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
    <title>CertiChain &middot; Verify a certificate</title>
</head>
<body>
<div class="hero">
    <%@ include file="/WEB-INF/jsp/fragments/public-nav.jspf" %>
    <div class="hero-inner">
        <div>
            <span class="badge badge-type">Tamper-evident credential registry</span>
            <h1 style="margin-top:.8rem">Fake certificates <span>cannot hide</span> here.</h1>
            <p class="lead">Every certificate issued by <c:out value="${institution.name}"/> is sealed into a SHA-256
                hash-chained ledger. Employers and universities can check any certificate in seconds, and even an
                insider editing the database is caught.</p>
            <div class="counter-row">
                <div><b><fmt:formatNumber value="${stats.total}"/></b><span>certificates issued</span></div>
                <div><b>#<c:out value="${head.seq}"/></b><span>latest ledger block</span></div>
                <div><b><fmt:formatNumber value="${stats.verificationsWeek}"/></b><span>verifications this week</span></div>
            </div>
        </div>
        <div class="verify-box">
            <h2>Verify a certificate</h2>
            <p class="muted small" style="margin:0">Enter the certificate number printed next to the QR code, or simply scan the QR code with your phone.</p>
            <form action="${ctx}/verify" method="get" class="verify-input">
                <input type="text" name="certNo" placeholder="${institution.shortCode}-2026-XXXXXXXX" required maxlength="40" data-upper autocomplete="off" aria-label="Certificate number">
                <button class="btn btn-primary" type="submit">Verify</button>
            </form>
            <p class="hint">Machine-to-machine: <code>GET ${ctx}/api/v1/verify/{certNo}</code> returns JSON.</p>
            <div class="small" style="margin-top:1rem;padding-top:1rem;border-top:1px solid var(--line)">
                <b>Ledger head</b>
                <div class="hash"><c:out value="${head.hash}"/></div>
            </div>
        </div>
    </div>
</div>

<section class="section">
    <div class="features">
        <div class="card feature">
            <div class="ic"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/><path d="M10 6.5h4a3 3 0 0 1 3 3V14"/></svg></div>
            <h3>Hash-chained ledger</h3>
            <p>Each block stores the SHA-256 of the previous block. Change, delete or reorder one record and every later link breaks.</p>
        </div>
        <div class="card feature">
            <div class="ic"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/><path d="M9 12l2 2 4-4"/></svg></div>
            <h3>Insider-proof</h3>
            <p>A background watchdog re-audits the whole chain on several threads and raises an alert the moment data drifts from its seal.</p>
        </div>
        <div class="card feature">
            <div class="ic"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><path d="M14 14h3v3h-3zM20 14v.01M14 20h.01M20 20h.01M17 17h3v3"/></svg></div>
            <h3>QR on every certificate</h3>
            <p>Printed certificates carry a QR code that opens this portal. Revoked certificates show as revoked instantly, worldwide.</p>
        </div>
    </div>
</section>
<footer class="footer">CertiChain &middot; Advanced Java project &middot; Spring Boot, Servlets, JSP, JDBC, Swing</footer>
</body>
</html>
