<%@ include file="../fragments/header.jspf" %>

<section class="section">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Administrator</p>
            <h1>Platform overview</h1>
            <p class="muted">Verify students, moderate listings, and monitor exchanges.</p>
        </div>
        <a class="button" href="${ctx}/admin/users">Manage users</a>
    </div>

    <div class="stat-grid">
        <article class="stat-card">
            <span class="stat-value">${stats['totalUsers']}</span>
            <span class="stat-label">Registered users</span>
        </article>
        <article class="stat-card">
            <span class="stat-value">${stats['pendingStudents']}</span>
            <span class="stat-label">Pending verifications</span>
        </article>
        <article class="stat-card">
            <span class="stat-value">${stats['availableResources']}</span>
            <span class="stat-label">Available listings</span>
        </article>
        <article class="stat-card">
            <span class="stat-value">${stats['pendingRequests']}</span>
            <span class="stat-label">Pending requests</span>
        </article>
        <article class="stat-card">
            <span class="stat-value">${stats['completedTransactions']}</span>
            <span class="stat-label">Completed exchanges</span>
        </article>
        <article class="stat-card">
            <span class="stat-value">${stats['totalReviews']}</span>
            <span class="stat-label">Reviews</span>
        </article>
    </div>

    <div class="admin-links">
        <a class="button button-ghost" href="${ctx}/admin/users">Users and verification</a>
        <a class="button button-ghost" href="${ctx}/admin/listings">Listing moderation</a>
        <a class="button button-ghost" href="${ctx}/admin/activity">Requests, transactions, reviews</a>
        <a class="button button-ghost" href="${ctx}/admin/backup">Catalogue backup</a>
    </div>
</section>

<section class="section">
    <div class="section-heading">
        <div><h2>Recent requests</h2></div>
        <a class="button button-ghost" href="${ctx}/admin/activity">View activity</a>
    </div>
    <c:choose>
        <c:when test="${empty requests}">
            <div class="empty-state"><p>No requests yet.</p></div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr><th>Resource</th><th>Requester</th><th>Owner</th><th>Status</th><th>Created</th></tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${requests}" var="request" end="4">
                        <tr>
                            <td><c:out value="${request.resourceTitle}"/></td>
                            <td><c:out value="${request.requesterName}"/></td>
                            <td><c:out value="${request.ownerName}"/></td>
                            <td><span class="badge status-${fn:toLowerCase(request.status.name())}">${request.status.label}</span></td>
                            <td>${request.createdAt}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<section class="section">
    <div class="section-heading">
        <div><h2>Recent transactions</h2></div>
        <a class="button button-ghost" href="${ctx}/admin/activity">View activity</a>
    </div>
    <c:choose>
        <c:when test="${empty transactions}">
            <div class="empty-state"><p>No transactions yet.</p></div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr><th>Resource</th><th>Type</th><th>Owner</th><th>Requester</th><th>Status</th></tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${transactions}" var="transaction" end="4">
                        <tr>
                            <td><c:out value="${transaction.resourceTitle}"/></td>
                            <td>${transaction.exchangeType.label}</td>
                            <td><c:out value="${transaction.ownerName}"/></td>
                            <td><c:out value="${transaction.requesterName}"/></td>
                            <td><span class="badge status-${fn:toLowerCase(transaction.status.name())}">${transaction.status.label}</span></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="../fragments/footer.jspf" %>
