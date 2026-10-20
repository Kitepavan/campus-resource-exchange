<%@ include file="../fragments/header.jspf" %>

<section class="section narrow">
    <div class="section-heading">
        <div>
            <p class="eyebrow">Administrator</p>
            <h1>Catalogue backup</h1>
            <p class="muted">
                Backups use Java serialization and contain listings only: no users,
                passwords, sessions, or credentials.
            </p>
        </div>
    </div>

    <div class="panel">
        <h2>Create a backup</h2>
        <p class="muted">
            Writes a timestamped <code>.ser</code> file to the configured backup directory.
        </p>
        <form method="post" action="${ctx}/admin/backup/create">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <button class="button" type="submit">Create backup now</button>
        </form>
    </div>

    <div class="panel">
        <h2>Existing backups</h2>
        <c:choose>
            <c:when test="${empty backups}">
                <p class="muted">No backups have been created yet.</p>
            </c:when>
            <c:otherwise>
                <div class="stack">
                    <c:forEach items="${backups}" var="backup">
                        <div class="list-row">
                            <div class="list-row-main">
                                <div>
                                    <strong><c:out value="${backup}"/></strong>
                                    <span class="muted block">
                                        Restore is additive: existing owner/title pairs are skipped.
                                    </span>
                                </div>
                            </div>
                            <div class="list-row-actions">
                                <form method="post" action="${ctx}/admin/backup/restore"
                                      data-confirm="Restore this backup? Existing listings are not deleted.">
                                    <input type="hidden" name="filename" value="${fn:escapeXml(backup)}">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                    <button class="button button-small" type="submit">Restore</button>
                                </form>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="panel notice-panel">
        <h2>Restore behaviour</h2>
        <ul class="plain-list">
            <li>Files are validated with an ObjectInputFilter allowlist.</li>
            <li>Corrupt, incompatible, or disallowed-class files are rejected.</li>
            <li>Every item is validated before the first insert.</li>
            <li>The restore runs in one database transaction and never wipes existing data.</li>
            <li>Reserved or lent listings are restored as available; inactive listings stay inactive.</li>
        </ul>
    </div>
</section>

<%@ include file="../fragments/footer.jspf" %>
