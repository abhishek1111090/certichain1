<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<c:set var="o" value="${result.outcome}"/>
<c:set var="c" value="${result.certificate}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
    <title><c:out value="${o.label}"/> &middot; <c:out value="${result.certNo}"/> &middot; CertiChain</title>
</head>
<body>
<%@ include file="/WEB-INF/jsp/fragments/public-nav.jspf" %>
<div class="section" style="padding-top:1rem;max-width:980px">
    <div class="verdict verdict-${o}">
        <div class="icon">
            <c:choose>
                <c:when test="${o == 'AUTHENTIC'}"><svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg></c:when>
                <c:when test="${o == 'NOT_FOUND'}"><svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/></svg></c:when>
                <c:otherwise><svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round"><path d="M12 7v6M12 17h.01"/><circle cx="12" cy="12" r="9"/></svg></c:otherwise>
            </c:choose>
        </div>
        <div>
            <div class="small" style="opacity:.85;font-family:var(--mono)"><c:out value="${result.certNo}"/></div>
            <h1><c:out value="${o.label}"/></h1>
            <p><c:out value="${o.explanation}"/></p>
        </div>
    </div>

    <div class="grid grid-3">
        <div>
            <c:if test="${c != null}">
                <div class="card">
                    <div class="card-head"><h2>Certificate details</h2>
                        <span class="badge badge-type"><c:out value="${c.credentialType.displayName}"/></span></div>
                    <dl class="kv">
                        <dt>Awarded to</dt><dd><c:out value="${c.studentName}"/></dd>
                        <dt>Roll number</dt><dd><c:out value="${c.rollNo}"/></dd>
                        <dt>Program</dt><dd><c:out value="${c.program}"/></dd>
                        <dt>Title</dt><dd><c:out value="${c.title}"/></dd>
                        <c:if test="${not empty c.grade}"><dt>Grade / result</dt><dd><c:out value="${c.grade}"/></dd></c:if>
                        <dt>Issued on</dt><dd><c:out value="${c.issueDateDisplay}"/></dd>
                        <dt>Issued by</dt><dd><c:out value="${institution.name}"/></dd>
                        <c:if test="${o == 'REVOKED'}"><dt>Revocation reason</dt><dd style="color:var(--warn)"><c:out value="${c.revokeReason}"/></dd></c:if>
                    </dl>
                    <c:if test="${o == 'TAMPERED'}">
                        <div class="alert alert-bad" style="margin:1rem 0 0">The details above are what the database currently says. They do
                            <b>not</b> match what the institution sealed, so they must not be relied on.</div>
                    </c:if>
                    <c:if test="${o == 'AUTHENTIC' || o == 'REVOKED'}">
                        <div style="margin-top:1.1rem;display:flex;gap:.5rem;flex-wrap:wrap">
                            <a class="btn btn-primary btn-sm" href="${ctx}/certificate/${fn:escapeXml(result.certNo)}">View printable certificate</a>
                            <button class="btn btn-ghost btn-sm" type="button" data-copy="${ctx}/verify/${fn:escapeXml(result.certNo)}">Copy verification link</button>
                        </div>
                    </c:if>
                </div>
            </c:if>
            <c:if test="${result.issueBlock != null}">
                <div class="card">
                    <div class="card-head"><h2>Ledger proof</h2><span class="muted small">block #<c:out value="${result.issueBlock.seq}"/></span></div>
                    <dl class="kv small">
                        <dt>Sealed at</dt><dd><fmt:formatDate value="${result.issueBlock.createdDate}" pattern="dd MMM yyyy, HH:mm:ss"/></dd>
                        <dt>Data hash (seal)</dt><dd class="hash"><c:out value="${result.issueBlock.dataHash}"/></dd>
                        <dt>Previous block</dt><dd class="hash"><c:out value="${result.issueBlock.prevHash}"/></dd>
                        <dt>Block hash</dt><dd class="hash"><c:out value="${result.issueBlock.blockHash}"/></dd>
                        <c:if test="${result.revokeBlock != null}">
                            <dt>Revoked in</dt><dd>block #<c:out value="${result.revokeBlock.seq}"/> on <fmt:formatDate value="${result.revokeBlock.createdDate}" pattern="dd MMM yyyy"/></dd>
                        </c:if>
                    </dl>
                </div>
            </c:if>
            <c:if test="${o == 'NOT_FOUND'}">
                <div class="card">
                    <h2>What this means</h2>
                    <p class="muted">The institution never issued a certificate with this number. If someone presented a document with this
                        number to you, treat it as forged. Check for typing mistakes: numbers look like
                        <code><c:out value="${institution.shortCode}"/>-2026-7K3QX9PD</code>.</p>
                    <form action="${ctx}/verify" method="get" class="verify-input">
                        <input type="text" name="certNo" value="${fn:escapeXml(result.certNo)}" required maxlength="40" data-upper aria-label="Certificate number">
                        <button class="btn btn-primary" type="submit">Try again</button>
                    </form>
                </div>
            </c:if>
        </div>
        <div class="card">
            <h2 style="font-size:1.05rem">Verification checks</h2>
            <ul class="checks">
                <c:forEach var="ch" items="${result.checks}">
                    <li class="${ch.passed ? 'pass' : 'fail'}">
                        <span class="tick">${ch.passed ? '&#10003;' : '&#10007;'}</span>
                        <div><b><c:out value="${ch.name}"/></b><span><c:out value="${ch.detail}"/></span></div>
                    </li>
                </c:forEach>
            </ul>
            <p class="hint" style="margin-top:1rem">This lookup was recorded. Repeated lookups of fake or altered certificates are flagged to the registrar.</p>
        </div>
    </div>
</div>
<footer class="footer"><a href="${ctx}/">Verify another certificate</a></footer>
</body>
</html>
