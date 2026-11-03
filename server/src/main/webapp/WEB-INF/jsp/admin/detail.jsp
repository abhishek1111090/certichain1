<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<c:set var="pageTitle" value="${c.certNo}"/>
<%@ include file="/WEB-INF/jsp/fragments/admin-top.jspf" %>
<c:set var="o" value="${result.outcome}"/>

<div class="verdict verdict-${o}" style="padding:1.1rem 1.4rem">
    <div><h1 style="font-size:1.4rem"><c:out value="${o.label}"/></h1><p><c:out value="${o.explanation}"/></p></div>
    <div style="margin-left:auto;display:flex;gap:.5rem;flex-wrap:wrap">
        <a class="btn btn-ghost btn-sm" style="background:#fff" href="${ctx}/certificate/${fn:escapeXml(c.certNo)}" target="_blank" rel="noopener">Printable</a>
        <a class="btn btn-ghost btn-sm" style="background:#fff" href="${ctx}/verify/${fn:escapeXml(c.certNo)}" target="_blank" rel="noopener">Public page</a>
    </div>
</div>

<div class="grid grid-3">
    <div>
        <div class="card">
            <div class="card-head"><h2>Certificate</h2><span class="badge badge-${c.status}"><c:out value="${c.status}"/></span></div>
            <dl class="kv">
                <dt>Student</dt><dd><c:out value="${c.studentName}"/></dd>
                <dt>Roll number</dt><dd><c:out value="${c.rollNo}"/></dd>
                <dt>Email</dt><dd><c:out value="${empty c.email ? '-' : c.email}"/></dd>
                <dt>Program</dt><dd><c:out value="${c.program}"/></dd>
                <dt>Type</dt><dd><c:out value="${c.credentialType.displayName}"/></dd>
                <dt>Title</dt><dd><c:out value="${c.title}"/></dd>
                <dt>Grade</dt><dd><c:out value="${empty c.grade ? '-' : c.grade}"/></dd>
                <dt>Issue date</dt><dd><c:out value="${c.issueDateDisplay}"/></dd>
                <dt>Issued by</dt><dd>@<c:out value="${c.issuedBy}"/></dd>
                <dt>Stored data hash</dt><dd class="hash"><c:out value="${c.dataHash}"/></dd>
                <dt>Recomputed now</dt><dd class="hash"><c:out value="${c.computeHash()}"/></dd>
                <c:if test="${c.revoked}"><dt>Revoke reason</dt><dd style="color:var(--warn)"><c:out value="${c.revokeReason}"/></dd></c:if>
            </dl>
        </div>

        <div class="card">
            <div class="card-head"><h2>Ledger blocks for this certificate</h2></div>
            <div class="chain">
                <c:forEach var="b" items="${blocks}">
                    <div class="block">
                        <div class="row"><b>Block #<c:out value="${b.seq}"/> <span class="badge badge-${b.type}"><c:out value="${b.type}"/></span></b>
                            <span class="muted small">by @<c:out value="${b.actor}"/> &middot; <fmt:formatDate value="${b.createdDate}" pattern="dd MMM yyyy, HH:mm:ss"/></span></div>
                        <div class="small" style="margin-top:.4rem">prev <span class="hash"><c:out value="${b.prevHash}"/></span></div>
                        <div class="small">data <span class="hash"><c:out value="${b.dataHash}"/></span></div>
                        <div class="small">hash <span class="hash"><c:out value="${b.blockHash}"/></span>
                            <c:if test="${!b.selfConsistent}"><span class="badge badge-TAMPERED">ALTERED</span></c:if></div>
                    </div>
                </c:forEach>
                <c:if test="${empty blocks}"><div class="alert alert-bad">No ledger block exists for this certificate. It was not issued through CertiChain.</div></c:if>
            </div>
        </div>
    </div>

    <div>
        <div class="card">
            <h2 style="font-size:1.05rem">Checks</h2>
            <ul class="checks">
                <c:forEach var="ch" items="${result.checks}">
                    <li class="${ch.passed ? 'pass' : 'fail'}"><span class="tick">${ch.passed ? '&#10003;' : '&#10007;'}</span>
                        <div><b><c:out value="${ch.name}"/></b><span><c:out value="${ch.detail}"/></span></div></li>
                </c:forEach>
            </ul>
        </div>

        <c:if test="${!c.revoked && o != 'REVOKED'}">
            <div class="card">
                <h2 style="font-size:1.05rem">Revoke certificate</h2>
                <c:choose>
                    <c:when test="${sessionScope.admin.admin}">
                        <form method="post" action="${ctx}/admin/certificates/${fn:escapeXml(c.certNo)}/revoke"
                              data-confirm="Revoke this certificate? This is permanent and is sealed into the ledger.">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <div class="field"><label for="reason">Reason (shown to verifiers)</label>
                                <textarea id="reason" name="reason" rows="3" required minlength="5" maxlength="250"></textarea></div>
                            <button class="btn btn-danger btn-sm" type="submit">Revoke permanently</button>
                        </form>
                    </c:when>
                    <c:otherwise><p class="muted small">Only users with the ADMIN role can revoke certificates.</p></c:otherwise>
                </c:choose>
            </div>
        </c:if>

        <div class="card">
            <h2 style="font-size:1.05rem">Who verified this</h2>
            <div class="table-wrap"><table>
                <thead><tr><th>When</th><th>Result</th><th>Via</th></tr></thead>
                <tbody>
                <c:forEach var="v" items="${lookups}">
                    <tr><td class="small"><fmt:formatDate value="${v.created}" pattern="dd MMM, HH:mm"/><div class="muted"><c:out value="${v.clientIp}"/></div></td>
                        <td><span class="badge badge-${v.result}"><c:out value="${v.result}"/></span></td><td class="small"><c:out value="${v.channel}"/></td></tr>
                </c:forEach>
                <c:if test="${empty lookups}"><tr><td colspan="3" class="empty">Not verified by anyone yet.</td></tr></c:if>
                </tbody>
            </table></div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/admin-bottom.jspf" %>
