import data.DB;
import services.AuthService;
import views.LoginWindow;

import javax.swing.*;

/**
 * Application entry point.
 * Initialises the database schema, then shows the login window.
 */
public class Main {

    public static void main(String[] args) {
        // Use system look-and-feel as a base, then override with our dark theme
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Initialise DB schema (creates tables + seed data if needed)
        try {
            DB.initializeSchema();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                "Database initialisation failed:\n" + ex.getMessage(),
                "Startup Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Launch UI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            LoginWindow login = new LoginWindow();
            login.setVisible(true);
        });
    }
}
