package data;

import models.Option;
import models.Question;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class QuestionRepository {

    /** Returns all questions for an exam, with their options loaded. */
    public List<Question> getByExam(int examID) {
        String qSql =
            "SELECT QuestionID, ExamID, QuestionText, QuestionType, Marks, OrderIndex " +
            "FROM   Questions " +
            "WHERE  ExamID = ? " +
            "ORDER BY OrderIndex";

        String oSql =
            "SELECT o.OptionID, o.QuestionID, o.OptionText, o.IsCorrect " +
            "FROM   Options o " +
            "JOIN   Questions q ON q.QuestionID = o.QuestionID " +
            "WHERE  q.ExamID = ?";

        List<Question> questions = new ArrayList<>();
        try (Connection conn = DB.getConnection()) {
            // Load questions
            try (PreparedStatement ps = conn.prepareStatement(qSql)) {
                ps.setInt(1, examID);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) questions.add(mapQuestion(rs));
                }
            }

            // Load all options for this exam in one query, then attach
            Map<Integer, List<Option>> optionMap = new HashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(oSql)) {
                ps.setInt(1, examID);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Option opt = mapOption(rs);
                        optionMap.computeIfAbsent(opt.getQuestionID(), k -> new ArrayList<>()).add(opt);
                    }
                }
            }

            for (Question q : questions) {
                List<Option> opts = optionMap.get(q.getQuestionID());
                if (opts != null) q.setOptions(opts);
            }

        } catch (SQLException e) {
            throw new DB.DatabaseException("getByExam questions failed: " + e.getMessage(), e);
        }
        return questions;
    }

    public int createQuestion(Question question) {
        String sql =
            "INSERT INTO Questions (ExamID, QuestionText, QuestionType, Marks, OrderIndex) " +
            "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, question.getExamID());
            ps.setString(2, question.getQuestionText());
            ps.setString(3, question.getQuestionType());
            ps.setBigDecimal(4, question.getMarks());
            ps.setInt(5, question.getOrderIndex());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("createQuestion failed: " + e.getMessage(), e);
        }
        return -1;
    }

    public void updateQuestion(Question question) {
        String sql =
            "UPDATE Questions SET QuestionText = ?, QuestionType = ?, Marks = ?, OrderIndex = ? " +
            "WHERE QuestionID = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, question.getQuestionText());
            ps.setString(2, question.getQuestionType());
            ps.setBigDecimal(3, question.getMarks());
            ps.setInt(4, question.getOrderIndex());
            ps.setInt(5, question.getQuestionID());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DB.DatabaseException("updateQuestion failed: " + e.getMessage(), e);
        }
    }

    public void deleteQuestion(int questionID) {
        String[] steps = {
            "DELETE FROM Answers  WHERE QuestionID = ?",
            "DELETE FROM Options  WHERE QuestionID = ?",
            "DELETE FROM Questions WHERE QuestionID = ?"
        };

        try (Connection conn = DB.getConnection()) {
            for (String sql : steps) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, questionID);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("deleteQuestion failed: " + e.getMessage(), e);
        }
    }

    public int addOption(Option option) {
        String sql =
            "INSERT INTO Options (QuestionID, OptionText, IsCorrect) VALUES (?, ?, ?)";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, option.getQuestionID());
            ps.setString(2, option.getOptionText());
            ps.setBoolean(3, option.isCorrect());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("addOption failed: " + e.getMessage(), e);
        }
        return -1;
    }

    public void deleteOptionsForQuestion(int questionID) {
        String sql = "DELETE FROM Options WHERE QuestionID = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionID);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DB.DatabaseException("deleteOptionsForQuestion failed: " + e.getMessage(), e);
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private static Question mapQuestion(ResultSet rs) throws SQLException {
        Question q = new Question();
        q.setQuestionID(rs.getInt("QuestionID"));
        q.setExamID(rs.getInt("ExamID"));
        q.setQuestionText(rs.getString("QuestionText"));
        q.setQuestionType(rs.getString("QuestionType"));
        q.setMarks(rs.getBigDecimal("Marks"));
        q.setOrderIndex(rs.getInt("OrderIndex"));
        return q;
    }

    private static Option mapOption(ResultSet rs) throws SQLException {
        Option o = new Option();
        o.setOptionID(rs.getInt("OptionID"));
        o.setQuestionID(rs.getInt("QuestionID"));
        o.setOptionText(rs.getString("OptionText"));
        o.setCorrect(rs.getBoolean("IsCorrect"));
        return o;
    }
}
