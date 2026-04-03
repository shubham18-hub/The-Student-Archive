package com.example.ui;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;

// Main Swing desktop window for The Student Archive.
// JFrame is the main window class in Java Swing.
// Layout: TOP = search bar + buttons, MIDDLE = results table, BOTTOM = status bar
public class AcademicSearchUI extends JFrame {

    private JTextField searchField;
    private JButton searchButton;
    private JButton refreshButton;
    private JButton githubLoginButton;
    private JButton logoutButton;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JLabel countLabel;
    private JLabel userLabel;
    private JProgressBar progressBar;
    private GitHubUserProfile currentUser; // null if not logged in

    public static void main(String[] args) {
        // invokeLater ensures the window is created on the correct Swing thread
        SwingUtilities.invokeLater(() -> new AcademicSearchUI());
    }

    public AcademicSearchUI() {
        setTitle("The Student Archive");
        setSize(1200, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // center on screen

        createUI();
        setVisible(true);

        // Auto-login: check if user was logged in before (token saved on disk)
        GitHubUserProfile savedProfile = com.example.auth.TokenManager.loadProfile();
        if (savedProfile != null) {
            currentUser = savedProfile;
            githubLoginButton.setVisible(false);
            logoutButton.setVisible(true);
            userLabel.setText("Logged in as: " + savedProfile.getLogin());
        }

        loadInitialData();
    }

    private void createUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(Color.WHITE);

        mainPanel.add(createSearchPanel(), BorderLayout.NORTH);
        mainPanel.add(createTablePanel(), BorderLayout.CENTER);
        mainPanel.add(createStatusPanel(), BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(new LineBorder(new Color(200, 200, 200), 1));

        // Title row with login/logout buttons
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(10, 10, 5, 10));

        JLabel titleLabel = new JLabel("The Student Archive — Academic Search");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titlePanel.add(titleLabel, BorderLayout.WEST);

        githubLoginButton = new JButton("Login with GitHub");
        githubLoginButton.setFont(new Font("Arial", Font.PLAIN, 11));
        githubLoginButton.setBackground(new Color(36, 41, 46));
        githubLoginButton.setForeground(Color.WHITE);
        githubLoginButton.setBorderPainted(false);
        githubLoginButton.setPreferredSize(new Dimension(150, 28));
        githubLoginButton.addActionListener(e -> loginWithGitHub());

        logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Arial", Font.PLAIN, 11));
        logoutButton.setBackground(new Color(200, 50, 50));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setBorderPainted(false);
        logoutButton.setPreferredSize(new Dimension(100, 28));
        logoutButton.setVisible(false); // hidden until user logs in
        logoutButton.addActionListener(e -> logout());

        JPanel authPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        authPanel.setOpaque(false);
        authPanel.add(githubLoginButton);
        authPanel.add(logoutButton);
        titlePanel.add(authPanel, BorderLayout.EAST);
        panel.add(titlePanel);

        // Username label (shown after login)
        userLabel = new JLabel(" ");
        userLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        userLabel.setForeground(new Color(100, 100, 100));
        userLabel.setBorder(new EmptyBorder(0, 10, 5, 10));
        panel.add(userLabel);

        // Search bar row
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setOpaque(false);

        controlPanel.add(new JLabel("Search:"));

        searchField = new JTextField(40);
        searchField.setFont(new Font("Arial", Font.PLAIN, 12));
        searchField.setPreferredSize(new Dimension(400, 35));
        // Press Enter to search
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) performSearch();
            }
        });
        controlPanel.add(searchField);

        searchButton = new JButton("Search");
        searchButton.setFont(new Font("Arial", Font.BOLD, 12));
        searchButton.setBackground(new Color(41, 128, 185));
        searchButton.setForeground(Color.WHITE);
        searchButton.setBorderPainted(false);
        searchButton.setPreferredSize(new Dimension(100, 35));
        searchButton.addActionListener(e -> performSearch());
        controlPanel.add(searchButton);

        refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 12));
        refreshButton.setBackground(new Color(46, 204, 113));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setBorderPainted(false);
        refreshButton.setPreferredSize(new Dimension(100, 35));
        refreshButton.addActionListener(e -> loadInitialData());
        controlPanel.add(refreshButton);

        panel.add(controlPanel);
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JLabel label = new JLabel("Search Results");
        label.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(label, BorderLayout.NORTH);

        // Table columns — no Rank column
        String[] columns = {"ID", "Title", "Department", "File Path"};
        tableModel = new DefaultTableModel(new Object[][]{}, columns) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false; // table is read-only
            }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setRowHeight(28);
        resultsTable.setFont(new Font("Arial", Font.PLAIN, 11));
        resultsTable.setGridColor(new Color(220, 220, 220));
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.setAutoCreateRowSorter(true); // click column header to sort

        // Style the header row
        JTableHeader header = resultsTable.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setBackground(new Color(52, 73, 94));
        header.setForeground(Color.WHITE);

        // Set column widths
        int[] widths = {50, 350, 150, 600};
        for (int i = 0; i < widths.length; i++) {
            resultsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        panel.add(new JScrollPane(resultsTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusPanel() {
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
        progressBar.setIndeterminate(true); // spinning animation
        progressBar.setPreferredSize(new Dimension(150, 20));
        panel.add(progressBar, BorderLayout.EAST);

        return panel;
    }

    // Called when user clicks Search or presses Enter
    private void performSearch() {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search term.");
            return;
        }

        statusLabel.setText("Status: Searching...");
        progressBar.setVisible(true);
        searchButton.setEnabled(false);

        // SwingWorker runs the database query in a background thread
        // This prevents the UI from freezing while waiting for results
        // doInBackground() runs on background thread — safe for slow operations
        // done() runs on the UI thread — safe for updating UI components
        new SwingWorker<List<Map<String, String>>, Void>() {
            @Override
            protected List<Map<String, String>> doInBackground() {
                return SearchService.searchMaterials(query); // runs in background
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, String>> results = get();
                    displayResults(results);
                    statusLabel.setText("Status: Found " + results.size() + " results");
                } catch (Exception e) {
                    statusLabel.setText("Status: Error — " + e.getMessage());
                } finally {
                    progressBar.setVisible(false);
                    searchButton.setEnabled(true);
                }
            }
        }.execute();
    }

    // Loads all materials when the app starts or Refresh is clicked
    private void loadInitialData() {
        statusLabel.setText("Status: Loading...");
        progressBar.setVisible(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                List<Map<String, String>> results = SearchService.getAllMaterials();
                int total = SearchService.countMaterials();

                // SwingUtilities.invokeLater updates the UI from the background thread safely
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

    // Puts the search results into the table
    private void displayResults(List<Map<String, String>> results) {
        tableModel.setRowCount(0); // clear old rows

        for (Map<String, String> row : results) {
            tableModel.addRow(new Object[]{
                row.get("id"),
                row.get("title"),
                row.get("department"),
                row.get("file_path")
            });
        }
    }

    // Opens GitHub in the browser and waits for the user to authorize
    private void loginWithGitHub() {
        statusLabel.setText("Status: Waiting for GitHub login...");
        githubLoginButton.setEnabled(false);

        new SwingWorker<GitHubUserProfile, Void>() {
            @Override
            protected GitHubUserProfile doInBackground() {
                try {
                    // authorize() opens the browser and waits for GitHub to redirect back
                    // .get() blocks this background thread until login is complete
                    return GitHubOAuthClient.authorize().get();
                } catch (Exception e) {
                    System.out.println("Login error: " + e.getMessage());
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    GitHubUserProfile profile = get();

                    if (profile != null) {
                        currentUser = profile;

                        // Save token to disk so user stays logged in next time
                        com.example.auth.TokenManager.saveToken(profile);

                        // Update UI to show logged-in state
                        githubLoginButton.setVisible(false);
                        logoutButton.setVisible(true);
                        userLabel.setText("Logged in as: " + profile.getDisplayName()
                                        + " (" + profile.getLogin() + ")");
                        statusLabel.setText("Status: Logged in successfully");

                        JOptionPane.showMessageDialog(AcademicSearchUI.this,
                            "Welcome, " + profile.getDisplayName() + "!",
                            "Login Successful", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        statusLabel.setText("Status: Login failed or cancelled");
                        JOptionPane.showMessageDialog(AcademicSearchUI.this,
                            "Login failed or was cancelled.",
                            "Login Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    statusLabel.setText("Status: Error — " + e.getMessage());
                } finally {
                    githubLoginButton.setEnabled(true);
                }
            }
        }.execute();
    }

    // Clears the login state and resets the UI
    private void logout() {
        GitHubOAuthClient.logout(); // deletes the token file from disk
        currentUser = null;
        githubLoginButton.setVisible(true);
        logoutButton.setVisible(false);
        userLabel.setText(" ");
        statusLabel.setText("Status: Logged out");
        tableModel.setRowCount(0); // clear the results table
    }
}
