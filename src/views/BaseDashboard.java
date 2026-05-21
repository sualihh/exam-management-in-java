package views;

import services.AuthService;
import services.InactivityService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Base class for all three dashboards.
 * Layout: 220px dark sidebar on left, content area (JPanel with CardLayout) on right.
 *
 * Subclasses must implement:
 *   - String getRoleLabel()          — shown in sidebar header
 *   - void   addNavButtons()         — add nav buttons via addNavButton(...)
 *   - JPanel buildDefaultPage()      — the panel shown on first open
 *
 * IMPORTANT: Do NOT call abstract methods from the constructor.
 * Use the init() pattern — call init() at the end of each subclass constructor.
 */
public abstract class BaseDashboard extends JFrame {

    protected final InactivityService inactivity = new InactivityService();
    protected JPanel contentArea;
    protected JPanel sidebar;

    // ── Constructor ───────────────────────────────────────────────────────────

    public BaseDashboard(String title) {
        super(title);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1200, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                doLogout();
            }
        });

        // Reset inactivity on mouse/key events
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) { inactivity.reset(); }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { inactivity.reset(); }
        });
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { inactivity.reset(); }
        });
    }

    /**
     * Must be called at the end of each subclass constructor.
     * Builds the full UI and starts the inactivity timer.
     */
    protected void init() {
        buildUI();
        inactivity.start(this::doLogout);
        showPage(buildDefaultPage());
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);

        // ── Sidebar ───────────────────────────────────────────────────────────
        sidebar = new JPanel();
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        // Sidebar header — orange background with role + user name
        JPanel header = new JPanel();
        header.setBackground(Theme.ACCENT);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(20, 20, 20, 20));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel roleLabel = new JLabel(getRoleLabel());
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        roleLabel.setForeground(new Color(255, 255, 255, 180));
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel(AuthService.getCurrentUser() != null
            ? AuthService.getCurrentUser().getFullName() : "");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(roleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(nameLabel);

        sidebar.add(header);
        sidebar.add(Box.createVerticalStrut(12));

        // Nav buttons — added by subclass
        addNavButtons();

        sidebar.add(Box.createVerticalGlue());

        // Logout button at bottom
        JButton btnLogout = Theme.navButton("  🚪  Logout");
        btnLogout.setForeground(Theme.ERROR);
        btnLogout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btnLogout.addActionListener(e -> doLogout());
        sidebar.add(btnLogout);
        sidebar.add(Box.createVerticalStrut(8));

        // ── Content area ──────────────────────────────────────────────────────
        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(Theme.BG);

        root.add(sidebar,     BorderLayout.WEST);
        root.add(contentArea, BorderLayout.CENTER);
        setContentPane(root);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    /** Adds a nav button to the sidebar. */
    protected JButton addNavButton(String text) {
        JButton btn = Theme.navButton(text);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        sidebar.add(btn);
        return btn;
    }

    /** Replaces the content area with the given panel. */
    public void showPage(JPanel page) {
        contentArea.removeAll();
        contentArea.add(page, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    protected void doLogout() {
        inactivity.stop();
        AuthService.logout();
        new LoginWindow().setVisible(true);
        dispose();
    }

    // ── Abstract methods ──────────────────────────────────────────────────────

    /** Returns the role label shown in the sidebar header (e.g. "ADMINISTRATOR"). */
    protected abstract String getRoleLabel();

    /** Subclass adds nav buttons to the sidebar via addNavButton(). */
    protected abstract void addNavButtons();

    /** Returns the panel to show when the dashboard first opens. */
    protected abstract JPanel buildDefaultPage();
}
