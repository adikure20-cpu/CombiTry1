import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.List;

public class CodeDistributorApp {

    private JFrame frame;
    private File selectedCSV;

    private static final String currentVersion = "1.0.12";
    private static final String LATEST_URL = "https://raw.githubusercontent.com/adikure20-cpu/CombiTry1/main/latest.txt";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame tempFrame = new JFrame();
            checkForUpdate(tempFrame);
            new CodeDistributorApp().createUI();
        });
    }

    public static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.trim().split("\\.");
        String[] parts2 = v2.trim().split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 < p2) return -1;
            if (p1 > p2) return 1;
        }
        return 0;
    }

    public static void checkForUpdate(Component parent) {
        try {
            java.net.URL url = new java.net.URL(LATEST_URL);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String latestVersion = in.readLine();
            String downloadUrl = in.readLine();
            in.close();

            if (latestVersion != null && downloadUrl != null
                    && compareVersions(latestVersion, currentVersion) > 0) {
                int option = JOptionPane.showConfirmDialog(parent,
                        "A new version (" + latestVersion + ") is available!\nDo you want to download it?",
                        "Update Available",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE
                );
                if (option == JOptionPane.YES_OPTION) {
                    Desktop.getDesktop().browse(new URI(downloadUrl));
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Exception in update check: " + e);
        }
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

                Map<String, Integer> codes5FB = distributeCodes(shopIds, prio200, prio100, prio50, total5);
                Map<String, Integer> codes10FB = distributeCodes(shopIds, prio200, prio100, prio50, total10);
                Map<String, Integer> codes5RFB = distributeCodes(shopIds, prio200, prio100, prio50, total5RFB);
                Map<String, Integer> codes10RFB = distributeCodes(shopIds, prio200, prio100, prio50, total10RFB);

                List<String> allPriorityShops = new ArrayList<>();
                allPriorityShops.addAll(prio200);
                allPriorityShops.addAll(prio100);
                allPriorityShops.addAll(prio50);

                File createdFile = saveToCSV(shopIds, allPriorityShops, codes5FB, codes10FB, codes5RFB, codes10RFB);
                JOptionPane.showMessageDialog(frame, "CSV file created successfully on your Desktop.");

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(createdFile);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Please enter valid numbers.");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
            }
        });

        frame.add(uploadButton);
        frame.add(fileLabel);

        frame.add(new JLabel("Total €5 Codes (5FB):"));
        frame.add(total5Field);

        frame.add(new JLabel("Total €10 Codes (10FB):"));
        frame.add(total10Field);

        frame.add(new JLabel("Total €5 RFB Codes (5RFB):"));
        frame.add(total5RFBField);

        frame.add(new JLabel("Total €10 RFB Codes (10RFB):"));
        frame.add(total10RFBField);

        frame.add(new JLabel("Priority 200 (IDs, comma separated):"));
        frame.add(priority200Field);

        frame.add(new JLabel("Priority 100 (IDs, comma separated):"));
        frame.add(priority100Field);

        frame.add(new JLabel("Priority 50 (IDs, comma separated):"));
        frame.add(priority50Field);

        frame.add(new JLabel()); // spacer
        frame.add(createButton);

        frame.setVisible(true);
    }

    private List<String> loadShopIds(File csvFile) throws IOException {
        Set<String> idSet = new LinkedHashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String header = br.readLine();
            if (header == null || !header.replaceAll("[^a-zA-Z0-9]", "").equalsIgnoreCase("ShopID")) {
                throw new IOException("Invalid CSV format. First line must be: Shop ID");
            }

            String line;
            int lineNumber = 2;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String cleaned = line.replaceAll("[^0-9]", "");

                if (!cleaned.matches("\\d+")) {
                    throw new IOException("Invalid Shop ID at line " + lineNumber + ": \"" + line + "\". Only numeric IDs allowed.");
                }

                idSet.add(cleaned);
                lineNumber++;
            }
        }

        if (idSet.isEmpty()) {
            throw new IOException("CSV contains no valid Shop IDs.");
        }

        return new ArrayList<>(idSet);
    }

    private List<String> parsePriorityShops(String input, List<String> allShopIds) {
        Set<String> validIds = new LinkedHashSet<>();
        String[] parts = input.split("[,;\\s]+");  // Split on comma, semicolon, whitespace, tabs, etc.
        for (String id : parts) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty() && allShopIds.contains(trimmed)) {
                validIds.add(trimmed);
            }
        }
        return new ArrayList<>(validIds);
    }


    private Map<String, Integer> distributeCodes(List<String> allShops,
                                                 List<String> prio200, List<String> prio100, List<String> prio50,
                                                 int total) {
        Map<String, Integer> result = new LinkedHashMap<>();

        Set<String> prioritySet = new LinkedHashSet<>();
        prioritySet.addAll(prio200);
        prioritySet.addAll(prio100);
        prioritySet.addAll(prio50);

        int fixedTotal = prio200.size() * 200 + prio100.size() * 100 + prio50.size() * 50;

        // Assign fixed values to priority shops
        for (String id : prio200) result.put(id, 200);
        for (String id : prio100) result.put(id, 100);
        for (String id : prio50) result.put(id, 50);

        int remaining = total - fixedTotal;
        if (remaining < 0) remaining = 0;

        // Distribute remaining codes to non-priority shops
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
            int finalAmount = Math.max(0, perOther + extra); // ensure no negative
            result.put(shop, finalAmount);
            if (leftover > 0) leftover--;
        }

        return result;
    }


    private File saveToCSV(List<String> allShopIds,
                           List<String> highPriorityShops,
                           Map<String, Integer> codes5FB,
                           Map<String, Integer> codes10FB,
                           Map<String, Integer> codes5RFB,
                           Map<String, Integer> codes10RFB) throws IOException {

        List<String> orderedShops = new ArrayList<>(highPriorityShops);
        for (String id : allShopIds) {
            if (!orderedShops.contains(id)) orderedShops.add(id);
        }

        String userHome = System.getProperty("user.home");
        File desktopDir = new File(userHome, "Desktop");
        File output = new File(desktopDir, "Code_Distribution_Output.csv");

        try (PrintWriter pw = new PrintWriter(new FileWriter(output))) {
            pw.println("AKID;5FB;10FB;5RFB;10RFB");

            for (String id : orderedShops) {
                String val5FB = getOrZero(codes5FB.get(id));
                String val10FB = getOrZero(codes10FB.get(id));
                String val5RFB = getOrZero(codes5RFB.get(id));
                String val10RFB = getOrZero(codes10RFB.get(id));
                pw.println(String.join(";", id, val5FB, val10FB, val5RFB, val10RFB));
            }
        }

        return output;
    }

    private String getOrZero(Integer val) {
        return (val == null) ? "0" : val.toString();
    }
}
