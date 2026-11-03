<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<c:set var="pageTitle" value="Ledger explorer"/>
<%@ include file="/WEB-INF/jsp/fragments/admin-top.jspf" %>

<div class="card">
    <div class="card-head"><h2>Head</h2><span class="muted small">newest block first</span></div>
    <dl class="kv small"><dt>Last block</dt><dd>#<c:out value="${head.seq}"/></dd>
        <dt>Head hash</dt><dd class="hash"><c:out value="${head.hash}"/></dd></dl>
</div>
<div class="card">
    <div class="table-wrap"><table>
        <thead><tr><th>#</th><th>Type</th><th>Certificate</th><th>Actor</th><th>Sealed</th><th>Prev hash</th><th>Block hash</th><th></th></tr></thead>
        <tbody>
        <c:forEach var="b" items="${blocks}">
            <tr>
                <td><b>#<c:out value="${b.seq}"/></b></td>
                <td><span class="badge badge-${b.type}"><c:out value="${b.type}"/></span></td>
                <td><a class="mono" href="${ctx}/admin/certificates/${fn:escapeXml(b.certNo)}"><c:out value="${b.certNo}"/></a></td>
                <td class="small">@<c:out value="${b.actor}"/></td>
                <td class="small" style="white-space:nowrap"><fmt:formatDate value="${b.createdDate}" pattern="dd MMM yyyy HH:mm:ss"/></td>
                <td class="hash"><c:out value="${fn:substring(b.prevHash, 0, 16)}"/>&hellip;</td>
                <td class="hash"><c:out value="${fn:substring(b.blockHash, 0, 16)}"/>&hellip;</td>
                <td><c:choose><c:when test="${b.selfConsistent}"><span class="badge badge-VALID">OK</span></c:when>
                    <c:otherwise><span class="badge badge-TAMPERED">ALTERED</span></c:otherwise></c:choose></td>
            </tr>
        </c:forEach>
        <c:if test="${empty blocks}"><tr><td colspan="8" class="empty">No blocks yet.</td></tr></c:if>
        </tbody>
    </table></div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/admin-bottom.jspf" %>
