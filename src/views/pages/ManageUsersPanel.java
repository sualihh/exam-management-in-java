package views.pages;

import data.UserRepository;
import models.User;
import services.AuthService;
import views.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

public class ManageUsersPanel extends JPanel {

    private final UserRepository repo = new UserRepository();
    private String activeFilter = "All";

    private JTable table;
    private UserTableModel tableModel;
    private JLabel lblCount;

    public ManageUsersPanel() {
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        loadUsers();
    }

    private void buildUI() {
        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG);
        header.setBorder(new EmptyBorder(20, 24, 12, 24));

        JLabel title = Theme.heading("Manage Users");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Theme.TEXT);
        header.add(title, BorderLayout.WEST);

        lblCount = Theme.label("0 users");
        header.add(lblCount, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Filter tabs ───────────────────────────────────────────────────────
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filterBar.setBackground(Theme.BG);
        filterBar.setBorder(new EmptyBorder(0, 24, 8, 24));

        String[] filters = {"All", "Admin", "Teacher", "Student"};
        for (String f : filters) {
            JButton btn = new JButton(f);
            btn.setFont(Theme.FONT_BODY);
            btn.setForeground(f.equals(activeFilter) ? Theme.ACCENT : Theme.TEXT_MUTED);
            btn.setBackground(Theme.BG);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                activeFilter = f;
                loadUsers();
                // Update tab colours
                for (Component c : filterBar.getComponents()) {
                    if (c instanceof JButton) {
                        JButton b = (JButton) c;
                        b.setForeground(b.getText().equals(activeFilter) ? Theme.ACCENT : Theme.TEXT_MUTED);
                    }
                }
            });
            filterBar.add(btn);
        }
        add(filterBar, BorderLayout.NORTH); // will be replaced below

        // ── Toolbar ───────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(Theme.BG);
        toolbar.setBorder(new EmptyBorder(0, 16, 0, 16));

        JButton btnAdd     = Theme.primaryButton("+ Add User");
        JButton btnEdit    = Theme.infoButton("Edit");
        JButton btnDelete  = Theme.dangerButton("Delete");
        JButton btnRefresh = Theme.secondaryButton("Refresh");

        toolbar.add(btnAdd);
        toolbar.add(btnEdit);
        toolbar.add(btnDelete);
        toolbar.add(btnRefresh);

        // ── Table ─────────────────────────────────────────────────────────────
        tableModel = new UserTableModel();
        table = new JTable(tableModel);
        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = Theme.scrollPane(table);

        // ── Layout ────────────────────────────────────────────────────────────
        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setBackground(Theme.BG);
        center.setBorder(new EmptyBorder(0, 16, 16, 16));

        JPanel topSection = new JPanel();
        topSection.setBackground(Theme.BG);
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.add(header);
        topSection.add(filterBar);
        topSection.add(toolbar);

        add(topSection, BorderLayout.NORTH);
        add(scroll,     BorderLayout.CENTER);

        // ── Events ────────────────────────────────────────────────────────────
        btnAdd.addActionListener(e -> {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            UserFormDialog dlg = new UserFormDialog(owner);
            dlg.setVisible(true);
            if (dlg.isSaved()) loadUsers();
        });

        btnEdit.addActionListener(e -> {
            User selected = getSelectedUser();
            if (selected == null) { warn("Select a user first."); return; }
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            UserFormDialog dlg = new UserFormDialog(owner, selected);
            dlg.setVisible(true);
            if (dlg.isSaved()) loadUsers();
        });

        btnDelete.addActionListener(e -> {
            User selected = getSelectedUser();
            if (selected == null) { warn("Select a user first."); return; }
            if (selected.getUserID() == AuthService.getCurrentUser().getUserID()) {
                warn("You cannot delete your own account.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                "Delete '" + selected.getFullName() + "'? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                try { repo.delete(selected.getUserID()); loadUsers(); }
                catch (Exception ex) { error("Delete failed: " + ex.getMessage()); }
            }
        });

        btnRefresh.addActionListener(e -> loadUsers());
    }

    private void loadUsers() {
        List<User> users = activeFilter.equals("All")
            ? repo.getAll()
            : repo.getByRole(activeFilter);
        tableModel.setData(users);
        lblCount.setText(users.size() + " user" + (users.size() == 1 ? "" : "s"));
    }

    private User getSelectedUser() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return tableModel.getRow(row);
    }

    private void warn(String msg)  { JOptionPane.showMessageDialog(this, msg, "Notice",  JOptionPane.WARNING_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error",   JOptionPane.ERROR_MESSAGE); }

    // ── Table model ───────────────────────────────────────────────────────────

    static class UserTableModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Full Name", "Username", "Role", "Active", "Created"};
        private List<User> data = new java.util.ArrayList<>();

        void setData(List<User> data) { this.data = data; fireTableDataChanged(); }
        User getRow(int row) { return data.get(row); }

        @Override public int getRowCount()    { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int col) { return cols[col]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }

        @Override
        public Object getValueAt(int row, int col) {
            User u = data.get(row);
            switch (col) {
                case 0: return u.getUserID();
                case 1: return u.getFullName();
                case 2: return u.getUsername();
                case 3: return u.getRoleName();
                case 4: return u.isActive() ? "Yes" : "No";
                case 5: return u.getCreatedAt() != null ? u.getCreatedAt().toLocalDate().toString() : "";
                default: return "";
            }
        }
    }
}
