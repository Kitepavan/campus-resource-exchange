package com.campus.exchange.dao;

import com.campus.exchange.model.Transaction;
import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.TransactionStatus;
import com.campus.exchange.util.DbConnection;
import com.campus.exchange.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC access for exchange transactions.
 */
public class TransactionDAO {

    private static final String SELECT_JOINED =
            "SELECT t.*, rr.resource_id AS resource_id, rr.message AS request_message, "
                    + "r.title AS resource_title, "
                    + "owner.full_name AS owner_name, "
                    + "requester.full_name AS requester_name "
                    + "FROM exchange_transactions t "
                    + "JOIN resource_requests rr ON t.request_id = rr.request_id "
                    + "JOIN resources r ON rr.resource_id = r.resource_id "
                    + "JOIN users owner ON t.owner_id = owner.user_id "
                    + "JOIN users requester ON t.requester_id = requester.user_id ";

    public int insert(Connection conn, Transaction transaction) throws SQLException {
        String sql = "INSERT INTO exchange_transactions "
                + "(request_id, owner_id, requester_id, exchange_type, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, transaction.getRequestId());
            ps.setInt(2, transaction.getOwnerId());
            ps.setInt(3, transaction.getRequesterId());
            ps.setString(4, transaction.getExchangeType().name());
            ps.setString(5, transaction.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    transaction.setTransactionId(keys.getInt(1));
                }
            }
            return transaction.getTransactionId();
        }
    }

    public Transaction findById(int transactionId) throws SQLException {
        String sql = SELECT_JOINED + "WHERE t.transaction_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Transaction findByRequestId(int requestId) throws SQLException {
        String sql = SELECT_JOINED + "WHERE t.request_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Transaction> findByParticipant(int userId) throws SQLException {
        String sql = SELECT_JOINED
                + "WHERE t.owner_id = ? OR t.requester_id = ? "
                + "ORDER BY t.created_at DESC";
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transactions.add(map(rs));
                }
            }
        }
        return transactions;
    }

    public List<Transaction> findAll() throws SQLException {
        String sql = SELECT_JOINED + "ORDER BY t.created_at DESC";
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                transactions.add(map(rs));
            }
        }
        return transactions;
    }

    public boolean updateStatus(Connection conn, int transactionId,
                                TransactionStatus expected, TransactionStatus next,
                                Timestamp completedAt, Timestamp returnedAt)
            throws SQLException {
        String sql = "UPDATE exchange_transactions SET status = ?, completed_at = ?, "
                + "returned_at = ? WHERE transaction_id = ? AND status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, next.name());
            ps.setTimestamp(2, completedAt);
            ps.setTimestamp(3, returnedAt);
            ps.setInt(4, transactionId);
            ps.setString(5, expected.name());
            return ps.executeUpdate() == 1;
        }
    }

    public int countAll() throws SQLException {
        return count("SELECT COUNT(*) FROM exchange_transactions");
    }

    public int countByStatus(TransactionStatus status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM exchange_transactions WHERE status = ?";
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

    private Transaction map(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(rs.getInt("transaction_id"));
        transaction.setRequestId(rs.getInt("request_id"));
        transaction.setOwnerId(rs.getInt("owner_id"));
        transaction.setRequesterId(rs.getInt("requester_id"));
        transaction.setExchangeType(ExchangeType.valueOf(rs.getString("exchange_type")));
        transaction.setStatus(TransactionStatus.valueOf(rs.getString("status")));
        transaction.setCreatedAt(JdbcUtil.toLocalDateTime(rs.getTimestamp("created_at")));
        transaction.setCompletedAt(JdbcUtil.toLocalDateTime(rs.getTimestamp("completed_at")));
        transaction.setReturnedAt(JdbcUtil.toLocalDateTime(rs.getTimestamp("returned_at")));
        transaction.setResourceId(rs.getInt("resource_id"));
        transaction.setResourceTitle(rs.getString("resource_title"));
        transaction.setOwnerName(rs.getString("owner_name"));
        transaction.setRequesterName(rs.getString("requester_name"));
        transaction.setRequestMessage(rs.getString("request_message"));
        return transaction;
    }
}
