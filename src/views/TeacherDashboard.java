package views;

import views.pages.*;

import javax.swing.*;

public class TeacherDashboard extends BaseDashboard {

    public TeacherDashboard() {
        super("ExamPlatform — Teacher Dashboard");
        init(); // must be last
    }

    @Override
    protected String getRoleLabel() { return "TEACHER"; }

    @Override
    protected void addNavButtons() {
        JButton btnExams   = addNavButton("  📝  My Exams");
        JButton btnResults = addNavButton("  📊  View Results");
        JButton btnGrade   = addNavButton("  ✏  Grade Answers");

        btnExams.addActionListener(e   -> showPage(new ManageExamsPanel()));
        btnResults.addActionListener(e -> showPage(new ViewResultsPanel()));
        btnGrade.addActionListener(e   -> showPage(new GradeAnswersPanel()));
    }

    @Override
    protected JPanel buildDefaultPage() {
        return new ManageExamsPanel();
    }
}
