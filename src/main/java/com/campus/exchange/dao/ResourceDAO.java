package com.campus.exchange.dao;

import com.campus.exchange.model.Book;
import com.campus.exchange.model.LabEquipment;
import com.campus.exchange.model.Resource;
import com.campus.exchange.model.DailyEssential;
import com.campus.exchange.model.StudyMaterial;
import com.campus.exchange.model.enums.Condition;
import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.ResourceStatus;
import com.campus.exchange.model.enums.ResourceType;
import com.campus.exchange.util.DbConnection;
import com.campus.exchange.util.JdbcUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC access for resource listings. Search filters are appended as
 * parameterized clauses, never as raw strings.
 */
public class ResourceDAO {

    public int insert(Resource resource) throws SQLException {
        try (Connection conn = DbConnection.getConnection()) {
            return insert(conn, resource);
        }
    }

    public int insert(Connection conn, Resource resource) throws SQLException {
        String sql = "INSERT INTO resources (owner_id, title, description, category, "
                + "resource_type, `condition`, exchange_type, price, lending_terms, "
                + "course_code, pickup_location, image_path, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, resource.getOwnerId());
            ps.setString(2, resource.getTitle());
            ps.setString(3, resource.getDescription());
            ps.setString(4, resource.getCategory());
            ps.setString(5, resource.getResourceType().name());
            ps.setString(6, resource.getCondition().name());
            ps.setString(7, resource.getExchangeType().name());
            if (resource.getPrice() == null) {
                ps.setNull(8, java.sql.Types.DECIMAL);
            } else {
                ps.setBigDecimal(8, resource.getPrice());
            }
            ps.setString(9, resource.getLendingTerms());
            ps.setString(10, resource.getCourseCode());
            ps.setString(11, resource.getPickupLocation());
            ps.setString(12, resource.getImagePath());
            ps.setString(13, resource.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    resource.setResourceId(keys.getInt(1));
                }
            }
            return resource.getResourceId();
        }
    }

    public boolean update(Resource resource) throws SQLException {
        String sql = "UPDATE resources SET title = ?, description = ?, category = ?, "
                + "resource_type = ?, `condition` = ?, exchange_type = ?, price = ?, "
                + "lending_terms = ?, course_code = ?, pickup_location = ?, image_path = ?, "
                + "status = ? WHERE resource_id = ? AND owner_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resource.getTitle());
            ps.setString(2, resource.getDescription());
            ps.setString(3, resource.getCategory());
            ps.setString(4, resource.getResourceType().name());
            ps.setString(5, resource.getCondition().name());
            ps.setString(6, resource.getExchangeType().name());
            if (resource.getPrice() == null) {
                ps.setNull(7, java.sql.Types.DECIMAL);
            } else {
                ps.setBigDecimal(7, resource.getPrice());
            }
            ps.setString(8, resource.getLendingTerms());
            ps.setString(9, resource.getCourseCode());
            ps.setString(10, resource.getPickupLocation());
            ps.setString(11, resource.getImagePath());
            ps.setString(12, resource.getStatus().name());
            ps.setInt(13, resource.getResourceId());
            ps.setInt(14, resource.getOwnerId());
            return ps.executeUpdate() == 1;
        }
    }

    public Resource findById(int resourceId) throws SQLException {
        try (Connection conn = DbConnection.getConnection()) {
            return findById(conn, resourceId);
        }
    }

    public Resource findById(Connection conn, int resourceId) throws SQLException {
        String sql = "SELECT * FROM resources WHERE resource_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Resource findByIdForUpdate(Connection conn, int resourceId) throws SQLException {
        String sql = "SELECT * FROM resources WHERE resource_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Resource> search(ResourceSearchCriteria criteria) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM resources WHERE status = 'AVAILABLE'");
        List<Object> params = new ArrayList<>();

        if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
            sql.append(" AND (title LIKE ? OR description LIKE ? OR category LIKE ? "
                    + "OR course_code LIKE ?)");
            String like = "%" + criteria.getKeyword().trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (criteria.getCategory() != null && !criteria.getCategory().isBlank()) {
            sql.append(" AND category = ?");
            params.add(criteria.getCategory().trim());
        }
        if (criteria.getResourceType() != null) {
            sql.append(" AND resource_type = ?");
            params.add(criteria.getResourceType().name());
        }
        if (criteria.getExchangeType() != null) {
            sql.append(" AND exchange_type = ?");
            params.add(criteria.getExchangeType().name());
        }
        if (criteria.getCondition() != null) {
            sql.append(" AND `condition` = ?");
            params.add(criteria.getCondition().name());
        }
        if (criteria.getMinPrice() != null) {
            sql.append(" AND price >= ?");
            params.add(criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            sql.append(" AND price <= ?");
            params.add(criteria.getMaxPrice());
        }
        if (criteria.getCourseCode() != null && !criteria.getCourseCode().isBlank()) {
            sql.append(" AND course_code LIKE ?");
            params.add("%" + criteria.getCourseCode().trim() + "%");
        }
        if (criteria.getPickupLocation() != null && !criteria.getPickupLocation().isBlank()) {
            sql.append(" AND pickup_location LIKE ?");
            params.add("%" + criteria.getPickupLocation().trim() + "%");
        }
        String sort = criteria.getSort() == null ? "" : criteria.getSort();
        sql.append(switch (sort) {
            case "priceAsc" -> " ORDER BY price IS NULL, price ASC, created_at DESC";
            case "priceDesc" -> " ORDER BY price DESC, created_at DESC";
            case "condition" -> " ORDER BY `condition` ASC, created_at DESC";
            default -> " ORDER BY created_at DESC";
        });

        List<Resource> resources = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resources.add(map(rs));
                }
            }
        }
        return resources;
    }

    public List<Resource> findByOwner(int ownerId) throws SQLException {
        String sql = "SELECT * FROM resources WHERE owner_id = ? ORDER BY created_at DESC";
        List<Resource> resources = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resources.add(map(rs));
                }
            }
        }
        return resources;
    }

    public List<Resource> findAll() throws SQLException {
        String sql = "SELECT * FROM resources ORDER BY created_at DESC";
        List<Resource> resources = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resources.add(map(rs));
            }
        }
        return resources;
    }

    public List<String> findDistinctCategories() throws SQLException {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM resources ORDER BY category";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString(1));
            }
        }
        return categories;
    }

    public boolean updateStatus(int resourceId, ResourceStatus status) throws SQLException {
        String sql = "UPDATE resources SET status = ? WHERE resource_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, resourceId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateStatusIfCurrent(Connection conn, int resourceId,
                                         ResourceStatus expected, ResourceStatus next)
            throws SQLException {
        String sql = "UPDATE resources SET status = ? WHERE resource_id = ? AND status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, next.name());
            ps.setInt(2, resourceId);
            ps.setString(3, expected.name());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean existsByOwnerAndTitle(Connection conn, int ownerId, String title)
            throws SQLException {
        String sql = "SELECT 1 FROM resources WHERE owner_id = ? AND title = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            ps.setString(2, title);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int countAll() throws SQLException {
        return count("SELECT COUNT(*) FROM resources");
    }

    public int countByStatus(ResourceStatus status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM resources WHERE status = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int count(String sql) throws SQLException {
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Resource map(ResultSet rs) throws SQLException {
        ResourceType type = ResourceType.valueOf(rs.getString("resource_type"));
        Resource resource = switch (type) {
            case BOOK -> new Book();
            case LAB_EQUIPMENT -> new LabEquipment();
            case STUDY_MATERIAL -> new StudyMaterial();
            case DAILY_ESSENTIAL -> new DailyEssential();
        };
        resource.setResourceId(rs.getInt("resource_id"));
        resource.setOwnerId(rs.getInt("owner_id"));
        resource.setTitle(rs.getString("title"));
        resource.setDescription(rs.getString("description"));
        resource.setCategory(rs.getString("category"));
        resource.setResourceType(type);
        resource.setCondition(Condition.valueOf(rs.getString("condition")));
        resource.setExchangeType(ExchangeType.valueOf(rs.getString("exchange_type")));
        BigDecimal price = JdbcUtil.toBigDecimal(rs, "price");
        resource.setPrice(price);
        resource.setLendingTerms(rs.getString("lending_terms"));
        resource.setCourseCode(rs.getString("course_code"));
        resource.setPickupLocation(rs.getString("pickup_location"));
        resource.setImagePath(rs.getString("image_path"));
        resource.setStatus(ResourceStatus.valueOf(rs.getString("status")));
        resource.setCreatedAt(JdbcUtil.toLocalDateTime(rs.getTimestamp("created_at")));
        resource.setUpdatedAt(JdbcUtil.toLocalDateTime(rs.getTimestamp("updated_at")));
        return resource;
    }
}
