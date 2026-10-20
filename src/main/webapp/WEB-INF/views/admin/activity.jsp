<%@ include file="../fragments/header.jspf" %>

<section class="section">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Administrator</p>
            <h1>Platform activity</h1>
            <p class="muted">Requests, transactions, and reviews across the platform.</p>
        </div>
        <a class="button button-ghost" href="${ctx}/admin">Back to overview</a>
    </div>
</section>

<section class="section">
    <h2>Requests</h2>
    <c:choose>
        <c:when test="${empty requests}">
            <div class="empty-state"><p>No requests.</p></div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr><th>#</th><th>Resource</th><th>Requester</th><th>Owner</th><th>Status</th><th>Created</th></tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${requests}" var="request">
                        <tr>
                            <td>${request.requestId}</td>
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
    <h2>Transactions</h2>
    <c:choose>
        <c:when test="${empty transactions}">
            <div class="empty-state"><p>No transactions.</p></div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr><th>#</th><th>Resource</th><th>Type</th><th>Owner</th><th>Requester</th><th>Status</th><th>Created</th></tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${transactions}" var="transaction">
                        <tr>
                            <td>${transaction.transactionId}</td>
                            <td><c:out value="${transaction.resourceTitle}"/></td>
                            <td>${transaction.exchangeType.label}</td>
                            <td><c:out value="${transaction.ownerName}"/></td>
                            <td><c:out value="${transaction.requesterName}"/></td>
                            <td><span class="badge status-${fn:toLowerCase(transaction.status.name())}">${transaction.status.label}</span></td>
                            <td>${transaction.createdAt}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<section class="section">
    <h2>Reviews</h2>
    <c:choose>
        <c:when test="${empty reviews}">
            <div class="empty-state"><p>No reviews.</p></div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr><th>#</th><th>Resource</th><th>Reviewer</th><th>Rating</th><th>Comment</th><th>Created</th></tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${reviews}" var="review">
                        <tr>
                            <td>${review.reviewId}</td>
                            <td><c:out value="${review.resourceTitle}"/></td>
                            <td><c:out value="${review.reviewerName}"/></td>
                            <td>${review.rating} / 5</td>
                            <td><c:out value="${review.comment}"/></td>
                            <td>${review.createdAt}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="../fragments/footer.jspf" %>
