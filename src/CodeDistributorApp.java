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

    // Your app's current version
    private static final String currentVersion = "1.0.9";
    // URL to your raw latest.txt on GitHub (make sure this is raw URL!)
    private static final String LATEST_URL = "https://raw.githubusercontent.com/adikure20-cpu/CombiTry1/main/latest.txt";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame tempFrame = new JFrame(); // For update popup dialog parent
            checkForUpdate(tempFrame);        // Check for updates before UI launch
            new CodeDistributorApp().createUI();
        });
    }

    // Version comparison helper: returns >0 if v1 > v2, 0 if equal, <0 if v1 < v2
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

    // Check for update by reading version and download URL from latest.txt on GitHub
    public static void checkForUpdate(Component parent) {
        try {
            java.net.URL url = new java.net.URL(LATEST_URL);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String latestVersion = in.readLine();
            String downloadUrl = in.readLine();
            in.close();

            // Debug output (useful to see if whitespace or version mismatch)
            System.out.println("DEBUG: currentVersion='" + currentVersion + "'");
            System.out.println("DEBUG: latestVersion='" + latestVersion + "'");
            System.out.println("DEBUG: downloadUrl='" + downloadUrl + "'");

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
            // Fail silently or log error for offline or connection issues
            System.out.println("DEBUG: Exception in update check: " + e);
        }
    }

    private void createUI() {
        frame = new JFrame("Code Distributor");

        // Removed icon setting code

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 450);
        frame.setLayout(new GridLayout(0, 2, 10, 10));

        JButton uploadButton = new JButton("Upload CSV");
        JLabel fileLabel = new JLabel("No file selected");

        JTextField total5Field = new JTextField();
        JTextField total10Field = new JTextField();
        JTextField total5RFBField = new JTextField();
        JTextField total10RFBField = new JTextField();

        JTextField highACShopsField = new JTextField();  // new field
        JTextField priorityAmountField = new JTextField();

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
                int priorityAmount = Integer.parseInt(priorityAmountField.getText().trim());

                List<String> shopIds = loadShopIds(selectedCSV);
                List<String> highACShops = parseHighACShops(highACShopsField.getText(), shopIds);

                Map<String, Integer> codes5FB = distributeCodes(shopIds, highACShops, total5, priorityAmount);
                Map<String, Integer> codes10FB = distributeCodes(shopIds, highACShops, total10, priorityAmount);
                Map<String, Integer> codes5RFB = distributeCodes(shopIds, highACShops, total5RFB, priorityAmount);
                Map<String, Integer> codes10RFB = distributeCodes(shopIds, highACShops, total10RFB, priorityAmount);

                File createdFile = saveToCSV(shopIds, highACShops, codes5FB, codes10FB, codes5RFB, codes10RFB);

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

        frame.add(new JLabel("Shops High AC (IDs, comma separated):"));
        frame.add(highACShopsField);

        frame.add(new JLabel("Priority Amount per Shop:"));
        frame.add(priorityAmountField);

        frame.add(new JLabel()); // spacer
        frame.add(createButton);

        frame.setVisible(true);
    }

    // Rest of your methods unchanged, but here for completeness:

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
                    throw new IOException("Invalid Shop ID at line " + lineNumber + ": \"" + line + "\". Only numeric IDs allowed.\n\nExample:\nShop ID\n12345\n67890");
                }

                idSet.add(cleaned);
                lineNumber++;
            }
        }

        if (idSet.isEmpty()) {
            throw new IOException("CSV contains no valid Shop IDs.\n\nExample:\nShop ID\n12345\n67890");
        }

        return new ArrayList<>(idSet);
    }

    private List<String> parseHighACShops(String input, List<String> allShopIds) {
        Set<String> validIds = new LinkedHashSet<>();
        for (String id : input.split(",")) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty() && allShopIds.contains(trimmed)) {
                validIds.add(trimmed);
            }
        }
        return new ArrayList<>(validIds);
    }

    private Map<String, Integer> distributeCodes(List<String> allShops, List<String> highACShops, int total, int priorityAmount) {
        Map<String, Integer> result = new LinkedHashMap<>();

        int highACCount = highACShops.size();
        int fixedTotal = highACCount * priorityAmount;

        if (total < fixedTotal && highACCount > 0) {
            int perPriority = total / highACCount;
            int leftoverPriority = total % highACCount;

            for (int i = 0; i < highACCount; i++) {
                int amount = perPriority + (leftoverPriority > 0 ? 1 : 0);
                if (amount < 0) amount = 0;
                result.put(highACShops.get(i), amount);
                if (leftoverPriority > 0) leftoverPriority--;
            }

            Set<String> highACSet = new HashSet<>(highACShops);
            for (String shop : allShops) {
                if (!highACSet.contains(shop)) {
                    result.put(shop, 0);
                }
            }
        } else {
            int remaining = total - fixedTotal;

            Set<String> highACSet = new HashSet<>(highACShops);
            List<String> others = new ArrayList<>();
            for (String shop : allShops) {
                if (!highACSet.contains(shop)) others.add(shop);
            }

            int perOther = (others.size() > 0) ? remaining / others.size() : 0;
            int leftover = (others.size() > 0) ? remaining % others.size() : 0;

            for (String id : highACShops) {
                int assigned = priorityAmount;
                if (assigned < 0) assigned = 0;
                result.put(id, assigned);
            }

            for (int i = 0; i < others.size(); i++) {
                String id = others.get(i);
                int amount = perOther + (leftover > 0 ? 1 : 0);
                if (amount < 0) amount = 0;
                result.put(id, amount);
                if (leftover > 0) leftover--;
            }
        }

        return result;
    }

    private File saveToCSV(List<String> allShopIds,
                           List<String> highACShops,
                           Map<String, Integer> codes5FB,
                           Map<String, Integer> codes10FB,
                           Map<String, Integer> codes5RFB,
                           Map<String, Integer> codes10RFB) throws IOException {

        List<String> orderedShops = new ArrayList<>(highACShops);
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
