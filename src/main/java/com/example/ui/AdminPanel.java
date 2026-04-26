package com.example.ui;

import com.example.auth.RoleManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

/**
 * Admin-only panel.
 *
 * Features:
 *   - View all indexed materials with full details
 *   - Delete a selected record from the database
 *   - View database statistics (total count, per-department breakdown)
 *   - Re-index trigger (calls PDFToDatabase logic)
 *
 * This window is only accessible when the logged-in GitHub user
 * has the ADMIN role in RoleManager.
 */
public class AdminPanel extends JFrame {

    private final String adminLogin;
    private JTable materialsTable;
    private DefaultTableModel tableModel;
    private JLabel statsLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    public AdminPanel(String adminLogin) {
        this.adminLogin = adminLogin;

        // Double-check role before showing anything
        if (!RoleManager.isAdmin(adminLogin)) {
            JOptionPane.showMessageDialog(null,
                "Access Denied. You do not have admin privileges.",
                "Unauthorized", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setTitle("Admin Panel — The Student Archive  [" + adminLogin + "]");
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        setVisible(true);
        loadAllMaterials();
        loadStats();
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        root.setBackground(new Color(245, 245, 245));

        root.add(buildHeader(),     BorderLayout.NORTH);
        root.add(buildTablePanel(), BorderLayout.CENTER);
        root.add(buildStatsPanel(), BorderLayout.EAST);
        root.add(buildStatusBar(),  BorderLayout.SOUTH);

        add(root);
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(52, 73, 94));
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel title = new JLabel("Admin Control Panel");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        panel.add(title, BorderLayout.WEST);

        JLabel roleTag = new JLabel("ROLE: ADMIN  |  " + adminLogin);
        roleTag.setFont(new Font("Arial", Font.PLAIN, 12));
        roleTag.setForeground(new Color(200, 230, 200));
        panel.add(roleTag, BorderLayout.EAST);

        return panel;
    }

    // ── Materials table ───────────────────────────────────────────────────────
    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new TitledBorder("All Indexed Materials"));

        tableModel = new DefaultTableModel(
            new Object[][]{},
            new String[]{"ID", "Title", "Department", "File Path", "Indexed At"}
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        materialsTable = new JTable(tableModel);
        materialsTable.setRowHeight(26);
        materialsTable.setFont(new Font("Arial", Font.PLAIN, 11));
        materialsTable.setGridColor(new Color(220, 220, 220));
        materialsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        materialsTable.setAutoCreateRowSorter(true);

        JTableHeader header = materialsTable.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setBackground(new Color(52, 73, 94));
        header.setForeground(Color.WHITE);

        int[] widths = {50, 300, 150, 450, 130};
        for (int i = 0; i < widths.length; i++)
            materialsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        panel.add(new JScrollPane(materialsTable), BorderLayout.CENTER);
        panel.add(buildActionBar(), BorderLayout.SOUTH);
        return panel;
    }

    // ── Action buttons ────────────────────────────────────────────────────────
    private JPanel buildActionBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panel.setBackground(new Color(240, 240, 240));

        JButton deleteBtn = makeButton("🗑  Delete Selected", new Color(192, 57, 43));
        deleteBtn.addActionListener(e -> deleteSelected());

        JButton refreshBtn = makeButton("🔄  Refresh List", new Color(41, 128, 185));
        refreshBtn.addActionListener(e -> { loadAllMaterials(); loadStats(); });

        JButton viewBtn = makeButton("📄  View PDF", new Color(39, 174, 96));
        viewBtn.addActionListener(e -> viewSelectedPDF());

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(120, 24));

        panel.add(deleteBtn);
        panel.add(refreshBtn);
        panel.add(viewBtn);
        panel.add(progressBar);
        return panel;
    }

    // ── Stats panel ───────────────────────────────────────────────────────────
    private JPanel buildStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(220, 0));
        panel.setBorder(new TitledBorder("Database Stats"));
        panel.setBackground(Color.WHITE);

        statsLabel = new JLabel("<html>Loading stats...</html>");
        statsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statsLabel.setVerticalAlignment(JLabel.TOP);
        statsLabel.setBorder(new EmptyBorder(8, 8, 8, 8));
        panel.add(statsLabel, BorderLayout.NORTH);

        return panel;
    }

    // ── Status bar ────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(230, 230, 230));
        panel.setBorder(new EmptyBorder(4, 10, 4, 10));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        panel.add(statusLabel, BorderLayout.WEST);
        return panel;
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    private void loadAllMaterials() {
        statusLabel.setText("Loading all materials...");
        progressBar.setVisible(true);

        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() {
                List<Object[]> rows = new ArrayList<>();
                String sql = "SELECT id, title, department, file_path, created_at "
                           + "FROM academic_materials ORDER BY created_at DESC";
                try (Connection conn = DatabaseConnection.getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        rows.add(new Object[]{
                            rs.getString("id"),
                            rs.getString("title"),
                            rs.getString("department"),
                            rs.getString("file_path"),
                            rs.getString("created_at")
                        });
                    }
                } catch (SQLException e) {
                    SwingUtilities.invokeLater(() ->
                        statusLabel.setText("Error loading: " + e.getMessage()));
                }
                return rows;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rows = get();
                    tableModel.setRowCount(0);
                    for (Object[] row : rows) tableModel.addRow(row);
                    statusLabel.setText("Loaded " + rows.size() + " records.");
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                } finally {
                    progressBar.setVisible(false);
                }
            }
        }.execute();
    }

    private void loadStats() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                StringBuilder sb = new StringBuilder("<html><b>Total Records:</b><br>");
                try (Connection conn = DatabaseConnection.getConnection()) {
                    // Total count
                    try (Statement s = conn.createStatement();
                         ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM academic_materials")) {
                        if (rs.next()) sb.append("&nbsp;&nbsp;").append(rs.getInt(1)).append("<br><br>");
                    }
                    // Per-department breakdown
                    sb.append("<b>By Department:</b><br>");
                    String deptSql = "SELECT department, COUNT(*) as cnt "
                                   + "FROM academic_materials GROUP BY department ORDER BY cnt DESC";
                    try (Statement s = conn.createStatement();
                         ResultSet rs = s.executeQuery(deptSql)) {
                        while (rs.next()) {
                            sb.append("&nbsp;&nbsp;")
                              .append(rs.getString("department"))
                              .append(": <b>")
                              .append(rs.getInt("cnt"))
                              .append("</b><br>");
                        }
                    }
                } catch (SQLException e) {
                    sb.append("Error: ").append(e.getMessage());
                }
                sb.append("</html>");
                return sb.toString();
            }

            @Override
            protected void done() {
                try { statsLabel.setText(get()); }
                catch (Exception e) { statsLabel.setText("Stats error"); }
            }
        }.execute();
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void deleteSelected() {
        int row = materialsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a record first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id    = (String) tableModel.getValueAt(row, 0);
        String title = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete record:\n\"" + title + "\" (ID: " + id + ")?\n\nThis cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        statusLabel.setText("Deleting...");
        progressBar.setVisible(true);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                         "DELETE FROM academic_materials WHERE id = ?")) {
                    stmt.setInt(1, Integer.parseInt(id));
                    return stmt.executeUpdate() > 0;
                } catch (SQLException e) {
                    SwingUtilities.invokeLater(() ->
                        statusLabel.setText("Delete failed: " + e.getMessage()));
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        tableModel.removeRow(row);
                        statusLabel.setText("Deleted: " + title);
                        loadStats();
                    }
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                } finally {
                    progressBar.setVisible(false);
                }
            }
        }.execute();
    }

    private void viewSelectedPDF() {
        int row = materialsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a record first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String filePath = (String) tableModel.getValueAt(row, 3);
        new PDFViewerWindow(filePath);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(170, 32));
        return btn;
    }
}
