package data;

import models.ExamSession;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamSessionRepository {

    /** Creates a new session and returns the SessionID. */
    public int startSession(int studentID, int examID) {
        String sql =
            "INSERT INTO ExamSessions (StudentID, ExamID, StartTime, IsSubmitted) " +
            "VALUES (?, ?, NOW(), 0)";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, studentID);
            ps.setInt(2, examID);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("startSession failed: " + e.getMessage(), e);
        }
        return -1;
    }

    public void submitSession(int sessionID, BigDecimal totalScore) {
        String sql =
            "UPDATE ExamSessions SET EndTime = NOW(), IsSubmitted = 1, TotalScore = ? " +
            "WHERE SessionID = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, totalScore);
            ps.setInt(2, sessionID);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DB.DatabaseException("submitSession failed: " + e.getMessage(), e);
        }
    }

    public ExamSession getSession(int studentID, int examID) {
        String sql =
            "SELECT s.SessionID, s.StudentID, u.FullName AS StudentName, " +
            "       s.ExamID, e.Title AS ExamTitle, " +
            "       s.StartTime, s.EndTime, s.IsSubmitted, s.TotalScore " +
            "FROM   ExamSessions s " +
            "JOIN   Users u ON u.UserID = s.StudentID " +
            "JOIN   Exams e ON e.ExamID = s.ExamID " +
            "WHERE  s.StudentID = ? AND s.ExamID = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentID);
            ps.setInt(2, examID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapSession(rs);
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("getSession failed: " + e.getMessage(), e);
        }
        return null;
    }

    public ExamSession getByID(int sessionID) {
        String sql =
            "SELECT s.SessionID, s.StudentID, u.FullName AS StudentName, " +
            "       s.ExamID, e.Title AS ExamTitle, " +
            "       s.StartTime, s.EndTime, s.IsSubmitted, s.TotalScore " +
            "FROM   ExamSessions s " +
            "JOIN   Users u ON u.UserID = s.StudentID " +
            "JOIN   Exams e ON e.ExamID = s.ExamID " +
            "WHERE  s.SessionID = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapSession(rs);
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("getByID session failed: " + e.getMessage(), e);
        }
        return null;
    }

    /** Recalculates and persists TotalScore after teacher grades short answers. */
    public void updateTotalScore(int sessionID) {
        String sql =
            "UPDATE ExamSessions " +
            "SET    TotalScore = ( " +
            "           SELECT IFNULL(SUM(ManualScore), 0) " +
            "           FROM   Answers " +
            "           WHERE  SessionID = ? AND IsGraded = 1 " +
            "       ) " +
            "WHERE  SessionID = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionID);
            ps.setInt(2, sessionID);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DB.DatabaseException("updateTotalScore failed: " + e.getMessage(), e);
        }
    }

    /** Deletes a session and all its answers — allows the student to retake. */
    public void deleteSession(int sessionID) {
        try (Connection conn = DB.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Answers WHERE SessionID = ?")) {
                ps.setInt(1, sessionID);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ExamSessions WHERE SessionID = ?")) {
                ps.setInt(1, sessionID);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("deleteSession failed: " + e.getMessage(), e);
        }
    }

    /** All sessions for a given exam (for teacher results view). */
    public List<ExamSession> getByExam(int examID) {
        String sql =
            "SELECT s.SessionID, s.StudentID, u.FullName AS StudentName, " +
            "       s.ExamID, e.Title AS ExamTitle, " +
            "       s.StartTime, s.EndTime, s.IsSubmitted, s.TotalScore " +
            "FROM   ExamSessions s " +
            "JOIN   Users u ON u.UserID = s.StudentID " +
            "JOIN   Exams e ON e.ExamID = s.ExamID " +
            "WHERE  s.ExamID = ? " +
            "ORDER BY u.FullName";

        List<ExamSession> list = new ArrayList<>();
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapSession(rs));
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("getByExam sessions failed: " + e.getMessage(), e);
        }
        return list;
    }

    /** All sessions for a student (their exam history). */
    public List<ExamSession> getByStudent(int studentID) {
        String sql =
            "SELECT s.SessionID, s.StudentID, u.FullName AS StudentName, " +
            "       s.ExamID, e.Title AS ExamTitle, " +
            "       s.StartTime, s.EndTime, s.IsSubmitted, s.TotalScore " +
            "FROM   ExamSessions s " +
            "JOIN   Users u ON u.UserID = s.StudentID " +
            "JOIN   Exams e ON e.ExamID = s.ExamID " +
            "WHERE  s.StudentID = ? " +
            "ORDER BY s.StartTime DESC";

        List<ExamSession> list = new ArrayList<>();
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapSession(rs));
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("getByStudent sessions failed: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private static ExamSession mapSession(ResultSet rs) throws SQLException {
        ExamSession s = new ExamSession();
        s.setSessionID(rs.getInt("SessionID"));
        s.setStudentID(rs.getInt("StudentID"));
        s.setStudentName(rs.getString("StudentName"));
        s.setExamID(rs.getInt("ExamID"));
        s.setExamTitle(rs.getString("ExamTitle"));
        Timestamp start = rs.getTimestamp("StartTime");
        if (start != null) s.setStartTime(start.toLocalDateTime());
        Timestamp end = rs.getTimestamp("EndTime");
        s.setEndTime(end == null ? null : end.toLocalDateTime());
        s.setSubmitted(rs.getBoolean("IsSubmitted"));
        BigDecimal score = rs.getBigDecimal("TotalScore");
        s.setTotalScore(rs.wasNull() ? null : score);
        return s;
    }
}
