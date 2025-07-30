import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class CodeDistributorApp {
    private static final String CURRENT_VERSION = "1.0.16";
    private static final String VERSION_URL = "https://raw.githubusercontent.com/adikure20-cpu/CombiTry1/main/latest.txt";
    private static final String DOWNLOAD_URL = "https://github.com/adikure20-cpu/CombiTry1/releases/download/v1.0.15/CombiTry1.jar";

    private JFrame frame;
    private File selectedCSV;

    public static void main(String[] args) {
        if (checkForUpdate()) {
            int result = JOptionPane.showConfirmDialog(null,
                    "A new version is available. Do you want to update now?",
                    "Update Available",
                    JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                downloadAndUpdate();
                return;
            }
        }

        SwingUtilities.invokeLater(() -> new CodeDistributorApp().createUI());
    }

    private static boolean checkForUpdate() {
        try {
            URL url = new URL(VERSION_URL);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String latestVersion = in.readLine().trim();
            in.close();
            return isNewVersionAvailable(CURRENT_VERSION, latestVersion);
        } catch (IOException e) {
            System.err.println("Failed to check for update: " + e.getMessage());
            return false;
        }
    }

    private static boolean isNewVersionAvailable(String current, String latest) {
        String[] currentParts = current.split("\\.");
        String[] latestParts = latest.split("\\.");

        int length = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            int currentNum = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int latestNum = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

            if (latestNum > currentNum) {
                return true;
            } else if (latestNum < currentNum) {
                return false;
            }
        }
        return false; // versions are equal
    }

    private static void downloadAndUpdate() {
        try {
            URL downloadUrl = new URL(DOWNLOAD_URL);
            File tempJar = File.createTempFile("update", ".jar");
            tempJar.deleteOnExit();

            try (InputStream in = downloadUrl.openStream()) {
                Files.copy(in, tempJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            Runtime.getRuntime().exec("java -jar " + tempJar.getAbsolutePath());
            System.exit(0);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to download the update: " + e.getMessage(),
                    "Update Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createUI() {
        frame = new JFrame("Code Distributor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 450);
        frame.setLayout(new GridLayout(0, 2, 10, 10));

        JButton uploadButton = new JButton("Upload CSV");
        JLabel fileLabel = new JLabel("No file selected");

        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
            int option = fileChooser.showOpenDialog(frame);
            if (option == JFileChooser.APPROVE_OPTION) {
                selectedCSV = fileChooser.getSelectedFile();
                fileLabel.setText(selectedCSV.getName());
            }
        });

        frame.add(uploadButton);
        frame.add(fileLabel);

        frame.setVisible(true);
    }
}
