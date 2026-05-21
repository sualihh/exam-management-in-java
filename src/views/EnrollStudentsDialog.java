package views;

import data.CourseRepository;
import data.UserRepository;
import models.Course;
import models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

public class EnrollStudentsDialog extends JDialog {

    private final CourseRepository courseRepo = new CourseRepository();
    private final UserRepository   userRepo   = new UserRepository();
    private final Course course;

    private EnrollTableModel tableModel;

    public EnrollStudentsDialog(Frame owner, Course course) {
        super(owner, "Enroll Students — " + course.getCourseName(), true);
        this.course = course;
        setSize(520, 500);
        setLocationRelativeTo(owner);
        setResizable(true);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Theme.CARD);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.SIDEBAR);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));
        JLabel title = Theme.heading("Enroll Students");
        title.setForeground(Theme.TEXT);
        header.add(title, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        // Table
        List<User> allStudents = userRepo.getByRole("Student");
        List<Course> enrolled  = courseRepo.getByStudent(-1); // placeholder
        tableModel = new EnrollTableModel(allStudents, course.getCourseID(), courseRepo);

        JTable table = new JTable(tableModel);
        Theme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);

        JScrollPane scroll = Theme.scrollPane(table);
        root.add(scroll, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        btnPanel.setBackground(Theme.CARD);
        JButton btnSelectAll   = Theme.secondaryButton("Select All");
        JButton btnDeselectAll = Theme.secondaryButton("Deselect All");
        JButton btnSave        = Theme.primaryButton("Save");
        JButton btnCancel      = Theme.secondaryButton("Cancel");

        btnPanel.add(btnSelectAll);
        btnPanel.add(btnDeselectAll);
        btnPanel.add(Box.createHorizontalStrut(20));
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        root.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(root);

        btnSelectAll.addActionListener(e   -> { tableModel.setAll(true);  tableModel.fireTableDataChanged(); });
        btnDeselectAll.addActionListener(e -> { tableModel.setAll(false); tableModel.fireTableDataChanged(); });
        btnSave.addActionListener(e        -> save());
        btnCancel.addActionListener(e      -> dispose());
    }

    private void save() {
        for (EnrollTableModel.Row row : tableModel.rows) {
            if (row.enrolled) courseRepo.enrollStudent(row.userID, course.getCourseID());
            else              courseRepo.unenrollStudent(row.userID, course.getCourseID());
        }
        JOptionPane.showMessageDialog(this, "Enrollment updated.", "Saved", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    // ── Table model ───────────────────────────────────────────────────────────

    static class EnrollTableModel extends AbstractTableModel {
        static class Row {
            int userID;
            String fullName;
            boolean enrolled;
        }

        final java.util.List<Row> rows = new java.util.ArrayList<>();
        private final String[] cols = {"Enrolled", "Student Name"};

        EnrollTableModel(List<User> students, int courseID, CourseRepository courseRepo) {
            for (User s : students) {
                Row r = new Row();
                r.userID   = s.getUserID();
                r.fullName = s.getFullName();
                // Check if enrolled
                List<Course> courses = courseRepo.getByStudent(s.getUserID());
                r.enrolled = courses.stream().anyMatch(c -> c.getCourseID() == courseID);
                rows.add(r);
            }
        }

        void setAll(boolean value) {
            for (Row r : rows) r.enrolled = value;
        }

        @Override public int getRowCount()    { return rows.size(); }
        @Override public int getColumnCount() { return 2; }
        @Override public String getColumnName(int col) { return cols[col]; }
        @Override public Class<?> getColumnClass(int col) { return col == 0 ? Boolean.class : String.class; }
        @Override public boolean isCellEditable(int row, int col) { return col == 0; }

        @Override
        public Object getValueAt(int row, int col) {
            Row r = rows.get(row);
            return col == 0 ? r.enrolled : r.fullName;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 0) {
                rows.get(row).enrolled = (Boolean) value;
                fireTableCellUpdated(row, col);
            }
        }
    }
}
