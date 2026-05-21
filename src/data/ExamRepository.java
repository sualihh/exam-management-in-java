package data;

import models.Exam;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamRepository {

    public List<Exam> getAll() {
        String sql =
            "SELECT e.ExamID, e.Title, e.CourseID, c.CourseName, " +
            "       e.CreatedBy, u.FullName AS CreatedByName, " +
            "       e.DurationMins, e.StartDateTime, e.EndDateTime, " +
            "       e.Instructions, e.IsPublished, e.CreatedAt " +
            "FROM   Exams e " +
            "JOIN   Courses c ON c.CourseID = e.CourseID " +
            "JOIN   Users   u ON u.UserID   = e.CreatedBy " +
            "ORDER BY e.StartDateTime DESC";
        return query(sql, null);
    }

    public List<Exam> getByTeacher(int teacherID) {
        String sql =
            "SELECT e.ExamID, e.Title, e.CourseID, c.CourseName, " +
            "       e.CreatedBy, u.FullName AS CreatedByName, " +
            "       e.DurationMins, e.StartDateTime, e.EndDateTime, " +
            "       e.Instructions, e.IsPublished, e.CreatedAt " +
            "FROM   Exams e " +
            "JOIN   Courses c ON c.CourseID = e.CourseID " +
            "JOIN   Users   u ON u.UserID   = e.CreatedBy " +
            "WHERE  e.CreatedBy = ? " +
            "ORDER BY e.StartDateTime DESC";
        return query(sql, ps -> ps.setInt(1, teacherID));
    }

    /** Returns published exams available to a student (enrolled + within schedule). */
    public List<Exam> getAvailableForStudent(int studentID) {
        String sql =
            "SELECT e.ExamID, e.Title, e.CourseID, c.CourseName, " +
            "       e.CreatedBy, u.FullName AS CreatedByName, " +
            "       e.DurationMins, e.StartDateTime, e.EndDateTime, " +
            "       e.Instructions, e.IsPublished, e.CreatedAt " +
            "FROM   Exams e " +
            "JOIN   Courses     c  ON c.CourseID  = e.CourseID " +
            "JOIN   Users       u  ON u.UserID    = e.CreatedBy " +
            "JOIN   Enrollments en ON en.CourseID = e.CourseID " +
            "WHERE  en.StudentID   = ? " +
            "  AND  e.IsPublished  = 1 " +
            "  AND  e.StartDateTime <= NOW() " +
            "  AND  e.EndDateTime   >= NOW() " +
            "  AND  NOT EXISTS ( " +
            "       SELECT 1 FROM ExamSessions s " +
            "       WHERE s.StudentID = ? AND s.ExamID = e.ExamID) " +
            "ORDER BY e.EndDateTime";
        return query(sql, ps -> { ps.setInt(1, studentID); ps.setInt(2, studentID); });
    }

    /**
     * Returns exams for courses where this user is the assigned teacher.
     * Covers exams created by admin on the teacher's course.
     */
    public List<Exam> getByCourseTeacher(int teacherID) {
        String sql =
            "SELECT e.ExamID, e.Title, e.CourseID, c.CourseName, " +
            "       e.CreatedBy, u.FullName AS CreatedByName, " +
            "       e.DurationMins, e.StartDateTime, e.EndDateTime, " +
            "       e.Instructions, e.IsPublished, e.CreatedAt " +
            "FROM   Exams e " +
            "JOIN   Courses c ON c.CourseID = e.CourseID " +
            "JOIN   Users   u ON u.UserID   = e.CreatedBy " +
            "WHERE  c.TeacherID = ? " +
            "ORDER BY e.StartDateTime DESC";
        return query(sql, ps -> ps.setInt(1, teacherID));
    }

    public Exam getByID(int examID) {
        String sql =
            "SELECT e.ExamID, e.Title, e.CourseID, c.CourseName, " +
            "       e.CreatedBy, u.FullName AS CreatedByName, " +
            "       e.DurationMins, e.StartDateTime, e.EndDateTime, " +
            "       e.Instructions, e.IsPublished, e.CreatedAt " +
            "FROM   Exams e " +
            "JOIN   Courses c ON c.CourseID = e.CourseID " +
            "JOIN   Users   u ON u.UserID   = e.CreatedBy " +
            "WHERE  e.ExamID = ?";
        List<Exam> list = query(sql, ps -> ps.setInt(1, examID));
        return list.isEmpty() ? null : list.get(0);
    }

    public int create(Exam exam) {
        String sql =
            "INSERT INTO Exams (Title, CourseID, CreatedBy, DurationMins, " +
            "                   StartDateTime, EndDateTime, Instructions, IsPublished) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setExamParams(ps, exam);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("create exam failed: " + e.getMessage(), e);
        }
        return -1;
    }

    public void update(Exam exam) {
        String sql =
            "UPDATE Exams SET Title = ?, CourseID = ?, DurationMins = ?, " +
            "StartDateTime = ?, EndDateTime = ?, Instructions = ?, IsPublished = ? " +
            "WHERE ExamID = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, exam.getTitle());
            ps.setInt(2, exam.getCourseID());
            ps.setInt(3, exam.getDurationMins());
            ps.setTimestamp(4, Timestamp.valueOf(exam.getStartDateTime()));
            ps.setTimestamp(5, Timestamp.valueOf(exam.getEndDateTime()));
            ps.setString(6, exam.getInstructions());
            ps.setBoolean(7, exam.isPublished());
            ps.setInt(8, exam.getExamID());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DB.DatabaseException("update exam failed: " + e.getMessage(), e);
        }
    }

    public void delete(int examID) {
        // Must delete in FK dependency order
        String[] steps = {
            "DELETE a FROM Answers a JOIN ExamSessions s ON s.SessionID = a.SessionID WHERE s.ExamID = ?",
            "DELETE FROM ExamSessions WHERE ExamID = ?",
            "DELETE o FROM Options o JOIN Questions q ON q.QuestionID = o.QuestionID WHERE q.ExamID = ?",
            "DELETE FROM Questions WHERE ExamID = ?",
            "DELETE FROM Exams WHERE ExamID = ?"
        };

        try (Connection conn = DB.getConnection()) {
            for (String sql : steps) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, examID);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("delete exam failed: " + e.getMessage(), e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<Exam> query(String sql, ParamSetter setter) {
        List<Exam> list = new ArrayList<>();
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (setter != null) setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapExam(rs));
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("query exams failed: " + e.getMessage(), e);
        }
        return list;
    }

    private static void setExamParams(PreparedStatement ps, Exam e) throws SQLException {
        ps.setString(1, e.getTitle());
        ps.setInt(2, e.getCourseID());
        ps.setInt(3, e.getCreatedBy());
        ps.setInt(4, e.getDurationMins());
        ps.setTimestamp(5, Timestamp.valueOf(e.getStartDateTime()));
        ps.setTimestamp(6, Timestamp.valueOf(e.getEndDateTime()));
        ps.setString(7, e.getInstructions());
        ps.setBoolean(8, e.isPublished());
    }

    private static Exam mapExam(ResultSet rs) throws SQLException {
        Exam e = new Exam();
        e.setExamID(rs.getInt("ExamID"));
        e.setTitle(rs.getString("Title"));
        e.setCourseID(rs.getInt("CourseID"));
        e.setCourseName(rs.getString("CourseName"));
        e.setCreatedBy(rs.getInt("CreatedBy"));
        e.setCreatedByName(rs.getString("CreatedByName"));
        e.setDurationMins(rs.getInt("DurationMins"));
        Timestamp start = rs.getTimestamp("StartDateTime");
        if (start != null) e.setStartDateTime(start.toLocalDateTime());
        Timestamp end = rs.getTimestamp("EndDateTime");
        if (end != null) e.setEndDateTime(end.toLocalDateTime());
        e.setInstructions(rs.getString("Instructions") == null ? "" : rs.getString("Instructions"));
        e.setPublished(rs.getBoolean("IsPublished"));
        Timestamp created = rs.getTimestamp("CreatedAt");
        if (created != null) e.setCreatedAt(created.toLocalDateTime());
        return e;
    }
}
