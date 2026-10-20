<%@ include file="../fragments/header.jspf" %>

<section class="section narrow">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Review</p>
            <h1>Review this exchange</h1>
            <p class="muted"><c:out value="${transaction.resourceTitle}"/></p>
        </div>
    </div>

    <form class="panel form-stack" method="post" action="${ctx}/reviews/new">
        <input type="hidden" name="transactionId" value="${transaction.transactionId}">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <label>
            Rating
            <select name="rating" required>
                <option value="">Select a rating</option>
                <option value="5">5 - Excellent</option>
                <option value="4">4 - Very good</option>
                <option value="3">3 - Good</option>
                <option value="2">2 - Fair</option>
                <option value="1">1 - Poor</option>
            </select>
        </label>
        <label>
            Comment (optional)
            <textarea name="comment" rows="4" maxlength="1000"
                      placeholder="How was the exchange?"></textarea>
        </label>
        <div class="form-actions">
            <button class="button" type="submit">Submit review</button>
            <a class="button button-ghost"
               href="${ctx}/transactions/view?id=${transaction.transactionId}">Cancel</a>
        </div>
    </form>
</section>

<%@ include file="../fragments/footer.jspf" %>
