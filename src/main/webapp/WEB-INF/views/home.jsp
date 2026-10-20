<%@ include file="fragments/header.jspf" %>

<section class="hero">
    <div class="hero-content">
        <p class="eyebrow">Your campus, shared</p>
        <h1>Find the book, kit, or notes you need.</h1>
        <p class="hero-copy">
            List what you no longer need, request what you do, and track every
            exchange with students on your campus.
        </p>
        <form class="search-bar" method="get" action="${ctx}/resources">
            <input type="search" name="keyword" placeholder="Search by title, course code, or category"
                   aria-label="Search resources">
            <button class="button" type="submit">Search</button>
        </form>
        <c:if test="${empty sessionScope.userId}">
            <p class="hero-note">
                <a href="${ctx}/register">Create a student account</a> to request or list resources.
            </p>
        </c:if>
    </div>
</section>

<section class="section">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Fresh listings</p>
            <h2>Recently added resources</h2>
        </div>
        <a class="button button-ghost" href="${ctx}/resources">Browse all</a>
    </div>

    <c:choose>
        <c:when test="${empty resources}">
            <div class="empty-state">
                <h3>No resources yet</h3>
                <p>The catalogue is waiting for its first listing.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="card-grid">
                <c:forEach items="${resources}" var="resource" end="5">
                    <article class="resource-card">
                        <a class="card-image" href="${ctx}/resources/view?id=${resource.resourceId}">
                            <c:choose>
                                <c:when test="${not empty resource.imagePath}">
                                    <img src="${ctx}/images/${resource.imagePath}"
                                         alt="${fn:escapeXml(resource.title)}" loading="lazy">
                                </c:when>
                                <c:otherwise>
                                    <span class="image-placeholder">
                                        ${resource.resourceType.label}
                                    </span>
                                </c:otherwise>
                            </c:choose>
                        </a>
                        <div class="card-body">
                            <div class="badge-row">
                                <span class="badge badge-exchange">${resource.exchangeType.label}</span>
                                <span class="badge">${resource.condition.label}</span>
                            </div>
                            <h3>
                                <a href="${ctx}/resources/view?id=${resource.resourceId}">
                                    <c:out value="${resource.title}"/>
                                </a>
                            </h3>
                            <p class="muted">${resource.displayDetails()}</p>
                            <div class="card-meta">
                                <span class="price">${resource.priceLabel}</span>
                                <span class="muted">
                                    <c:out value="${resource.pickupLocation}"/>
                                </span>
                            </div>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<section class="section how-it-works">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Simple by design</p>
            <h2>How the exchange works</h2>
        </div>
    </div>
    <div class="step-grid">
        <article class="step-card">
            <span class="step-number">01</span>
            <h3>List</h3>
            <p>Verified students add books, lab equipment, or study material with a pickup location.</p>
        </article>
        <article class="step-card">
            <span class="step-number">02</span>
            <h3>Request</h3>
            <p>Send a request to the owner and agree on the exchange details on campus.</p>
        </article>
        <article class="step-card">
            <span class="step-number">03</span>
            <h3>Exchange</h3>
            <p>Track the handover or loan, mark it complete, and leave a review.</p>
        </article>
    </div>
</section>

<%@ include file="fragments/footer.jspf" %>
