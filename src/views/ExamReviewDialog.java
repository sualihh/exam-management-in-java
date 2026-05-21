package views;

import data.AnswerRepository;
import data.QuestionRepository;
import models.Answer;
import models.ExamSession;
import models.Option;
import models.Question;
import services.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.List;

/**
 * ExamReviewDialog — shows a full review of a student's exam session.
 *
 * TEACHER / ADMIN view:
 *   - Shows student name in the title
 *   - For MCQ/TF: shows student's answer AND the correct answer side by side
 *   - For SHORT: shows the student's answer, the score given, and max marks
 *   - No "Your answer" / "Pending" language — uses neutral "Student's answer"
 *   - Always shows the correct answer for MCQ/TF regardless of right/wrong
 *
 * STUDENT view:
 *   - Uses "Your answer" language
 *   - For SHORT: shows "Pending teacher review" ONLY if not yet graded (ManualScore is null)
 *   - Once graded, shows the score given — no more "Pending"
 */
public class ExamReviewDialog extends JDialog {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color GREEN_BG   = new Color(0x1E, 0x3A, 0x2A);
    private static final Color RED_BG     = new Color(0x3A, 0x1E, 0x1E);
    private static final Color NEUTRAL_BG = new Color(0x1E, 0x1E, 0x2E);
    private static final Color GREEN_FG   = new Color(0xA6, 0xE3, 0xA1);
    private static final Color RED_FG     = new Color(0xF3, 0x8B, 0xA8);
    private static final Color BLUE_FG    = new Color(0x89, 0xB4, 0xFA);
    private static final Color ORANGE_FG  = new Color(0xFF, 0xB3, 0x00);
    private static final Color TEXT_FG    = new Color(0xCD, 0xD6, 0xF4);
    private static final Color MUTED_FG   = new Color(0xA6, 0xAD, 0xC8);

    // Whether the viewer is a teacher/admin (true) or a student (false)
    private final boolean isTeacherView;

    public ExamReviewDialog(Frame owner, ExamSession session) {
        super(owner, buildTitle(session), true);
        setSize(820, 700);
        setLocationRelativeTo(owner);
        setResizable(true);

        // Determine view mode from the currently logged-in user's role
        String role = AuthService.getCurrentUser() != null
            ? AuthService.getCurrentUser().getRoleName() : "Student";
        this.isTeacherView = "Admin".equals(role) || "Teacher".equals(role);

        buildUI(session);
    }

    private static String buildTitle(ExamSession session) {
        String role = AuthService.getCurrentUser() != null
            ? AuthService.getCurrentUser().getRoleName() : "Student";
        boolean teacher = "Admin".equals(role) || "Teacher".equals(role);
        if (teacher) {
            return "Review — " + session.getStudentName() + " — " + session.getExamTitle();
        }
        return "My Review — " + session.getExamTitle();
    }

    // ── Build UI ──────────────────────────────────────────────────────────────
    private void buildUI(ExamSession session) {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Theme.BG);

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.SIDEBAR);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Title differs by role
        String titleText = isTeacherView
            ? "Student: " + session.getStudentName() + "   |   " + session.getExamTitle()
            : "Review: " + session.getExamTitle();
        JLabel titleLbl = new JLabel(titleText);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLbl.setForeground(Color.WHITE);
        header.add(titleLbl, BorderLayout.WEST);

        // Stats row (right side of header)
        JPanel stats = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        stats.setOpaque(false);

        JLabel lblCorrect = new JLabel("✔ Correct: 0");
        lblCorrect.setFont(Theme.FONT_BOLD);
        lblCorrect.setForeground(GREEN_FG);

        JLabel lblWrong = new JLabel("✘ Wrong: 0");
        lblWrong.setFont(Theme.FONT_BOLD);
        lblWrong.setForeground(RED_FG);

        String scoreText = session.getTotalScore() != null
            ? session.getTotalScore().toPlainString() : "0";
        JLabel lblScore = new JLabel("Score: " + scoreText);
        lblScore.setFont(Theme.FONT_BOLD);
        lblScore.setForeground(Theme.ACCENT);

        stats.add(lblCorrect);
        stats.add(lblWrong);
        stats.add(lblScore);
        header.add(stats, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // ── Question cards ────────────────────────────────────────────────────
        JPanel reviewPanel = new JPanel();
        reviewPanel.setBackground(Theme.BG);
        reviewPanel.setLayout(new BoxLayout(reviewPanel, BoxLayout.Y_AXIS));
        reviewPanel.setBorder(new EmptyBorder(16, 24, 24, 24));

        AnswerRepository   answerRepo   = new AnswerRepository();
        QuestionRepository questionRepo = new QuestionRepository();

        List<Answer>   answers   = answerRepo.getBySession(session.getSessionID());
        List<Question> questions = questionRepo.getByExam(session.getExamID());

        int correct = 0, wrong = 0;

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            Answer a = answers.stream()
                .filter(x -> x.getQuestionID() == q.getQuestionID())
                .findFirst().orElse(null);

            // Determine correctness for MCQ/TF
            boolean isCorrect = false;
            if (("MCQ".equals(q.getQuestionType()) || "TF".equals(q.getQuestionType())) && a != null) {
                Option correctOpt = q.getOptions().stream()
                    .filter(Option::isCorrect).findFirst().orElse(null);
                isCorrect = a.getSelectedOptionID() != null && correctOpt != null
                    && a.getSelectedOptionID().equals(correctOpt.getOptionID());
                if (isCorrect) correct++; else wrong++;
            }

            JPanel card = isTeacherView
                ? buildTeacherCard(i + 1, q, a, isCorrect)
                : buildStudentCard(i + 1, q, a, isCorrect);

            reviewPanel.add(card);
            reviewPanel.add(Box.createVerticalStrut(12));
        }

        lblCorrect.setText("✔ Correct: " + correct);
        lblWrong.setText("✘ Wrong: " + wrong);

        JScrollPane scroll = Theme.scrollPane(reviewPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG);
        root.add(scroll, BorderLayout.CENTER);

        // ── Footer ────────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
        footer.setBackground(Theme.SIDEBAR);
        JButton btnClose = Theme.secondaryButton("Close");
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── TEACHER card ──────────────────────────────────────────────────────────
    /**
     * Teacher/Admin view of a single question.
     * - MCQ/TF: shows student's answer + correct answer always
     * - SHORT:  shows student's answer + score given / max marks
     * - Neutral language — no "Your answer", no "Pending"
     */
    private JPanel buildTeacherCard(int number, Question q, Answer a, boolean isCorrect) {
        String type = q.getQuestionType();

        // Background and border colour
        Color bg, borderColor;
        if ("SHORT".equals(type)) {
            // For short answers: green if graded, neutral if not yet graded
            boolean graded = a != null && a.getManualScore() != null;
            bg          = graded ? GREEN_BG   : NEUTRAL_BG;
            borderColor = graded ? GREEN_FG   : ORANGE_FG;
        } else {
            bg          = isCorrect ? GREEN_BG : RED_BG;
            borderColor = isCorrect ? GREEN_FG : RED_FG;
        }

        JPanel card = makeCard(bg, borderColor);

        // Question number + text
        card.add(questionHeader(number, q.getQuestionText()));
        card.add(Box.createVerticalStrut(8));

        if ("MCQ".equals(type) || "TF".equals(type)) {
            // Status line
            String statusText = isCorrect ? "✔ Correct" : "✘ Incorrect";
            Color  statusColor = isCorrect ? GREEN_FG : RED_FG;
            card.add(statusLabel(statusText, statusColor));
            card.add(Box.createVerticalStrut(8));

            // Student's answer
            String studentAns = getSelectedOptionText(q, a);
            card.add(answerRow("Student's answer:", studentAns,
                isCorrect ? GREEN_FG : RED_FG));

            // Always show correct answer (teacher needs to see it)
            Option correctOpt = q.getOptions().stream()
                .filter(Option::isCorrect).findFirst().orElse(null);
            if (correctOpt != null) {
                card.add(answerRow("Correct answer:", correctOpt.getOptionText(), GREEN_FG));
            }

        } else { // SHORT
            boolean graded = a != null && a.getManualScore() != null;

            String statusText  = graded ? "Graded" : "Not yet graded";
            Color  statusColor = graded ? GREEN_FG : ORANGE_FG;
            card.add(statusLabel(statusText, statusColor));
            card.add(Box.createVerticalStrut(8));

            // Student's written answer
            String shortAns = (a == null || a.getShortAnswerText() == null
                || a.getShortAnswerText().isBlank()) ? "(No answer provided)" : a.getShortAnswerText();
            card.add(answerRow("Student's answer:", shortAns, MUTED_FG));

            // Score line
            String maxMarks = q.getMarks() != null ? q.getMarks().toPlainString() : "?";
            if (graded) {
                card.add(answerRow("Score:", a.getManualScore().toPlainString() + " / " + maxMarks, BLUE_FG));
            } else {
                card.add(answerRow("Score:", "Not graded  (max: " + maxMarks + ")", ORANGE_FG));
            }
        }

        return card;
    }

    // ── STUDENT card ──────────────────────────────────────────────────────────
    /**
     * Student view of a single question.
     * - MCQ/TF: shows "Your answer" + correct answer if wrong
     * - SHORT:  shows "Your answer" + score if graded, OR "Pending" if NOT yet graded
     */
    private JPanel buildStudentCard(int number, Question q, Answer a, boolean isCorrect) {
        String type = q.getQuestionType();

        Color bg, borderColor;
        if ("SHORT".equals(type)) {
            // Graded = green, not graded = neutral/orange
            boolean graded = a != null && a.getManualScore() != null;
            bg          = graded ? GREEN_BG   : NEUTRAL_BG;
            borderColor = graded ? GREEN_FG   : ORANGE_FG;
        } else {
            bg          = isCorrect ? GREEN_BG : RED_BG;
            borderColor = isCorrect ? GREEN_FG : RED_FG;
        }

        JPanel card = makeCard(bg, borderColor);

        // Question number + text
        card.add(questionHeader(number, q.getQuestionText()));
        card.add(Box.createVerticalStrut(8));

        if ("MCQ".equals(type) || "TF".equals(type)) {
            String statusText  = isCorrect ? "✔ Correct" : "✘ Wrong";
            Color  statusColor = isCorrect ? GREEN_FG : RED_FG;
            card.add(statusLabel(statusText, statusColor));
            card.add(Box.createVerticalStrut(8));

            // Student's own answer
            String yourAns = getSelectedOptionText(q, a);
            card.add(answerRow("Your answer:", yourAns, isCorrect ? GREEN_FG : RED_FG));

            // Show correct answer only if student got it wrong
            if (!isCorrect) {
                Option correctOpt = q.getOptions().stream()
                    .filter(Option::isCorrect).findFirst().orElse(null);
                if (correctOpt != null) {
                    card.add(answerRow("Correct answer:", correctOpt.getOptionText(), GREEN_FG));
                }
            }

        } else { // SHORT
            boolean graded = a != null && a.getManualScore() != null;

            if (graded) {
                // Graded — show score, no "Pending"
                card.add(statusLabel("Graded", GREEN_FG));
                card.add(Box.createVerticalStrut(8));
                String shortAns = (a.getShortAnswerText() == null || a.getShortAnswerText().isBlank())
                    ? "(No answer provided)" : a.getShortAnswerText();
                card.add(answerRow("Your answer:", shortAns, MUTED_FG));
                String maxMarks = q.getMarks() != null ? q.getMarks().toPlainString() : "?";
                card.add(answerRow("Score:", a.getManualScore().toPlainString() + " / " + maxMarks, BLUE_FG));
            } else {
                // Not yet graded — show "Pending"
                card.add(statusLabel("⏳ Pending teacher review", ORANGE_FG));
                card.add(Box.createVerticalStrut(8));
                String shortAns = (a == null || a.getShortAnswerText() == null
                    || a.getShortAnswerText().isBlank()) ? "(No answer provided)" : a.getShortAnswerText();
                card.add(answerRow("Your answer:", shortAns, MUTED_FG));
            }
        }

        return card;
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    /** Creates the base card panel with left-border accent. */
    private JPanel makeCard(Color bg, Color borderColor) {
        JPanel card = new JPanel();
        card.setBackground(bg);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 4, 0, 0, borderColor),
            new EmptyBorder(14, 16, 14, 16)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    /** "Q1.  Question text here" header row. */
    private JPanel questionHeader(int number, String questionText) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel num = new JLabel("Q" + number + ".  ");
        num.setFont(new Font("Segoe UI", Font.BOLD, 14));
        num.setForeground(BLUE_FG);

        JLabel text = new JLabel("<html><body style='width:560px'>" + questionText + "</body></html>");
        text.setFont(Theme.FONT_BODY);
        text.setForeground(TEXT_FG);

        row.add(num);
        row.add(text);
        return row;
    }

    /** Coloured status badge label. */
    private JLabel statusLabel(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_BOLD);
        lbl.setForeground(color);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    /** "Label:  Value" row. */
    private JPanel answerRow(String label, String value, Color valueColor) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.FONT_BOLD);
        lbl.setForeground(MUTED_FG);

        JLabel val = new JLabel("<html><body style='width:420px'>" + value + "</body></html>");
        val.setFont(Theme.FONT_BODY);
        val.setForeground(valueColor);

        row.add(lbl);
        row.add(val);
        return row;
    }

    /** Gets the text of the option the student selected, or "No answer". */
    private String getSelectedOptionText(Question q, Answer a) {
        if (a == null || a.getSelectedOptionID() == null) return "No answer";
        return q.getOptions().stream()
            .filter(o -> o.getOptionID() == a.getSelectedOptionID())
            .map(Option::getOptionText)
            .findFirst().orElse("—");
    }
}
