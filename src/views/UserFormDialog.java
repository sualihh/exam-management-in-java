package views;

import data.UserRepository;
import models.User;
import services.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class UserFormDialog extends JDialog {

    private final UserRepository repo = new UserRepository();
    private final User editUser;
    private final boolean isEdit;

    private JTextField     txtFullName;
    private JTextField     txtUsername;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbRole;
    private JLabel         lblError;

    private boolean saved = false;

    /** Add mode */
    public UserFormDialog(Frame owner) {
        super(owner, "Add User", true);
        this.isEdit   = false;
        this.editUser = null;
        setSize(460, 440);
        setLocationRelativeTo(owner);
        setResizable(false);
        buildUI();
    }

    /** Edit mode */
    public UserFormDialog(Frame owner, User user) {
        super(owner, "Edit User", true);
        this.isEdit   = true;
        this.editUser = user;
        setSize(460, 440);
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

        JLabel title = Theme.heading(isEdit ? "Edit User" : "Add User");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(22));

        // Full name
        root.add(fieldRow("Full Name"));
        txtFullName = Theme.inputField();
        txtFullName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtFullName.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(txtFullName);
        root.add(Box.createVerticalStrut(12));

        // Username
        root.add(fieldRow("Username"));
        txtUsername = Theme.inputField();
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(txtUsername);
        root.add(Box.createVerticalStrut(12));

        // Password
        String passLabel = isEdit ? "New Password (leave blank to keep current)" : "Password";
        root.add(fieldRow(passLabel));
        txtPassword = Theme.passwordField();
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(txtPassword);
        root.add(Box.createVerticalStrut(12));

        // Role
        root.add(fieldRow("Role"));
        cmbRole = Theme.comboBox();
        cmbRole.addItem("Admin");
        cmbRole.addItem("Teacher");
        cmbRole.addItem("Student");
        cmbRole.setSelectedIndex(2);
        cmbRole.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        cmbRole.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(cmbRole);
        root.add(Box.createVerticalStrut(8));

        lblError = Theme.errorLabel();
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(lblError);
        root.add(Box.createVerticalStrut(14));

        // Buttons
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

    private JLabel fieldRow(String text) {
        JLabel lbl = Theme.label(text);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private void populateFields() {
        txtFullName.setText(editUser.getFullName());
        txtUsername.setText(editUser.getUsername());
        txtUsername.setEnabled(false); // username cannot be changed
        cmbRole.setSelectedItem(editUser.getRoleName());
    }

    private void save() {
        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        String role     = (String) cmbRole.getSelectedItem();

        if (fullName.isEmpty() || username.isEmpty() || role == null) {
            lblError.setText("Full name, username, and role are required.");
            return;
        }
        if (!isEdit && password.isEmpty()) {
            lblError.setText("Password is required for new users.");
            return;
        }
        if (!isEdit && password.length() < 8) {
            lblError.setText("Password must be at least 8 characters.");
            return;
        }

        int roleID;
        switch (role) {
            case "Admin":   roleID = 1; break;
            case "Teacher": roleID = 2; break;
            default:        roleID = 3; break;
        }

        try {
            if (isEdit) {
                editUser.setFullName(fullName);
                editUser.setRoleID(roleID);
                repo.update(editUser);

                if (!password.isEmpty()) {
                    if (password.length() < 8) {
                        lblError.setText("Password must be at least 8 characters.");
                        return;
                    }
                    repo.updatePassword(editUser.getUserID(), AuthService.hashPassword(password));
                }
            } else {
                if (repo.getByUsername(username) != null) {
                    lblError.setText("Username '" + username + "' is already taken.");
                    return;
                }
                User newUser = new User();
                newUser.setFullName(fullName);
                newUser.setUsername(username);
                newUser.setPasswordHash(AuthService.hashPassword(password));
                newUser.setRoleID(roleID);
                repo.create(newUser);
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
