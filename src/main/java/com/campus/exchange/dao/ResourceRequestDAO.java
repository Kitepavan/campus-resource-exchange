package com.campus.exchange.dao;

import com.campus.exchange.model.ResourceRequest;
import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.RequestStatus;
import com.campus.exchange.util.DbConnection;
import com.campus.exchange.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC access for resource requests.
 */
public class ResourceRequestDAO {

    private static final String SELECT_JOINED =
            "SELECT rr.*, r.title AS resource_title, r.owner_id AS owner_id, "
                    + "r.exchange_type AS exchange_type, "
                    + "owner.full_name AS owner_name, "
                    + "requester.full_name AS requester_name "
                    + "FROM resource_requests rr "
                    + "JOIN resources r ON rr.resource_id = r.resource_id "
                    + "JOIN users owner ON r.owner_id = owner.user_id "
                    + "JOIN users requester ON rr.requester_id = requester.user_id ";

    public int insert(Connection conn, ResourceRequest request) throws SQLException {
        String sql = "INSERT INTO resource_requests "
                + "(resource_id, requester_id, message, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, request.getResourceId());
            ps.setInt(2, request.getRequesterId());
            ps.setString(3, request.getMessage());
            ps.setString(4, request.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    request.setRequestId(keys.getInt(1));
                }
            }
            return request.getRequestId();
        }
    }

    public ResourceRequest findById(int requestId) throws SQLException {
        String sql = SELECT_JOINED + "WHERE rr.request_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapJoined(rs) : null;
            }
        }
    }

    /**
     * Locks only the request row. Callers that need the resource row lock it
     * separately with ResourceDAO.findByIdForUpdate in the same transaction.
     */
    public ResourceRequest findByIdForUpdate(Connection conn, int requestId) throws SQLException {
        String sql = "SELECT * FROM resource_requests WHERE request_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapBasic(rs) : null;
            }
        }
    }

    public List<ResourceRequest> findOutgoing(int requesterId) throws SQLException {
        String sql = SELECT_JOINED + "WHERE rr.requester_id = ? ORDER BY rr.created_at DESC";
        return queryList(sql, requesterId);
    }

    public List<ResourceRequest> findIncoming(int ownerId) throws SQLException {
        String sql = SELECT_JOINED + "WHERE r.owner_id = ? ORDER BY rr.created_at DESC";
        return queryList(sql, ownerId);
    }

    public List<ResourceRequest> findAll() throws SQLException {
        String sql = SELECT_JOINED + "ORDER BY rr.created_at DESC";
        List<ResourceRequest> requests = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                requests.add(mapJoined(rs));
            }
        }
        return requests;
    }

    private List<ResourceRequest> queryList(String sql, int parameter) throws SQLException {
        List<ResourceRequest> requests = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parameter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapJoined(rs));
                }
            }
        }
        return requests;
    }

    public boolean hasActiveRequest(Connection conn, int resourceId, int requesterId)
            throws SQLException {
        String sql = "SELECT 1 FROM resource_requests WHERE resource_id = ? "
                + "AND requester_id = ? AND status IN ('PENDING', 'ACCEPTED') LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            ps.setInt(2, requesterId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean updateStatusIfCurrent(Connection conn, int requestId,
                                         RequestStatus expected, RequestStatus next)
            throws SQLException {
        String sql = "UPDATE resource_requests SET status = ? "
                + "WHERE request_id = ? AND status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, next.name());
            ps.setInt(2, requestId);
            ps.setString(3, expected.name());
            return ps.executeUpdate() == 1;
        }
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM resource_requests";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countByStatus(RequestStatus status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM resource_requests WHERE status = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private ResourceRequest mapBasic(ResultSet rs) throws SQLException {
        ResourceRequest request = new ResourceRequest();
        request.setRequestId(rs.getInt("request_id"));
        request.setResourceId(rs.getInt("resource_id"));
        request.setRequesterId(rs.getInt("requester_id"));
        request.setMessage(rs.getString("message"));
        request.setStatus(RequestStatus.valueOf(rs.getString("status")));
        request.setCreatedAt(JdbcUtil.toLocalDateTime(rs.getTimestamp("created_at")));
        request.setUpdatedAt(JdbcUtil.toLocalDateTime(rs.getTimestamp("updated_at")));
        return request;
    }

    private ResourceRequest mapJoined(ResultSet rs) throws SQLException {
        ResourceRequest request = mapBasic(rs);
        request.setResourceTitle(rs.getString("resource_title"));
        request.setResourceOwnerId(rs.getInt("owner_id"));
        request.setOwnerName(rs.getString("owner_name"));
        request.setRequesterName(rs.getString("requester_name"));
        request.setExchangeType(ExchangeType.valueOf(rs.getString("exchange_type")));
        return request;
    }
}
