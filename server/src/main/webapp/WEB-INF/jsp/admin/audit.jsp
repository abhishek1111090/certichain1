<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<c:set var="pageTitle" value="Audit trail"/>
<%@ include file="/WEB-INF/jsp/fragments/admin-top.jspf" %>

<div class="card">
    <div class="card-head"><h2>Actions</h2><span class="muted small">written by a Spring AOP aspect around every @Audited method</span></div>
    <div class="table-wrap"><table>
        <thead><tr><th>When</th><th>Actor</th><th>Action</th><th>Detail</th><th>Outcome</th><th>ms</th><th>Source</th></tr></thead>
        <tbody>
        <c:forEach var="e" items="${entries}">
            <tr><td class="small" style="white-space:nowrap"><fmt:formatDate value="${e.created}" pattern="dd MMM HH:mm:ss"/></td>
                <td class="small">@<c:out value="${e.actor}"/></td>
                <td class="mono small"><c:out value="${e.action}"/></td>
                <td class="small"><c:out value="${e.detail}"/></td>
                <td><span class="badge badge-${e.outcome}"><c:out value="${e.outcome}"/></span></td>
                <td class="small"><c:out value="${e.durationMs}"/></td>
                <td class="small"><c:out value="${e.source}"/></td></tr>
        </c:forEach>
        <c:if test="${empty entries}"><tr><td colspan="7" class="empty">Nothing recorded yet.</td></tr></c:if>
        </tbody>
    </table></div>
</div>

<div class="card">
    <div class="card-head"><h2>Suspicious verification attempts</h2></div>
    <div class="table-wrap"><table>
        <thead><tr><th>When</th><th>Number presented</th><th>Result</th><th>Via</th><th>IP</th><th>Client</th></tr></thead>
        <tbody>
        <c:forEach var="v" items="${suspicious}">
            <tr><td class="small" style="white-space:nowrap"><fmt:formatDate value="${v.created}" pattern="dd MMM HH:mm:ss"/></td>
                <td class="mono small"><c:out value="${v.certNo}"/></td>
                <td><span class="badge badge-${v.result}"><c:out value="${v.result}"/></span></td>
                <td class="small"><c:out value="${v.channel}"/></td>
                <td class="small"><c:out value="${v.clientIp}"/></td>
                <td class="small muted"><c:out value="${fn:substring(v.userAgent, 0, 60)}"/></td></tr>
        </c:forEach>
        <c:if test="${empty suspicious}"><tr><td colspan="6" class="empty">None.</td></tr></c:if>
        </tbody>
    </table></div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/admin-bottom.jspf" %>
