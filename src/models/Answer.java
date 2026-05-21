package models;

import java.math.BigDecimal;

public class Answer {
    private int answerID;
    private int sessionID;
    private int questionID;
    private String questionText = "";
    private String questionType = "";
    private BigDecimal marks;           // max marks for this question
    private Integer selectedOptionID;   // nullable
    private String selectedOptionText = "";
    private String shortAnswerText = "";
    private BigDecimal manualScore;     // nullable
    private boolean isGraded;

    public Answer() {}

    public int getAnswerID() { return answerID; }
    public void setAnswerID(int answerID) { this.answerID = answerID; }

    public int getSessionID() { return sessionID; }
    public void setSessionID(int sessionID) { this.sessionID = sessionID; }

    public int getQuestionID() { return questionID; }
    public void setQuestionID(int questionID) { this.questionID = questionID; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public BigDecimal getMarks() { return marks; }
    public void setMarks(BigDecimal marks) { this.marks = marks; }

    public Integer getSelectedOptionID() { return selectedOptionID; }
    public void setSelectedOptionID(Integer selectedOptionID) { this.selectedOptionID = selectedOptionID; }

    public String getSelectedOptionText() { return selectedOptionText; }
    public void setSelectedOptionText(String selectedOptionText) { this.selectedOptionText = selectedOptionText; }

    public String getShortAnswerText() { return shortAnswerText; }
    public void setShortAnswerText(String shortAnswerText) { this.shortAnswerText = shortAnswerText; }

    public BigDecimal getManualScore() { return manualScore; }
    public void setManualScore(BigDecimal manualScore) { this.manualScore = manualScore; }

    public boolean isGraded() { return isGraded; }
    public void setGraded(boolean graded) { isGraded = graded; }
}
