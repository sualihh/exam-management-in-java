package data;

import models.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    public User getByUsername(String username) {
        String sql =
            "SELECT u.UserID, u.FullName, u.Username, u.PasswordHash, " +
            "       u.RoleID, r.RoleName, u.IsActive, u.MustChangePassword, u.CreatedAt " +
            "FROM   Users u " +
            "JOIN   Roles r ON r.RoleID = u.RoleID " +
            "WHERE  u.Username = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("getByUsername failed: " + e.getMessage(), e);
        }
        return null;
    }

    public User getByID(int userID) {
        String sql =
            "SELECT u.UserID, u.FullName, u.Username, u.PasswordHash, " +
            "       u.RoleID, r.RoleName, u.IsActive, u.MustChangePassword, u.CreatedAt " +
            "FROM   Users u " +
            "JOIN   Roles r ON r.RoleID = u.RoleID " +
            "WHERE  u.UserID = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("getByID failed: " + e.getMessage(), e);
        }
        return null;
    }

    public List<User> getAll() {
        String sql =
            "SELECT u.UserID, u.FullName, u.Username, u.PasswordHash, " +
            "       u.RoleID, r.RoleName, u.IsActive, u.MustChangePassword, u.CreatedAt " +
            "FROM   Users u " +
            "JOIN   Roles r ON r.RoleID = u.RoleID " +
            "ORDER BY u.FullName";

        List<User> list = new ArrayList<>();
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapUser(rs));
        } catch (SQLException e) {
            throw new DB.DatabaseException("getAll users failed: " + e.getMessage(), e);
        }
        return list;
    }

    public List<User> getByRole(String roleName) {
        String sql =
            "SELECT u.UserID, u.FullName, u.Username, u.PasswordHash, " +
            "       u.RoleID, r.RoleName, u.IsActive, u.MustChangePassword, u.CreatedAt " +
            "FROM   Users u " +
            "JOIN   Roles r ON r.RoleID = u.RoleID " +
            "WHERE  r.RoleName = ? " +
            "ORDER BY u.FullName";

        List<User> list = new ArrayList<>();
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("getByRole failed: " + e.getMessage(), e);
        }
        return list;
    }

    /** Creates a new user. Returns the new UserID. */
    public int create(User user) {
        String sql =
            "INSERT INTO Users (FullName, Username, PasswordHash, RoleID, IsActive, MustChangePassword) " +
            "VALUES (?, ?, ?, ?, 1, 1)";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPasswordHash());
            ps.setInt(4, user.getRoleID());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("create user failed: " + e.getMessage(), e);
        }
        return -1;
    }

    public void update(User user) {
        String sql =
            "UPDATE Users SET FullName = ?, IsActive = ? WHERE UserID = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setBoolean(2, user.isActive());
            ps.setInt(3, user.getUserID());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DB.DatabaseException("update user failed: " + e.getMessage(), e);
        }
    }

    public void updatePassword(int userID, String newPasswordHash) {
        String sql =
            "UPDATE Users SET PasswordHash = ?, MustChangePassword = 0 WHERE UserID = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userID);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DB.DatabaseException("updatePassword failed: " + e.getMessage(), e);
        }
    }

    public void delete(int userID) {
        String sql = "DELETE FROM Users WHERE UserID = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userID);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DB.DatabaseException("delete user failed: " + e.getMessage(), e);
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private static User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserID(rs.getInt("UserID"));
        u.setFullName(rs.getString("FullName"));
        u.setUsername(rs.getString("Username"));
        u.setPasswordHash(rs.getString("PasswordHash"));
        u.setRoleID(rs.getInt("RoleID"));
        u.setRoleName(rs.getString("RoleName"));
        u.setActive(rs.getBoolean("IsActive"));
        u.setMustChangePassword(rs.getBoolean("MustChangePassword"));
        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
        return u;
    }
}
