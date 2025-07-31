import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.Desktop;
import java.awt.GridLayout;

import java.io.*;
import java.util.*;

public class CodeDistributorApp {

    private JFrame frame;
    private File selectedCSV;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CodeDistributorApp().createUI());
    }

    private void createUI() {
        frame = new JFrame("Code Distributor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLayout(new GridLayout(0, 2, 10, 10));

        JButton uploadButton = new JButton("Upload CSV");
        JLabel fileLabel = new JLabel("No file selected");

        JTextField total5Field = new JTextField();
        JTextField total10Field = new JTextField();
        JTextField total5RFBField = new JTextField();
        JTextField total10RFBField = new JTextField();

        JTextField priority200Field = new JTextField();
        JTextField priority100Field = new JTextField();
        JTextField priority50Field = new JTextField();

        JButton createButton = new JButton("Create CSV");

        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                selectedCSV = fileChooser.getSelectedFile();
                fileLabel.setText(selectedCSV.getName());
            }
        });

        createButton.addActionListener(e -> {
            if (selectedCSV == null) {
                JOptionPane.showMessageDialog(frame, "Please upload a CSV file.");
                return;
            }

            try {
                int total5 = Integer.parseInt(total5Field.getText().trim());
                int total10 = Integer.parseInt(total10Field.getText().trim());
                int total5RFB = Integer.parseInt(total5RFBField.getText().trim());
                int total10RFB = Integer.parseInt(total10RFBField.getText().trim());

                List<String> shopIds = loadShopIds(selectedCSV);
                List<String> prio200 = parsePriorityShops(priority200Field.getText(), shopIds);
                List<String> prio100 = parsePriorityShops(priority100Field.getText(), shopIds);
                List<String> prio50 = parsePriorityShops(priority50Field.getText(), shopIds);

                validateCodeBudget(total5, prio200.size(), prio100.size(), prio50.size());
                validateCodeBudget(total10, prio200.size(), prio100.size(), prio50.size());

                Map<String, Integer> codes5FB = distributeCodes(shopIds, prio200, prio100, prio50, total5);
                Map<String, Integer> codes10FB = distributeCodes(shopIds, prio200, prio100, prio50, total10);
                Map<String, Integer> codes5RFB = (total5RFB > 0)
                        ? distributeCodes(shopIds, prio200, prio100, prio50, total5RFB)
                        : emptyMap(shopIds);

                Map<String, Integer> codes10RFB = (total10RFB > 0)
                        ? distributeCodes(shopIds, prio200, prio100, prio50, total10RFB)
                        : emptyMap(shopIds);

                List<String> allPriorityShops = new ArrayList<>();
                allPriorityShops.addAll(prio200);
                allPriorityShops.addAll(prio100);
                allPriorityShops.addAll(prio50);

                File createdFile = saveToCSV(shopIds, allPriorityShops, codes5FB, codes10FB, codes5RFB, codes10RFB);
                JOptionPane.showMessageDialog(frame, "CSV file created successfully on your Desktop.");

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(createdFile);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
            }
        });

        frame.add(uploadButton); frame.add(fileLabel);
        frame.add(new JLabel("Total €5 Codes (5FB):")); frame.add(total5Field);
        frame.add(new JLabel("Total €10 Codes (10FB):")); frame.add(total10Field);
        frame.add(new JLabel("Total €5 RFB Codes (5RFB):")); frame.add(total5RFBField);
        frame.add(new JLabel("Total €10 RFB Codes (10RFB):")); frame.add(total10RFBField);
        frame.add(new JLabel("Priority 200 (IDs, comma or space separated):")); frame.add(priority200Field);
        frame.add(new JLabel("Priority 100 (IDs):")); frame.add(priority100Field);
        frame.add(new JLabel("Priority 50 (IDs):")); frame.add(priority50Field);
        frame.add(new JLabel()); frame.add(createButton);

        frame.setVisible(true);
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
            if (!cleaned.isEmpty() && allShopIds.contains(cleaned)) {
                validIds.add(cleaned);
            }
        }
        return new ArrayList<>(validIds);
    }

    private void validateCodeBudget(int total, int count200, int count100, int count50) {
        int fixed = count200 * 200 + count100 * 100 + count50 * 50;
        if (fixed > total) {
            throw new IllegalArgumentException("Not enough codes available. Fixed priority total (" + fixed + ") exceeds budget (" + total + ").");
        }
    }

    private Map<String, Integer> distributeCodes(List<String> allShops, List<String> prio200, List<String> prio100, List<String> prio50, int total) {
        Map<String, Integer> result = new LinkedHashMap<>();

        Set<String> prioritySet = new LinkedHashSet<>();
        prioritySet.addAll(prio200);
        prioritySet.addAll(prio100);
        prioritySet.addAll(prio50);

        int fixedTotal = prio200.size() * 200 + prio100.size() * 100 + prio50.size() * 50;

        for (String id : prio200) result.put(id, 200);
        for (String id : prio100) result.put(id, 100);
        for (String id : prio50) result.put(id, 50);

        int remaining = total - fixedTotal;
        if (remaining < 0) remaining = 0;

        List<String> nonPriorityShops = new ArrayList<>();
        for (String shop : allShops) {
            if (!prioritySet.contains(shop)) {
                nonPriorityShops.add(shop);
            }
        }

        int perOther = nonPriorityShops.size() > 0 ? remaining / nonPriorityShops.size() : 0;
        int leftover = nonPriorityShops.size() > 0 ? remaining % nonPriorityShops.size() : 0;

        for (String shop : nonPriorityShops) {
            int extra = leftover > 0 ? 1 : 0;
            int finalAmount = Math.max(0, perOther + extra);
            result.put(shop, finalAmount);
            if (leftover > 0) leftover--;
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

    private File saveToCSV(List<String> allShopIds, List<String> highPriorityShops,
                           Map<String, Integer> codes5FB, Map<String, Integer> codes10FB,
                           Map<String, Integer> codes5RFB, Map<String, Integer> codes10RFB) {

        List<String> orderedShops = new ArrayList<>(highPriorityShops);
        for (String id : allShopIds) {
            if (!orderedShops.contains(id)) orderedShops.add(id);
        }

        File output = new File(System.getProperty("user.home") + "/Desktop/Code_Distribution_Output.csv");

        try (PrintWriter pw = new PrintWriter(new FileWriter(output))) {
            pw.println("AKID;5FB;10FB;5RFB;10RFB");
            for (String id : orderedShops) {
                pw.println(String.join(";",
                        id,
                        String.valueOf(codes5FB.getOrDefault(id, 0)),
                        String.valueOf(codes10FB.getOrDefault(id, 0)),
                        String.valueOf(codes5RFB.getOrDefault(id, 0)),
                        String.valueOf(codes10RFB.getOrDefault(id, 0))));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CSV file", e);
        }

        return output;
    }
}
