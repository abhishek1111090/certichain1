<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
    <title><c:out value="${empty title ? 'Error' : title}"/> &middot; CertiChain</title>
</head>
<body>
<div class="section" style="max-width:560px;padding-top:5rem">
    <div class="card">
        <h1 style="font-size:1.4rem"><c:out value="${empty title ? 'Something went wrong' : title}"/></h1>
        <p class="muted"><c:out value="${empty message ? 'The page could not be shown.' : message}"/></p>
        <a class="btn btn-primary" href="${ctx}/">Go to verification portal</a>
    </div>
</div>
</body>
</html>
