<%@ include file="../fragments/header.jspf" %>

<section class="section">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Exchange history</p>
            <h1>My transactions</h1>
            <p class="muted">${fn:length(transactions)} transaction(s)</p>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty transactions}">
            <div class="empty-state">
                <h3>No transactions yet</h3>
                <p>An accepted request creates a transaction that both participants can track.</p>
                <a class="button" href="${ctx}/resources">Browse resources</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Resource</th>
                        <th>Type</th>
                        <th>Owner</th>
                        <th>Requester</th>
                        <th>Status</th>
                        <th>Created</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${transactions}" var="transaction">
                        <tr>
                            <td><c:out value="${transaction.resourceTitle}"/></td>
                            <td>${transaction.exchangeType.label}</td>
                            <td><c:out value="${transaction.ownerName}"/></td>
                            <td><c:out value="${transaction.requesterName}"/></td>
                            <td>
                                <span class="badge status-${fn:toLowerCase(transaction.status.name())}">
                                    ${transaction.status.label}
                                </span>
                            </td>
                            <td>${transaction.createdAt}</td>
                            <td>
                                <a class="button button-small button-ghost"
                                   href="${ctx}/transactions/view?id=${transaction.transactionId}">
                                    View
                                </a>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="../fragments/footer.jspf" %>
