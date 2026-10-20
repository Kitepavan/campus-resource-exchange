<%@ include file="../fragments/header.jspf" %>

<nav class="breadcrumbs">
    <a href="${ctx}/transactions">Transactions</a>
    <span>/</span>
    <span>#${transaction.transactionId}</span>
</nav>

<section class="section narrow">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Transaction #${transaction.transactionId}</p>
            <h1><c:out value="${transaction.resourceTitle}"/></h1>
            <p class="muted">${transaction.exchangeType.label} exchange</p>
        </div>
        <span class="badge status-${fn:toLowerCase(transaction.status.name())}">
            ${transaction.status.label}
        </span>
    </div>

    <div class="panel">
        <dl class="detail-list">
            <div><dt>Owner</dt><dd><c:out value="${transaction.ownerName}"/></dd></div>
            <div><dt>Requester</dt><dd><c:out value="${transaction.requesterName}"/></dd></div>
            <div><dt>Request message</dt><dd><c:out value="${transaction.requestMessage}"/></dd></div>
            <div><dt>Created</dt><dd>${transaction.createdAt}</dd></div>
            <c:if test="${not empty transaction.completedAt}">
                <div><dt>Completed</dt><dd>${transaction.completedAt}</dd></div>
            </c:if>
            <c:if test="${not empty transaction.returnedAt}">
                <div><dt>Returned</dt><dd>${transaction.returnedAt}</dd></div>
            </c:if>
        </dl>
    </div>

    <div class="panel">
        <h2>Actions</h2>
        <c:choose>
            <c:when test="${transaction.lend and transaction.pendingHandover and transaction.ownerId == currentUser.userId}">
                <p class="muted">Hand the resource to the requester, then record the loan.</p>
                <form method="post" action="${ctx}/transactions/lend" class="form-inline">
                    <input type="hidden" name="id" value="${transaction.transactionId}">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <button class="button" type="submit">Mark as lent</button>
                </form>
            </c:when>
            <c:when test="${transaction.lend and transaction.activeLoan and transaction.ownerId == currentUser.userId}">
                <p class="muted">Record the return when the resource is back with you.</p>
                <form method="post" action="${ctx}/transactions/return" class="form-inline"
                      data-confirm="Record the return and make the listing available again?">
                    <input type="hidden" name="id" value="${transaction.transactionId}">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <button class="button" type="submit">Record return</button>
                </form>
            </c:when>
            <c:when test="${transaction.saleOrDonation and transaction.pendingHandover}">
                <p class="muted">When the handover is complete, either participant can record it.</p>
                <form method="post" action="${ctx}/transactions/complete" class="form-inline"
                      data-confirm="Mark this exchange as completed?">
                    <input type="hidden" name="id" value="${transaction.transactionId}">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <button class="button" type="submit">Mark handover complete</button>
                </form>
            </c:when>
            <c:when test="${transaction.completed}">
                <p class="muted">This exchange is complete.</p>
                <c:if test="${canReview}">
                    <a class="button" href="${ctx}/reviews/new?transactionId=${transaction.transactionId}">
                        Leave a review
                    </a>
                </c:if>
            </c:when>
            <c:otherwise>
                <p class="muted">No action is available for you right now.</p>
            </c:otherwise>
        </c:choose>
    </div>

    <c:if test="${not empty review}">
        <div class="panel">
            <h2>Review</h2>
            <div class="review">
                <div class="review-head">
                    <strong><c:out value="${review.reviewerName}"/></strong>
                    <span class="rating">${review.rating} / 5</span>
                </div>
                <p><c:out value="${review.comment}"/></p>
                <span class="muted">${review.createdAt}</span>
            </div>
        </div>
    </c:if>
</section>

<%@ include file="../fragments/footer.jspf" %>
