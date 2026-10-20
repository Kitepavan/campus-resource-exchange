package com.campus.exchange.dao;

import com.campus.exchange.model.Review;
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
 * JDBC access for reviews and ratings.
 */
public class ReviewDAO {

    private static final String SELECT_JOINED =
            "SELECT rv.*, u.full_name AS reviewer_name, r.title AS resource_title "
                    + "FROM reviews rv "
                    + "JOIN users u ON rv.reviewer_id = u.user_id "
                    + "JOIN exchange_transactions t ON rv.transaction_id = t.transaction_id "
                    + "JOIN resource_requests rr ON t.request_id = rr.request_id "
                    + "JOIN resources r ON rr.resource_id = r.resource_id ";

    public int insert(Review review) throws SQLException {
        String sql = "INSERT INTO reviews (transaction_id, reviewer_id, rating, comment) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, review.getTransactionId());
            ps.setInt(2, review.getReviewerId());
            ps.setInt(3, review.getRating());
            ps.setString(4, review.getComment());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    review.setReviewId(keys.getInt(1));
                }
            }
            return review.getReviewId();
        }
    }

    public Review findByTransactionId(int transactionId) throws SQLException {
        String sql = SELECT_JOINED + "WHERE rv.transaction_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Review> findForOwner(int ownerId) throws SQLException {
        String sql = SELECT_JOINED
                + "WHERE t.owner_id = ? ORDER BY rv.created_at DESC";
        List<Review> reviews = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reviews.add(map(rs));
                }
            }
        }
        return reviews;
    }

    public List<Review> findAll() throws SQLException {
        String sql = SELECT_JOINED + "ORDER BY rv.created_at DESC";
        List<Review> reviews = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reviews.add(map(rs));
            }
        }
        return reviews;
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM reviews";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Review map(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setReviewId(rs.getInt("review_id"));
        review.setTransactionId(rs.getInt("transaction_id"));
        review.setReviewerId(rs.getInt("reviewer_id"));
        review.setRating(rs.getInt("rating"));
        review.setComment(rs.getString("comment"));
        review.setCreatedAt(JdbcUtil.toLocalDateTime(rs.getTimestamp("created_at")));
        review.setReviewerName(rs.getString("reviewer_name"));
        review.setResourceTitle(rs.getString("resource_title"));
        return review;
    }
}
