<%@ include file="../fragments/header.jspf" %>

<section class="section">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Requests</p>
            <h1>My outgoing requests</h1>
            <p class="muted">${fn:length(requests)} request(s)</p>
        </div>
        <a class="button button-ghost" href="${ctx}/requests/incoming">Incoming requests</a>
    </div>

    <c:choose>
        <c:when test="${empty requests}">
            <div class="empty-state">
                <h3>No requests sent</h3>
                <p>Find something useful in the catalogue and send the owner a request.</p>
                <a class="button" href="${ctx}/resources">Browse resources</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Resource</th>
                        <th>Owner</th>
                        <th>Exchange</th>
                        <th>Status</th>
                        <th>Sent</th>
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
                            <td><c:out value="${request.ownerName}"/></td>
                            <td>${request.exchangeType.label}</td>
                            <td>
                                <span class="badge status-${fn:toLowerCase(request.status.name())}">
                                    ${request.status.label}
                                </span>
                            </td>
                            <td>${request.createdAt}</td>
                            <td class="table-actions">
                                <c:if test="${request.pending}">
                                    <form method="post" action="${ctx}/requests/cancel"
                                          data-confirm="Cancel this request?">
                                        <input type="hidden" name="requestId" value="${request.requestId}">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <button class="button button-small button-danger" type="submit">
                                            Cancel
                                        </button>
                                    </form>
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
