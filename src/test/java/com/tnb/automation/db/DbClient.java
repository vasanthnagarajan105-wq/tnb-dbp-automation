package com.tnb.automation.db;

import com.tnb.automation.config.ConfigManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** Database checks and test-data setup, using plain JDBC. */
public final class DbClient {

    private DbClient() {
    }

    /** The two columns the login tests care about. */
    public record CustomerState(int failedAttempts, boolean locked) {
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(
                ConfigManager.get("db.url"),
                ConfigManager.get("db.user"),
                ConfigManager.get("db.password"));
    }

    /** Same as: SELECT failed_attempts, locked FROM customers WHERE customer_id = ? */
    public static CustomerState customerState(String customerId) {
        String sql = "SELECT failed_attempts, locked FROM customers WHERE customer_id = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Customer not found in DB: " + customerId);
                }
                return new CustomerState(rs.getInt("failed_attempts"), rs.getBoolean("locked"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("DB query failed for customer " + customerId, e);
        }
    }

    /** The two otp_challenges columns the OTP tests care about. */
    public record ChallengeState(int resendCount, boolean used) {
    }

    /** Same as: SELECT resend_count, used FROM otp_challenges WHERE id = ? */
    public static ChallengeState challengeState(String challengeId) {
        String sql = "SELECT resend_count, used FROM otp_challenges WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(challengeId));   // id column is UUID, not text
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("OTP challenge not found in DB: " + challengeId);
                }
                return new ChallengeState(rs.getInt("resend_count"), rs.getBoolean("used"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("DB query failed for challenge " + challengeId, e);
        }
    }

    /** Test-data reset: same as the UPDATE you ran in DBeaver before each test. */
    public static void resetCustomer(String customerId) {
        String sql = "UPDATE customers SET failed_attempts = 0, locked = false, locked_at = NULL "
                + "WHERE customer_id = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new IllegalStateException("Expected to reset 1 customer, but updated " + rows);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("DB reset failed for customer " + customerId, e);
        }
    }
}