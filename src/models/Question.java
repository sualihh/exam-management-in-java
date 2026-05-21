package models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * QuestionType values: "MCQ", "TF", "SHORT"
 */
public class Question {
    private int questionID;
    private int examID;
    private String questionText = "";
    private String questionType = "";   // MCQ | TF | SHORT
    private BigDecimal marks;
    private int orderIndex;
    private List<Option> options = new ArrayList<>();

    public Question() {}

    public int getQuestionID() { return questionID; }
    public void setQuestionID(int questionID) { this.questionID = questionID; }

    public int getExamID() { return examID; }
    public void setExamID(int examID) { this.examID = examID; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public BigDecimal getMarks() { return marks; }
    public void setMarks(BigDecimal marks) { this.marks = marks; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public List<Option> getOptions() { return options; }
    public void setOptions(List<Option> options) { this.options = options; }

    @Override
    public String toString() { return questionText; }
}
