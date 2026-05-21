package views;

import data.QuestionRepository;
import models.Exam;
import models.Option;
import models.Question;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.tree.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class ManageQuestionsDialog extends JDialog {

    private final QuestionRepository repo = new QuestionRepository();
    private final Exam exam;
    private Question editingQuestion = null;

    // Tree
    private JTree questionsTree;
    private DefaultMutableTreeNode rootNode;
    private DefaultMutableTreeNode groupMCQ;
    private DefaultMutableTreeNode groupTF;
    private DefaultMutableTreeNode groupSHORT;

    // Editor fields
    private JTextArea     txtQuestion;
    private JTextField    txtMarks;
    private JComboBox<String> cmbType;
    private JTextField    txtOpt1, txtOpt2, txtOpt3, txtOpt4;
    private JRadioButton  rbOpt1, rbOpt2, rbOpt3, rbOpt4;
    private JRadioButton  rbTrue, rbFalse;
    private JPanel        optionsPanel;
    private JPanel        tfPanel;
    private JLabel        lblEditorHeading;
    private JLabel        lblError;
    private JLabel        lblCount;

    public ManageQuestionsDialog(Frame owner, Exam exam) {
        super(owner, "Manage Questions — " + exam.getTitle(), true);
        this.exam = exam;
        setSize(1000, 680);
        setLocationRelativeTo(owner);
        setResizable(true);
        buildUI();
        loadQuestions();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Theme.BG);

        // ── Left: question tree ───────────────────────────────────────────────
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(Theme.SIDEBAR);
        leftPanel.setPreferredSize(new Dimension(300, 0));
        leftPanel.setBorder(new MatteBorder(0, 0, 0, 1, Theme.BORDER_COL));

        JPanel treeHeader = new JPanel(new BorderLayout());
        treeHeader.setBackground(Theme.SIDEBAR);
        treeHeader.setBorder(new EmptyBorder(14, 16, 10, 16));

        JLabel treeTitle = Theme.heading("Questions");
        treeTitle.setForeground(Theme.TEXT);
        treeHeader.add(treeTitle, BorderLayout.WEST);

        lblCount = Theme.label("0 questions");
        treeHeader.add(lblCount, BorderLayout.EAST);

        leftPanel.add(treeHeader, BorderLayout.NORTH);

        // Build tree
        rootNode   = new DefaultMutableTreeNode("Questions");
        groupMCQ   = new DefaultMutableTreeNode("Multiple Choice (0)");
        groupTF    = new DefaultMutableTreeNode("True / False (0)");
        groupSHORT = new DefaultMutableTreeNode("Short Answer (0)");
        rootNode.add(groupMCQ);
        rootNode.add(groupTF);
        rootNode.add(groupSHORT);

        questionsTree = new JTree(rootNode);
        questionsTree.setBackground(Theme.SIDEBAR);
        questionsTree.setForeground(Theme.TEXT);
        questionsTree.setFont(Theme.FONT_BODY);
        questionsTree.setRootVisible(false);
        questionsTree.setShowsRootHandles(true);
        questionsTree.setBorder(new EmptyBorder(4, 8, 4, 8));
        questionsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Custom renderer for dark theme
        questionsTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                    boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                setBackground(sel ? Theme.ACCENT : Theme.SIDEBAR);
                setForeground(sel ? Color.WHITE : Theme.TEXT);
                setFont(Theme.FONT_BODY);
                setOpaque(true);
                return this;
            }
        });

        questionsTree.addTreeSelectionListener(e -> onTreeSelect());

        JScrollPane treeScroll = Theme.scrollPane(questionsTree);
        treeScroll.setBorder(null);
        leftPanel.add(treeScroll, BorderLayout.CENTER);

        // Tree action buttons
        JPanel treeButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        treeButtons.setBackground(Theme.SIDEBAR);
        JButton btnAdd    = Theme.primaryButton("+ New");
        JButton btnDelete = Theme.dangerButton("Delete");
        treeButtons.add(btnAdd);
        treeButtons.add(btnDelete);
        leftPanel.add(treeButtons, BorderLayout.SOUTH);

        btnAdd.addActionListener(e    -> clearEditor());
        btnDelete.addActionListener(e -> deleteSelected());

        // ── Right: editor ─────────────────────────────────────────────────────
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(Theme.BG);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        lblEditorHeading = Theme.heading("New Question");
        lblEditorHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(lblEditorHeading);
        rightPanel.add(Box.createVerticalStrut(16));

        // Type selector
        JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        typeRow.setOpaque(false);
        typeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        typeRow.add(Theme.label("Type: "));
        cmbType = Theme.comboBox();
        cmbType.addItem("MCQ");
        cmbType.addItem("TF");
        cmbType.addItem("SHORT");
        cmbType.setPreferredSize(new Dimension(140, 36));
        typeRow.add(cmbType);
        rightPanel.add(typeRow);
        rightPanel.add(Box.createVerticalStrut(12));

        // Question text
        JLabel lblQ = Theme.label("Question Text");
        lblQ.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(lblQ);
        rightPanel.add(Box.createVerticalStrut(4));
        txtQuestion = Theme.textArea();
        txtQuestion.setRows(4);
        JScrollPane qScroll = Theme.scrollPane(txtQuestion);
        qScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        qScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(qScroll);
        rightPanel.add(Box.createVerticalStrut(10));

        // Marks
        JLabel lblM = Theme.label("Marks");
        lblM.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(lblM);
        rightPanel.add(Box.createVerticalStrut(4));
        txtMarks = Theme.inputField();
        txtMarks.setText("1");
        txtMarks.setMaximumSize(new Dimension(120, 40));
        txtMarks.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(txtMarks);
        rightPanel.add(Box.createVerticalStrut(14));

        // MCQ options panel
        optionsPanel = new JPanel();
        optionsPanel.setOpaque(false);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        ButtonGroup mcqGroup = new ButtonGroup();
        rbOpt1 = new JRadioButton(); rbOpt2 = new JRadioButton();
        rbOpt3 = new JRadioButton(); rbOpt4 = new JRadioButton();
        mcqGroup.add(rbOpt1); mcqGroup.add(rbOpt2);
        mcqGroup.add(rbOpt3); mcqGroup.add(rbOpt4);
        styleRadio(rbOpt1); styleRadio(rbOpt2);
        styleRadio(rbOpt3); styleRadio(rbOpt4);
        rbOpt1.setSelected(true);

        txtOpt1 = Theme.inputField(); txtOpt2 = Theme.inputField();
        txtOpt3 = Theme.inputField(); txtOpt4 = Theme.inputField();

        optionsPanel.add(Theme.label("Options (select correct answer):"));
        optionsPanel.add(Box.createVerticalStrut(6));
        optionsPanel.add(optionRow("A", rbOpt1, txtOpt1));
        optionsPanel.add(Box.createVerticalStrut(4));
        optionsPanel.add(optionRow("B", rbOpt2, txtOpt2));
        optionsPanel.add(Box.createVerticalStrut(4));
        optionsPanel.add(optionRow("C", rbOpt3, txtOpt3));
        optionsPanel.add(Box.createVerticalStrut(4));
        optionsPanel.add(optionRow("D", rbOpt4, txtOpt4));
        rightPanel.add(optionsPanel);

        // TF panel
        tfPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tfPanel.setOpaque(false);
        tfPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        ButtonGroup tfGroup = new ButtonGroup();
        rbTrue  = new JRadioButton("True");
        rbFalse = new JRadioButton("False");
        styleRadio(rbTrue); styleRadio(rbFalse);
        rbTrue.setSelected(true);
        tfGroup.add(rbTrue); tfGroup.add(rbFalse);
        tfPanel.add(Theme.label("Correct answer: "));
        tfPanel.add(rbTrue);
        tfPanel.add(Box.createHorizontalStrut(16));
        tfPanel.add(rbFalse);
        tfPanel.setVisible(false);
        rightPanel.add(tfPanel);

        rightPanel.add(Box.createVerticalStrut(10));

        lblError = Theme.errorLabel();
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(lblError);
        rightPanel.add(Box.createVerticalStrut(12));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton btnSave  = Theme.primaryButton("Save Question");
        JButton btnClose = Theme.secondaryButton("Close");
        btnRow.add(btnSave);
        btnRow.add(Box.createHorizontalStrut(10));
        btnRow.add(btnClose);
        rightPanel.add(btnRow);

        JScrollPane rightScroll = new JScrollPane(rightPanel);
        rightScroll.setBorder(null);
        rightScroll.getViewport().setBackground(Theme.BG);

        root.add(leftPanel,   BorderLayout.WEST);
        root.add(rightScroll, BorderLayout.CENTER);
        setContentPane(root);

        btnSave.addActionListener(e  -> saveQuestion());
        btnClose.addActionListener(e -> dispose());

        cmbType.addActionListener(e -> updateTypeVisibility());
        updateTypeVisibility();
    }

    private void styleRadio(JRadioButton rb) {
        rb.setOpaque(false);
        rb.setForeground(Theme.TEXT);
        rb.setFont(Theme.FONT_BODY);
    }

    private JPanel optionRow(String letter, JRadioButton rb, JTextField tf) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(80, 36));
        JLabel lbl = new JLabel(letter);
        lbl.setFont(Theme.FONT_BOLD);
        lbl.setForeground(Theme.ACCENT);
        left.add(rb);
        left.add(lbl);

        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.add(left, BorderLayout.WEST);
        row.add(tf,   BorderLayout.CENTER);
        return row;
    }

    private void updateTypeVisibility() {
        String type = (String) cmbType.getSelectedItem();
        optionsPanel.setVisible("MCQ".equals(type));
        tfPanel.setVisible("TF".equals(type));
    }

    // ── Load questions into tree ──────────────────────────────────────────────

    private void loadQuestions() {
        List<Question> questions = repo.getByExam(exam.getExamID());

        groupMCQ.removeAllChildren();
        groupTF.removeAllChildren();
        groupSHORT.removeAllChildren();

        for (Question q : questions) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(q);
            switch (q.getQuestionType()) {
                case "MCQ":   groupMCQ.add(node);   break;
                case "TF":    groupTF.add(node);    break;
                case "SHORT": groupSHORT.add(node); break;
            }
        }

        groupMCQ.setUserObject("Multiple Choice (" + groupMCQ.getChildCount() + ")");
        groupTF.setUserObject("True / False (" + groupTF.getChildCount() + ")");
        groupSHORT.setUserObject("Short Answer (" + groupSHORT.getChildCount() + ")");

        int total = questions.size();
        lblCount.setText(total + " question" + (total == 1 ? "" : "s"));

        DefaultTreeModel model = (DefaultTreeModel) questionsTree.getModel();
        model.reload();

        // Expand all groups
        for (int i = 0; i < questionsTree.getRowCount(); i++) {
            questionsTree.expandRow(i);
        }

        clearEditor();
    }

    private void onTreeSelect() {
        TreePath path = questionsTree.getSelectionPath();
        if (path == null) return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (!(node.getUserObject() instanceof Question)) return;

        Question q = (Question) node.getUserObject();
        editingQuestion = q;
        lblEditorHeading.setText("Edit Question");
        txtQuestion.setText(q.getQuestionText());
        txtMarks.setText(q.getMarks().toPlainString());

        switch (q.getQuestionType()) {
            case "MCQ":   cmbType.setSelectedItem("MCQ");   break;
            case "TF":    cmbType.setSelectedItem("TF");    break;
            case "SHORT": cmbType.setSelectedItem("SHORT"); break;
        }

        if ("MCQ".equals(q.getQuestionType()) && !q.getOptions().isEmpty()) {
            List<Option> opts = q.getOptions();
            txtOpt1.setText(opts.size() > 0 ? opts.get(0).getOptionText() : "");
            txtOpt2.setText(opts.size() > 1 ? opts.get(1).getOptionText() : "");
            txtOpt3.setText(opts.size() > 2 ? opts.get(2).getOptionText() : "");
            txtOpt4.setText(opts.size() > 3 ? opts.get(3).getOptionText() : "");
            rbOpt1.setSelected(opts.size() > 0 && opts.get(0).isCorrect());
            rbOpt2.setSelected(opts.size() > 1 && opts.get(1).isCorrect());
            rbOpt3.setSelected(opts.size() > 2 && opts.get(2).isCorrect());
            rbOpt4.setSelected(opts.size() > 3 && opts.get(3).isCorrect());
        } else if ("TF".equals(q.getQuestionType()) && !q.getOptions().isEmpty()) {
            Option correct = q.getOptions().stream().filter(Option::isCorrect).findFirst().orElse(null);
            rbTrue.setSelected(correct != null && "True".equals(correct.getOptionText()));
            rbFalse.setSelected(correct != null && "False".equals(correct.getOptionText()));
        }
    }

    private void clearEditor() {
        editingQuestion = null;
        lblEditorHeading.setText("New Question");
        txtQuestion.setText("");
        txtMarks.setText("1");
        cmbType.setSelectedIndex(0);
        txtOpt1.setText(""); txtOpt2.setText(""); txtOpt3.setText(""); txtOpt4.setText("");
        rbOpt1.setSelected(true);
        rbTrue.setSelected(true);
        lblError.setText(" ");
    }

    private void deleteSelected() {
        TreePath path = questionsTree.getSelectionPath();
        if (path == null) {
            JOptionPane.showMessageDialog(this, "Select a question first.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (!(node.getUserObject() instanceof Question)) {
            JOptionPane.showMessageDialog(this, "Select a question (not a group).", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Question q = (Question) node.getUserObject();
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete this question?\n\n\"" + q.getQuestionText() + "\"",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            repo.deleteQuestion(q.getQuestionID());
            loadQuestions();
        }
    }

    private void saveQuestion() {
        String text = txtQuestion.getText().trim();
        String type = (String) cmbType.getSelectedItem();

        if (text.isEmpty()) { lblError.setText("Question text is required."); return; }

        BigDecimal marks;
        try {
            marks = new BigDecimal(txtMarks.getText().trim());
            if (marks.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            lblError.setText("Enter valid marks (e.g. 1 or 2.5).");
            return;
        }

        if ("MCQ".equals(type)) {
            if (txtOpt1.getText().trim().isEmpty() || txtOpt2.getText().trim().isEmpty()) {
                lblError.setText("MCQ requires at least 2 options (A and B).");
                return;
            }
            if (!rbOpt1.isSelected() && !rbOpt2.isSelected() && !rbOpt3.isSelected() && !rbOpt4.isSelected()) {
                lblError.setText("Select the correct answer.");
                return;
            }
        }

        int orderIndex = repo.getByExam(exam.getExamID()).size() + 1;

        try {
            if (editingQuestion != null) {
                editingQuestion.setQuestionText(text);
                editingQuestion.setQuestionType(type);
                editingQuestion.setMarks(marks);
                repo.updateQuestion(editingQuestion);
                repo.deleteOptionsForQuestion(editingQuestion.getQuestionID());
                saveOptions(editingQuestion.getQuestionID(), type);
            } else {
                Question q = new Question();
                q.setExamID(exam.getExamID());
                q.setQuestionText(text);
                q.setQuestionType(type);
                q.setMarks(marks);
                q.setOrderIndex(orderIndex);
                int qID = repo.createQuestion(q);
                saveOptions(qID, type);
            }
        } catch (Exception ex) {
            lblError.setText("Error: " + ex.getMessage());
            return;
        }

        loadQuestions();
        lblError.setText(" ");
    }

    private void saveOptions(int questionID, String type) {
        if ("MCQ".equals(type)) {
            String[] texts = {
                txtOpt1.getText().trim(), txtOpt2.getText().trim(),
                txtOpt3.getText().trim(), txtOpt4.getText().trim()
            };
            boolean[] correct = {
                rbOpt1.isSelected(), rbOpt2.isSelected(),
                rbOpt3.isSelected(), rbOpt4.isSelected()
            };
            for (int i = 0; i < 4; i++) {
                if (!texts[i].isEmpty()) {
                    Option o = new Option();
                    o.setQuestionID(questionID);
                    o.setOptionText(texts[i]);
                    o.setCorrect(correct[i]);
                    repo.addOption(o);
                }
            }
        } else if ("TF".equals(type)) {
            Option oTrue = new Option();
            oTrue.setQuestionID(questionID);
            oTrue.setOptionText("True");
            oTrue.setCorrect(rbTrue.isSelected());
            repo.addOption(oTrue);

            Option oFalse = new Option();
            oFalse.setQuestionID(questionID);
            oFalse.setOptionText("False");
            oFalse.setCorrect(rbFalse.isSelected());
            repo.addOption(oFalse);
        }
        // SHORT has no options
    }
}
