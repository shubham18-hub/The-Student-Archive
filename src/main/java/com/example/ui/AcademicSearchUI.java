package com.example.ui;

import com.example.auth.RoleManager;
import com.example.auth.TokenManager;
import com.example.nlp.NLPQueryProcessor;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * Main application window — The Student Archive.
 *
 * Role-aware UI:
 *   GUEST  — search only, login prompt shown
 *   USER   — search + view PDF (double-click opens embedded viewer)
 *   ADMIN  — all of the above + "Admin Panel" button
 *
 * NLP search:
 *   Queries are expanded via NLPQueryProcessor before hitting PostgreSQL.
 *   A "Raw Search" checkbox bypasses NLP for exact matching.
 *
 * PDF Viewer:
 *   Double-clicking a result opens PDFViewerWindow (embedded, no external app).
 */
public class AcademicSearchUI extends JFrame {

    // ── UI components ─────────────────────────────────────────────────────────
    private JTextField    searchField;
    private JCheckBox     rawSearchCheckbox;
    private JButton       searchButton;
    private JButton       refreshButton;
    private JButton       githubLoginButton;
    private JButton       logoutButton;
    private JButton       adminPanelButton;
    private JTable        resultsTable;
    private DefaultTableModel tableModel;
    private JLabel        statusLabel;
    private JLabel        countLabel;
    private JLabel        userLabel;
    private JLabel        roleLabel;
    private JProgressBar  progressBar;

    // ── State ─────────────────────────────────────────────────────────────────
    private GitHubUserProfile currentUser;

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(AcademicSearchUI::new);
    }

    public AcademicSearchUI() {
        setTitle("The Student Archive");
        setSize(1200, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        buildUI();
        setVisible(true);

        // Restore previous session if a saved token exists
        GitHubUserProfile saved = TokenManager.loadProfile();
        if (saved != null) {
            applyLogin(saved);
        }

        loadInitialData();
    }

    // ── UI construction ───────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(15, 15, 15, 15));
        main.setBackground(Color.WHITE);
        main.add(buildSearchPanel(), BorderLayout.NORTH);
        main.add(buildTablePanel(),  BorderLayout.CENTER);
        main.add(buildStatusBar(),   BorderLayout.SOUTH);
        add(main);
    }

    // ── Search panel ──────────────────────────────────────────────────────────
    private JPanel buildSearchPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(new LineBorder(new Color(200, 200, 200), 1));

        panel.add(buildTitleRow());
        panel.add(buildUserRow());
        panel.add(buildControlsRow());
        return panel;
    }

    private JPanel buildTitleRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(10, 10, 5, 10));

        JLabel title = new JLabel("The Student Archive — Academic Search");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        row.add(title, BorderLayout.WEST);

        // Auth buttons
        githubLoginButton = makeButton("Login with GitHub", new Color(36, 41, 46), 155, 28);
        githubLoginButton.addActionListener(e -> loginWithGitHub());

        logoutButton = makeButton("Logout", new Color(200, 50, 50), 100, 28);
        logoutButton.setVisible(false);
        logoutButton.addActionListener(e -> logout());

        adminPanelButton = makeButton("⚙ Admin Panel", new Color(142, 68, 173), 130, 28);
        adminPanelButton.setVisible(false);
        adminPanelButton.addActionListener(e -> openAdminPanel());

        JPanel authRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        authRow.setOpaque(false);
        authRow.add(adminPanelButton);
        authRow.add(githubLoginButton);
        authRow.add(logoutButton);
        row.add(authRow, BorderLayout.EAST);
        return row;
    }

    private JPanel buildUserRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, 10, 4, 10));

        userLabel = new JLabel("Not logged in");
        userLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        userLabel.setForeground(new Color(120, 120, 120));

        roleLabel = new JLabel("");
        roleLabel.setFont(new Font("Arial", Font.BOLD, 11));
        roleLabel.setOpaque(true);
        roleLabel.setBorder(new EmptyBorder(2, 6, 2, 6));
        roleLabel.setVisible(false);

        row.add(userLabel);
        row.add(roleLabel);
        return row;
    }

    private JPanel buildControlsRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        row.setOpaque(false);

        row.add(new JLabel("Search:"));

        searchField = new JTextField(40);
        searchField.setFont(new Font("Arial", Font.PLAIN, 12));
        searchField.setPreferredSize(new Dimension(400, 35));
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) performSearch();
            }
        });
        row.add(searchField);

        searchButton = makeButton("Search", new Color(41, 128, 185), 100, 35);
        searchButton.setFont(new Font("Arial", Font.BOLD, 12));
        searchButton.addActionListener(e -> performSearch());
        row.add(searchButton);

        refreshButton = makeButton("Refresh", new Color(46, 204, 113), 100, 35);
        refreshButton.setFont(new Font("Arial", Font.BOLD, 12));
        refreshButton.addActionListener(e -> loadInitialData());
        row.add(refreshButton);

        rawSearchCheckbox = new JCheckBox("Exact (no NLP)");
        rawSearchCheckbox.setFont(new Font("Arial", Font.PLAIN, 11));
        rawSearchCheckbox.setOpaque(false);
        rawSearchCheckbox.setToolTipText("Bypass NLP expansion — search for exact words only");
        row.add(rawSearchCheckbox);

        return row;
    }

    // ── Results table ─────────────────────────────────────────────────────────
    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);

        JLabel label = new JLabel("Search Results  (double-click to open PDF)");
        label.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(label, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
            new Object[][]{},
            new String[]{"ID", "Title", "Department", "File Path"}
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setRowHeight(28);
        resultsTable.setFont(new Font("Arial", Font.PLAIN, 11));
        resultsTable.setGridColor(new Color(220, 220, 220));
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.setAutoCreateRowSorter(true);

        JTableHeader header = resultsTable.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setBackground(new Color(52, 73, 94));
        header.setForeground(Color.WHITE);

        int[] widths = {50, 350, 150, 600};
        for (int i = 0; i < widths.length; i++)
            resultsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Double-click → open embedded PDF viewer
        resultsTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = resultsTable.rowAtPoint(e.getPoint());
                    if (row >= 0) openPDF((String) tableModel.getValueAt(row, 3));
                }
            }
        });

        panel.add(new JScrollPane(resultsTable), BorderLayout.CENTER);
        return panel;
    }

    // ── Status bar ────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(new EmptyBorder(8, 10, 8, 10));
        panel.setBackground(new Color(240, 240, 240));

        statusLabel = new JLabel("Status: Ready");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        panel.add(statusLabel, BorderLayout.WEST);

        countLabel = new JLabel("Total: 0 materials");
        countLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        countLabel.setHorizontalAlignment(JLabel.CENTER);
        panel.add(countLabel, BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(150, 20));
        panel.add(progressBar, BorderLayout.EAST);

        return panel;
    }

    // ── Search logic ──────────────────────────────────────────────────────────
    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search term.");
            return;
        }

        boolean useRaw = rawSearchCheckbox.isSelected();
        String modeLabel = useRaw ? "Exact" : "NLP";

        statusLabel.setText("Status: Searching [" + modeLabel + "]...");
        progressBar.setVisible(true);
        searchButton.setEnabled(false);

        new SwingWorker<List<Map<String, String>>, Void>() {
            @Override
            protected List<Map<String, String>> doInBackground() {
                return useRaw
                    ? SearchService.searchRaw(query)
                    : SearchService.searchMaterials(query);
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, String>> results = get();
                    displayResults(results);

                    // Show NLP explanation in status bar
                    if (!useRaw) {
                        String nlpInfo = NLPQueryProcessor.explain(query);
                        statusLabel.setText("Found " + results.size() + " results  |  " + nlpInfo);
                    } else {
                        statusLabel.setText("Status: Found " + results.size() + " results [Exact match]");
                    }
                } catch (Exception e) {
                    statusLabel.setText("Status: Error — " + e.getMessage());
                } finally {
                    progressBar.setVisible(false);
                    searchButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void loadInitialData() {
        statusLabel.setText("Status: Loading...");
        progressBar.setVisible(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                List<Map<String, String>> results = SearchService.getAllMaterials();
                int total = SearchService.countMaterials();
                SwingUtilities.invokeLater(() -> {
                    displayResults(results);
                    countLabel.setText("Total: " + total + " materials in database");
                    statusLabel.setText("Status: Ready — showing " + results.size() + " recent materials");
                    progressBar.setVisible(false);
                });
                return null;
            }
        }.execute();
    }

    private void displayResults(List<Map<String, String>> results) {
        tableModel.setRowCount(0);
        for (Map<String, String> row : results) {
            tableModel.addRow(new Object[]{
                row.get("id"), row.get("title"), row.get("department"), row.get("file_path")
            });
        }
    }

    // ── PDF viewer ────────────────────────────────────────────────────────────
    private void openPDF(String filePath) {
        if (filePath == null || filePath.isEmpty()) return;

        java.io.File f = new java.io.File(filePath);
        if (!f.exists()) {
            JOptionPane.showMessageDialog(this,
                "File not found:\n" + filePath, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Open embedded viewer — no external app needed
        new PDFViewerWindow(filePath);
        statusLabel.setText("Status: Opened viewer for " + f.getName());
    }

    // ── Auth ──────────────────────────────────────────────────────────────────
    private void loginWithGitHub() {
        statusLabel.setText("Status: Waiting for GitHub login...");
        githubLoginButton.setEnabled(false);

        new SwingWorker<GitHubUserProfile, Void>() {
            @Override
            protected GitHubUserProfile doInBackground() {
                try { return GitHubOAuthClient.authorize().get(); }
                catch (Exception e) { return null; }
            }

            @Override
            protected void done() {
                try {
                    GitHubUserProfile profile = get();
                    if (profile != null) {
                        TokenManager.saveToken(profile);
                        applyLogin(profile);
                        JOptionPane.showMessageDialog(AcademicSearchUI.this,
                            "Welcome, " + profile.getDisplayName() + "!\nRole: " + profile.getRoleLabel(),
                            "Login Successful", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        statusLabel.setText("Status: Login failed or cancelled");
                    }
                } catch (Exception e) {
                    statusLabel.setText("Status: Error — " + e.getMessage());
                } finally {
                    githubLoginButton.setEnabled(true);
                }
            }
        }.execute();
    }

    /**
     * Applies a logged-in profile to the UI — updates labels, shows/hides buttons
     * based on the user's role.
     */
    private void applyLogin(GitHubUserProfile profile) {
        currentUser = profile;

        githubLoginButton.setVisible(false);
        logoutButton.setVisible(true);

        userLabel.setText("Logged in as: " + profile.getDisplayName() + " (" + profile.getLogin() + ")");

        // Role badge
        roleLabel.setVisible(true);
        if (profile.isAdmin()) {
            roleLabel.setText("  ADMIN  ");
            roleLabel.setBackground(new Color(142, 68, 173));
            roleLabel.setForeground(Color.WHITE);
            adminPanelButton.setVisible(true);
        } else {
            roleLabel.setText("  USER  ");
            roleLabel.setBackground(new Color(39, 174, 96));
            roleLabel.setForeground(Color.WHITE);
            adminPanelButton.setVisible(false);
        }

        statusLabel.setText("Status: Logged in as " + profile.getRoleLabel());
    }

    private void logout() {
        GitHubOAuthClient.logout();
        currentUser = null;

        githubLoginButton.setVisible(true);
        logoutButton.setVisible(false);
        adminPanelButton.setVisible(false);
        roleLabel.setVisible(false);
        userLabel.setText("Not logged in");
        statusLabel.setText("Status: Logged out");
        tableModel.setRowCount(0);
    }

    private void openAdminPanel() {
        if (currentUser == null || !currentUser.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                "Admin access required.", "Unauthorized", JOptionPane.ERROR_MESSAGE);
            return;
        }
        new AdminPanel(currentUser.getLogin());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JButton makeButton(String text, Color bg, int w, int h) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.PLAIN, 11));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(w, h));
        return btn;
    }
}
