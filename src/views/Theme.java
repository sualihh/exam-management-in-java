package views;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Central theme constants and factory methods for the dark UI.
 */
public class Theme {

    // ── Colours ───────────────────────────────────────────────────────────────
    public static final Color BG          = new Color(0x12, 0x12, 0x12);
    public static final Color SIDEBAR     = new Color(0x1A, 0x1A, 0x1A);
    public static final Color CARD        = new Color(0x1E, 0x1E, 0x1E);
    public static final Color CARD2       = new Color(0x25, 0x25, 0x25);
    public static final Color ACCENT      = new Color(0xFF, 0x6D, 0x00);
    public static final Color ACCENT_DARK = new Color(0xCC, 0x55, 0x00);
    public static final Color TEXT        = Color.WHITE;
    public static final Color TEXT_MUTED  = new Color(0x88, 0x88, 0x88);
    public static final Color TEXT_DIM    = new Color(0x55, 0x55, 0x55);
    public static final Color ERROR       = new Color(0xF4, 0x43, 0x36);
    public static final Color SUCCESS     = new Color(0x4C, 0xAF, 0x50);
    public static final Color WARNING     = new Color(0xFF, 0xB3, 0x00);
    public static final Color INFO        = new Color(0x21, 0x96, 0xF3);
    public static final Color DANGER      = new Color(0xF4, 0x43, 0x36);
    public static final Color TABLE_SEL   = new Color(0xFF, 0x6D, 0x00, 80);
    public static final Color INPUT_BG    = new Color(0x2A, 0x2A, 0x2A);
    public static final Color BORDER_COL  = new Color(0x33, 0x33, 0x33);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD,  22);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD,  16);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_MONO    = new Font("Consolas",  Font.PLAIN, 13);

    // ── Button factory ────────────────────────────────────────────────────────

    /** Orange primary button */
    public static JButton primaryButton(String text) {
        return styledButton(text, ACCENT, ACCENT_DARK, Color.WHITE);
    }

    /** Red danger button */
    public static JButton dangerButton(String text) {
        return styledButton(text, new Color(0xC6, 0x28, 0x28), new Color(0xA0, 0x1E, 0x1E), Color.WHITE);
    }

    /** Blue info button */
    public static JButton infoButton(String text) {
        return styledButton(text, new Color(0x15, 0x65, 0xC0), new Color(0x0D, 0x47, 0xA1), Color.WHITE);
    }

    /** Dark secondary button */
    public static JButton secondaryButton(String text) {
        return styledButton(text, new Color(0x33, 0x33, 0x33), new Color(0x44, 0x44, 0x44), TEXT_MUTED);
    }

    private static JButton styledButton(String text, Color bg, Color hover, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BOLD);
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    // ── Sidebar nav button ────────────────────────────────────────────────────

    public static JButton navButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? new Color(0x2A, 0x2A, 0x2A) : SIDEBAR);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BODY);
        btn.setForeground(TEXT);
        btn.setBackground(SIDEBAR);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(12, 20, 12, 20));
        return btn;
    }

    // ── Input field ───────────────────────────────────────────────────────────

    public static JTextField inputField() {
        JTextField tf = new JTextField();
        styleInput(tf);
        return tf;
    }

    public static JPasswordField passwordField() {
        JPasswordField pf = new JPasswordField();
        styleInput(pf);
        return pf;
    }

    public static JTextArea textArea() {
        JTextArea ta = new JTextArea();
        ta.setFont(FONT_BODY);
        ta.setBackground(INPUT_BG);
        ta.setForeground(TEXT);
        ta.setCaretColor(TEXT);
        ta.setBorder(new EmptyBorder(8, 10, 8, 10));
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        return ta;
    }

    private static void styleInput(JTextField tf) {
        tf.setFont(FONT_BODY);
        tf.setBackground(INPUT_BG);
        tf.setForeground(TEXT);
        tf.setCaretColor(TEXT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL, 1),
            new EmptyBorder(8, 10, 8, 10)
        ));
    }

    // ── Label ─────────────────────────────────────────────────────────────────

    public static JLabel label(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BODY);
        lbl.setForeground(TEXT_MUTED);
        return lbl;
    }

    public static JLabel heading(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_HEADING);
        lbl.setForeground(TEXT);
        return lbl;
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    public static void styleTable(JTable table) {
        table.setBackground(CARD);
        table.setForeground(TEXT);
        table.setFont(FONT_BODY);
        table.setRowHeight(32);
        table.setGridColor(BORDER_COL);
        table.setSelectionBackground(ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(SIDEBAR);
        table.getTableHeader().setForeground(TEXT_MUTED);
        table.getTableHeader().setFont(FONT_BOLD);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));
    }

    public static JScrollPane scrollPane(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBackground(CARD);
        sp.getViewport().setBackground(CARD);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));
        sp.getVerticalScrollBar().setBackground(CARD);
        sp.getHorizontalScrollBar().setBackground(CARD);
        return sp;
    }

    // ── ComboBox ──────────────────────────────────────────────────────────────

    public static <T> JComboBox<T> comboBox() {
        JComboBox<T> cb = new JComboBox<>();
        cb.setBackground(INPUT_BG);
        cb.setForeground(TEXT);
        cb.setFont(FONT_BODY);
        cb.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));
        return cb;
    }

    // ── Panel helpers ─────────────────────────────────────────────────────────

    public static JPanel darkPanel() {
        JPanel p = new JPanel();
        p.setBackground(BG);
        return p;
    }

    public static JPanel cardPanel() {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        return p;
    }

    public static JPanel sidebarPanel() {
        JPanel p = new JPanel();
        p.setBackground(SIDEBAR);
        return p;
    }

    // ── Error label ───────────────────────────────────────────────────────────

    public static JLabel errorLabel() {
        JLabel lbl = new JLabel(" ");
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(ERROR);
        return lbl;
    }

    // ── Separator ─────────────────────────────────────────────────────────────

    public static JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_COL);
        sep.setBackground(BORDER_COL);
        return sep;
    }
}
