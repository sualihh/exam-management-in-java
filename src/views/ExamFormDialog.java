package views;

import data.CourseRepository;
import data.ExamRepository;
import models.Course;
import models.Exam;
import services.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;

public class ExamFormDialog extends JDialog {

    private final ExamRepository   examRepo   = new ExamRepository();
    private final CourseRepository courseRepo = new CourseRepository();
    private final Exam editExam;
    private final boolean isEdit;

    private JTextField        txtTitle;
    private JTextField        txtDuration;
    private JComboBox<Course> cmbCourse;
    private JTextField        txtStartDate;   // yyyy-MM-dd
    private JTextField        txtStartTime;   // HH:mm
    private JTextField        txtEndDate;
    private JTextField        txtEndTime;
    private JTextArea         txtInstructions;
    private JCheckBox         chkPublished;
    private JLabel            lblError;

    private boolean saved = false;

    public ExamFormDialog(Frame owner) {
        super(owner, "Add Exam", true);
        this.isEdit   = false;
        this.editExam = null;
        setSize(520, 560);
        setLocationRelativeTo(owner);
        setResizable(false);
        buildUI();
    }

    public ExamFormDialog(Frame owner, Exam exam) {
        super(owner, "Edit Exam", true);
        this.isEdit   = true;
        this.editExam = exam;
        setSize(520, 560);
        setLocationRelativeTo(owner);
        setResizable(false);
        buildUI();
        populateFields();
    }

    private void buildUI() {
        JPanel root = new JPanel();
        root.setBackground(Theme.CARD);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(24, 32, 24, 32));

        JLabel title = Theme.heading(isEdit ? "Edit Exam" : "Add Exam");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(18));

        // Exam title
        root.add(lbl("Exam Title"));
        txtTitle = Theme.inputField();
        txtTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(txtTitle);
        root.add(Box.createVerticalStrut(10));

        // Course
        root.add(lbl("Course"));
        cmbCourse = Theme.comboBox();
        loadCourses();
        cmbCourse.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        cmbCourse.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(cmbCourse);
        root.add(Box.createVerticalStrut(10));

        // Duration
        root.add(lbl("Duration (minutes)"));
        txtDuration = Theme.inputField();
        txtDuration.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtDuration.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(txtDuration);
        root.add(Box.createVerticalStrut(10));

        // Start date/time row
        root.add(lbl("Start Date (yyyy-MM-dd)  &  Start Time (HH:mm)"));
        JPanel startRow = new JPanel(new GridLayout(1, 2, 8, 0));
        startRow.setOpaque(false);
        startRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        startRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtStartDate = Theme.inputField();
        txtStartTime = Theme.inputField();
        txtStartDate.setText(java.time.LocalDate.now().toString());
        txtStartTime.setText("08:00");
        startRow.add(txtStartDate);
        startRow.add(txtStartTime);
        root.add(startRow);
        root.add(Box.createVerticalStrut(10));

        // End date/time row
        root.add(lbl("End Date (yyyy-MM-dd)  &  End Time (HH:mm)"));
        JPanel endRow = new JPanel(new GridLayout(1, 2, 8, 0));
        endRow.setOpaque(false);
        endRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        endRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtEndDate = Theme.inputField();
        txtEndTime = Theme.inputField();
        txtEndDate.setText(java.time.LocalDate.now().toString());
        txtEndTime.setText("10:00");
        endRow.add(txtEndDate);
        endRow.add(txtEndTime);
        root.add(endRow);
        root.add(Box.createVerticalStrut(10));

        // Instructions
        root.add(lbl("Instructions (optional)"));
        txtInstructions = Theme.textArea();
        txtInstructions.setRows(3);
        JScrollPane instrScroll = Theme.scrollPane(txtInstructions);
        instrScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        instrScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(instrScroll);
        root.add(Box.createVerticalStrut(10));

        // Published checkbox
        chkPublished = new JCheckBox("Published (visible to students)");
        chkPublished.setOpaque(false);
        chkPublished.setForeground(Theme.TEXT);
        chkPublished.setFont(Theme.FONT_BODY);
        chkPublished.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(chkPublished);
        root.add(Box.createVerticalStrut(8));

        lblError = Theme.errorLabel();
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(lblError);
        root.add(Box.createVerticalStrut(12));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton btnSave   = Theme.primaryButton("Save");
        JButton btnCancel = Theme.secondaryButton("Cancel");
        btnRow.add(btnSave);
        btnRow.add(Box.createHorizontalStrut(10));
        btnRow.add(btnCancel);
        root.add(btnRow);

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.CARD);
        setContentPane(scroll);

        btnSave.addActionListener(e   -> save());
        btnCancel.addActionListener(e -> dispose());
    }

    private JLabel lbl(String text) {
        JLabel l = Theme.label(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void loadCourses() {
        String role = AuthService.getCurrentUser().getRoleName();
        List<Course> courses = role.equals("Admin")
            ? courseRepo.getAll()
            : courseRepo.getByTeacher(AuthService.getCurrentUser().getUserID());
        cmbCourse.removeAllItems();
        for (Course c : courses) cmbCourse.addItem(c);
    }

    private void populateFields() {
        txtTitle.setText(editExam.getTitle());
        txtDuration.setText(String.valueOf(editExam.getDurationMins()));
        txtInstructions.setText(editExam.getInstructions());
        chkPublished.setSelected(editExam.isPublished());

        LocalDateTime start = editExam.getStartDateTime();
        LocalDateTime end   = editExam.getEndDateTime();
        if (start != null) {
            txtStartDate.setText(start.toLocalDate().toString());
            txtStartTime.setText(String.format("%02d:%02d", start.getHour(), start.getMinute()));
        }
        if (end != null) {
            txtEndDate.setText(end.toLocalDate().toString());
            txtEndTime.setText(String.format("%02d:%02d", end.getHour(), end.getMinute()));
        }

        // Select matching course
        for (int i = 0; i < cmbCourse.getItemCount(); i++) {
            if (cmbCourse.getItemAt(i).getCourseID() == editExam.getCourseID()) {
                cmbCourse.setSelectedIndex(i);
                break;
            }
        }
    }

    private void save() {
        String title = txtTitle.getText().trim();
        if (title.isEmpty()) { lblError.setText("Exam title is required."); return; }

        Course course = (Course) cmbCourse.getSelectedItem();
        if (course == null) { lblError.setText("Please select a course."); return; }

        int duration;
        try {
            duration = Integer.parseInt(txtDuration.getText().trim());
            if (duration <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            lblError.setText("Enter a valid duration in minutes.");
            return;
        }

        LocalDateTime start, end;
        try {
            start = LocalDateTime.parse(txtStartDate.getText().trim() + "T" + txtStartTime.getText().trim() + ":00");
        } catch (Exception ex) {
            lblError.setText("Invalid start date/time. Use yyyy-MM-dd and HH:mm.");
            return;
        }
        try {
            end = LocalDateTime.parse(txtEndDate.getText().trim() + "T" + txtEndTime.getText().trim() + ":00");
        } catch (Exception ex) {
            lblError.setText("Invalid end date/time. Use yyyy-MM-dd and HH:mm.");
            return;
        }

        if (!end.isAfter(start)) {
            lblError.setText("End date/time must be after start date/time.");
            return;
        }

        try {
            Exam exam = isEdit ? editExam : new Exam();
            exam.setTitle(title);
            exam.setCourseID(course.getCourseID());
            exam.setCreatedBy(AuthService.getCurrentUser().getUserID());
            exam.setDurationMins(duration);
            exam.setStartDateTime(start);
            exam.setEndDateTime(end);
            exam.setInstructions(txtInstructions.getText().trim());
            exam.setPublished(chkPublished.isSelected());

            if (isEdit) examRepo.update(exam);
            else        examRepo.create(exam);
        } catch (Exception ex) {
            lblError.setText("Error: " + ex.getMessage());
            return;
        }

        saved = true;
        dispose();
    }

    public boolean isSaved() { return saved; }
}
