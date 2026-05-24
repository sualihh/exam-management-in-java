package views.pages;

import data.ExamSessionRepository;
import models.ExamSession;
import services.AuthService;
import views.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StudentResultsPanel extends JPanel {

    private final ExamSessionRepository repo = new ExamSessionRepository();
    private JPanel cardsPanel;
    private JLabel lblEmpty;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    public StudentResultsPanel() {
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        loadResults();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG);
        header.setBorder(new EmptyBorder(20, 24, 8, 24));
        JLabel title = Theme.heading("My Results");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.add(title, BorderLayout.WEST);

        JButton btnRefresh = Theme.secondaryButton("Refresh");
        header.add(btnRefresh, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        cardsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 16, 16));
        cardsPanel.setBackground(Theme.BG);
        cardsPanel.setBorder(new EmptyBorder(8, 16, 16, 16));

        lblEmpty = new JLabel("You haven't taken any exams yet.");
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

        btnRefresh.addActionListener(e -> loadResults());
    }

    private void loadResults() {
        int userID = AuthService.getCurrentUser().getUserID();
        List<ExamSession> sessions = repo.getByStudent(userID);

        cardsPanel.removeAll();

        if (sessions.isEmpty()) {
            lblEmpty.setVisible(true);
        } else {
            lblEmpty.setVisible(false);
            for (ExamSession s : sessions) cardsPanel.add(buildCard(s));
        }

        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private JPanel buildCard(ExamSession session) {
        String scoreText;
        Color  scoreColor;
        Color  stripColor;

        if (!session.isSubmitted()) {
            scoreText  = "In Progress";
            scoreColor = Theme.ACCENT;
            stripColor = Theme.ACCENT;
        } else if (session.getTotalScore() == null) {
            scoreText  = "Pending";
            scoreColor = Theme.WARNING;
            stripColor = Theme.WARNING;
        } else {
            scoreText  = session.getTotalScore().toPlainString() + " pts";
            scoreColor = session.getTotalScore().signum() > 0 ? Theme.SUCCESS : Theme.ERROR;
            stripColor = scoreColor;
        }

        JPanel card = new JPanel();
        card.setBackground(Theme.CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(280, 200));
        card.setBorder(BorderFactory.createLineBorder(Theme.BORDER_COL, 1));

        // Colored top strip
        JPanel strip = new JPanel();
        strip.setBackground(stripColor);
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        card.add(strip);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(14, 16, 16, 16));

        // Title + score badge row
        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel titleLbl = new JLabel("<html><body style='width:160px'>" + session.getExamTitle() + "</body></html>");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(Theme.TEXT);

        JLabel scoreBadge = new JLabel(scoreText);
        scoreBadge.setFont(Theme.FONT_BOLD);
        scoreBadge.setForeground(scoreColor);
        scoreBadge.setOpaque(true);
        scoreBadge.setBackground(new Color(0x2A, 0x2A, 0x2A));
        scoreBadge.setBorder(new EmptyBorder(4, 10, 4, 10));
        scoreBadge.setHorizontalAlignment(SwingConstants.CENTER);

        titleRow.add(titleLbl,   BorderLayout.CENTER);
        titleRow.add(scoreBadge, BorderLayout.EAST);
        body.add(titleRow);
        body.add(Box.createVerticalStrut(8));

        JLabel startLbl = new JLabel("Taken: " + (session.getStartTime() != null ? session.getStartTime().format(FMT) : "—"));
        startLbl.setFont(Theme.FONT_SMALL);
        startLbl.setForeground(Theme.TEXT_DIM);
        startLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(startLbl);

        if (session.getEndTime() != null) {
            JLabel endLbl = new JLabel("Submitted: " + session.getEndTime().format(FMT));
            endLbl.setFont(Theme.FONT_SMALL);
            endLbl.setForeground(Theme.TEXT_DIM);
            endLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(endLbl);
        }

        if (session.isSubmitted()) {
            body.add(Box.createVerticalStrut(12));
            JButton btnReview = Theme.secondaryButton("REVIEW ANSWERS");
            btnReview.setForeground(Theme.ACCENT);
            btnReview.setAlignmentX(Component.LEFT_ALIGNMENT);
            btnReview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            btnReview.addActionListener(e -> {
                Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
                new ExamReviewDialog(owner, session).setVisible(true);
            });
            body.add(btnReview);
        }

        card.add(body);
        return card;
    }
}
