package views;

import services.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ChangePasswordDialog extends JDialog {

    private JPasswordField txtNew;
    private JPasswordField txtConfirm;
    private JLabel         lblError;

    public ChangePasswordDialog(Frame owner) {
        super(owner, "Change Password", true);
        setSize(420, 320);
        setLocationRelativeTo(owner);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel();
        root.setBackground(Theme.CARD);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(30, 36, 30, 36));

        JLabel title = Theme.heading("Change Password");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(title);
        root.add(Box.createVerticalStrut(24));

        // New password
        JLabel lblNew = Theme.label("New Password (min 8 characters)");
        lblNew.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtNew = Theme.passwordField();
        txtNew.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtNew.setAlignmentX(Component.LEFT_ALIGNMENT);

        root.add(lblNew);
        root.add(Box.createVerticalStrut(6));
        root.add(txtNew);
        root.add(Box.createVerticalStrut(14));

        // Confirm password
        JLabel lblConfirm = Theme.label("Confirm Password");
        lblConfirm.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtConfirm = Theme.passwordField();
        txtConfirm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtConfirm.setAlignmentX(Component.LEFT_ALIGNMENT);

        root.add(lblConfirm);
        root.add(Box.createVerticalStrut(6));
        root.add(txtConfirm);
        root.add(Box.createVerticalStrut(8));

        lblError = Theme.errorLabel();
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(lblError);
        root.add(Box.createVerticalStrut(16));

        JButton btnSave = Theme.primaryButton("Save Password");
        btnSave.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(btnSave);

        setContentPane(root);

        btnSave.addActionListener(e -> save());
    }

    private void save() {
        String newPass     = new String(txtNew.getPassword());
        String confirmPass = new String(txtConfirm.getPassword());

        if (newPass.length() < 8) {
            lblError.setText("Password must be at least 8 characters.");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            lblError.setText("Passwords do not match.");
            return;
        }

        AuthService.changePassword(newPass);
        JOptionPane.showMessageDialog(this, "Password changed successfully.", "Done",
            JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}
