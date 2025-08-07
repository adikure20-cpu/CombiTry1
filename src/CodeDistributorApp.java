import com.formdev.flatlaf.FlatLightLaf;

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
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("FlatLaf not loaded");
        }

        Updater.checkForUpdates();

        SwingUtilities.invokeLater(() -> new CodeDistributorApp().createUI());
    }

    private void createUI() {
        frame = new JFrame("Code Distributor Tool");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Standard Distribution", createStandardPanel());
        tabbedPane.addTab("Single Per Shop", createSinglePerShopPanel());

        frame.getContentPane().add(tabbedPane);
        frame.setVisible(true);
    }

    private JPanel createStandardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JButton uploadButton = new JButton("Upload CSV");
        JLabel fileLabel = new JLabel("No file selected");

        JTextField total5Field = new JTextField();
        JTextField total10Field = new JTextField();
        JTextField total5RFBField = new JTextField();
        JTextField total10RFBField = new JTextField();

        JTextField priority200Field = new JTextField();
        JTextField priority100Field = new JTextField();
        JTextField priority50Field = new JTextField();

        JButton createButton = new JButton("Preview & Confirm");

        int row = 0;
        gridAdd(grid, gbc, uploadButton, 0, row);
        gridAdd(grid, gbc, fileLabel, 1, row++);

        gridAdd(grid, gbc, new JLabel("Total €5 Codes (5FB):"), 0, row);
        gridAdd(grid, gbc, total5Field, 1, row++);

        gridAdd(grid, gbc, new JLabel("Total €10 Codes (10FB):"), 0, row);
        gridAdd(grid, gbc, total10Field, 1, row++);

        gridAdd(grid, gbc, new JLabel("Total €5 RFB Codes (5RFB):"), 0, row);
        gridAdd(grid, gbc, total5RFBField, 1, row++);

        gridAdd(grid, gbc, new JLabel("Total €10 RFB Codes (10RFB):"), 0, row);
        gridAdd(grid, gbc, total10RFBField, 1, row++);

        gridAdd(grid, gbc, new JLabel("Priority 200 (IDs):"), 0, row);
        gridAdd(grid, gbc, priority200Field, 1, row++);

        gridAdd(grid, gbc, new JLabel("Priority 100 (IDs):"), 0, row);
        gridAdd(grid, gbc, priority100Field, 1, row++);

        gridAdd(grid, gbc, new JLabel("Priority 50 (IDs):"), 0, row);
        gridAdd(grid, gbc, priority50Field, 1, row++);

        gridAdd(grid, gbc, createButton, 1, row);

        uploadButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                selectedCSV = chooser.getSelectedFile();
                fileLabel.setText(selectedCSV.getName());
            }
        });

        createButton.addActionListener(e -> {
            if (selectedCSV == null) {
                JOptionPane.showMessageDialog(frame, "Please upload a CSV file.");
                return;
            }

            try {
                int total5 = parseOrZero(total5Field.getText());
                int total10 = parseOrZero(total10Field.getText());
                int total5RFB = parseOrZero(total5RFBField.getText());
                int total10RFB = parseOrZero(total10RFBField.getText());

                List<String> shopIds = loadShopIds(selectedCSV);
                List<String> prio200 = parsePriorityShops(priority200Field.getText(), shopIds);
                List<String> prio100 = parsePriorityShops(priority100Field.getText(), shopIds);
                List<String> prio50 = parsePriorityShops(priority50Field.getText(), shopIds);

                validateCodeBudget(total5, prio200.size(), prio100.size(), prio50.size());
                validateCodeBudget(total10, prio200.size(), prio100.size(), prio50.size());

                Map<String, Integer> codes5FB = distributeCodes(shopIds, prio200, prio100, prio50, total5);
                Map<String, Integer> codes10FB = distributeCodes(shopIds, prio200, prio100, prio50, total10);
                Map<String, Integer> codes5RFB = total5RFB > 0 ? distributeCodes(shopIds, prio200, prio100, prio50, total5RFB) : emptyMap(shopIds);
                Map<String, Integer> codes10RFB = total10RFB > 0 ? distributeCodes(shopIds, prio200, prio100, prio50, total10RFB) : emptyMap(shopIds);

                List<String> orderedShops = new ArrayList<>(prio200);
                orderedShops.addAll(prio100);
                orderedShops.addAll(prio50);
                for (String id : shopIds) {
                    if (!orderedShops.contains(id)) orderedShops.add(id);
                }

                previewAndConfirm(orderedShops, codes5FB, codes10FB, codes5RFB, codes10RFB, "Code_Distribution_Output.csv");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
            }
        });

        panel.add(grid);
        return panel;
    }

    private JPanel createSinglePerShopPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Single Shop Mode", TitledBorder.LEFT, TitledBorder.TOP));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField acidField = new JTextField();
        JTextField fixed5FB = new JTextField();
        JTextField fixed10FB = new JTextField();
        JTextField fixed5RFB = new JTextField();
        JTextField fixed10RFB = new JTextField();
        JButton previewButton = new JButton("Preview & Confirm");

        int row = 0;
        gridAdd(panel, gbc, new JLabel("Shop IDs (comma or space):"), 0, row);
        gridAdd(panel, gbc, acidField, 1, row++);

        gridAdd(panel, gbc, new JLabel("€5 FB per shop:"), 0, row);
        gridAdd(panel, gbc, fixed5FB, 1, row++);

        gridAdd(panel, gbc, new JLabel("€10 FB per shop:"), 0, row);
        gridAdd(panel, gbc, fixed10FB, 1, row++);

        gridAdd(panel, gbc, new JLabel("€5 RFB per shop:"), 0, row);
        gridAdd(panel, gbc, fixed5RFB, 1, row++);

        gridAdd(panel, gbc, new JLabel("€10 RFB per shop:"), 0, row);
        gridAdd(panel, gbc, fixed10RFB, 1, row++);

        gridAdd(panel, gbc, previewButton, 1, row);

        previewButton.addActionListener(e -> {
            try {
                String[] ids = acidField.getText().split("[,\\s]+");
                int val5 = parseOrZero(fixed5FB.getText());
                int val10 = parseOrZero(fixed10FB.getText());
                int val5R = parseOrZero(fixed5RFB.getText());
                int val10R = parseOrZero(fixed10RFB.getText());

                List<String> uniqueIds = new ArrayList<>();
                for (String id : ids) {
                    String cleaned = id.trim().replaceAll("[^0-9]", "");
                    if (!cleaned.isEmpty() && !uniqueIds.contains(cleaned)) uniqueIds.add(cleaned);
                }

                Map<String, Integer> map5 = new HashMap<>();
                Map<String, Integer> map10 = new HashMap<>();
                Map<String, Integer> map5R = new HashMap<>();
                Map<String, Integer> map10R = new HashMap<>();
                for (String id : uniqueIds) {
                    map5.put(id, val5);
                    map10.put(id, val10);
                    map5R.put(id, val5R);
                    map10R.put(id, val10R);
                }

                previewAndConfirm(uniqueIds, map5, map10, map5R, map10R, "Single_Shop_Codes.csv");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
            }
        });

        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    private void gridAdd(JPanel panel, GridBagConstraints gbc, Component comp, int x, int y) {
        gbc.gridx = x;
        gbc.gridy = y;
        panel.add(comp, gbc);
    }

    private int parseOrZero(String input) {
        try {
            return Integer.parseInt(input.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private List<String> loadShopIds(File csvFile) throws IOException {
        Set<String> idSet = new LinkedHashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String cleaned = line.split("[;,\t]")[0].replaceAll("[^0-9]", "").trim();
                if (!cleaned.isEmpty()) idSet.add(cleaned);
            }
        }
        return new ArrayList<>(idSet);
    }

    private List<String> parsePriorityShops(String input, List<String> allShopIds) {
        Set<String> validIds = new LinkedHashSet<>();
        String[] parts = input.split("[\\s,;]+");
        for (String id : parts) {
            String cleaned = id.trim().replaceAll("[^0-9]", "");
            if (!cleaned.isEmpty() && allShopIds.contains(cleaned)) validIds.add(cleaned);
        }
        return new ArrayList<>(validIds);
    }

    private void validateCodeBudget(int total, int count200, int count100, int count50) {
        int fixed = count200 * 200 + count100 * 100 + count50 * 50;
        if (fixed > total) {
            throw new IllegalArgumentException("Not enough codes. Fixed total (" + fixed + ") exceeds budget (" + total + ").");
        }
    }

    private Map<String, Integer> distributeCodes(List<String> allShops, List<String> prio200, List<String> prio100, List<String> prio50, int total) {
        Map<String, Integer> result = new LinkedHashMap<>();
        Set<String> prioritySet = new HashSet<>();
        prioritySet.addAll(prio200);
        prioritySet.addAll(prio100);
        prioritySet.addAll(prio50);

        int fixedTotal = prio200.size() * 200 + prio100.size() * 100 + prio50.size() * 50;
        for (String id : prio200) result.put(id, 200);
        for (String id : prio100) result.put(id, 100);
        for (String id : prio50) result.put(id, 50);

        int remaining = total - fixedTotal;
        List<String> others = new ArrayList<>();
        for (String id : allShops) {
            if (!prioritySet.contains(id)) others.add(id);
        }

        int per = others.size() > 0 ? remaining / others.size() : 0;
        int extra = others.size() > 0 ? remaining % others.size() : 0;

        for (String id : others) {
            int val = per + (extra-- > 0 ? 1 : 0);
            result.put(id, val);
        }

        return result;
    }

    private Map<String, Integer> emptyMap(List<String> ids) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String id : ids) {
            map.put(id, 0);
        }
        return map;
    }

    private void previewAndConfirm(List<String> shopIds, Map<String, Integer> codes5FB, Map<String, Integer> codes10FB, Map<String, Integer> codes5RFB, Map<String, Integer> codes10RFB, String outputFileName) {
        String[] columns = {"AKID", "5FB", "10FB", "5RFB", "10RFB"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        for (String id : shopIds) {
            model.addRow(new Object[]{
                    id,
                    codes5FB.getOrDefault(id, 0),
                    codes10FB.getOrDefault(id, 0),
                    codes5RFB.getOrDefault(id, 0),
                    codes10RFB.getOrDefault(id, 0)
            });
        }

        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(700, 300));

        int option = JOptionPane.showConfirmDialog(frame, scrollPane, "Preview CSV - Confirm to Save", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(System.getProperty("user.home") + "/Desktop/" + outputFileName))) {
                pw.println("AKID;5FB;10FB;5RFB;10RFB");
                for (int i = 0; i < model.getRowCount(); i++) {
                    pw.printf("%s;%s;%s;%s;%s%n",
                            model.getValueAt(i, 0),
                            model.getValueAt(i, 1),
                            model.getValueAt(i, 2),
                            model.getValueAt(i, 3),
                            model.getValueAt(i, 4));
                }
                JOptionPane.showMessageDialog(frame, "CSV saved to Desktop.");
                Desktop.getDesktop().open(new File(System.getProperty("user.home") + "/Desktop/" + outputFileName));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Error writing file: " + e.getMessage());
            }
        }
    }
}
