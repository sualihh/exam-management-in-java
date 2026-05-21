package data;

import models.Course;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseRepository {

    public List<Course> getAll() {
        String sql =
            "SELECT c.CourseID, c.CourseName, c.CourseCode, c.TeacherID, " +
            "       u.FullName AS TeacherName, c.CreatedAt " +
            "FROM   Courses c " +
            "JOIN   Users u ON u.UserID = c.TeacherID " +
            "ORDER BY c.CourseName";

        List<Course> list = new ArrayList<>();
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapCourse(rs));
        } catch (SQLException e) {
            throw new DB.DatabaseException("getAll courses failed: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Course> getByTeacher(int teacherID) {
        String sql =
            "SELECT c.CourseID, c.CourseName, c.CourseCode, c.TeacherID, " +
            "       u.FullName AS TeacherName, c.CreatedAt " +
            "FROM   Courses c " +
            "JOIN   Users u ON u.UserID = c.TeacherID " +
            "WHERE  c.TeacherID = ? " +
            "ORDER BY c.CourseName";

        List<Course> list = new ArrayList<>();
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teacherID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapCourse(rs));
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("getByTeacher failed: " + e.getMessage(), e);
        }
        return list;
    }

    /** Returns courses a student is enrolled in. */
    public List<Course> getByStudent(int studentID) {
        String sql =
            "SELECT c.CourseID, c.CourseName, c.CourseCode, c.TeacherID, " +
            "       u.FullName AS TeacherName, c.CreatedAt " +
            "FROM   Courses c " +
            "JOIN   Users u ON u.UserID = c.TeacherID " +
            "JOIN   Enrollments e ON e.CourseID = c.CourseID " +
            "WHERE  e.StudentID = ? " +
            "ORDER BY c.CourseName";

        List<Course> list = new ArrayList<>();
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapCourse(rs));
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("getByStudent failed: " + e.getMessage(), e);
        }
        return list;
    }

    public int create(Course course) {
        String sql =
            "INSERT INTO Courses (CourseName, CourseCode, TeacherID) VALUES (?, ?, ?)";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, course.getCourseName());
            ps.setString(2, course.getCourseCode());
            ps.setInt(3, course.getTeacherID());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DB.DatabaseException("create course failed: " + e.getMessage(), e);
        }
        return -1;
    }

    public void update(Course course) {
        String sql =
            "UPDATE Courses SET CourseName = ?, CourseCode = ? WHERE CourseID = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, course.getCourseName());
            ps.setString(2, course.getCourseCode());
            ps.setInt(3, course.getCourseID());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DB.DatabaseException("update course failed: " + e.getMessage(), e);
        }
    }

    public void delete(int courseID) {
        String sql = "DELETE FROM Courses WHERE CourseID = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseID);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DB.DatabaseException("delete course failed: " + e.getMessage(), e);
        }
    }

    public void enrollStudent(int studentID, int courseID) {
        String sql =
            "INSERT IGNORE INTO Enrollments (StudentID, CourseID) VALUES (?, ?)";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentID);
            ps.setInt(2, courseID);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DB.DatabaseException("enrollStudent failed: " + e.getMessage(), e);
        }
    }

    public void unenrollStudent(int studentID, int courseID) {
        String sql =
            "DELETE FROM Enrollments WHERE StudentID = ? AND CourseID = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentID);
            ps.setInt(2, courseID);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DB.DatabaseException("unenrollStudent failed: " + e.getMessage(), e);
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private static Course mapCourse(ResultSet rs) throws SQLException {
        Course c = new Course();
        c.setCourseID(rs.getInt("CourseID"));
        c.setCourseName(rs.getString("CourseName"));
        c.setCourseCode(rs.getString("CourseCode"));
        c.setTeacherID(rs.getInt("TeacherID"));
        c.setTeacherName(rs.getString("TeacherName"));
        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) c.setCreatedAt(ts.toLocalDateTime());
        return c;
    }
}
