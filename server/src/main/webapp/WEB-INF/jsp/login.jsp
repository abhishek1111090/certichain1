<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
    <title>Sign in &middot; CertiChain</title>
</head>
<body>
<div class="login-wrap">
    <div class="login-art">
        <a class="brand" href="${ctx}/"><%@ include file="/WEB-INF/jsp/fragments/brand.jspf" %><span>CertiChain</span></a>
        <div>
            <h2>Issue credentials that can be verified forever.</h2>
            <p style="opacity:.85;max-width:26rem">Every action in this console is sealed into the ledger and written to the audit trail.</p>
        </div>
        <div class="small" style="opacity:.7"><c:out value="${institution.name}"/></div>
    </div>
    <div class="login-form">
        <form method="post" action="${ctx}/login" autocomplete="on">
            <h1 style="font-size:1.6rem">Registrar sign in</h1>
            <p class="muted" style="margin-top:0">Use your institution account.</p>
            <c:if test="${param.bye != null}"><div class="alert alert-ok">You have been signed out.</div></c:if>
            <c:if test="${not empty error}"><div class="alert alert-bad"><c:out value="${error}"/></div></c:if>
            <input type="hidden" name="next" value="${fn:escapeXml(next)}">
            <div class="field">
                <label for="username">Username</label>
                <input type="text" id="username" name="username" value="${fn:escapeXml(username)}" required autofocus autocomplete="username">
            </div>
            <div class="field">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" required autocomplete="current-password">
            </div>
            <button class="btn btn-primary btn-block" type="submit">Sign in</button>
            <c:if test="${demoMode}">
                <div class="alert alert-warn small" style="margin:1rem 0 0">
                    <b>Demo accounts</b> (created on first start):<br>
                    <code>admin</code> / <code>admin123</code> &middot; administrator<br>
                    <code>registrar</code> / <code>registrar123</code> &middot; staff
                </div>
            </c:if>
            <p class="hint" style="margin-top:1rem">Five wrong passwords lock the account for five minutes.</p>
            <p class="small" style="margin-top:1.5rem"><a href="${ctx}/">&larr; Back to verification portal</a></p>
        </form>
    </div>
</div>
</body>
</html>
