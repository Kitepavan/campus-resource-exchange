<%@ include file="../fragments/header.jspf" %>

<section class="section">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Administrator</p>
            <h1>Listing moderation</h1>
            <p class="muted">${fn:length(resources)} listing(s), including inactive records</p>
        </div>
    </div>

    <div class="table-wrap">
        <table>
            <thead>
            <tr>
                <th>Resource</th>
                <th>Owner</th>
                <th>Type</th>
                <th>Exchange</th>
                <th>Price</th>
                <th>Status</th>
                <th></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${resources}" var="resource">
                <tr>
                    <td>
                        <a href="${ctx}/resources/view?id=${resource.resourceId}">
                            <c:out value="${resource.title}"/>
                        </a>
                        <span class="muted block"><c:out value="${resource.category}"/></span>
                    </td>
                    <td><c:out value="${userNames[resource.ownerId]}"/></td>
                    <td>${resource.resourceType.label}</td>
                    <td>${resource.exchangeType.label}</td>
                    <td>${resource.priceLabel}</td>
                    <td>
                        <span class="badge status-${fn:toLowerCase(resource.status.name())}">
                            ${resource.status.label}
                        </span>
                    </td>
                    <td class="table-actions">
                        <form method="post" action="${ctx}/admin/listings/status"
                              data-confirm="${resource.inactive ? 'Restore this listing?' : 'Deactivate this listing?'}">
                            <input type="hidden" name="resourceId" value="${resource.resourceId}">
                            <input type="hidden" name="deactivate" value="${resource.inactive ? 'false' : 'true'}">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <button class="button button-small ${resource.inactive ? '' : 'button-danger'}"
                                    type="submit">
                                ${resource.inactive ? 'Restore' : 'Deactivate'}
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
