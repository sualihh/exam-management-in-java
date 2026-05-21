package views;

import data.CourseRepository;
import data.UserRepository;
import models.Course;
import models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class CourseFormDialog extends JDialog {

    private final CourseRepository repo     = new CourseRepository();
    private final UserRepository   userRepo = new UserRepository();
    private final Course editCourse;
    private final boolean isEdit;

    private JTextField        txtCourseName;
    private JTextField        txtCourseCode;
    private JComboBox<User>   cmbTeacher;
    private JLabel            lblError;

    private boolean saved = false;

    /** Add mode */
    public CourseFormDialog(Frame owner) {
        super(owner, "Add Course", true);
        this.isEdit     = false;
        this.editCourse = null;
        setSize(460, 360);
        setLocationRelativeTo(owner);
        setResizable(false);
        buildUI();
    }

    /** Edit mode */
    public CourseFormDialog(Frame owner, Course course) {
        super(owner, "Edit Course", true);
        this.isEdit     = true;
        this.editCourse = course;
        setSize(460, 360);
        setLocationRelativeTo(owner);
        setResizable(false);
        buildUI();
        populateFields();
    }

    private void buildUI() {
        JPanel root = new JPanel();
        root.setBackground(Theme.CARD);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(28, 36, 28, 36));

        JLabel title = Theme.heading(isEdit ? "Edit Course" : "Add Course");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(22));

        // Course name
        root.add(lbl("Course Name"));
        txtCourseName = Theme.inputField();
        txtCourseName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtCourseName.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(txtCourseName);
        root.add(Box.createVerticalStrut(12));

        // Course code
        root.add(lbl("Course Code"));
        txtCourseCode = Theme.inputField();
        txtCourseCode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtCourseCode.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(txtCourseCode);
        root.add(Box.createVerticalStrut(12));

        // Teacher
        root.add(lbl("Assign Teacher"));
        cmbTeacher = Theme.comboBox();
        loadTeachers();
        cmbTeacher.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        cmbTeacher.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(cmbTeacher);
        root.add(Box.createVerticalStrut(8));

        lblError = Theme.errorLabel();
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(lblError);
        root.add(Box.createVerticalStrut(14));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton btnSave   = Theme.primaryButton("Save");
        JButton btnCancel = Theme.secondaryButton("Cancel");
        btnRow.add(btnSave);
        btnRow.add(Box.createHorizontalStrut(10));
        btnRow.add(btnCancel);
        root.add(btnRow);

        setContentPane(root);

        btnSave.addActionListener(e   -> save());
        btnCancel.addActionListener(e -> dispose());
    }

    private JLabel lbl(String text) {
        JLabel l = Theme.label(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void loadTeachers() {
        List<User> teachers = userRepo.getByRole("Teacher");
        cmbTeacher.removeAllItems();
        for (User t : teachers) cmbTeacher.addItem(t);
    }

    private void populateFields() {
        txtCourseName.setText(editCourse.getCourseName());
        txtCourseCode.setText(editCourse.getCourseCode());
        // Select the teacher that matches
        for (int i = 0; i < cmbTeacher.getItemCount(); i++) {
            if (cmbTeacher.getItemAt(i).getUserID() == editCourse.getTeacherID()) {
                cmbTeacher.setSelectedIndex(i);
                break;
            }
        }
    }

    private void save() {
        String name = txtCourseName.getText().trim();
        String code = txtCourseCode.getText().trim();

        if (name.isEmpty() || code.isEmpty()) {
            lblError.setText("Course name and code are required.");
            return;
        }

        User teacher = (User) cmbTeacher.getSelectedItem();
        if (teacher == null) {
            lblError.setText("Please assign a teacher.");
            return;
        }

        try {
            if (isEdit) {
                editCourse.setCourseName(name);
                editCourse.setCourseCode(code);
                repo.update(editCourse);
            } else {
                Course c = new Course();
                c.setCourseName(name);
                c.setCourseCode(code);
                c.setTeacherID(teacher.getUserID());
                repo.create(c);
            }
        } catch (Exception ex) {
            lblError.setText("Error: " + ex.getMessage());
            return;
        }

        saved = true;
        dispose();
    }

    public boolean isSaved() { return saved; }
}
