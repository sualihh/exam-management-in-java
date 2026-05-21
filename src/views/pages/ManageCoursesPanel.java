package views.pages;

import data.CourseRepository;
import models.Course;
import views.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class ManageCoursesPanel extends JPanel {

    private final CourseRepository repo = new CourseRepository();
    private Course selected = null;

    private JPanel cardsPanel;
    private JLabel lblEmpty;

    public ManageCoursesPanel() {
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        loadCourses();
    }

    private void buildUI() {
        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG);
        header.setBorder(new EmptyBorder(20, 24, 8, 24));
        JLabel title = Theme.heading("Manage Courses");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.add(title, BorderLayout.WEST);

        // ── Toolbar ───────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(Theme.BG);
        toolbar.setBorder(new EmptyBorder(0, 16, 0, 16));

        JButton btnAdd     = Theme.primaryButton("+ Add Course");
        JButton btnEdit    = Theme.infoButton("Edit");
        JButton btnDelete  = Theme.dangerButton("Delete");
        JButton btnEnroll  = Theme.secondaryButton("Enroll Students");
        JButton btnRefresh = Theme.secondaryButton("Refresh");

        toolbar.add(btnAdd);
        toolbar.add(btnEdit);
        toolbar.add(btnDelete);
        toolbar.add(btnEnroll);
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

        lblEmpty = new JLabel("No courses found. Click '+ Add Course' to create one.");
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
            CourseFormDialog dlg = new CourseFormDialog(owner);
            dlg.setVisible(true);
            if (dlg.isSaved()) loadCourses();
        });

        btnEdit.addActionListener(e -> {
            if (selected == null) { warn("Click a course card first."); return; }
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            CourseFormDialog dlg = new CourseFormDialog(owner, selected);
            dlg.setVisible(true);
            if (dlg.isSaved()) loadCourses();
        });

        btnDelete.addActionListener(e -> {
            if (selected == null) { warn("Click a course card first."); return; }
            int confirm = JOptionPane.showConfirmDialog(this,
                "Delete \"" + selected.getCourseName() + "\"?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                try { repo.delete(selected.getCourseID()); loadCourses(); }
                catch (Exception ex) { error("Delete failed: " + ex.getMessage()); }
            }
        });

        btnEnroll.addActionListener(e -> {
            if (selected == null) { warn("Click a course card first."); return; }
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            new EnrollStudentsDialog(owner, selected).setVisible(true);
        });

        btnRefresh.addActionListener(e -> loadCourses());
    }

    private void loadCourses() {
        List<Course> courses = repo.getAll();
        selected = null;
        cardsPanel.removeAll();

        if (courses.isEmpty()) {
            lblEmpty.setVisible(true);
        } else {
            lblEmpty.setVisible(false);
            for (Course c : courses) cardsPanel.add(buildCard(c));
        }

        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private JPanel buildCard(Course course) {
        JPanel card = new JPanel();
        card.setBackground(Theme.CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(260, 160));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER_COL, 1),
            new EmptyBorder(0, 0, 14, 0)
        ));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Orange top strip
        JPanel strip = new JPanel();
        strip.setBackground(Theme.ACCENT);
        strip.setPreferredSize(new Dimension(260, 5));
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        card.add(strip);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(12, 16, 0, 16));

        // Course code chip
        JLabel codeChip = new JLabel(course.getCourseCode());
        codeChip.setFont(new Font("Segoe UI", Font.BOLD, 11));
        codeChip.setForeground(Theme.ACCENT);
        codeChip.setOpaque(true);
        codeChip.setBackground(new Color(0x2A, 0x2A, 0x2A));
        codeChip.setBorder(new EmptyBorder(3, 8, 3, 8));
        codeChip.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(codeChip);
        body.add(Box.createVerticalStrut(8));

        JLabel nameLbl = new JLabel("<html><body style='width:200px'>" + course.getCourseName() + "</body></html>");
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLbl.setForeground(Theme.TEXT);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(nameLbl);
        body.add(Box.createVerticalStrut(8));

        JLabel teacherLbl = new JLabel("👤  " + course.getTeacherName());
        teacherLbl.setFont(Theme.FONT_SMALL);
        teacherLbl.setForeground(Theme.TEXT_MUTED);
        teacherLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(teacherLbl);

        card.add(body);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                selectCard(course, card);
            }
        });

        return card;
    }

    private void selectCard(Course course, JPanel card) {
        // Deselect all
        for (Component c : cardsPanel.getComponents()) {
            if (c instanceof JPanel) {
                ((JPanel) c).setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER_COL, 1),
                    new EmptyBorder(0, 0, 14, 0)
                ));
            }
        }
        // Highlight selected
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.ACCENT, 2),
            new EmptyBorder(0, 0, 14, 0)
        ));
        selected = course;
    }

    private void warn(String msg)  { JOptionPane.showMessageDialog(this, msg, "Notice", JOptionPane.INFORMATION_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error",  JOptionPane.ERROR_MESSAGE); }
}
