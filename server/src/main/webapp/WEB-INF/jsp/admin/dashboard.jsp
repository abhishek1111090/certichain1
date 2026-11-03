<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<c:set var="pageTitle" value="Dashboard"/>
<%@ include file="/WEB-INF/jsp/fragments/admin-top.jspf" %>

<div class="stats">
    <div class="stat"><div class="label">Certificates issued</div><div class="value"><fmt:formatNumber value="${stats.total}"/></div>
        <div class="sub"><c:out value="${stats.valid}"/> valid &middot; <c:out value="${stats.revoked}"/> revoked</div></div>
    <div class="stat"><div class="label">Ledger length</div><div class="value"><fmt:formatNumber value="${stats.blocks}"/></div>
        <div class="sub">hash-chained blocks</div></div>
    <div class="stat"><div class="label">Verifications today</div><div class="value"><fmt:formatNumber value="${stats.verificationsToday}"/></div>
        <div class="sub"><c:out value="${stats.verificationsWeek}"/> in the last 7 days</div></div>
    <div class="stat ${stats.suspiciousWeek > 0 ? 'bad' : 'ok'}"><div class="label">Fraud radar (7 days)</div>
        <div class="value"><fmt:formatNumber value="${stats.suspiciousWeek}"/></div>
        <div class="sub">fake, altered or revoked certificates presented</div></div>
</div>

<div class="grid grid-3">
    <div class="card">
        <div class="card-head"><h2>Verification traffic</h2><span class="muted small">last 7 days</span></div>
        <div class="bars">
            <c:forEach var="b" items="${stats.verificationBars}">
                <div class="bar"><span class="n"><c:out value="${b.count}"/></span>
                    <div class="fill" style="height:${b.percent}%"></div><span class="d"><c:out value="${b.label}"/></span></div>
            </c:forEach>
        </div>
        <div style="display:flex;gap:.5rem;flex-wrap:wrap;margin-top:1rem">
            <c:forEach var="e" items="${stats.resultsWeek}">
                <span class="badge badge-${e.key}"><c:out value="${e.key}"/>: <c:out value="${e.value}"/></span>
            </c:forEach>
            <c:if test="${empty stats.resultsWeek}"><span class="muted small">No lookups yet. Open the public portal and verify a certificate.</span></c:if>
        </div>
    </div>
    <div class="card">
        <div class="card-head"><h2>Ledger health</h2></div>
        <c:choose>
            <c:when test="${integrity == null}"><p class="muted">The watchdog runs its first audit a few seconds after start-up.</p></c:when>
            <c:when test="${integrity.intact}">
                <div class="alert alert-ok" style="margin-bottom:.6rem"><b>Intact.</b> <c:out value="${integrity.blocksChecked}"/> blocks re-hashed
                    on <c:out value="${integrity.threadsUsed}"/> threads in <c:out value="${integrity.durationMs}"/> ms.</div>
            </c:when>
            <c:otherwise>
                <div class="alert alert-bad" style="margin-bottom:.6rem"><b>Breach detected.</b> <c:out value="${integrity.summary()}"/></div>
            </c:otherwise>
        </c:choose>
        <dl class="kv small" style="grid-template-columns:120px 1fr">
            <dt>Active sessions</dt><dd><c:out value="${activeSessions}"/></dd>
            <dt>Your role</dt><dd><c:out value="${sessionScope.admin.role}"/></dd>
        </dl>
        <form method="post" action="${ctx}/admin/integrity" style="margin-top:1rem">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <button class="btn btn-ghost btn-sm" type="submit">Run audit now</button>
        </form>
    </div>
</div>

<div class="grid grid-2" style="margin-top:1.25rem">
    <div class="card">
        <div class="card-head"><h2>Latest ledger blocks</h2><a class="small" href="${ctx}/admin/ledger">Explorer &rarr;</a></div>
        <div class="table-wrap"><table>
            <thead><tr><th>#</th><th>Type</th><th>Certificate</th><th>Block hash</th></tr></thead>
            <tbody>
            <c:forEach var="b" items="${recentBlocks}">
                <tr><td>#<c:out value="${b.seq}"/></td><td><span class="badge badge-${b.type}"><c:out value="${b.type}"/></span></td>
                    <td><a href="${ctx}/admin/certificates/${fn:escapeXml(b.certNo)}" class="mono"><c:out value="${b.certNo}"/></a></td>
                    <td class="hash"><c:out value="${b.shortHash}"/>&hellip;</td></tr>
            </c:forEach>
            <c:if test="${empty recentBlocks}"><tr><td colspan="4" class="empty">The ledger is empty.</td></tr></c:if>
            </tbody>
        </table></div>
    </div>
    <div class="card">
        <div class="card-head"><h2>Suspicious lookups</h2><a class="small" href="${ctx}/admin/audit">All &rarr;</a></div>
        <div class="table-wrap"><table>
            <thead><tr><th>When</th><th>Number presented</th><th>Result</th><th>Via</th></tr></thead>
            <tbody>
            <c:forEach var="v" items="${suspicious}">
                <tr><td class="small"><fmt:formatDate value="${v.created}" pattern="dd MMM, HH:mm"/></td>
                    <td class="mono"><c:out value="${v.certNo}"/></td>
                    <td><span class="badge badge-${v.result}"><c:out value="${v.result}"/></span></td>
                    <td class="small"><c:out value="${v.channel}"/></td></tr>
            </c:forEach>
            <c:if test="${empty suspicious}"><tr><td colspan="4" class="empty">No fake or altered certificates presented.</td></tr></c:if>
            </tbody>
        </table></div>
    </div>
</div>

<div class="card" style="margin-top:1.25rem">
    <div class="card-head"><h2>Certificates by type</h2></div>
    <div style="display:flex;gap:.6rem;flex-wrap:wrap">
        <c:forEach var="t" items="${stats.byType}">
            <a class="badge badge-type" href="${ctx}/admin/certificates?type=${t.key}"><c:out value="${t.key}"/> &middot; <c:out value="${t.value}"/></a>
        </c:forEach>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/admin-bottom.jspf" %>
