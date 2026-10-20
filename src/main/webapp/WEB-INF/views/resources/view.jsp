<%@ include file="../fragments/header.jspf" %>

<nav class="breadcrumbs">
    <a href="${ctx}/resources">Catalogue</a>
    <span>/</span>
    <span><c:out value="${resource.title}"/></span>
</nav>

<section class="detail-layout">
    <div class="detail-main">
        <div class="detail-image">
            <c:choose>
                <c:when test="${not empty resource.imagePath}">
                    <img src="${ctx}/images/${resource.imagePath}"
                         alt="${fn:escapeXml(resource.title)}">
                </c:when>
                <c:otherwise>
                    <span class="image-placeholder large">${resource.resourceType.label}</span>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="detail-heading">
            <div class="badge-row">
                <span class="badge badge-exchange">${resource.exchangeType.label}</span>
                <span class="badge">${resource.condition.label}</span>
                <span class="badge badge-status status-${fn:toLowerCase(resource.status.name())}">
                    ${resource.status.label}
                </span>
            </div>
            <h1><c:out value="${resource.title}"/></h1>
            <p class="muted">${resource.displayDetails()}</p>
        </div>

        <section class="panel">
            <h2>About this resource</h2>
            <p class="description"><c:out value="${resource.description}"/></p>
        </section>

        <section class="panel">
            <h2>Exchange details</h2>
            <dl class="detail-list">
                <div><dt>Category</dt><dd><c:out value="${resource.category}"/></dd></div>
                <div><dt>Course code</dt><dd><c:out value="${resource.courseCode}"/></dd></div>
                <div><dt>Pickup location</dt><dd><c:out value="${resource.pickupLocation}"/></dd></div>
                <div><dt>Price</dt><dd>${resource.priceLabel}</dd></div>
                <c:if test="${resource.exchangeType == 'LEND'}">
                    <div><dt>Lending terms</dt><dd><c:out value="${resource.lendingTerms}"/></dd></div>
                </c:if>
                <div><dt>Listed</dt><dd>${resource.createdAt}</dd></div>
            </dl>
        </section>

        <section class="panel">
            <h2>Reviews for this owner</h2>
            <c:choose>
                <c:when test="${empty reviews}">
                    <p class="muted">No reviews yet.</p>
                </c:when>
                <c:otherwise>
                    <div class="review-list">
                        <c:forEach items="${reviews}" var="review">
                            <article class="review">
                                <div class="review-head">
                                    <strong><c:out value="${review.reviewerName}"/></strong>
                                    <span class="rating">${review.rating} / 5</span>
                                </div>
                                <p><c:out value="${review.comment}"/></p>
                                <span class="muted">${review.createdAt}</span>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </div>

    <aside class="detail-side">
        <section class="panel owner-panel">
            <p class="eyebrow">Listed by</p>
            <h2><c:out value="${owner.fullName}"/></h2>
            <p class="muted"><c:out value="${owner.department}"/></p>
            <c:if test="${not empty owner.yearOfStudy}">
                <p class="muted">Year ${owner.yearOfStudy}</p>
            </c:if>
            <span class="badge ${owner.verified ? 'badge-verified' : 'badge-pending'}">
                ${owner.verificationStatus.label}
            </span>
        </section>

        <c:choose>
            <c:when test="${empty currentUser}">
                <section class="panel">
                    <h3>Interested?</h3>
                    <p class="muted">Log in as a verified student to send a request.</p>
                    <a class="button button-block" href="${ctx}/login">Log in</a>
                </section>
            </c:when>
            <c:when test="${resource.ownerId == currentUser.userId}">
                <section class="panel">
                    <h3>Your listing</h3>
                    <p class="muted">You can edit or deactivate this listing.</p>
                    <a class="button button-block" href="${ctx}/resources/edit?id=${resource.resourceId}">
                        Edit listing
                    </a>
                    <c:if test="${not resource.reserved and not resource.lent}">
                        <form method="post" action="${ctx}/resources/delete"
                              class="stack-top" data-confirm="Deactivate this listing?">
                            <input type="hidden" name="id" value="${resource.resourceId}">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                            <button class="button button-danger button-block" type="submit">
                                Deactivate
                            </button>
                        </form>
                    </c:if>
                </section>
            </c:when>
            <c:otherwise>
                <section class="panel">
                    <c:choose>
                        <c:when test="${not resource.available}">
                            <h3>Not available</h3>
                            <p class="muted">This resource is currently ${resource.status.label}.</p>
                        </c:when>
                        <c:when test="${not currentUser.verified}">
                            <h3>Verification pending</h3>
                            <p class="muted">
                                An administrator must verify your account before you can
                                submit requests.
                            </p>
                        </c:when>
                        <c:otherwise>
                            <h3>Request this resource</h3>
                            <form method="post" action="${ctx}/requests/new" class="form-stack">
                                <input type="hidden" name="resourceId" value="${resource.resourceId}">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <label>
                                    Message to the owner (optional)
                                    <textarea name="message" rows="3" maxlength="1000"
                                              placeholder="Introduce yourself and suggest a pickup time."></textarea>
                                </label>
                                <button class="button button-block" type="submit">Send request</button>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </section>
            </c:otherwise>
        </c:choose>
    </aside>
</section>

<%@ include file="../fragments/footer.jspf" %>
