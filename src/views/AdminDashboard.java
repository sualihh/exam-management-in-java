package views;

import views.pages.*;

import javax.swing.*;

public class AdminDashboard extends BaseDashboard {

    public AdminDashboard() {
        super("ExamPlatform — Admin Dashboard");
        init(); // must be last
    }

    @Override
    protected String getRoleLabel() { return "ADMINISTRATOR"; }

    @Override
    protected void addNavButtons() {
        JButton btnUsers   = addNavButton("  👥  Manage Users");
        JButton btnCourses = addNavButton("  📚  Manage Courses");
        JButton btnExams   = addNavButton("  📝  Manage Exams");
        JButton btnResults = addNavButton("  📊  View Results");

        btnUsers.addActionListener(e   -> showPage(new ManageUsersPanel()));
        btnCourses.addActionListener(e -> showPage(new ManageCoursesPanel()));
        btnExams.addActionListener(e   -> showPage(new ManageExamsPanel()));
        btnResults.addActionListener(e -> showPage(new ViewResultsPanel()));
    }

    @Override
    protected JPanel buildDefaultPage() {
        return new ManageUsersPanel();
    }
}
