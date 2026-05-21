package models;

import java.time.LocalDateTime;

public class Exam {
    private int examID;
    private String title = "";
    private int courseID;
    private String courseName = "";
    private int createdBy;
    private String createdByName = "";
    private int durationMins;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String instructions = "";
    private boolean isPublished;
    private LocalDateTime createdAt;

    public Exam() {}

    public int getExamID() { return examID; }
    public void setExamID(int examID) { this.examID = examID; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getCourseID() { return courseID; }
    public void setCourseID(int courseID) { this.courseID = courseID; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public int getDurationMins() { return durationMins; }
    public void setDurationMins(int durationMins) { this.durationMins = durationMins; }

    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }

    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean published) { isPublished = published; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return title; }
}
