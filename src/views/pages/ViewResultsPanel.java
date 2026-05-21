package views.pages;

import data.ExamRepository;
import data.ExamSessionRepository;
import models.Exam;
import models.ExamSession;
import services.AuthService;
import views.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ViewResultsPanel extends JPanel {

    private final ExamRepository        examRepo    = new ExamRepository();
    private final ExamSessionRepository sessionRepo = new ExamSessionRepository();

    private JComboBox<Exam> cmbExams;
    private JTable          table;
    private SessionTableModel tableModel;
    private JButton         btnResetSession;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public ViewResultsPanel() {
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        loadExamFilter();
    }

    private void buildUI() {
        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG);
        header.setBorder(new EmptyBorder(20, 24, 8, 24));
        JLabel title = Theme.heading("View Results");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.add(title, BorderLayout.WEST);

        // ── Filter row ────────────────────────────────────────────────────────
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filterRow.setBackground(Theme.BG);
        filterRow.setBorder(new EmptyBorder(0, 16, 0, 16));

        filterRow.add(Theme.label("Exam:"));
        cmbExams = Theme.comboBox();
        cmbExams.setPreferredSize(new Dimension(320, 36));
        filterRow.add(cmbExams);

        JButton btnRefresh = Theme.secondaryButton("Refresh");
        filterRow.add(btnRefresh);

        // ── Toolbar ───────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(Theme.BG);
        toolbar.setBorder(new EmptyBorder(0, 16, 0, 16));

        JButton btnReview = Theme.infoButton("Review Answers");
        btnResetSession   = Theme.dangerButton("Reset Session");

        // Show Reset only for Admin
        boolean isAdmin = "Admin".equals(AuthService.getCurrentUser().getRoleName());
        btnResetSession.setVisible(isAdmin);

        toolbar.add(btnReview);
        toolbar.add(btnResetSession);

        JPanel top = new JPanel();
        top.setBackground(Theme.BG);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(header);
        top.add(filterRow);
        top.add(toolbar);
        add(top, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────────────────────
        tableModel = new SessionTableModel();
        table = new JTable(tableModel);
        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = Theme.scrollPane(table);
        scroll.setBorder(new EmptyBorder(0, 16, 16, 16));
        add(scroll, BorderLayout.CENTER);

        // ── Events ────────────────────────────────────────────────────────────
        cmbExams.addActionListener(e -> {
            Exam exam = (Exam) cmbExams.getSelectedItem();
            if (exam != null) tableModel.setData(sessionRepo.getByExam(exam.getExamID()));
        });

        btnRefresh.addActionListener(e -> loadExamFilter());

        btnReview.addActionListener(e -> {
            ExamSession session = getSelectedSession();
            if (session == null) { warn("Select a student row first."); return; }
            if (!session.isSubmitted()) { warn("This exam has not been submitted yet."); return; }
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            new ExamReviewDialog(owner, session).setVisible(true);
        });

        btnResetSession.addActionListener(e -> {
            ExamSession session = getSelectedSession();
            if (session == null) { warn("Select a student session first."); return; }
            int confirm = JOptionPane.showConfirmDialog(this,
                "Reset session for \"" + session.getStudentName() + "\" on \"" + session.getExamTitle() + "\"?\n\n" +
                "This will delete their answers and score, allowing them to retake the exam.\n\nThis cannot be undone.",
                "Confirm Reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                sessionRepo.deleteSession(session.getSessionID());
                Exam exam = (Exam) cmbExams.getSelectedItem();
                if (exam != null) tableModel.setData(sessionRepo.getByExam(exam.getExamID()));
                JOptionPane.showMessageDialog(this,
                    session.getStudentName() + " can now retake \"" + session.getExamTitle() + "\".",
                    "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    private void loadExamFilter() {
        String role = AuthService.getCurrentUser().getRoleName();
        List<Exam> exams = role.equals("Admin")
            ? examRepo.getAll()
            : examRepo.getByCourseTeacher(AuthService.getCurrentUser().getUserID());

        cmbExams.removeAllItems();
        for (Exam e : exams) cmbExams.addItem(e);
        if (!exams.isEmpty()) cmbExams.setSelectedIndex(0);
    }

    private ExamSession getSelectedSession() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return tableModel.getRow(row);
    }

    private void warn(String msg) { JOptionPane.showMessageDialog(this, msg, "Notice", JOptionPane.INFORMATION_MESSAGE); }

    // ── Table model ───────────────────────────────────────────────────────────

    static class SessionTableModel extends AbstractTableModel {
        private final String[] cols = {"Student", "Started", "Submitted", "Score", "Status"};
        private List<ExamSession> data = new ArrayList<>();

        void setData(List<ExamSession> data) { this.data = data; fireTableDataChanged(); }
        ExamSession getRow(int row) { return data.get(row); }

        @Override public int getRowCount()    { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int col) { return cols[col]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }

        @Override
        public Object getValueAt(int row, int col) {
            ExamSession s = data.get(row);
            switch (col) {
                case 0: return s.getStudentName();
                case 1: return s.getStartTime() != null ? s.getStartTime().format(FMT) : "";
                case 2: return s.getEndTime()   != null ? s.getEndTime().format(FMT)   : "—";
                case 3: return s.getTotalScore() != null ? s.getTotalScore().toPlainString() : "—";
                case 4: return s.isSubmitted() ? "Submitted" : "In Progress";
                default: return "";
            }
        }
    }
}
