<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<c:set var="pageTitle" value="Issue certificate"/>
<%@ include file="/WEB-INF/jsp/fragments/admin-top.jspf" %>

<div class="grid grid-3">
    <div class="card">
        <c:if test="${not empty errors}">
            <div class="alert alert-bad"><b>Please fix the following:</b>
                <ul><c:forEach var="e" items="${errors}"><li><c:out value="${e}"/></li></c:forEach></ul></div>
        </c:if>
        <form method="post" action="${ctx}/admin/certificates">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <div class="form-grid">
                <div class="field"><label for="studentName">Student name *</label>
                    <input type="text" id="studentName" name="studentName" value="${fn:escapeXml(form.studentName)}" required maxlength="120"></div>
                <div class="field"><label for="rollNo">Roll number *</label>
                    <input type="text" id="rollNo" name="rollNo" value="${fn:escapeXml(form.rollNo)}" required maxlength="30" placeholder="22CS1001"></div>
                <div class="field"><label for="email">Student email</label>
                    <input type="email" id="email" name="email" value="${fn:escapeXml(form.email)}" maxlength="120">
                    <div class="hint">The student is notified with the verification link.</div></div>
                <div class="field"><label for="program">Program *</label>
                    <input type="text" id="program" name="program" value="${fn:escapeXml(empty form.program ? 'B.Tech CSE' : form.program)}" required maxlength="120"></div>
                <div class="field"><label for="credentialType">Credential type *</label>
                    <select id="credentialType" name="credentialType" required>
                        <c:forEach var="t" items="${credentialTypes}">
                            <option value="${t.name()}" ${t == form.credentialType ? 'selected' : ''}><c:out value="${t.displayName}"/></option>
                        </c:forEach>
                    </select></div>
                <div class="field"><label for="issueDate">Issue date *</label>
                    <input type="date" id="issueDate" name="issueDate" value="${form.issueDate}" required></div>
                <div class="field span-2"><label for="title">Title *</label>
                    <input type="text" id="title" name="title" value="${fn:escapeXml(form.title)}" required maxlength="200"
                           placeholder="e.g. Advanced Java Programming (INT3140)"></div>
                <div class="field"><label for="grade">Grade / result</label>
                    <input type="text" id="grade" name="grade" value="${fn:escapeXml(form.grade)}" maxlength="20" placeholder="A+, 8.9 CGPA, 1st Prize"></div>
            </div>
            <button class="btn btn-primary" type="submit">Issue &amp; seal into ledger</button>
            <a class="btn btn-ghost" href="${ctx}/admin/certificates">Cancel</a>
        </form>
    </div>
    <div class="card">
        <h2 style="font-size:1.05rem">What happens when you issue</h2>
        <ol class="small" style="padding-left:1.1rem;color:var(--ink-2)">
            <li>The fields are validated and normalised.</li>
            <li>A random, non-guessable number like <code><c:out value="${institution.shortCode}"/>-2026-7K3QX9PD</code> is assigned.</li>
            <li>The SHA-256 of the certificate data is computed.</li>
            <li>In <b>one database transaction</b> the row is stored and a new ledger block is appended, linked to the previous block's hash.</li>
            <li>After commit, an event notifies the student and the action lands in the audit trail.</li>
        </ol>
        <div class="alert alert-warn small" style="margin:0">Issued certificates can never be edited, only revoked. Double-check the spelling of the name.</div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/fragments/admin-bottom.jspf" %>
