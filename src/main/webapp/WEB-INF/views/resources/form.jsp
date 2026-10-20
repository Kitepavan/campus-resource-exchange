<%@ include file="../fragments/header.jspf" %>
<c:set var="editing" value="${not empty resource.resourceId}"/>

<section class="section narrow">
    <div class="section-heading">
        <div>
            <p class="eyebrow">${editing ? 'Update' : 'New listing'}</p>
            <h1>${editing ? 'Edit resource' : 'Add a resource'}</h1>
            <p class="muted">
                Required fields are marked below. Listings are visible to the whole campus.
            </p>
        </div>
    </div>

    <form class="panel form-stack" method="post" action="${ctx}${formAction}"
          enctype="multipart/form-data">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">

        <label>
            Title
            <input type="text" name="title" required minlength="3" maxlength="150"
                   value="${fn:escapeXml(resource.title)}">
        </label>

        <label>
            Description
            <textarea name="description" rows="5" required minlength="10" maxlength="5000"><c:out value="${resource.description}"/></textarea>
        </label>

        <div class="form-row">
            <label>
                Resource type
                <select name="resourceType" required>
                    <option value="">Select type</option>
                    <c:forEach items="${resourceTypes}" var="type">
                        <option value="${type}"
                                ${resource.resourceType == type ? 'selected' : ''}>
                            ${type.label}
                        </option>
                    </c:forEach>
                </select>
            </label>
            <label>
                Category
                <input type="text" name="category" required maxlength="80"
                       placeholder="e.g. Computer Science"
                       value="${fn:escapeXml(resource.category)}">
            </label>
        </div>

        <div class="form-row">
            <label>
                Condition
                <select name="condition" required>
                    <option value="">Select condition</option>
                    <c:forEach items="${conditions}" var="condition">
                        <option value="${condition}"
                                ${resource.condition == condition ? 'selected' : ''}>
                            ${condition.label}
                        </option>
                    </c:forEach>
                </select>
            </label>
            <label>
                Exchange type
                <select name="exchangeType" id="exchangeType" required>
                    <option value="">Select exchange</option>
                    <c:forEach items="${exchangeTypes}" var="exchange">
                        <option value="${exchange}"
                                ${resource.exchangeType == exchange ? 'selected' : ''}>
                            ${exchange.label}
                        </option>
                    </c:forEach>
                </select>
            </label>
        </div>

        <div class="form-row" id="priceRow">
            <label>
                Price
                <input type="number" name="price" min="0" step="0.01" max="1000000"
                       value="${resource.price}">
                <span class="field-hint">Required when selling. Donations are free.</span>
            </label>
            <label>
                Course code
                <input type="text" name="courseCode" maxlength="20"
                       value="${fn:escapeXml(resource.courseCode)}">
            </label>
        </div>

        <label id="lendingRow">
            Lending terms
            <textarea name="lendingTerms" rows="3" maxlength="500"
                      placeholder="Loan period, return location, and condition expectations."><c:out value="${resource.lendingTerms}"/></textarea>
            <span class="field-hint">Required when lending.</span>
        </label>

        <div class="form-row">
            <label>
                Campus pickup location
                <input type="text" name="pickupLocation" required maxlength="150"
                       value="${fn:escapeXml(resource.pickupLocation)}">
            </label>
            <label>
                Image (optional)
                <input type="file" name="image" accept="image/jpeg,image/png,image/gif">
                <span class="field-hint">JPEG, PNG, or GIF; maximum 5 MB.</span>
            </label>
        </div>

        <c:if test="${not empty resource.imagePath}">
            <div class="current-image">
                <span class="muted">Current image</span>
                <img src="${ctx}/images/${resource.imagePath}"
                     alt="Current listing image">
            </div>
        </c:if>

        <div class="form-actions">
            <button class="button" type="submit">
                ${editing ? 'Save changes' : 'Create listing'}
            </button>
            <c:choose>
                <c:when test="${editing}">
                    <a class="button button-ghost"
                       href="${ctx}/resources/view?id=${resource.resourceId}">Cancel</a>
                </c:when>
                <c:otherwise>
                    <a class="button button-ghost" href="${ctx}/resources">Cancel</a>
                </c:otherwise>
            </c:choose>
        </div>
    </form>
</section>

<%@ include file="../fragments/footer.jspf" %>
