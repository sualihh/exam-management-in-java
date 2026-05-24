package views.pages;

import data.UserRepository;
import services.AuthService;
import views.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StudentProfilePanel extends JPanel {

    private final UserRepository repo = new UserRepository();

    private JTextField     txtFullName;
    private JPasswordField txtNewPassword;
    private JPasswordField txtConfirmPassword;
    private JLabel         lblMessage;

    public StudentProfilePanel() {
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    private void buildUI() {
        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG);
        header.setBorder(new EmptyBorder(20, 24, 8, 24));
        JLabel title = Theme.heading("My Profile");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ── Form ──────────────────────────────────────────────────────────────
        JPanel formWrapper = new JPanel(new GridBagLayout());
        formWrapper.setBackground(Theme.BG);

        JPanel form = new JPanel();
        form.setBackground(Theme.CARD);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(28, 36, 28, 36));
        form.setMaximumSize(new Dimension(480, 500));

        JLabel subTitle = Theme.heading("Account Settings");
        subTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(subTitle);
        form.add(Box.createVerticalStrut(22));

        // Full name
        JLabel lblName = Theme.label("Full Name");
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblName);
        form.add(Box.createVerticalStrut(6));
        txtFullName = Theme.inputField();
        txtFullName.setText(AuthService.getCurrentUser().getFullName());
        txtFullName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtFullName.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(txtFullName);
        form.add(Box.createVerticalStrut(16));

        // Username (read-only)
        JLabel lblUser = Theme.label("Username (cannot be changed)");
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblUser);
        form.add(Box.createVerticalStrut(6));
        JTextField txtUsername = Theme.inputField();
        txtUsername.setText(AuthService.getCurrentUser().getUsername());
        txtUsername.setEnabled(false);
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(txtUsername);
        form.add(Box.createVerticalStrut(20));

        // Separator
        form.add(Theme.separator());
        form.add(Box.createVerticalStrut(16));

        JLabel lblPwSection = Theme.label("Change Password (leave blank to keep current)");
        lblPwSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblPwSection);
        form.add(Box.createVerticalStrut(12));

        // New password
        JLabel lblNew = Theme.label("New Password (min 8 characters)");
        lblNew.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblNew);
        form.add(Box.createVerticalStrut(6));
        txtNewPassword = Theme.passwordField();
        txtNewPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtNewPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(txtNewPassword);
        form.add(Box.createVerticalStrut(12));

        // Confirm password
        JLabel lblConfirm = Theme.label("Confirm Password");
        lblConfirm.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblConfirm);
        form.add(Box.createVerticalStrut(6));
        txtConfirmPassword = Theme.passwordField();
        txtConfirmPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtConfirmPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(txtConfirmPassword);
        form.add(Box.createVerticalStrut(8));

        lblMessage = new JLabel(" ");
        lblMessage.setFont(Theme.FONT_SMALL);
        lblMessage.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblMessage);
        form.add(Box.createVerticalStrut(16));

        JButton btnSave = Theme.primaryButton("Save Changes");
        btnSave.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(btnSave);

        formWrapper.add(form);
        add(formWrapper, BorderLayout.CENTER);

        btnSave.addActionListener(e -> save());
    }

    private void save() {
        String name    = txtFullName.getText().trim();
        String newPw   = new String(txtNewPassword.getPassword());
        String confPw  = new String(txtConfirmPassword.getPassword());

        if (name.isEmpty()) {
            showMessage("Full name cannot be empty.", true);
            return;
        }

        // Update name if changed
        if (!name.equals(AuthService.getCurrentUser().getFullName())) {
            AuthService.getCurrentUser().setFullName(name);
            try { repo.update(AuthService.getCurrentUser()); }
            catch (Exception ex) { showMessage("Error updating name: " + ex.getMessage(), true); return; }
        }

        // Update password if provided
        if (!newPw.isEmpty()) {
            if (newPw.length() < 8) { showMessage("Password must be at least 8 characters.", true); return; }
            if (!newPw.equals(confPw)) { showMessage("Passwords do not match.", true); return; }
            try {
                AuthService.changePassword(newPw);
                txtNewPassword.setText("");
                txtConfirmPassword.setText("");
            } catch (Exception ex) { showMessage("Error changing password: " + ex.getMessage(), true); return; }
        }

        showMessage("Profile updated successfully.", false);
    }

    private void showMessage(String msg, boolean isError) {
        lblMessage.setText(msg);
        lblMessage.setForeground(isError ? Theme.ERROR : Theme.SUCCESS);
    }
}
