package models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExamSession {
    private int sessionID;
    private int studentID;
    private String studentName = "";
    private int examID;
    private String examTitle = "";
    private LocalDateTime startTime;
    private LocalDateTime endTime;   // nullable
    private boolean isSubmitted;
    private BigDecimal totalScore;   // nullable

    public ExamSession() {}

    public int getSessionID() { return sessionID; }
    public void setSessionID(int sessionID) { this.sessionID = sessionID; }

    public int getStudentID() { return studentID; }
    public void setStudentID(int studentID) { this.studentID = studentID; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public int getExamID() { return examID; }
    public void setExamID(int examID) { this.examID = examID; }

    public String getExamTitle() { return examTitle; }
    public void setExamTitle(String examTitle) { this.examTitle = examTitle; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public boolean isSubmitted() { return isSubmitted; }
    public void setSubmitted(boolean submitted) { isSubmitted = submitted; }

    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }

    @Override
    public String toString() { return studentName + " — " + examTitle; }
}
