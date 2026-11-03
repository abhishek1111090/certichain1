<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<c:set var="o" value="${result.outcome}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
    <title><c:out value="${c.studentName}"/> &middot; <c:out value="${c.certNo}"/></title>
</head>
<body>
<div class="print-bar no-print">
    <a class="btn btn-ghost" href="${ctx}/verify/${fn:escapeXml(c.certNo)}">&larr; Verification result</a>
    <button class="btn btn-primary" type="button" data-print>Print / save as PDF</button>
</div>
<div class="certificate">
    <c:if test="${o != 'AUTHENTIC'}">
        <div class="watermark"><span><c:out value="${fn:toUpperCase(o.label)}"/></span></div>
    </c:if>
    <div class="frame">
        <div class="cert-head">
            <div class="inst"><c:out value="${institution.name}"/></div>
            <div class="addr"><c:out value="${institution.address}"/> &middot; <c:out value="${institution.website}"/></div>
        </div>
        <div class="cert-kind"><c:out value="${c.credentialType.displayName}"/></div>
        <div class="cert-body">
            <div>This is to certify that</div>
            <div class="name"><c:out value="${c.studentName}"/></div>
            <div class="roll">Roll No. <c:out value="${c.rollNo}"/> &middot; <c:out value="${c.program}"/></div>
            <div style="margin-top:1rem">
                <c:choose>
                    <c:when test="${c.credentialType == 'DEGREE'}">has fulfilled all requirements and is conferred the degree of</c:when>
                    <c:when test="${c.credentialType == 'COURSE_COMPLETION'}">has successfully completed the course</c:when>
                    <c:when test="${c.credentialType == 'INTERNSHIP'}">has successfully completed the</c:when>
                    <c:when test="${c.credentialType == 'HACKATHON'}">is hereby recognised as</c:when>
                    <c:when test="${c.credentialType == 'WORKSHOP'}">has actively participated in the</c:when>
                    <c:otherwise>is awarded the distinction of</c:otherwise>
                </c:choose>
            </div>
            <div class="title"><c:out value="${c.title}"/></div>
            <c:if test="${not empty c.grade}"><div>with <b><c:out value="${c.grade}"/></b></div></c:if>
            <div style="margin-top:.6rem">on <c:out value="${c.issueDateDisplay}"/>.</div>
        </div>
        <div class="cert-foot">
            <div class="sig"><div class="line">Dean</div>Academic Affairs</div>
            <div class="cert-qr">
                <img src="${ctx}/qr?certNo=${fn:escapeXml(c.certNo)}&amp;size=256" alt="QR code linking to the verification page">
                <div class="no"><c:out value="${c.certNo}"/></div>
                <div class="muted" style="font-size:.7rem">Scan to verify</div>
            </div>
            <div class="sig"><div class="line"><c:out value="${institution.registrarName}"/></div><c:out value="${institution.registrarTitle}"/></div>
        </div>
        <div class="cert-seal-line">
            Ledger block #<c:out value="${result.issueBlock.seq}"/> &middot; SHA-256 <c:out value="${c.dataHash}"/>
        </div>
    </div>
</div>
</body>
</html>
