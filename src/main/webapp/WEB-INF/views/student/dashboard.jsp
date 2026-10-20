<%@ include file="../fragments/header.jspf" %>

<section class="section">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Student dashboard</p>
            <h1>Welcome, <c:out value="${sessionScope.displayName}"/></h1>
            <p class="muted">
                ${sessionScope.verified ? 'Your account is verified.' : 'Your account is waiting for administrator verification.'}
            </p>
        </div>
        <a class="button" href="${ctx}/resources/new">Add a listing</a>
    </div>

    <div class="stat-grid">
        <a class="stat-card" href="${ctx}/resources/mine">
            <span class="stat-value">${listingCount}</span>
            <span class="stat-label">My listings</span>
        </a>
        <a class="stat-card" href="${ctx}/requests/outgoing">
            <span class="stat-value">${pendingOutgoing}</span>
            <span class="stat-label">Pending requests sent</span>
        </a>
        <a class="stat-card" href="${ctx}/requests/incoming">
            <span class="stat-value">${pendingIncoming}</span>
            <span class="stat-label">Requests to review</span>
        </a>
        <a class="stat-card" href="${ctx}/transactions">
            <span class="stat-value">${activeTransactions}</span>
            <span class="stat-label">Active exchanges</span>
        </a>
    </div>
</section>

<section class="section">
    <div class="section-heading">
        <div>
            <h2>Recent requests</h2>
        </div>
        <a class="button button-ghost" href="${ctx}/requests/outgoing">View all</a>
    </div>
    <c:choose>
        <c:when test="${empty outgoing and empty incoming}">
            <div class="empty-state">
                <h3>No requests yet</h3>
                <p>Browse the catalogue or wait for a student to request one of your listings.</p>
                <a class="button" href="${ctx}/resources">Browse resources</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="two-column">
                <div class="panel">
                    <h3>Sent</h3>
                    <c:choose>
                        <c:when test="${empty outgoing}">
                            <p class="muted">You have not sent any requests.</p>
                        </c:when>
                        <c:otherwise>
                            <ul class="simple-list">
                                <c:forEach items="${outgoing}" var="request" end="4">
                                    <li>
                                        <a href="${ctx}/resources/view?id=${request.resourceId}">
                                            <c:out value="${request.resourceTitle}"/>
                                        </a>
                                        <span class="badge status-${fn:toLowerCase(request.status.name())}">
                                            ${request.status.label}
                                        </span>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:otherwise>
                    </c:choose>
                </div>
                <div class="panel">
                    <h3>Received</h3>
                    <c:choose>
                        <c:when test="${empty incoming}">
                            <p class="muted">No one has requested your listings yet.</p>
                        </c:when>
                        <c:otherwise>
                            <ul class="simple-list">
                                <c:forEach items="${incoming}" var="request" end="4">
                                    <li>
                                        <span>
                                            <a href="${ctx}/requests/incoming">
                                                <c:out value="${request.resourceTitle}"/>
                                            </a>
                                            <c:choose>
                                                <c:when test="${empty request.message}">
                                                    <span class="muted request-message">
                                                        No message provided.
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="request-message">
                                                        <c:out value="${request.message}"/>
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                        </span>
                                        <span class="badge status-${fn:toLowerCase(request.status.name())}">
                                            ${request.status.label}
                                        </span>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<section class="section">
    <div class="section-heading">
        <div>
            <h2>Recent exchanges</h2>
        </div>
        <a class="button button-ghost" href="${ctx}/transactions">View all</a>
    </div>
    <c:choose>
        <c:when test="${empty transactions}">
            <div class="empty-state">
                <h3>No exchanges yet</h3>
                <p>When an owner accepts a request, the exchange appears here.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="stack">
                <c:forEach items="${transactions}" var="transaction" end="4">
                    <article class="list-row">
                        <div class="list-row-main">
                            <div>
                                <span class="badge status-${fn:toLowerCase(transaction.status.name())}">
                                    ${transaction.status.label}
                                </span>
                                <h3>
                                    <a href="${ctx}/transactions/view?id=${transaction.transactionId}">
                                        <c:out value="${transaction.resourceTitle}"/>
                                    </a>
                                </h3>
                                <p class="muted">
                                    ${transaction.exchangeType.label} with
                                    <c:out value="${transaction.ownerId == currentUser.userId ? transaction.requesterName : transaction.ownerName}"/>
                                </p>
                            </div>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="../fragments/footer.jspf" %>
