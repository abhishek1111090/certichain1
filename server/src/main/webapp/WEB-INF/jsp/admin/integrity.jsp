<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<c:set var="pageTitle" value="Integrity audit"/>
<%@ include file="/WEB-INF/jsp/fragments/admin-top.jspf" %>

<div class="card">
    <div class="card-head">
        <div><h2>Full-chain audit</h2>
            <p class="muted small" style="margin:.2rem 0 0">Re-hashes every block and certificate on a thread pool and cross-checks the head pointer.
                The watchdog repeats this automatically every minute.</p></div>
        <form method="post" action="${ctx}/admin/integrity">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <button class="btn btn-primary" type="submit">Run audit now</button>
        </form>
    </div>
    <c:if test="${report != null}">
        <div class="stats" style="margin:0">
            <div class="stat ${report.intact ? 'ok' : 'bad'}"><div class="label">Result</div><div class="value">${report.intact ? 'Intact' : 'Breach'}</div>
                <div class="sub"><c:out value="${report.issueCount}"/> issue(s)</div></div>
            <div class="stat"><div class="label">Blocks re-hashed</div><div class="value"><c:out value="${report.blocksChecked}"/></div>
                <div class="sub"><c:out value="${report.certificatesChecked}"/> certificates</div></div>
            <div class="stat"><div class="label">Threads</div><div class="value"><c:out value="${report.threadsUsed}"/></div>
                <div class="sub">ExecutorService pool</div></div>
            <div class="stat"><div class="label">Duration</div><div class="value"><c:out value="${report.durationMs}"/><span style="font-size:1rem"> ms</span></div>
                <div class="sub"><c:out value="${trigger}"/> &middot; <fmt:formatDate value="${report.finishedDate}" pattern="HH:mm:ss"/></div></div>
        </div>
    </c:if>
    <c:if test="${report == null}"><p class="muted">No audit has run yet.</p></c:if>
</div>

<c:if test="${report != null && !report.intact}">
    <div class="card">
        <div class="card-head"><h2>Findings</h2>
            <span class="muted small">Affected: <c:forEach var="n" items="${report.compromisedCertificates}" varStatus="s">
                <a class="mono" href="${ctx}/admin/certificates/${fn:escapeXml(n)}"><c:out value="${n}"/></a>${s.last ? '' : ', '}</c:forEach></span></div>
        <div class="table-wrap"><table>
            <thead><tr><th>Block</th><th>Finding</th><th>Certificate</th><th>Detail</th></tr></thead>
            <tbody>
            <c:forEach var="i" items="${report.issues}">
                <tr><td><c:out value="${i.seq < 0 ? '-' : i.seq}"/></td>
                    <td><span class="badge badge-TAMPERED"><c:out value="${i.type}"/></span><div class="small muted"><c:out value="${i.description}"/></div></td>
                    <td class="mono"><c:out value="${i.certNo}"/></td>
                    <td class="small"><c:out value="${i.detail}"/></td></tr>
            </c:forEach>
            </tbody>
        </table></div>
    </div>
</c:if>

<div class="card">
    <h2 style="font-size:1.05rem">What the audit catches</h2>
    <div class="table-wrap"><table>
        <tbody>
        <tr><td><b>Edited certificate</b></td><td class="small">Name, grade, title... changed with SQL: the data no longer hashes to the sealed value.</td></tr>
        <tr><td><b>Forged row</b></td><td class="small">A certificate inserted directly into the table has no ISSUE block.</td></tr>
        <tr><td><b>Deleted / reordered blocks</b></td><td class="small">The next block's prev_hash no longer matches; sequence numbers have a gap.</td></tr>
        <tr><td><b>Edited block</b></td><td class="small">The block hash no longer matches its own contents.</td></tr>
        <tr><td><b>Hidden revocation</b></td><td class="small">A revoked certificate flipped back to VALID: the REVOKE block still exists.</td></tr>
        <tr><td><b>Truncated chain</b></td><td class="small">Blocks removed from the end: the head pointer no longer matches.</td></tr>
        </tbody>
    </table></div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/admin-bottom.jspf" %>
