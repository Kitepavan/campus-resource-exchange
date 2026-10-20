<%@ include file="../fragments/header.jspf" %>

<section class="auth-shell">
    <div class="auth-card">
        <p class="eyebrow">Welcome back</p>
        <h1>Log in</h1>
        <p class="muted">Use the email and password you registered with.</p>
        <form method="post" action="${ctx}/login" class="form-stack">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <label>
                Email
                <input type="email" name="email" required autocomplete="email"
                       value="${fn:escapeXml(param.email)}">
            </label>
            <label>
                Password
                <input type="password" name="password" required autocomplete="current-password">
            </label>
            <button class="button button-block" type="submit">Log in</button>
        </form>
        <p class="auth-switch">
            New here? <a href="${ctx}/register">Create a student account</a>.
        </p>
        <div class="demo-note">
            <strong>Development accounts</strong>
            <span>admin@campus.test / Admin@12345</span>
            <span>alice@campus.test / Student@123</span>
        </div>
    </div>
</section>

<%@ include file="../fragments/footer.jspf" %>
