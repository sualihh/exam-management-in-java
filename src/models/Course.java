package models;

import java.time.LocalDateTime;

public class Course {
    private int courseID;
    private String courseName = "";
    private String courseCode = "";
    private int teacherID;
    private String teacherName = "";
    private LocalDateTime createdAt;

    public Course() {}

    public int getCourseID() { return courseID; }
    public void setCourseID(int courseID) { this.courseID = courseID; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public int getTeacherID() { return teacherID; }
    public void setTeacherID(int teacherID) { this.teacherID = teacherID; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return courseName + " (" + courseCode + ")"; }
}
