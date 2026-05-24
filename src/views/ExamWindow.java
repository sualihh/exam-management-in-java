package views;

import data.AnswerRepository;
import data.ExamSessionRepository;
import data.QuestionRepository;
import models.Exam;
import models.Option;
import models.Question;
import services.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fullscreen exam window with full lockdown:
 * - Always on top, undecorated, maximized
 * - Blocks close, minimize, and iconify until submitted
 * - Restores focus if student tries to switch away
 * - Countdown timer with auto-submit on expiry
 */
public class ExamWindow extends JFrame {

    private final QuestionRepository    questionRepo = new QuestionRepository();
    private final ExamSessionRepository sessionRepo  = new ExamSessionRepository();
    private final AnswerRepository      answerRepo   = new AnswerRepository();

    private final Exam exam;
    private List<Question> questions = new ArrayList<>();
    private int sessionID;
    private Timer timer;
    private int secondsLeft;
    private boolean submitted = false;

    // Key dispatcher reference — stored so we can remove it after submission
    private KeyEventDispatcher keyBlocker;

    // Per-question answer controls keyed by QuestionID
    private final Map<Integer, List<JRadioButton>> mcqControls   = new HashMap<>();
    private final Map<Integer, List<JRadioButton>> tfControls    = new HashMap<>();
    private final Map<Integer, JTextArea>          shortControls = new HashMap<>();

    // UI
    private JLabel lblTimer;
    private JLabel lblAnswered;
    private JPanel questionsPanel;

    public ExamWindow(Exam exam) {
        super("Exam: " + exam.getTitle());
        this.exam = exam;

        // ── Window setup ──────────────────────────────────────────────────────
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setAlwaysOnTop(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);   // remove title bar so student can't drag/minimize

        // ── Block close ───────────────────────────────────────────────────────
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!submitted) blockAction("close");
            }

            // Block minimize / iconify
            @Override
            public void windowIconified(WindowEvent e) {
                if (!submitted) {
                    // Immediately restore
                    setExtendedState(JFrame.MAXIMIZED_BOTH);
                    toFront();
                    requestFocus();
                    blockAction("minimize");
                }
            }

            // If window loses focus (Alt+Tab, clicking taskbar, etc.) bring it back
            @Override
            public void windowDeactivated(WindowEvent e) {
                if (!submitted) {
                    // Small delay so the dialog can appear on top
                    SwingUtilities.invokeLater(() -> {
                        if (!submitted) {
                            toFront();
                            requestFocus();
                        }
                    });
                }
            }
        });

        // ── Block Alt+F4 and other key combos ─────────────────────────────────
        keyBlocker = e -> {
            if (submitted) return false;
            // Block Alt+F4
            if (e.getID() == KeyEvent.KEY_PRESSED
                    && e.getKeyCode() == KeyEvent.VK_F4
                    && (e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0) {
                blockAction("close");
                return true;
            }
            // Block Windows key
            if (e.getID() == KeyEvent.KEY_PRESSED
                    && (e.getKeyCode() == KeyEvent.VK_WINDOWS)) {
                return true;
            }
            // Block Alt+Tab
            if (e.getID() == KeyEvent.KEY_PRESSED
                    && e.getKeyCode() == KeyEvent.VK_TAB
                    && (e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0) {
                toFront(); requestFocus();
                return true;
            }
            // Block Escape
            if (e.getID() == KeyEvent.KEY_PRESSED
                    && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                return true;
            }
            return false;
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(keyBlocker);

        buildUI();
    }

    /** Shows a warning and forces the window back to front. */
    private void blockAction(String action) {
        setAlwaysOnTop(false);
        JOptionPane.showMessageDialog(null,
            "You cannot " + action + " during an exam.\nPlease submit the exam first.",
            "Exam in Progress", JOptionPane.WARNING_MESSAGE);
        setAlwaysOnTop(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        toFront();
        requestFocus();
    }

    public void startExam() {
        int userID = AuthService.getCurrentUser().getUserID();

        try {
            sessionID = sessionRepo.startSession(userID, exam.getExamID());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Could not start exam: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            submitted = true;
            dispose();
            return;
        }

        questions = questionRepo.getByExam(exam.getExamID());
        if (questions.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "This exam has no questions.", "Empty Exam", JOptionPane.WARNING_MESSAGE);
            submitted = true;
            dispose();
            return;
        }

        secondsLeft = exam.getDurationMins() * 60;
        updateTimerLabel();

        timer = new Timer(1000, e -> tick());
        timer.start();

        buildQuestionCards();
        updateAnsweredCount();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Theme.BG);

        // ── Top bar ───────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Theme.SIDEBAR);
        topBar.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel lblTitle = new JLabel(exam.getTitle());
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        lblTimer = new JLabel("--:--:--");
        lblTimer.setFont(new Font("Consolas", Font.BOLD, 22));
        lblTimer.setForeground(Theme.SUCCESS);

        lblAnswered = Theme.label("Answered: 0 / 0");
        lblAnswered.setForeground(Theme.TEXT_MUTED);

        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        rightBar.setOpaque(false);
        rightBar.add(lblAnswered);
        rightBar.add(lblTimer);

        JButton btnSubmit = Theme.primaryButton("Submit Exam");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 14));
        rightBar.add(btnSubmit);

        topBar.add(lblTitle, BorderLayout.WEST);
        topBar.add(rightBar, BorderLayout.EAST);
        root.add(topBar, BorderLayout.NORTH);

        // ── Questions scroll area ─────────────────────────────────────────────
        questionsPanel = new JPanel();
        questionsPanel.setBackground(Theme.BG);
        questionsPanel.setLayout(new BoxLayout(questionsPanel, BoxLayout.Y_AXIS));
        questionsPanel.setBorder(new EmptyBorder(20, 60, 40, 60));

        JScrollPane scroll = Theme.scrollPane(questionsPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG);
        root.add(scroll, BorderLayout.CENTER);

        setContentPane(root);

        btnSubmit.addActionListener(e -> confirmSubmit());
    }

    private void buildQuestionCards() {
        questionsPanel.removeAll();
        mcqControls.clear();
        tfControls.clear();
        shortControls.clear();

        for (int i = 0; i < questions.size(); i++) {
            questionsPanel.add(buildCard(i + 1, questions.get(i)));
            questionsPanel.add(Box.createVerticalStrut(20));
        }

        questionsPanel.revalidate();
        questionsPanel.repaint();
    }

    private JPanel buildCard(int number, Question q) {
        JPanel card = new JPanel();
        card.setBackground(new Color(0x1E, 0x1E, 0x2E));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER_COL, 1),
            new EmptyBorder(20, 24, 20, 24)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Question header
        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel numLbl = new JLabel("Q" + number + ".  ");
        numLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        numLbl.setForeground(new Color(0x89, 0xB4, 0xFA));

        JLabel qText = new JLabel("<html><body style='width:600px'>" + q.getQuestionText() + "</body></html>");
        qText.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        qText.setForeground(new Color(0xCD, 0xD6, 0xF4));

        headerRow.add(numLbl);
        headerRow.add(qText);
        card.add(headerRow);
        card.add(Box.createVerticalStrut(6));

        JLabel marksLbl = new JLabel(q.getMarks().toPlainString() + " mark(s)");
        marksLbl.setFont(Theme.FONT_SMALL);
        marksLbl.setForeground(Theme.TEXT_MUTED);
        marksLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(marksLbl);
        card.add(Box.createVerticalStrut(14));

        switch (q.getQuestionType()) {
            case "MCQ":   addMcqOptions(card, q);   break;
            case "TF":    addTfOptions(card, q);    break;
            case "SHORT": addShortAnswer(card, q);  break;
        }

        return card;
    }

    private void addMcqOptions(JPanel parent, Question q) {
        List<JRadioButton> radios = new ArrayList<>();
        ButtonGroup group = new ButtonGroup();
        String[] letters = {"A", "B", "C", "D"};

        for (int i = 0; i < q.getOptions().size() && i < 4; i++) {
            Option opt = q.getOptions().get(i);

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            JRadioButton rb = new JRadioButton();
            rb.setOpaque(false);
            rb.setForeground(Theme.TEXT);
            rb.putClientProperty("optionID", opt.getOptionID());
            group.add(rb);
            rb.addActionListener(e -> updateAnsweredCount());

            JLabel letter = new JLabel(letters[i]);
            letter.setFont(Theme.FONT_BOLD);
            letter.setForeground(new Color(0x89, 0xB4, 0xFA));
            letter.setPreferredSize(new Dimension(20, 20));

            JLabel optText = new JLabel(opt.getOptionText());
            optText.setFont(Theme.FONT_BODY);
            optText.setForeground(new Color(0xCD, 0xD6, 0xF4));

            row.add(rb);
            row.add(letter);
            row.add(optText);
            parent.add(row);
            radios.add(rb);
        }

        mcqControls.put(q.getQuestionID(), radios);
    }

    private void addTfOptions(JPanel parent, Question q) {
        List<JRadioButton> radios = new ArrayList<>();
        ButtonGroup group = new ButtonGroup();

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] labels = {"True", "False"};
        Color[] colors  = {new Color(0xA6, 0xE3, 0xA1), new Color(0xF3, 0x8B, 0xA8)};

        for (int i = 0; i < 2; i++) {
            JRadioButton rb = new JRadioButton(labels[i]);
            rb.setOpaque(false);
            rb.setForeground(colors[i]);
            rb.setFont(new Font("Segoe UI", Font.BOLD, 14));
            rb.putClientProperty("label", labels[i]);
            group.add(rb);
            rb.addActionListener(e -> updateAnsweredCount());
            row.add(rb);
            radios.add(rb);
        }

        parent.add(row);
        tfControls.put(q.getQuestionID(), radios);
    }

    private void addShortAnswer(JPanel parent, Question q) {
        JLabel lbl = Theme.label("Your Answer:");
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(lbl);
        parent.add(Box.createVerticalStrut(6));

        JTextArea ta = Theme.textArea();
        ta.setRows(4);
        ta.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { updateAnsweredCount(); }
        });

        JScrollPane sp = Theme.scrollPane(ta);
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(sp);
        shortControls.put(q.getQuestionID(), ta);
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private void tick() {
        secondsLeft--;
        updateTimerLabel();

        if (secondsLeft <= 0) {
            timer.stop();
            setAlwaysOnTop(false);
            JOptionPane.showMessageDialog(null,
                "Time is up! Your exam is being submitted automatically.",
                "Time Up", JOptionPane.INFORMATION_MESSAGE);
            submitExam();
        } else if (secondsLeft <= 60) {
            lblTimer.setForeground(Theme.ERROR);
        } else if (secondsLeft <= 300) {
            lblTimer.setForeground(Theme.WARNING);
        }
    }

    private void updateTimerLabel() {
        int h = secondsLeft / 3600;
        int m = (secondsLeft % 3600) / 60;
        int s = secondsLeft % 60;
        lblTimer.setText(String.format("%02d:%02d:%02d", h, m, s));
    }

    // ── Answered count ────────────────────────────────────────────────────────

    private void updateAnsweredCount() {
        int answered = 0;
        for (Question q : questions) {
            switch (q.getQuestionType()) {
                case "MCQ":
                    List<JRadioButton> mcq = mcqControls.get(q.getQuestionID());
                    if (mcq != null && mcq.stream().anyMatch(JRadioButton::isSelected)) answered++;
                    break;
                case "TF":
                    List<JRadioButton> tf = tfControls.get(q.getQuestionID());
                    if (tf != null && tf.stream().anyMatch(JRadioButton::isSelected)) answered++;
                    break;
                case "SHORT":
                    JTextArea ta = shortControls.get(q.getQuestionID());
                    if (ta != null && !ta.getText().trim().isEmpty()) answered++;
                    break;
            }
        }
        lblAnswered.setText("Answered: " + answered + " / " + questions.size());
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    private void confirmSubmit() {
        int unanswered = countUnanswered();
        String msg = unanswered > 0
            ? "You have " + unanswered + " unanswered question(s).\n\nSubmit anyway?"
            : "Submit the exam? You cannot change answers after submission.";

        setAlwaysOnTop(false);
        int result = JOptionPane.showConfirmDialog(null, msg, "Submit Exam",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (!submitted) setAlwaysOnTop(true);

        if (result == JOptionPane.YES_OPTION) submitExam();
    }

    private int countUnanswered() {
        int n = 0;
        for (Question q : questions) {
            switch (q.getQuestionType()) {
                case "MCQ":
                    List<JRadioButton> mcq = mcqControls.get(q.getQuestionID());
                    if (mcq == null || mcq.stream().noneMatch(JRadioButton::isSelected)) n++;
                    break;
                case "TF":
                    List<JRadioButton> tf = tfControls.get(q.getQuestionID());
                    if (tf == null || tf.stream().noneMatch(JRadioButton::isSelected)) n++;
                    break;
                case "SHORT":
                    JTextArea ta = shortControls.get(q.getQuestionID());
                    if (ta == null || ta.getText().trim().isEmpty()) n++;
                    break;
            }
        }
        return n;
    }

    private void submitExam() {
        submitted = true;
        if (timer != null) timer.stop();

        // Release key blocker so normal keyboard works again
        if (keyBlocker != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .removeKeyEventDispatcher(keyBlocker);
        }

        // Save all answers
        for (Question q : questions) {
            Integer selectedOptionID = null;
            String  shortText        = null;

            switch (q.getQuestionType()) {
                case "MCQ":
                    List<JRadioButton> mcq = mcqControls.get(q.getQuestionID());
                    if (mcq != null) {
                        for (JRadioButton rb : mcq) {
                            if (rb.isSelected()) {
                                selectedOptionID = (Integer) rb.getClientProperty("optionID");
                                break;
                            }
                        }
                    }
                    break;
                case "TF":
                    List<JRadioButton> tf = tfControls.get(q.getQuestionID());
                    if (tf != null) {
                        for (JRadioButton rb : tf) {
                            if (rb.isSelected()) {
                                String label = (String) rb.getClientProperty("label");
                                selectedOptionID = q.getOptions().stream()
                                    .filter(o -> o.getOptionText().equals(label))
                                    .map(Option::getOptionID)
                                    .findFirst().orElse(null);
                                break;
                            }
                        }
                    }
                    break;
                case "SHORT":
                    JTextArea ta = shortControls.get(q.getQuestionID());
                    if (ta != null) shortText = ta.getText().trim();
                    break;
            }

            answerRepo.saveAnswer(sessionID, q.getQuestionID(), selectedOptionID, shortText);
        }

        // Auto-grade MCQ + TF
        BigDecimal autoScore = answerRepo.autoGradeSession(sessionID);
        sessionRepo.submitSession(sessionID, autoScore);

        // Release lockdown BEFORE showing dialog
        setAlwaysOnTop(false);

        JOptionPane.showMessageDialog(null,
            "Exam submitted!\n\nAuto-graded score: " + autoScore + " pts\n" +
            "(Short answers will be graded by your teacher.)",
            "Submitted", JOptionPane.INFORMATION_MESSAGE);

        // Close exam window and go back to login
        dispose();
        SwingUtilities.invokeLater(() -> new LoginWindow().setVisible(true));
    }
}
