<%@ include file="fragments/header.jspf" %>

<section class="section narrow">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Account</p>
            <h1>Your profile</h1>
            <p class="muted">Email, student ID, role, and verification are managed by the platform.</p>
        </div>
        <span class="badge ${user.verified ? 'badge-verified' : 'badge-pending'}">
            ${user.verificationStatus.label}
        </span>
    </div>

    <div class="panel">
        <dl class="detail-list">
            <div><dt>Email</dt><dd><c:out value="${user.email}"/></dd></div>
            <div><dt>Student ID</dt><dd><c:out value="${user.studentId}"/></dd></div>
            <div><dt>Role</dt><dd>${user.role.label}</dd></div>
            <div><dt>Member since</dt><dd>${user.createdAt}</dd></div>
        </dl>
    </div>

    <form class="panel form-stack" method="post" action="${ctx}/profile">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <label>
            Full name
            <input type="text" name="fullName" required maxlength="120"
                   value="${fn:escapeXml(user.fullName)}">
        </label>
        <c:choose>
            <c:when test="${user.student}">
                <div class="form-row">
                    <label>
                        Department
                        <input type="text" name="department" required maxlength="100"
                               value="${fn:escapeXml(user.department)}">
                    </label>
                    <label>
                        Year of study
                        <select name="yearOfStudy" required>
                            <c:forEach begin="1" end="8" var="year">
                                <option value="${year}" ${user.yearOfStudy == year ? 'selected' : ''}>
                                    Year ${year}
                                </option>
                            </c:forEach>
                        </select>
                    </label>
                </div>
            </c:when>
            <c:otherwise>
                <div class="form-row">
                    <label>
                        Department (optional)
                        <input type="text" name="department" maxlength="100"
                               value="${fn:escapeXml(user.department)}">
                    </label>
                </div>
            </c:otherwise>
        </c:choose>
        <div class="form-actions">
            <button class="button" type="submit">Save profile</button>
        </div>
    </form>
</section>

<%@ include file="fragments/footer.jspf" %>
