<%@ include file="../fragments/header.jspf" %>

<section class="section">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Catalogue</p>
            <h1>Browse academic resources</h1>
            <p class="muted">${fn:length(resources)} available listing(s)</p>
        </div>
        <c:if test="${not empty currentUser and currentUser.student and currentUser.verified}">
            <a class="button" href="${ctx}/resources/new">Add a listing</a>
        </c:if>
    </div>

    <form class="filter-panel" method="get" action="${ctx}/resources">
        <div class="filter-grid">
            <label>
                Keyword
                <input type="search" name="keyword" value="${fn:escapeXml(criteria.keyword)}"
                       placeholder="Title, description, course">
            </label>
            <label>
                Category
                <select name="category">
                    <option value="">All categories</option>
                    <c:forEach items="${categories}" var="category">
                        <option value="${fn:escapeXml(category)}"
                                ${criteria.category == category ? 'selected' : ''}>
                            <c:out value="${category}"/>
                        </option>
                    </c:forEach>
                </select>
            </label>
            <label>
                Type
                <select name="resourceType">
                    <option value="">All types</option>
                    <c:forEach items="${resourceTypes}" var="type">
                        <option value="${type}"
                                ${criteria.resourceType == type ? 'selected' : ''}>
                            ${type.label}
                        </option>
                    </c:forEach>
                </select>
            </label>
            <label>
                Exchange
                <select name="exchangeType">
                    <option value="">Any exchange</option>
                    <c:forEach items="${exchangeTypes}" var="exchange">
                        <option value="${exchange}"
                                ${criteria.exchangeType == exchange ? 'selected' : ''}>
                            ${exchange.label}
                        </option>
                    </c:forEach>
                </select>
            </label>
            <label>
                Condition
                <select name="condition">
                    <option value="">Any condition</option>
                    <c:forEach items="${conditions}" var="condition">
                        <option value="${condition}"
                                ${criteria.condition == condition ? 'selected' : ''}>
                            ${condition.label}
                        </option>
                    </c:forEach>
                </select>
            </label>
            <label>
                Course code
                <input type="text" name="courseCode" value="${fn:escapeXml(criteria.courseCode)}"
                       placeholder="e.g. CS301">
            </label>
            <label>
                Pickup location
                <input type="text" name="pickupLocation"
                       value="${fn:escapeXml(criteria.pickupLocation)}"
                       placeholder="e.g. Library">
            </label>
            <label>
                Min price
                <input type="number" name="minPrice" min="0" step="0.01"
                       value="${criteria.minPrice}">
            </label>
            <label>
                Max price
                <input type="number" name="maxPrice" min="0" step="0.01"
                       value="${criteria.maxPrice}">
            </label>
            <label>
                Sort by
                <select name="sort">
                    <option value="">Newest first</option>
                    <option value="priceAsc" ${criteria.sort == 'priceAsc' ? 'selected' : ''}>Price: low to high</option>
                    <option value="priceDesc" ${criteria.sort == 'priceDesc' ? 'selected' : ''}>Price: high to low</option>
                    <option value="condition" ${criteria.sort == 'condition' ? 'selected' : ''}>Best condition first</option>
                </select>
            </label>
        </div>
        <div class="filter-actions">
            <button class="button" type="submit">Apply filters</button>
            <a class="button button-ghost" href="${ctx}/resources">Reset</a>
        </div>
    </form>

    <c:choose>
        <c:when test="${empty resources}">
            <div class="empty-state">
                <h3>No matching resources</h3>
                <p>Try a broader keyword or clear the filters.</p>
                <a class="button button-ghost" href="${ctx}/resources">Clear filters</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="card-grid">
                <c:forEach items="${resources}" var="resource">
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
            <c:if test="${pageCount > 1}">
                <nav class="pagination" aria-label="Catalogue pages">
                    <c:choose>
                        <c:when test="${page > 1}">
                            <a class="button button-ghost"
                               href="${ctx}/resources?${queryBase}page=${page - 1}">Previous</a>
                        </c:when>
                        <c:otherwise>
                            <span class="button button-ghost is-disabled">Previous</span>
                        </c:otherwise>
                    </c:choose>
                    <span class="muted">Page ${page} of ${pageCount} &middot; ${total} listings</span>
                    <c:choose>
                        <c:when test="${page < pageCount}">
                            <a class="button button-ghost"
                               href="${ctx}/resources?${queryBase}page=${page + 1}">Next</a>
                        </c:when>
                        <c:otherwise>
                            <span class="button button-ghost is-disabled">Next</span>
                        </c:otherwise>
                    </c:choose>
                </nav>
            </c:if>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="../fragments/footer.jspf" %>
