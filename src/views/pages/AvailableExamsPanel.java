package views.pages;

import data.ExamRepository;
import models.Exam;
import services.AuthService;
import views.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class AvailableExamsPanel extends JPanel {

    private final ExamRepository repo = new ExamRepository();
    private JPanel cardsPanel;
    private JLabel lblEmpty;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public AvailableExamsPanel() {
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

        JLabel title = Theme.heading("Available Exams");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.add(title, BorderLayout.WEST);

        JButton btnRefresh = Theme.secondaryButton("Refresh");
        header.add(btnRefresh, BorderLayout.EAST);

        // Welcome message
        JLabel lblWelcome = Theme.label("Hello, " + AuthService.getCurrentUser().getFullName()
            + " — select an exam and click Start when you're ready.");
        lblWelcome.setBorder(new EmptyBorder(0, 24, 8, 24));

        JPanel top = new JPanel();
        top.setBackground(Theme.BG);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(header);
        top.add(lblWelcome);
        add(top, BorderLayout.NORTH);

        // ── Cards area ────────────────────────────────────────────────────────
        cardsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 16, 16));
        cardsPanel.setBackground(Theme.BG);
        cardsPanel.setBorder(new EmptyBorder(8, 16, 16, 16));

        lblEmpty = new JLabel("No exams available right now. Check back later.");
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

        btnRefresh.addActionListener(e -> loadExams());
    }

    private void loadExams() {
        int userID = AuthService.getCurrentUser().getUserID();
        List<Exam> exams = repo.getAvailableForStudent(userID);

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
        long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), exam.getEndDateTime());
        String deadline = daysLeft <= 0 ? "Closes today" : "Closes in " + daysLeft + " day(s)";
        Color deadlineColor = daysLeft <= 1 ? Theme.ERROR : Theme.ACCENT;

        JPanel card = new JPanel();
        card.setBackground(Theme.CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(280, 220));
        card.setBorder(BorderFactory.createLineBorder(Theme.BORDER_COL, 1));

        // Orange top strip
        JPanel strip = new JPanel();
        strip.setBackground(Theme.ACCENT);
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        card.add(strip);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(14, 16, 16, 16));

        // Course chip
        JLabel courseChip = new JLabel(exam.getCourseName());
        courseChip.setFont(new Font("Segoe UI", Font.BOLD, 11));
        courseChip.setForeground(Theme.ACCENT);
        courseChip.setOpaque(true);
        courseChip.setBackground(new Color(0x2A, 0x2A, 0x2A));
        courseChip.setBorder(new EmptyBorder(3, 8, 3, 8));
        courseChip.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(courseChip);
        body.add(Box.createVerticalStrut(10));

        JLabel titleLbl = new JLabel("<html><body style='width:220px'>" + exam.getTitle() + "</body></html>");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(Theme.TEXT);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(titleLbl);
        body.add(Box.createVerticalStrut(10));

        JLabel durationLbl = new JLabel("⏱  " + exam.getDurationMins() + " minutes");
        durationLbl.setFont(Theme.FONT_SMALL);
        durationLbl.setForeground(Theme.TEXT_MUTED);
        durationLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(durationLbl);

        if (exam.getEndDateTime() != null) {
            JLabel endLbl = new JLabel("📅  Ends " + exam.getEndDateTime().format(FMT));
            endLbl.setFont(Theme.FONT_SMALL);
            endLbl.setForeground(deadlineColor);
            endLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(endLbl);
        }

        body.add(Box.createVerticalStrut(12));

        JButton btnStart = Theme.primaryButton("START EXAM  ▶");
        btnStart.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnStart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnStart.putClientProperty("exam", exam);
        btnStart.addActionListener(e -> startExam(exam));
        body.add(btnStart);

        card.add(body);
        return card;
    }

    private void startExam(Exam exam) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Start \"" + exam.getTitle() + "\"?\n\nDuration: " + exam.getDurationMins() + " minutes\n\n" +
            "Once started you cannot exit until you submit or time runs out.",
            "Start Exam", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        ExamWindow examWin = new ExamWindow(exam);
        examWin.setVisible(true);
        examWin.startExam();

        // Close the dashboard
        Window dashboard = SwingUtilities.getWindowAncestor(this);
        if (dashboard != null) dashboard.dispose();
    }
}
