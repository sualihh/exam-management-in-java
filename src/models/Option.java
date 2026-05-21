package models;

public class Option {
    private int optionID;
    private int questionID;
    private String optionText = "";
    private boolean isCorrect;

    public Option() {}

    public int getOptionID() { return optionID; }
    public void setOptionID(int optionID) { this.optionID = optionID; }

    public int getQuestionID() { return questionID; }
    public void setQuestionID(int questionID) { this.questionID = questionID; }

    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }

    @Override
    public String toString() { return optionText; }
}
