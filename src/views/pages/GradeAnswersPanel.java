package views.pages;

import data.AnswerRepository;
import data.ExamRepository;
import data.ExamSessionRepository;
import models.Answer;
import models.Exam;
import models.ExamSession;
import services.AuthService;
import views.ExamReviewDialog;
import views.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Grade Answers Panel — allows teachers/admins to manually grade SHORT answer questions.
 *
 * Layout:
 *   TOP    : title + filter row (exam combo, student combo, refresh)
 *   CENTER : table of short answers (top) + grading preview panel (bottom) in a JSplitPane
 */
public class GradeAnswersPanel extends JPanel {

    // ── Repositories ──────────────────────────────────────────────────────────
    private final ExamRepository        examRepo    = new ExamRepository();
    private final ExamSessionRepository sessionRepo = new ExamSessionRepository();
    private final AnswerRepository      answerRepo  = new AnswerRepository();

    // ── State ─────────────────────────────────────────────────────────────────
    private List<Exam>        examList    = new ArrayList<>();
    private List<ExamSession> sessionList = new ArrayList<>();
    private List<Answer>      answerList  = new ArrayList<>();

    // ── UI components ─────────────────────────────────────────────────────────
    private JComboBox<String> cmbExams;
    private JComboBox<String> cmbSessions;
    private JTable            table;
    private AnswerTableModel  tableModel;

    // Preview / grading area
    private JPanel     previewCard;
    private JLabel     lblQuestion;
    private JTextArea  txtAnswer;
    private JLabel     lblMaxMarks;
    private JTextField txtScore;
    private JLabel     lblScoreError;
    private JButton    btnSave;

    // ── Constructor ───────────────────────────────────────────────────────────
    public GradeAnswersPanel() {
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        loadExams();
    }

    // ── Build UI ──────────────────────────────────────────────────────────────
    private void buildUI() {

        // ── TOP: title + filters ──────────────────────────────────────────────
        JPanel top = new JPanel();
        top.setBackground(Theme.BG);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setBackground(Theme.BG);
        titleRow.setBorder(new EmptyBorder(20, 24, 4, 24));
        JLabel title = new JLabel("Grade Short Answers");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Theme.TEXT);
        titleRow.add(title, BorderLayout.WEST);
        top.add(titleRow);

        // Filter row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterRow.setBackground(Theme.BG);
        filterRow.setBorder(new EmptyBorder(0, 16, 0, 16));

        filterRow.add(makeLabel("Exam:"));
        cmbExams = makeCombo();
        cmbExams.setPreferredSize(new Dimension(260, 34));
        filterRow.add(cmbExams);

        filterRow.add(makeLabel("Student:"));
        cmbSessions = makeCombo();
        cmbSessions.setPreferredSize(new Dimension(200, 34));
        filterRow.add(cmbSessions);

        JButton btnRefresh = Theme.secondaryButton("Refresh");
        filterRow.add(btnRefresh);

        JButton btnReview = Theme.infoButton("Full Review");
        filterRow.add(btnReview);

        top.add(filterRow);

        // Hint label
        JLabel hint = new JLabel("  Select an exam and student to see their short answers below.");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(Theme.TEXT_MUTED);
        hint.setBorder(new EmptyBorder(0, 24, 8, 24));
        top.add(hint);

        add(top, BorderLayout.NORTH);

        // ── CENTER: split pane ────────────────────────────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setBackground(Theme.BG);
        split.setBorder(null);
        split.setDividerSize(5);
        split.setResizeWeight(0.55);

        // ── TOP of split: answers table ───────────────────────────────────────
        tableModel = new AnswerTableModel();
        table = new JTable(tableModel);
        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(70);

        JScrollPane tableScroll = Theme.scrollPane(table);
        tableScroll.setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER_COL));

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBackground(Theme.BG);
        tableWrapper.setBorder(new EmptyBorder(0, 16, 0, 16));
        tableWrapper.add(tableScroll, BorderLayout.CENTER);
        split.setTopComponent(tableWrapper);

        // ── BOTTOM of split: grading preview card ─────────────────────────────
        previewCard = new JPanel(new BorderLayout(0, 10));
        previewCard.setBackground(Theme.CARD);
        previewCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Question text
        lblQuestion = new JLabel("Select a row above to grade it.");
        lblQuestion.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblQuestion.setForeground(Theme.TEXT);
        previewCard.add(lblQuestion, BorderLayout.NORTH);

        // Center: answer text area
        JPanel centerPanel = new JPanel(new BorderLayout(0, 6));
        centerPanel.setBackground(Theme.CARD);

        JLabel lblAnsTitle = new JLabel("Student's Answer:");
        lblAnsTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblAnsTitle.setForeground(Theme.TEXT_MUTED);
        centerPanel.add(lblAnsTitle, BorderLayout.NORTH);

        txtAnswer = Theme.textArea();
        txtAnswer.setEditable(false);
        txtAnswer.setRows(4);
        txtAnswer.setBackground(new Color(0x1A, 0x1A, 0x1A));
        JScrollPane ansScroll = Theme.scrollPane(txtAnswer);
        centerPanel.add(ansScroll, BorderLayout.CENTER);

        previewCard.add(centerPanel, BorderLayout.CENTER);

        // South: score input row
        JPanel southPanel = new JPanel(new BorderLayout(0, 4));
        southPanel.setBackground(Theme.CARD);

        lblMaxMarks = new JLabel("Max marks: —");
        lblMaxMarks.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMaxMarks.setForeground(Theme.TEXT_MUTED);
        southPanel.add(lblMaxMarks, BorderLayout.NORTH);

        JPanel scoreInputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        scoreInputRow.setBackground(Theme.CARD);

        JLabel lblScoreLbl = new JLabel("Score:");
        lblScoreLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblScoreLbl.setForeground(Theme.TEXT);
        scoreInputRow.add(lblScoreLbl);

        txtScore = Theme.inputField();
        txtScore.setPreferredSize(new Dimension(90, 34));
        scoreInputRow.add(txtScore);

        btnSave = Theme.primaryButton("Save Score");
        scoreInputRow.add(btnSave);

        lblScoreError = new JLabel(" ");
        lblScoreError.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblScoreError.setForeground(Theme.ERROR);
        scoreInputRow.add(lblScoreError);

        southPanel.add(scoreInputRow, BorderLayout.CENTER);
        previewCard.add(southPanel, BorderLayout.SOUTH);

        JPanel previewWrapper = new JPanel(new BorderLayout());
        previewWrapper.setBackground(Theme.BG);
        previewWrapper.setBorder(new EmptyBorder(0, 16, 16, 16));
        previewWrapper.add(previewCard, BorderLayout.CENTER);
        split.setBottomComponent(previewWrapper);

        add(split, BorderLayout.CENTER);

        // ── Wire up events ────────────────────────────────────────────────────
        cmbExams.addActionListener(e -> {
            if (cmbExams.getSelectedIndex() >= 0) onExamSelected();
        });

        cmbSessions.addActionListener(e -> {
            if (cmbSessions.getSelectedIndex() >= 0) onSessionSelected();
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onAnswerSelected();
        });

        btnSave.addActionListener(e -> saveScore());

        btnRefresh.addActionListener(e -> loadExams());

        btnReview.addActionListener(e -> {
            int idx = cmbSessions.getSelectedIndex();
            if (idx < 0 || idx >= sessionList.size()) {
                JOptionPane.showMessageDialog(this, "Select a student first.", "Notice", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            new ExamReviewDialog(owner, sessionList.get(idx)).setVisible(true);
        });
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadExams() {
        try {
            String role = AuthService.getCurrentUser().getRoleName();
            examList = role.equals("Admin")
                ? examRepo.getAll()
                : examRepo.getByCourseTeacher(AuthService.getCurrentUser().getUserID());

            // Temporarily remove listener to avoid firing during population
            var listeners = cmbExams.getActionListeners();
            for (var l : listeners) cmbExams.removeActionListener(l);

            cmbExams.removeAllItems();
            for (Exam e : examList) cmbExams.addItem(e.getTitle());

            for (var l : listeners) cmbExams.addActionListener(l);

            // Reset sessions and answers
            clearSessions();

            if (!examList.isEmpty()) {
                cmbExams.setSelectedIndex(0);
                onExamSelected();
            }
        } catch (Exception ex) {
            showError("Failed to load exams: " + ex.getMessage());
        }
    }

    private void onExamSelected() {
        int idx = cmbExams.getSelectedIndex();
        if (idx < 0 || idx >= examList.size()) return;

        try {
            Exam exam = examList.get(idx);
            sessionList = sessionRepo.getByExam(exam.getExamID())
                .stream()
                .filter(ExamSession::isSubmitted)
                .collect(Collectors.toList());

            // Temporarily remove listener
            var listeners = cmbSessions.getActionListeners();
            for (var l : listeners) cmbSessions.removeActionListener(l);

            cmbSessions.removeAllItems();
            for (ExamSession s : sessionList) cmbSessions.addItem(s.getStudentName());

            for (var l : listeners) cmbSessions.addActionListener(l);

            clearAnswers();

            if (!sessionList.isEmpty()) {
                cmbSessions.setSelectedIndex(0);
                onSessionSelected();
            }
        } catch (Exception ex) {
            showError("Failed to load sessions: " + ex.getMessage());
        }
    }

    private void onSessionSelected() {
        int idx = cmbSessions.getSelectedIndex();
        if (idx < 0 || idx >= sessionList.size()) return;

        try {
            ExamSession session = sessionList.get(idx);
            answerList = answerRepo.getBySession(session.getSessionID())
                .stream()
                .filter(a -> "SHORT".equals(a.getQuestionType()))
                .collect(Collectors.toList());

            tableModel.setData(answerList);
            resetPreview();
        } catch (Exception ex) {
            showError("Failed to load answers: " + ex.getMessage());
        }
    }

    private void onAnswerSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= answerList.size()) {
            resetPreview();
            return;
        }

        Answer a = answerList.get(row);

        // Question text
        String qText = a.getQuestionText();
        lblQuestion.setText("<html><b>Q:</b> " + (qText.length() > 120 ? qText.substring(0, 120) + "…" : qText) + "</html>");

        // Student's answer
        String ans = a.getShortAnswerText();
        txtAnswer.setText(ans == null || ans.isBlank() ? "(No answer provided)" : ans);
        txtAnswer.setCaretPosition(0);

        // Max marks
        lblMaxMarks.setText("Max marks: " + (a.getMarks() != null ? a.getMarks().toPlainString() : "?"));

        // Pre-fill score if already graded
        txtScore.setText(a.getManualScore() != null ? a.getManualScore().toPlainString() : "");
        lblScoreError.setText(" ");
    }

    private void saveScore() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= answerList.size()) {
            JOptionPane.showMessageDialog(this, "Select an answer row first.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Answer answer = answerList.get(row);

        // Validate score input
        BigDecimal score;
        try {
            score = new BigDecimal(txtScore.getText().trim());
            if (answer.getMarks() != null && score.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
            if (answer.getMarks() != null && score.compareTo(answer.getMarks()) > 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            String max = answer.getMarks() != null ? answer.getMarks().toPlainString() : "?";
            lblScoreError.setText("Enter a number between 0 and " + max);
            return;
        }

        try {
            // Save the score
            answerRepo.gradeShortAnswer(answer.getAnswerID(), score);

            // Recalculate session total
            int idx = cmbSessions.getSelectedIndex();
            if (idx >= 0 && idx < sessionList.size()) {
                sessionRepo.updateTotalScore(sessionList.get(idx).getSessionID());
            }

            // Reload answers to reflect updated state
            onSessionSelected();

            // Re-select the same row
            if (row < tableModel.getRowCount()) {
                table.setRowSelectionInterval(row, row);
                onAnswerSelected();
            }

            lblScoreError.setText(" ");
            JOptionPane.showMessageDialog(this,
                "Score saved successfully. Session total updated.",
                "Saved", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            showError("Failed to save score: " + ex.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clearSessions() {
        var listeners = cmbSessions.getActionListeners();
        for (var l : listeners) cmbSessions.removeActionListener(l);
        cmbSessions.removeAllItems();
        for (var l : listeners) cmbSessions.addActionListener(l);
        sessionList = new ArrayList<>();
        clearAnswers();
    }

    private void clearAnswers() {
        answerList = new ArrayList<>();
        tableModel.setData(answerList);
        resetPreview();
    }

    private void resetPreview() {
        lblQuestion.setText("Select a row above to grade it.");
        txtAnswer.setText("");
        lblMaxMarks.setText("Max marks: —");
        txtScore.setText("");
        lblScoreError.setText(" ");
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    private <T> JComboBox<T> makeCombo() {
        JComboBox<T> cb = new JComboBox<>();
        cb.setBackground(Theme.INPUT_BG);
        cb.setForeground(Theme.TEXT);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cb.setBorder(BorderFactory.createLineBorder(Theme.BORDER_COL, 1));
        // Style the dropdown list
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? Theme.ACCENT : Theme.INPUT_BG);
                setForeground(Theme.TEXT);
                setBorder(new EmptyBorder(4, 8, 4, 8));
                return this;
            }
        });
        return cb;
    }

    // ── Table model ───────────────────────────────────────────────────────────

    static class AnswerTableModel extends AbstractTableModel {

        private static final String[] COLS = {
            "Question", "Student's Answer", "Max Marks", "Score Given", "Graded"
        };

        private List<Answer> data = new ArrayList<>();

        void setData(List<Answer> data) {
            this.data = new ArrayList<>(data);
            fireTableDataChanged();
        }

        @Override public int getRowCount()    { return data.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int col) { return COLS[col]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= data.size()) return "";
            Answer a = data.get(row);
            return switch (col) {
                case 0 -> {
                    String t = a.getQuestionText() != null ? a.getQuestionText() : "";
                    yield t.length() > 55 ? t.substring(0, 55) + "…" : t;
                }
                case 1 -> {
                    String t = a.getShortAnswerText() != null ? a.getShortAnswerText() : "";
                    if (t.isBlank()) yield "(no answer)";
                    yield t.length() > 60 ? t.substring(0, 60) + "…" : t;
                }
                case 2 -> a.getMarks() != null ? a.getMarks().toPlainString() : "—";
                case 3 -> a.getManualScore() != null ? a.getManualScore().toPlainString() : "—";
                case 4 -> a.isGraded() ? "Yes" : "No";
                default -> "";
            };
        }

        // Custom row color: graded rows get a subtle green tint
        public boolean isGraded(int row) {
            return row < data.size() && data.get(row).isGraded();
        }
    }
}
