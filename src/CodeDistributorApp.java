import com.formdev.flatlaf.FlatIntelliJLaf;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class CodeDistributorApp {
    private JFrame frame;
    private File selectedCSV;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf: " + e.getMessage());
        }

        Updater.checkForUpdates();
        SwingUtilities.invokeLater(() -> new CodeDistributorApp().initUI());
    }
    private JPanel createSinglePerShopTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Single Per Shop Distribution", TitledBorder.LEFT, TitledBorder.TOP));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField acid = createFieldWithTooltip("Enter Shop IDs (space or comma separated)");
        JTextField f5 = createFieldWithTooltip("€5 FB per shop");
        JTextField f10 = createFieldWithTooltip("€10 FB per shop");
        JTextField f5r = createFieldWithTooltip("€5 RFB per shop");
        JTextField f10r = createFieldWithTooltip("€10 RFB per shop");

        JButton preview = new JButton("Preview & Confirm", UIManager.getIcon("FileView.detailsViewIcon"));

        int row = 0;
        panel.add(new JLabel("Shop IDs:"), gbc(0, row));
        panel.add(acid, gbc(1, row++));

        panel.add(new JLabel("€5 FB per shop:"), gbc(0, row));
        panel.add(f5, gbc(1, row++));

        panel.add(new JLabel("€10 FB per shop:"), gbc(0, row));
        panel.add(f10, gbc(1, row++));

        panel.add(new JLabel("€5 RFB per shop:"), gbc(0, row));
        panel.add(f5r, gbc(1, row++));

        panel.add(new JLabel("€10 RFB per shop:"), gbc(0, row));
        panel.add(f10r, gbc(1, row++));

        gbc.gridwidth = 2;
        panel.add(preview, gbc(0, row++));

        preview.addActionListener(e -> {
            try {
                List<String> ids = parseList(acid.getText());
                int v5 = parse(f5.getText()), v10 = parse(f10.getText());
                int v5r = parse(f5r.getText()), v10r = parse(f10r.getText());

                Map<String, Integer> m5 = mapFixed(ids, v5);
                Map<String, Integer> m10 = mapFixed(ids, v10);
                Map<String, Integer> m5rb = mapFixed(ids, v5r);
                Map<String, Integer> m10rb = mapFixed(ids, v10r);

                preview(ids, m5, m10, m5rb, m10rb, "Single_Shop_Codes.csv");
            } catch (Exception ex) {
                showMessage("❌ Error: " + ex.getMessage());
            }
        });

        return panel;
    }


    private void initUI() {
        frame = new JFrame("Code Distributor Tool");
        frame.setSize(950, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Standard Distribution", createStandardDistributionTab());
        tabs.addTab("Single Per Shop", createSinglePerShopTab());

        JPanel outer = new JPanel(new BorderLayout());
        outer.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        outer.add(tabs, BorderLayout.CENTER);

        frame.getContentPane().add(outer, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private JPanel createStandardDistributionTab() {
        JPanel inner = new JPanel(new GridBagLayout());
        inner.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Standard Code Distribution", TitledBorder.LEFT, TitledBorder.TOP));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel fileLabel = new JLabel("No file selected");
        JButton uploadButton = new JButton("Upload CSV", UIManager.getIcon("FileView.directoryIcon"));

        JTextField total5 = createFieldWithTooltip("Total number of €5 codes to distribute (FB)");
        JTextField total10 = createFieldWithTooltip("Total number of €10 codes to distribute (FB)");
        JTextField total5RFB = createFieldWithTooltip("Total number of €5 RFB codes to distribute");
        JTextField total10RFB = createFieldWithTooltip("Total number of €10 RFB codes to distribute");

        JTextField prio200 = createFieldWithTooltip("Shop IDs with 200 code priority (space or comma separated)");
        JTextField prio100 = createFieldWithTooltip("Shop IDs with 100 code priority (space or comma separated)");
        JTextField prio50 = createFieldWithTooltip("Shop IDs with 50 code priority (space or comma separated)");

        JButton previewButton = new JButton("Preview & Confirm", UIManager.getIcon("FileView.detailsViewIcon"));

        int row = 0;
        inner.add(uploadButton, gbc(0, row));
        inner.add(fileLabel, gbc(1, row++, GridBagConstraints.WEST));

        inner.add(new JLabel("Total €5 Codes (5FB):"), gbc(0, row));
        inner.add(total5, gbc(1, row++));

        inner.add(new JLabel("Total €10 Codes (10FB):"), gbc(0, row));
        inner.add(total10, gbc(1, row++));

        inner.add(new JLabel("Total €5 RFB Codes:"), gbc(0, row));
        inner.add(total5RFB, gbc(1, row++));

        inner.add(new JLabel("Total €10 RFB Codes:"), gbc(0, row));
        inner.add(total10RFB, gbc(1, row++));

        inner.add(new JLabel("Priority 200 (ACID):"), gbc(0, row));
        inner.add(prio200, gbc(1, row++));

        inner.add(new JLabel("Priority 100 (ACID):"), gbc(0, row));
        inner.add(prio100, gbc(1, row++));

        inner.add(new JLabel("Priority 50 (ACID):"), gbc(0, row));
        inner.add(prio50, gbc(1, row++));

        gbc.gridwidth = 2;
        inner.add(previewButton, gbc(0, row++));

        uploadButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                selectedCSV = chooser.getSelectedFile();
                fileLabel.setText("✔ " + selectedCSV.getName());
            }
        });

        previewButton.addActionListener(e -> {
            if (selectedCSV == null) {
                showMessage("Please select a CSV file first.");
                return;
            }
            try {
                List<String> shops = loadShopIds(selectedCSV);
                int val5 = parse(total5.getText()), val10 = parse(total10.getText());
                int val5r = parse(total5RFB.getText()), val10r = parse(total10RFB.getText());

                List<String> p200 = parsePriorities(prio200.getText(), shops);
                List<String> p100 = parsePriorities(prio100.getText(), shops);
                List<String> p50 = parsePriorities(prio50.getText(), shops);

                validate(val5, p200.size(), p100.size(), p50.size());
                validate(val10, p200.size(), p100.size(), p50.size());

                Map<String, Integer> m5 = distribute(shops, p200, p100, p50, val5);
                Map<String, Integer> m10 = distribute(shops, p200, p100, p50, val10);
                Map<String, Integer> m5r = val5r > 0 ? distribute(shops, p200, p100, p50, val5r) : zeroMap(shops);
                Map<String, Integer> m10r = val10r > 0 ? distribute(shops, p200, p100, p50, val10r) : zeroMap(shops);

                List<String> ordered = new ArrayList<>();
                ordered.addAll(p200); ordered.addAll(p100); ordered.addAll(p50);
                for (String s : shops) if (!ordered.contains(s)) ordered.add(s);

                preview(ordered, m5, m10, m5r, m10r, "Code_Distribution_Output.csv");
            } catch (Exception ex) {
                showMessage("❌ Error: " + ex.getMessage());
            }
        });

        return inner;
    }

    private JTextField createFieldWithTooltip(String tooltip) {
        JTextField field = new JTextField(20);
        field.setToolTipText(tooltip);
        return field;
    }

    private void preview(List<String> ids, Map<String, Integer> m5, Map<String, Integer> m10, Map<String, Integer> m5r, Map<String, Integer> m10r, String filename) {
        String[] cols = {"AKID", "5FB", "10FB", "5RFB", "10RFB"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        for (String id : ids) {
            model.addRow(new Object[]{
                    id,
                    m5.getOrDefault(id, 0),
                    m10.getOrDefault(id, 0),
                    m5r.getOrDefault(id, 0),
                    m10r.getOrDefault(id, 0)
            });
        }

        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(800, 350));

        int opt = JOptionPane.showConfirmDialog(frame, scroll, "Confirm Export", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(System.getProperty("user.home") + "/Desktop/" + filename))) {
                pw.println("AKID;5FB;10FB;5RFB;10RFB");
                for (int i = 0; i < model.getRowCount(); i++) {
                    pw.printf("%s;%s;%s;%s;%s%n",
                            model.getValueAt(i, 0),
                            model.getValueAt(i, 1),
                            model.getValueAt(i, 2),
                            model.getValueAt(i, 3),
                            model.getValueAt(i, 4));
                }
                showMessage("✅ CSV saved to Desktop.");
            } catch (IOException e) {
                showMessage("❌ Error writing CSV: " + e.getMessage());
            }
        }
    }

    private void validate(int total, int c200, int c100, int c50) {
        int need = c200 * 200 + c100 * 100 + c50 * 50;
        if (total < need) throw new IllegalArgumentException("Budget too low. Required: " + need + ", Provided: " + total);
    }

    private Map<String, Integer> distribute(List<String> all, List<String> p200, List<String> p100, List<String> p50, int total) {
        Map<String, Integer> result = new LinkedHashMap<>();
        int fixed = 0;

        for (String id : p200) { result.put(id, 200); fixed += 200; }
        for (String id : p100) { result.put(id, 100); fixed += 100; }
        for (String id : p50)  { result.put(id, 50);  fixed += 50;  }

        int remain = total - fixed;
        List<String> rest = new ArrayList<>();
        for (String id : all) if (!result.containsKey(id)) rest.add(id);

        int each = !rest.isEmpty() ? remain / rest.size() : 0;
        int extra = !rest.isEmpty() ? remain % rest.size() : 0;

        for (String id : rest) {
            result.put(id, each + (extra-- > 0 ? 1 : 0));
        }

        return result;
    }

    private Map<String, Integer> mapFixed(List<String> ids, int val) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String id : ids) map.put(id, val);
        return map;
    }

    private Map<String, Integer> zeroMap(List<String> ids) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String id : ids) map.put(id, 0);
        return map;
    }

    private List<String> parsePriorities(String raw, List<String> base) {
        Set<String> set = new LinkedHashSet<>();
        for (String s : raw.split("[,\\s]+")) {
            if (base.contains(s.trim())) set.add(s.trim());
        }
        return new ArrayList<>(set);
    }

    private List<String> parseList(String input) {
        Set<String> set = new LinkedHashSet<>();
        for (String s : input.split("[,\\s]+")) {
            String id = s.trim().replaceAll("[^0-9]", "");
            if (!id.isEmpty()) set.add(id);
        }
        return new ArrayList<>(set);
    }

    private List<String> loadShopIds(File file) throws IOException {
        Set<String> ids = new LinkedHashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String id = line.split("[;,\\s]")[0].replaceAll("[^0-9]", "").trim();
                if (!id.isEmpty()) ids.add(id);
            }
        }
        return new ArrayList<>(ids);
    }

    private int parse(String input) {
        try {
            return Integer.parseInt(input.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void showMessage(String msg) {
        JOptionPane.showMessageDialog(frame, msg);
    }

    private GridBagConstraints gbc(int x, int y) {
        return gbc(x, y, GridBagConstraints.CENTER);
    }

    private GridBagConstraints gbc(int x, int y, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.anchor = anchor;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 5, 4, 5);
        return gbc;
    }
}
