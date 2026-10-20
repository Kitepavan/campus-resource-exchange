<%@ include file="../fragments/header.jspf" %>

<section class="section">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Administrator</p>
            <h1>Users and verification</h1>
            <p class="muted">${fn:length(users)} account(s)</p>
        </div>
    </div>

    <form class="filter-panel" method="get" action="${ctx}/admin/users">
        <div class="filter-grid compact">
            <label>
                Role
                <select name="role">
                    <option value="">All roles</option>
                    <c:forEach items="${roles}" var="role">
                        <option value="${role}" ${roleFilter == role ? 'selected' : ''}>
                            ${role.label}
                        </option>
                    </c:forEach>
                </select>
            </label>
            <label>
                Verification
                <select name="verification">
                    <option value="">Any status</option>
                    <c:forEach items="${verificationStatuses}" var="status">
                        <option value="${status}" ${verificationFilter == status ? 'selected' : ''}>
                            ${status.label}
                        </option>
                    </c:forEach>
                </select>
            </label>
            <div class="filter-actions">
                <button class="button" type="submit">Filter</button>
                <a class="button button-ghost" href="${ctx}/admin/users">Reset</a>
            </div>
        </div>
    </form>

    <div class="table-wrap">
        <table>
            <thead>
            <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Student ID</th>
                <th>Role</th>
                <th>Verification</th>
                <th>Account</th>
                <th></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${users}" var="user">
                <tr>
                    <td>
                        <strong><c:out value="${user.fullName}"/></strong>
                        <c:if test="${not empty user.department}">
                            <span class="muted block"><c:out value="${user.department}"/></span>
                        </c:if>
                    </td>
                    <td><c:out value="${user.email}"/></td>
                    <td><c:out value="${user.studentId}"/></td>
                    <td>${user.role.label}</td>
                    <td>
                        <span class="badge ${user.verified ? 'badge-verified' : 'badge-pending'}">
                            ${user.verificationStatus.label}
                        </span>
                    </td>
                    <td>
                        <span class="badge ${user.active ? 'status-available' : 'status-inactive'}">
                            ${user.active ? 'Active' : 'Suspended'}
                        </span>
                    </td>
                    <td class="table-actions">
                        <c:if test="${user.student}">
                            <c:if test="${not user.verified}">
                                <form method="post" action="${ctx}/admin/users/verify">
                                    <input type="hidden" name="userId" value="${user.userId}">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <button class="button button-small" type="submit">Verify</button>
                                </form>
                            </c:if>
                            <c:if test="${user.verificationStatus.name() != 'REJECTED'}">
                                <form method="post" action="${ctx}/admin/users/reject"
                                      data-confirm="Reject this student's verification?">
                                    <input type="hidden" name="userId" value="${user.userId}">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <button class="button button-small button-danger" type="submit">
                                        Reject
                                    </button>
                                </form>
                            </c:if>
                        </c:if>
                        <form method="post" action="${ctx}/admin/users/status"
                              data-confirm="${user.active ? 'Suspend this account?' : 'Activate this account?'}">
                            <input type="hidden" name="userId" value="${user.userId}">
                            <input type="hidden" name="active" value="${user.active ? 'false' : 'true'}">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <button class="button button-small button-ghost" type="submit">
                                ${user.active ? 'Suspend' : 'Activate'}
                            </button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</section>

<%@ include file="../fragments/footer.jspf" %>
