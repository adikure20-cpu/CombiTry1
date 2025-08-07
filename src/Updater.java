import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.awt.BorderLayout;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Updater {

    private static final String LATEST_INFO_URL = "https://raw.githubusercontent.com/adikure20-cpu/CombiTry1/main/latest.txt";
    private static final String CURRENT_VERSION = "1.0.30"; // Update this manually per release
    private static final String VERSION_CACHE_FILE = System.getProperty("user.home") + "/.combitry1_version.txt";

    public static void checkForUpdates() {
        try {
            // Fetch latest.txt from GitHub
            URL url = new URL(LATEST_INFO_URL);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String latestVersion = reader.readLine().trim();
            String jarDownloadUrl = reader.readLine().trim();
            reader.close();

            // Read cached version
            String cachedVersion = readCachedVersion();

            // Skip if latest already downloaded and launched
            if (CURRENT_VERSION.equals(latestVersion) || latestVersion.equals(cachedVersion)) {
                System.out.println("✅ Already on latest version.");
                return;
            }

            // Ask user
            int confirm = JOptionPane.showConfirmDialog(null,
                    "New version " + latestVersion + " available.\nDownload and restart now?",
                    "Update Available", JOptionPane.YES_NO_OPTION);

            if (confirm != JOptionPane.YES_OPTION) return;

            File targetFile = new File(System.getProperty("user.home") + "/Downloads/CombiTry1.jar");

            // Download with progress UI
            JDialog progressDialog = new JDialog((JFrame) null, "Downloading Update", true);
            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            progressDialog.add(BorderLayout.CENTER, progressBar);
            progressDialog.setSize(400, 80);
            progressDialog.setLocationRelativeTo(null);

            SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
                @Override
                protected Void doInBackground() throws Exception {
                    HttpURLConnection connection = (HttpURLConnection) new URL(jarDownloadUrl).openConnection();
                    int fileSize = connection.getContentLength();

                    try (InputStream in = connection.getInputStream();
                         FileOutputStream out = new FileOutputStream(targetFile)) {

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        int totalRead = 0;

                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;
                            int percent = (int) ((totalRead / (float) fileSize) * 100);
                            publish(percent);
                        }
                    }

                    return null;
                }

                @Override
                protected void process(java.util.List<Integer> chunks) {
                    progressBar.setValue(chunks.get(chunks.size() - 1));
                }

                @Override
                protected void done() {
                    progressDialog.dispose();
                    saveCachedVersion(latestVersion);
                    launchNewJar(targetFile);
                }
            };

            worker.execute();
            progressDialog.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "❌ Update failed: " + e.getMessage());
        }
    }

    private static void launchNewJar(File downloadedJar) {
        try {
            new ProcessBuilder("java", "-jar", downloadedJar.getAbsolutePath()).start();
            System.exit(0);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "❌ Failed to launch new version: " + ex.getMessage());
        }
    }

    private static String readCachedVersion() {
        try {
            File file = new File(VERSION_CACHE_FILE);
            if (file.exists()) {
                return new String(Files.readAllBytes(file.toPath())).trim();
            }
        } catch (IOException ignored) {}
        return "";
    }

    private static void saveCachedVersion(String version) {
        try {
            Files.write(new File(VERSION_CACHE_FILE).toPath(), version.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
