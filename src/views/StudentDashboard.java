package views;

import views.pages.*;

import javax.swing.*;

public class StudentDashboard extends BaseDashboard {

    public StudentDashboard() {
        super("ExamPlatform — Student Dashboard");
        init(); // must be last
    }

    @Override
    protected String getRoleLabel() { return "STUDENT"; }

    @Override
    protected void addNavButtons() {
        JButton btnExams   = addNavButton("  📋  Available Exams");
        JButton btnResults = addNavButton("  📊  My Results");
        JButton btnProfile = addNavButton("  👤  My Profile");

        btnExams.addActionListener(e   -> showPage(new AvailableExamsPanel()));
        btnResults.addActionListener(e -> showPage(new StudentResultsPanel()));
        btnProfile.addActionListener(e -> showPage(new StudentProfilePanel()));
    }

    @Override
    protected JPanel buildDefaultPage() {
        return new AvailableExamsPanel();
    }
}
