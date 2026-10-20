<%@ include file="../fragments/header.jspf" %>

<section class="section">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Requests</p>
            <h1>Incoming requests</h1>
            <p class="muted">${fn:length(requests)} request(s) for your listings</p>
        </div>
        <a class="button button-ghost" href="${ctx}/requests/outgoing">Outgoing requests</a>
    </div>

    <c:choose>
        <c:when test="${empty requests}">
            <div class="empty-state">
                <h3>No incoming requests</h3>
                <p>When someone requests one of your listings, it appears here.</p>
                <a class="button" href="${ctx}/resources/mine">Review my listings</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Resource</th>
                        <th>Requester</th>
                        <th>Message</th>
                        <th>Status</th>
                        <th>Received</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${requests}" var="request">
                        <tr>
                            <td>
                                <a href="${ctx}/resources/view?id=${request.resourceId}">
                                    <c:out value="${request.resourceTitle}"/>
                                </a>
                            </td>
                            <td><c:out value="${request.requesterName}"/></td>
                            <td class="message-cell">
                                <c:choose>
                                    <c:when test="${empty request.message}">
                                        <span class="muted">No message provided.</span>
                                    </c:when>
                                    <c:otherwise><c:out value="${request.message}"/></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <span class="badge status-${fn:toLowerCase(request.status.name())}">
                                    ${request.status.label}
                                </span>
                            </td>
                            <td>${request.createdAt}</td>
                            <td class="table-actions">
                                <c:if test="${request.pending}">
                                    <form method="post" action="${ctx}/requests/accept"
                                          data-confirm="Accept this request and create a transaction?">
                                        <input type="hidden" name="requestId" value="${request.requestId}">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <button class="button button-small" type="submit">Accept</button>
                                    </form>
                                    <form method="post" action="${ctx}/requests/reject"
                                          data-confirm="Reject this request?">
                                        <input type="hidden" name="requestId" value="${request.requestId}">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <button class="button button-small button-danger" type="submit">
                                            Reject
                                        </button>
                                    </form>
                                </c:if>
                                <c:if test="${request.accepted}">
                                    <a class="button button-small button-ghost"
                                       href="${ctx}/transactions">View exchange</a>
                                </c:if>
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
