package services;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Tracks user inactivity on a Dashboard window.
 * Warns at 28 minutes, logs out at 30 minutes of no activity.
 * Call start() when a dashboard opens and stop() when it closes.
 * Call reset() on any mouse/keyboard activity.
 */
public class InactivityService {

    private static final int TIMEOUT_MINUTES = 30;
    private static final int WARN_MINUTES    = 28; // warn 2 minutes before logout

    private Timer timer;
    private LocalDateTime lastActivity;
    private boolean warnShown;
    private Runnable onLogout;

    public void start(Runnable onLogout) {
        this.onLogout     = onLogout;
        this.lastActivity = LocalDateTime.now();
        this.warnShown    = false;

        // Check every 30 seconds
        timer = new Timer(30_000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkInactivity();
            }
        });
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    /** Call this on any user interaction to reset the idle clock. */
    public void reset() {
        lastActivity = LocalDateTime.now();
        warnShown    = false;
    }

    private void checkInactivity() {
        long idleMinutes = ChronoUnit.MINUTES.between(lastActivity, LocalDateTime.now());

        if (idleMinutes >= TIMEOUT_MINUTES) {
            stop();
            JOptionPane.showMessageDialog(null,
                "You have been logged out due to inactivity.",
                "Session Expired", JOptionPane.INFORMATION_MESSAGE);
            if (onLogout != null) onLogout.run();

        } else if (idleMinutes >= WARN_MINUTES && !warnShown) {
            warnShown = true;
            int result = JOptionPane.showConfirmDialog(null,
                "You will be logged out in 2 minutes due to inactivity.\n\nClick OK to stay logged in.",
                "Inactivity Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                reset(); // user chose to stay
            }
        }
    }
}
