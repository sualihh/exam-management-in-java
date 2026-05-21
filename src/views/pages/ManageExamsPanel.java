package views.pages;

import data.ExamRepository;
import models.Exam;
import services.AuthService;
import views.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ManageExamsPanel extends JPanel {

    private final ExamRepository repo = new ExamRepository();
    private Exam selected = null;

    private JPanel cardsPanel;
    private JLabel lblEmpty;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public ManageExamsPanel() {
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        loadExams();
    }

    private void buildUI() {
        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG);
        header.setBorder(new EmptyBorder(20, 24, 8, 24));
        JLabel title = Theme.heading("Manage Exams");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.add(title, BorderLayout.WEST);

        // ── Toolbar ───────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(Theme.BG);
        toolbar.setBorder(new EmptyBorder(0, 16, 0, 16));

        JButton btnAdd       = Theme.primaryButton("+ Add Exam");
        JButton btnEdit      = Theme.infoButton("Edit");
        JButton btnQuestions = Theme.secondaryButton("Questions");
        JButton btnDelete    = Theme.dangerButton("Delete");
        JButton btnRefresh   = Theme.secondaryButton("Refresh");

        toolbar.add(btnAdd);
        toolbar.add(btnEdit);
        toolbar.add(btnQuestions);
        toolbar.add(btnDelete);
        toolbar.add(btnRefresh);

        JPanel top = new JPanel();
        top.setBackground(Theme.BG);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(header);
        top.add(toolbar);
        add(top, BorderLayout.NORTH);

        // ── Cards area ────────────────────────────────────────────────────────
        cardsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 16, 16));
        cardsPanel.setBackground(Theme.BG);
        cardsPanel.setBorder(new EmptyBorder(8, 16, 16, 16));

        lblEmpty = new JLabel("No exams found. Click '+ Add Exam' to create one.");
        lblEmpty.setFont(Theme.FONT_BODY);
        lblEmpty.setForeground(Theme.TEXT_MUTED);
        lblEmpty.setHorizontalAlignment(SwingConstants.CENTER);
        lblEmpty.setVisible(false);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(Theme.BG);
        centerWrapper.add(cardsPanel, BorderLayout.NORTH);
        centerWrapper.add(lblEmpty,   BorderLayout.CENTER);

        JScrollPane scroll = Theme.scrollPane(centerWrapper);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG);
        add(scroll, BorderLayout.CENTER);

        // ── Events ────────────────────────────────────────────────────────────
        btnAdd.addActionListener(e -> {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            ExamFormDialog dlg = new ExamFormDialog(owner);
            dlg.setVisible(true);
            if (dlg.isSaved()) loadExams();
        });

        btnEdit.addActionListener(e -> {
            if (selected == null) { warn("Click an exam card first."); return; }
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            ExamFormDialog dlg = new ExamFormDialog(owner, selected);
            dlg.setVisible(true);
            if (dlg.isSaved()) loadExams();
        });

        btnQuestions.addActionListener(e -> {
            if (selected == null) { warn("Click an exam card first."); return; }
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            new ManageQuestionsDialog(owner, selected).setVisible(true);
            loadExams();
        });

        btnDelete.addActionListener(e -> {
            if (selected == null) { warn("Click an exam card first."); return; }
            int confirm = JOptionPane.showConfirmDialog(this,
                "Delete \"" + selected.getTitle() + "\"? All questions and results will be lost.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                try { repo.delete(selected.getExamID()); loadExams(); }
                catch (Exception ex) { error("Delete failed: " + ex.getMessage()); }
            }
        });

        btnRefresh.addActionListener(e -> loadExams());
    }

    private void loadExams() {
        String role = AuthService.getCurrentUser().getRoleName();
        List<Exam> exams = role.equals("Admin")
            ? repo.getAll()
            : repo.getByCourseTeacher(AuthService.getCurrentUser().getUserID());

        selected = null;
        cardsPanel.removeAll();

        if (exams.isEmpty()) {
            lblEmpty.setVisible(true);
        } else {
            lblEmpty.setVisible(false);
            for (Exam exam : exams) cardsPanel.add(buildCard(exam));
        }

        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private JPanel buildCard(Exam exam) {
        boolean published = exam.isPublished();
        Color stripColor  = published ? Theme.ACCENT : Theme.TEXT_DIM;
        Color statusColor = published ? Theme.SUCCESS : Theme.TEXT_MUTED;

        JPanel card = new JPanel();
        card.setBackground(Theme.CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(280, 180));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER_COL, 1),
            new EmptyBorder(0, 0, 14, 0)
        ));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Top strip
        JPanel strip = new JPanel();
        strip.setBackground(stripColor);
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        card.add(strip);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(12, 16, 0, 16));

        // Course chip + status row
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel courseChip = new JLabel(exam.getCourseName());
        courseChip.setFont(new Font("Segoe UI", Font.BOLD, 10));
        courseChip.setForeground(Theme.ACCENT);
        courseChip.setOpaque(true);
        courseChip.setBackground(new Color(0x2A, 0x2A, 0x2A));
        courseChip.setBorder(new EmptyBorder(2, 6, 2, 6));

        JLabel statusBadge = new JLabel(published ? "Published" : "Draft");
        statusBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        statusBadge.setForeground(statusColor);
        statusBadge.setOpaque(true);
        statusBadge.setBackground(new Color(0x2A, 0x2A, 0x2A));
        statusBadge.setBorder(new EmptyBorder(2, 6, 2, 6));

        topRow.add(courseChip);
        topRow.add(statusBadge);
        body.add(topRow);
        body.add(Box.createVerticalStrut(8));

        JLabel titleLbl = new JLabel("<html><body style='width:220px'>" + exam.getTitle() + "</body></html>");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(Theme.TEXT);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(titleLbl);
        body.add(Box.createVerticalStrut(8));

        JLabel durationLbl = new JLabel("⏱  " + exam.getDurationMins() + " min");
        durationLbl.setFont(Theme.FONT_SMALL);
        durationLbl.setForeground(Theme.TEXT_MUTED);
        durationLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(durationLbl);

        if (exam.getStartDateTime() != null && exam.getEndDateTime() != null) {
            JLabel dateLbl = new JLabel("📅  " + exam.getStartDateTime().format(FMT)
                + " – " + exam.getEndDateTime().format(FMT));
            dateLbl.setFont(Theme.FONT_SMALL);
            dateLbl.setForeground(Theme.TEXT_MUTED);
            dateLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(dateLbl);
        }

        card.add(body);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                selectCard(exam, card);
            }
        });

        return card;
    }

    private void selectCard(Exam exam, JPanel card) {
        for (Component c : cardsPanel.getComponents()) {
            if (c instanceof JPanel) {
                ((JPanel) c).setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER_COL, 1),
                    new EmptyBorder(0, 0, 14, 0)
                ));
            }
        }
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.ACCENT, 2),
            new EmptyBorder(0, 0, 14, 0)
        ));
        selected = exam;
    }

    private void warn(String msg)  { JOptionPane.showMessageDialog(this, msg, "Notice", JOptionPane.INFORMATION_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error",  JOptionPane.ERROR_MESSAGE); }
}
