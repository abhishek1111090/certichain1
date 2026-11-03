<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<c:set var="pageTitle" value="Certificates"/>
<%@ include file="/WEB-INF/jsp/fragments/admin-top.jspf" %>

<div class="card">
    <form class="toolbar" method="get" action="${ctx}/admin/certificates">
        <div class="field grow"><label for="q">Search</label>
            <input type="search" id="q" name="q" value="${fn:escapeXml(q)}" placeholder="Name, roll no, certificate no or title"></div>
        <div class="field"><label for="status">Status</label>
            <select id="status" name="status"><option value="">Any</option>
                <c:forEach var="s" items="${statuses}"><option value="${s}" ${s == status ? 'selected' : ''}><c:out value="${s}"/></option></c:forEach>
            </select></div>
        <div class="field"><label for="type">Type</label>
            <select id="type" name="type"><option value="">Any</option>
                <c:forEach var="t" items="${credentialTypes}"><option value="${t.name()}" ${t.name() == type ? 'selected' : ''}><c:out value="${t.displayName}"/></option></c:forEach>
            </select></div>
        <button class="btn btn-primary" type="submit">Filter</button>
        <a class="btn btn-ghost" href="${ctx}/admin/certificates/new">+ Issue new</a>
    </form>
    <div class="table-wrap"><table>
        <thead><tr><th>Certificate no</th><th>Student</th><th>Title</th><th>Type</th><th>Issued</th><th>Status</th></tr></thead>
        <tbody>
        <c:forEach var="c" items="${certificates}">
            <tr>
                <td><a class="mono" href="${ctx}/admin/certificates/${fn:escapeXml(c.certNo)}"><c:out value="${c.certNo}"/></a></td>
                <td><b><c:out value="${c.studentName}"/></b><div class="muted small"><c:out value="${c.rollNo}"/> &middot; <c:out value="${c.program}"/></div></td>
                <td><c:out value="${c.title}"/></td>
                <td><span class="badge badge-type"><c:out value="${c.credentialType.displayName}"/></span></td>
                <td class="small" style="white-space:nowrap"><c:out value="${c.issueDate}"/></td>
                <td><span class="badge badge-${c.status}"><c:out value="${c.status}"/></span></td>
            </tr>
        </c:forEach>
        <c:if test="${empty certificates}"><tr><td colspan="6" class="empty">No certificates match these filters.</td></tr></c:if>
        </tbody>
    </table></div>
    <p class="muted small" style="margin:.8rem 0 0"><c:out value="${fn:length(certificates)}"/> certificate(s)</p>
</div>

<%@ include file="/WEB-INF/jsp/fragments/admin-bottom.jspf" %>
