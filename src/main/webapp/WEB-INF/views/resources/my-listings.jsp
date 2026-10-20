<%@ include file="../fragments/header.jspf" %>

<section class="section">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Your inventory</p>
            <h1>My listings</h1>
            <p class="muted">${fn:length(resources)} listing(s)</p>
        </div>
        <a class="button" href="${ctx}/resources/new">Add a listing</a>
    </div>

    <c:choose>
        <c:when test="${empty resources}">
            <div class="empty-state">
                <h3>No listings yet</h3>
                <p>Add a textbook, lab kit, or set of notes to get started.</p>
                <a class="button" href="${ctx}/resources/new">Add your first listing</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="stack">
                <c:forEach items="${resources}" var="resource">
                    <article class="list-row">
                        <div class="list-row-main">
                            <div>
                                <span class="badge badge-status status-${fn:toLowerCase(resource.status.name())}">
                                    ${resource.status.label}
                                </span>
                                <h3>
                                    <a href="${ctx}/resources/view?id=${resource.resourceId}">
                                        <c:out value="${resource.title}"/>
                                    </a>
                                </h3>
                                <p class="muted">${resource.displayDetails()}</p>
                            </div>
                            <div class="list-row-meta">
                                <span>${resource.exchangeType.label}</span>
                                <span>${resource.priceLabel}</span>
                                <span><c:out value="${resource.pickupLocation}"/></span>
                            </div>
                        </div>
                        <div class="list-row-actions">
                            <a class="button button-small button-ghost"
                               href="${ctx}/resources/edit?id=${resource.resourceId}">Edit</a>
                            <c:if test="${not resource.reserved and not resource.lent and not resource.inactive}">
                                <form method="post" action="${ctx}/resources/delete"
                                      data-confirm="Deactivate this listing?">
                                    <input type="hidden" name="id" value="${resource.resourceId}">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <button class="button button-small button-danger" type="submit">
                                        Deactivate
                                    </button>
                                </form>
                            </c:if>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="../fragments/footer.jspf" %>
