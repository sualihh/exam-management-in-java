package data;

import models.Answer;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnswerRepository {

    /** Saves or updates a student's answer for a question within a session. */
    public void saveAnswer(int sessionID, int questionID, Integer selectedOptionID, String shortAnswerText) {
        // Check if answer already exists
        String checkSql = "SELECT AnswerID FROM Answers WHERE SessionID = ? AND QuestionID = ?";
        String updateSql =
            "UPDATE Answers SET SelectedOptionID = ?, ShortAnswerText = ? " +
            "WHERE SessionID = ? AND QuestionID = ?";
        String insertSql =
            "INSERT INTO Answers (SessionID, QuestionID, SelectedOptionID, ShortAnswerText, IsGraded) " +
            "VALUES (?, ?, ?, ?, 0)";

        try (Connection conn = DB.getConnection()) {
            boolean exists = false;
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setInt(1, sessionID);
                ps.setInt(2, questionID);
                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }

            if (exists) {
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    if (selectedOptionID != null) ps.setInt(1, selectedOptionID);
                    else ps.setNull(1, Types.INTEGER);
                    if (shortAnswerText != null && !shortAnswerText.isEmpty()) ps.setString(2, shortAnswerText);
                    else ps.setNull(2, Types.VARCHAR);
                    ps.setInt(3, sessionID);
                    ps.setInt(4, questionID);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, sessionID);
                    ps.setInt(2, questionID);
                    if (selectedOptionID != null) ps.setInt(3, selectedOptionID);
                    else ps.setNull(3, Types.INTEGER);
                    if (shortAnswerText != null && !shortAnswerText.isEmpty()) ps.setString(4, shortAnswerText);
                    else ps.setNull(4, Types.VARCHAR);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("saveAnswer failed: " + e.getMessage(), e);
        }
    }

    /** Returns all answers for a session (used for grading and results view). */
    public List<Answer> getBySession(int sessionID) {
        String sql =
            "SELECT a.AnswerID, a.SessionID, a.QuestionID, " +
            "       q.QuestionText, q.QuestionType, q.Marks, " +
            "       a.SelectedOptionID, " +
            "       IFNULL(o.OptionText, '') AS SelectedOptionText, " +
            "       IFNULL(a.ShortAnswerText, '') AS ShortAnswerText, " +
            "       a.ManualScore, a.IsGraded " +
            "FROM   Answers a " +
            "JOIN   Questions q ON q.QuestionID = a.QuestionID " +
            "LEFT JOIN Options o ON o.OptionID = a.SelectedOptionID " +
            "WHERE  a.SessionID = ? " +
            "ORDER BY q.OrderIndex";

        List<Answer> list = new ArrayList<>();
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAnswer(rs));
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("getBySession answers failed: " + e.getMessage(), e);
        }
        return list;
    }

    /** Teacher grades a short answer manually. */
    public void gradeShortAnswer(int answerID, BigDecimal score) {
        String sql =
            "UPDATE Answers SET ManualScore = ?, IsGraded = 1 WHERE AnswerID = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, score);
            ps.setInt(2, answerID);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DB.DatabaseException("gradeShortAnswer failed: " + e.getMessage(), e);
        }
    }

    /**
     * Auto-grades MCQ and TF answers. Returns the total score earned.
     * Short answers are excluded — they need manual grading.
     */
    public BigDecimal autoGradeSession(int sessionID) {
        String sql =
            "SELECT a.AnswerID, a.SelectedOptionID, o.IsCorrect, q.Marks, q.QuestionType " +
            "FROM   Answers a " +
            "JOIN   Questions q ON q.QuestionID = a.QuestionID " +
            "LEFT JOIN Options o ON o.OptionID = a.SelectedOptionID " +
            "WHERE  a.SessionID = ? " +
            "  AND  q.QuestionType IN ('MCQ', 'TF')";

        BigDecimal total = BigDecimal.ZERO;
        List<int[]> updates = new ArrayList<>(); // [answerID, score*100 for precision]
        List<BigDecimal> scores = new ArrayList<>();

        try (Connection conn = DB.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, sessionID);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        boolean isCorrect = !rs.wasNull() && rs.getBoolean("IsCorrect");
                        BigDecimal marks = isCorrect ? rs.getBigDecimal("Marks") : BigDecimal.ZERO;
                        total = total.add(marks);
                        updates.add(new int[]{rs.getInt("AnswerID")});
                        scores.add(marks);
                    }
                }
            }

            // Mark auto-graded answers
            String updateSql = "UPDATE Answers SET ManualScore = ?, IsGraded = 1 WHERE AnswerID = ?";
            for (int i = 0; i < updates.size(); i++) {
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setBigDecimal(1, scores.get(i));
                    ps.setInt(2, updates.get(i)[0]);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("autoGradeSession failed: " + e.getMessage(), e);
        }
        return total;
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private static Answer mapAnswer(ResultSet rs) throws SQLException {
        Answer a = new Answer();
        a.setAnswerID(rs.getInt("AnswerID"));
        a.setSessionID(rs.getInt("SessionID"));
        a.setQuestionID(rs.getInt("QuestionID"));
        a.setQuestionText(rs.getString("QuestionText"));
        a.setQuestionType(rs.getString("QuestionType"));
        a.setMarks(rs.getBigDecimal("Marks"));

        int optID = rs.getInt("SelectedOptionID");
        a.setSelectedOptionID(rs.wasNull() ? null : optID);
        a.setSelectedOptionText(rs.getString("SelectedOptionText"));
        a.setShortAnswerText(rs.getString("ShortAnswerText"));

        BigDecimal ms = rs.getBigDecimal("ManualScore");
        a.setManualScore(rs.wasNull() ? null : ms);
        a.setGraded(rs.getBoolean("IsGraded"));
        return a;
    }
}
