<%@ include file="../fragments/header.jspf" %>

<section class="auth-shell auth-shell-wide">
    <div class="auth-card">
        <p class="eyebrow">Join the exchange</p>
        <h1>Create a student account</h1>
        <p class="muted">
            Accounts start as pending. An administrator verifies students before they
            can create listings or submit requests.
        </p>
        <form method="post" action="${ctx}/register" class="form-stack">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <label>
                Full name
                <input type="text" name="fullName" required maxlength="120"
                       value="${fn:escapeXml(param.fullName)}">
            </label>
            <div class="form-row">
                <label>
                    Email
                    <input type="email" name="email" required maxlength="190"
                           value="${fn:escapeXml(param.email)}">
                </label>
                <label>
                    Student ID
                    <input type="text" name="studentId" required maxlength="40"
                           value="${fn:escapeXml(param.studentId)}">
                </label>
            </div>
            <div class="form-row">
                <label>
                    Department
                    <input type="text" name="department" required maxlength="100"
                           value="${fn:escapeXml(param.department)}">
                </label>
                <label>
                    Year of study
                    <select name="yearOfStudy" required>
                        <option value="">Select year</option>
                        <c:forEach begin="1" end="8" var="year">
                            <option value="${year}" ${param.yearOfStudy == year ? 'selected' : ''}>
                                Year ${year}
                            </option>
                        </c:forEach>
                    </select>
                </label>
            </div>
            <label>
                Password
                <input type="password" name="password" required minlength="8"
                       autocomplete="new-password">
                <span class="field-hint">At least 8 characters with a letter and a digit.</span>
            </label>
            <button class="button button-block" type="submit">Create account</button>
        </form>
        <p class="auth-switch">
            Already registered? <a href="${ctx}/login">Log in</a>.
        </p>
    </div>
</section>

<%@ include file="../fragments/footer.jspf" %>
