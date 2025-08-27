import com.formdev.flatlaf.FlatIntelliJLaf;
import javax.swing.text.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.RowFilter;
import java.util.regex.Pattern;

public class CodeDistributorApp {
    private JFrame frame;
    private File selectedCSV;

    private final LinkedHashSet<String> sel200 = new LinkedHashSet<>();
    private final LinkedHashSet<String> sel100 = new LinkedHashSet<>();
    private final LinkedHashSet<String> sel50  = new LinkedHashSet<>();
    private List<String> shopsCache = new ArrayList<>();

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(new FlatIntelliJLaf()); } catch (Exception e) { System.err.println("Failed to initialize FlatLaf: " + e.getMessage()); }
        SwingUtilities.invokeLater(() -> {
            Splash splash = new Splash();
            splash.setVisible(true);
            new SwingWorker<Void, Void>() {
                protected Void doInBackground() { Updater.checkForUpdates(); return null; }
                protected void done() { new CodeDistributorApp().initUI(); splash.dispose(); }
            }.execute();
        });
    }

    private void limitNumeric(JTextField field, int maxDigits) {
        AbstractDocument doc = (AbstractDocument) field.getDocument();
        doc.setDocumentFilter(new NumericLimitFilter(maxDigits));
    }

    private void stylePrimary(JButton b) {
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.putClientProperty("FlatLaf.style", "background: #1976D2; arc: 18; focusWidth: 1; borderColor: #1565C0; hoverBackground: #1E88E5; pressedBackground: #0D47A1");
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
    }

    private void styleSecondary(JButton b) {
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.putClientProperty("FlatLaf.style", "background: #E0E0E0; arc: 18; focusWidth: 1; borderColor: #BDBDBD; hoverBackground: #EEEEEE; pressedBackground: #BDBDBD");
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
    }

    private void applyButtonState(JButton b, boolean active) {
        if (active) { stylePrimary(b); b.setEnabled(true); } else { styleSecondary(b); b.setEnabled(false); }
        b.putClientProperty("JButton.defaultButton", Boolean.FALSE);
        SwingUtilities.invokeLater(() -> {
            JRootPane rp = frame != null ? frame.getRootPane() : null;
            if (rp != null && rp.getDefaultButton() == b) rp.setDefaultButton(null);
        });
    }

    private JPanel createSinglePerShopTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField acid = createFieldWithTooltip("Enter Shop IDs (space or comma separated)");
        JTextField f5 = createFieldWithTooltip("€5 FB per shop");
        JTextField f10 = createFieldWithTooltip("€10 FB per shop");
        JTextField f5r = createFieldWithTooltip("€5 RFB per shop");
        JTextField f10r = createFieldWithTooltip("€10 RFB per shop");

        limitNumeric(f5, 6); limitNumeric(f10, 6); limitNumeric(f5r, 6); limitNumeric(f10r, 6);

        JButton preview = new JButton("Preview & Confirm", UIManager.getIcon("FileView.detailsViewIcon"));
        applyButtonState(preview, false);
        f5.setEnabled(false); f10.setEnabled(false); f5r.setEnabled(false); f10r.setEnabled(false);

        int row = 0;
        panel.add(new JLabel("Shop IDs:"), gbc(0, row)); panel.add(acid, gbc(1, row++));
        panel.add(new JLabel("€5 FB per shop:"), gbc(0, row)); panel.add(f5, gbc(1, row++));
        panel.add(new JLabel("€10 FB per shop:"), gbc(0, row)); panel.add(f10, gbc(1, row++));
        panel.add(new JLabel("€5 RFB per shop:"), gbc(0, row)); panel.add(f5r, gbc(1, row++));
        panel.add(new JLabel("€10 RFB per shop:"), gbc(0, row)); panel.add(f10r, gbc(1, row++));
        gbc.gridwidth = 2; panel.add(preview, gbc(0, row++));

        Runnable unlockIfAnyId = () -> {
            boolean hasId = !parseList(acid.getText()).isEmpty();
            f5.setEnabled(hasId); f10.setEnabled(hasId); f5r.setEnabled(hasId); f10r.setEnabled(hasId);
            applyButtonState(preview, hasId);
        };
        addDocListener(acid, unlockIfAnyId);

        preview.addActionListener(e -> {
            try {
                List<String> ids = parseList(acid.getText());
                if (ids.isEmpty()) { showMessage("Add at least one Shop ID."); return; }
                int v5 = parse(f5.getText()), v10 = parse(f10.getText()), v5r = parse(f5r.getText()), v10r = parse(f10r.getText());
                Map<String, Integer> m5 = mapFixed(ids, v5), m10 = mapFixed(ids, v10), m5rb = mapFixed(ids, v5r), m10rb = mapFixed(ids, v10r);
                preview(ids, m5, m10, m5rb, m10rb, "Single_Shop_Codes.csv");
            } catch (Exception ex) { showMessage("❌ Error: " + ex.getMessage()); }
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
        inner.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel fileLabel = new JLabel("No file selected");
        JButton uploadButton = new JButton("Upload CSV", UIManager.getIcon("FileView.directoryIcon"));
        styleSecondary(uploadButton);

        JTextField total5 = createFieldWithTooltip("Total number of €5 codes to distribute (FB)");
        JTextField total10 = createFieldWithTooltip("Total number of €10 codes to distribute (FB)");
        JTextField total5RFB = createFieldWithTooltip("Total number of €5 RFB codes to distribute");
        JTextField total10RFB = createFieldWithTooltip("Total number of €10 RFB codes to distribute");

        JButton pick200 = new JButton("Select (0)");
        JButton pick100 = new JButton("Select (0)");
        JButton pick50  = new JButton("Select (0)");
        styleSecondary(pick200); styleSecondary(pick100); styleSecondary(pick50);

        ((AbstractDocument) total5.getDocument()).setDocumentFilter(new NumericLimitFilter(6));
        ((AbstractDocument) total10.getDocument()).setDocumentFilter(new NumericLimitFilter(6));
        ((AbstractDocument) total5RFB.getDocument()).setDocumentFilter(new NumericLimitFilter(6));
        ((AbstractDocument) total10RFB.getDocument()).setDocumentFilter(new NumericLimitFilter(6));

        JButton previewButton = new JButton("Preview & Confirm", UIManager.getIcon("FileView.detailsViewIcon"));
        applyButtonState(previewButton, false);

        total5.setEnabled(false); total10.setEnabled(false); total5RFB.setEnabled(false); total10RFB.setEnabled(false);
        pick200.setEnabled(false); pick100.setEnabled(false); pick50.setEnabled(false);

        int row = 0;
        inner.add(uploadButton, gbc(0, row)); inner.add(fileLabel, gbc(1, row++, GridBagConstraints.WEST));
        inner.add(new JLabel("Total €5 Codes (5FB):"), gbc(0, row)); inner.add(total5, gbc(1, row++));
        inner.add(new JLabel("Total €10 Codes (10FB):"), gbc(0, row)); inner.add(total10, gbc(1, row++));
        inner.add(new JLabel("Total €5 RFB Codes:"), gbc(0, row)); inner.add(total5RFB, gbc(1, row++));
        inner.add(new JLabel("Total €10 RFB Codes:"), gbc(0, row)); inner.add(total10RFB, gbc(1, row++));
        inner.add(new JLabel("Priority 200:"), gbc(0, row)); inner.add(pick200, gbc(1, row++));
        inner.add(new JLabel("Priority 100:"), gbc(0, row)); inner.add(pick100, gbc(1, row++));
        inner.add(new JLabel("Priority 50:"), gbc(0, row)); inner.add(pick50, gbc(1, row++));
        gbc.gridwidth = 2; inner.add(previewButton, gbc(0, row++));

        uploadButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                selectedCSV = chooser.getSelectedFile();
                try { shopsCache = loadShopIds(selectedCSV); } catch (IOException ex) { showMessage("❌ Error reading CSV: " + ex.getMessage()); return; }
                fileLabel.setText("✔ " + selectedCSV.getName());
                total5.setEnabled(true); total10.setEnabled(true); total5RFB.setEnabled(true); total10RFB.setEnabled(true);
                pick200.setEnabled(true); pick100.setEnabled(true); pick50.setEnabled(true);
                applyButtonState(previewButton, true);
                sel200.clear(); sel100.clear(); sel50.clear();
                refreshPickButtonText(pick200, sel200); refreshPickButtonText(pick100, sel100); refreshPickButtonText(pick50, sel50);
            }
        });

        pick200.addActionListener(e -> {
            LinkedHashSet<String> chosen = MultiSelectDialog.pick(
                    frame,
                    "Select Priority 200",
                    shopsCache,              // all shops available
                    sel200
            );
            if (chosen != null) {
                sel200.clear();
                sel200.addAll(chosen);
                ensureDisjoint();
                refreshPickButtonText(pick200, sel200);
                refreshPickButtonText(pick100, sel100);
                refreshPickButtonText(pick50,  sel50);
            }
        });

        pick100.addActionListener(e -> {
            List<String> available100 = exclude(shopsCache, sel200); // hide 200 selections
            LinkedHashSet<String> chosen = MultiSelectDialog.pick(
                    frame,
                    "Select Priority 100",
                    available100,
                    sel100
            );
            if (chosen != null) {
                sel100.clear();
                sel100.addAll(chosen);
                ensureDisjoint();
                refreshPickButtonText(pick200, sel200);
                refreshPickButtonText(pick100, sel100);
                refreshPickButtonText(pick50,  sel50);
            }
        });

        pick50.addActionListener(e -> {
            List<String> available50 = exclude(shopsCache, sel200, sel100); // hide 200 & 100
            LinkedHashSet<String> chosen = MultiSelectDialog.pick(
                    frame,
                    "Select Priority 50",
                    available50,
                    sel50
            );
            if (chosen != null) {
                sel50.clear();
                sel50.addAll(chosen);
                ensureDisjoint();
                refreshPickButtonText(pick200, sel200);
                refreshPickButtonText(pick100, sel100);
                refreshPickButtonText(pick50,  sel50);
            }
        });

        previewButton.addActionListener(e -> {
            if (selectedCSV == null) { showMessage("Please select a CSV file first."); return; }
            try {
                int val5  = parse(total5.getText());
                int val10 = parse(total10.getText());
                int val5r = parse(total5RFB.getText());
                int val10r= parse(total10RFB.getText());

                List<String> p200 = keepOrder(shopsCache, sel200);
                List<String> p100 = keepOrder(shopsCache, sel100);
                List<String> p50  = keepOrder(shopsCache, sel50);

                if (val5  > 0) validate(val5,  p200.size(), p100.size(), p50.size());
                if (val10 > 0) validate(val10, p200.size(), p100.size(), p50.size());

                Map<String, Integer> m5   = distribute(shopsCache, p200, p100, p50, val5);
                Map<String, Integer> m10  = distribute(shopsCache, p200, p100, p50, val10);
                Map<String, Integer> m5r  = val5r  > 0 ? distribute(shopsCache, p200, p100, p50, val5r)  : zeroAll(shopsCache);
                Map<String, Integer> m10r = val10r > 0 ? distribute(shopsCache, p200, p100, p50, val10r) : zeroAll(shopsCache);

                List<String> ordered = new ArrayList<>();
                ordered.addAll(p200); ordered.addAll(p100); ordered.addAll(p50);
                for (String s : shopsCache) if (!ordered.contains(s)) ordered.add(s);

                preview(ordered, m5, m10, m5r, m10r, "Code_Distribution_Output.csv");
            } catch (Exception ex) { showMessage("❌ Error: " + ex.getMessage()); }
        });

        return inner;
    }

    private void refreshPickButtonText(JButton b, Collection<String> set) {
        int n = set.size();
        String tip = n == 0 ? "None selected" : previewIds(set);
        b.setText("Select (" + n + ")");
        b.setToolTipText(tip);
    }

    private String previewIds(Collection<String> ids) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String s : ids) {
            if (i > 0) sb.append(", ");
            sb.append(s);
            if (++i == 10) { sb.append("…"); break; }
        }
        return sb.toString();
    }

    private List<String> exclude(Collection<String> base, Collection<String>... toRemove) {
        LinkedHashSet<String> out = new LinkedHashSet<>(base);
        for (Collection<String> r : toRemove) out.removeAll(r);
        return new ArrayList<>(out);
    }

    private void ensureDisjoint() {
        sel100.removeAll(sel200);
        sel50.removeAll(sel200);
        sel50.removeAll(sel100);
    }

    private void addDocListener(JTextComponent c, Runnable r) {
        Document d = c.getDocument();
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        };
        if (d instanceof AbstractDocument) ((AbstractDocument) d).addDocumentListener(dl); else d.addDocumentListener(dl);
    }

    private JTextField createFieldWithTooltip(String tooltip) {
        JTextField field = new JTextField(50);
        field.setToolTipText(tooltip);
        return field;
    }

    private void preview(List<String> ids, Map<String, Integer> m5, Map<String, Integer> m10, Map<String, Integer> m5r, Map<String, Integer> m10r, String filename) {
        String[] cols = {"AKID", "5FB", "10FB", "5RFB", "10RFB"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        for (String id : ids) model.addRow(new Object[]{ id, m5.getOrDefault(id,0), m10.getOrDefault(id,0), m5r.getOrDefault(id,0), m10r.getOrDefault(id,0) });
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
            } catch (IOException e) { showMessage("❌ Error writing CSV: " + e.getMessage()); }
        }
    }

    private void validate(int total, int c200, int c100, int c50) {
        if (total <= 0) return;
        int need = c200 * 200 + c100 * 100 + c50 * 50;
        if (total < need) throw new IllegalArgumentException("Budget too low. Required: " + need + ", Provided: " + total);
    }

    private Map<String, Integer> distribute(List<String> all, List<String> p200, List<String> p100, List<String> p50, int total) {
        if (total <= 0) return zeroAll(all);
        Map<String, Integer> result = new LinkedHashMap<>();
        int fixed = 0;
        for (String id : p200) { if (!result.containsKey(id)) { result.put(id, 200); fixed += 200; } }
        for (String id : p100) { if (!result.containsKey(id)) { result.put(id, 100); fixed += 100; } }
        for (String id : p50)  { if (!result.containsKey(id)) { result.put(id,  50); fixed +=  50; } }
        int remain = total - fixed;
        if (remain < 0) remain = 0;
        List<String> rest = new ArrayList<>();
        for (String id : all) if (!result.containsKey(id)) rest.add(id);
        int each = rest.isEmpty() ? 0 : remain / rest.size();
        int extra = rest.isEmpty() ? 0 : remain % rest.size();
        for (String id : rest) result.put(id, each + (extra-- > 0 ? 1 : 0));
        return result;
    }

    private Map<String, Integer> mapFixed(List<String> ids, int val) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String id : ids) map.put(id, val);
        return map;
    }

    private Map<String, Integer> zeroAll(List<String> ids) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String id : ids) map.put(id, 0);
        return map;
    }

    private String norm(String s) {
        if (s == null) return "";
        String digits = s.replaceAll("\\D", "");
        if (digits.isEmpty() || digits.matches("0+")) return "";
        return digits.replaceFirst("^0+(?!$)", "");
    }

    private List<String> keepOrder(List<String> base, Collection<String> selected) {
        List<String> out = new ArrayList<>();
        LinkedHashSet<String> set = new LinkedHashSet<>(selected);
        for (String s : base) if (set.contains(s)) out.add(s);
        return out;
    }

    private List<String> parseList(String input) {
        if (input == null || input.trim().isEmpty()) return new ArrayList<>();
        Set<String> set = new LinkedHashSet<>();
        for (String s : input.split("[,\\s]+")) {
            String id = norm(s);
            if (!id.isEmpty()) set.add(id);
        }
        return new ArrayList<>(set);
    }

    private List<String> loadShopIds(File file) throws IOException {
        Set<String> ids = new LinkedHashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String first = line.split("[;,\\s]")[0];
                String id = norm(first);
                if (!id.isEmpty()) ids.add(id);
            }
        }
        return new ArrayList<>(ids);
    }

    private int parse(String input) {
        try { return Integer.parseInt((input == null ? "" : input.trim())); } catch (Exception e) { return 0; }
    }

    private void showMessage(String msg) { JOptionPane.showMessageDialog(frame, msg); }

    private GridBagConstraints gbc(int x, int y) { return gbc(x, y, GridBagConstraints.CENTER); }

    private GridBagConstraints gbc(int x, int y, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x; gbc.gridy = y; gbc.anchor = anchor; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 5, 4, 5);
        return gbc;
    }

    class NumericLimitFilter extends DocumentFilter {
        private final int maxLength;
        public NumericLimitFilter(int maxLength) { this.maxLength = maxLength; }
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string != null && string.matches("\\d*")) if (fb.getDocument().getLength() + string.length() <= maxLength) super.insertString(fb, offset, string, attr);
        }
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text != null && text.matches("\\d*")) if (fb.getDocument().getLength() - length + text.length() <= maxLength) super.replace(fb, offset, length, text, attrs);
        }
    }

    static class Splash extends JWindow {
        Splash() {
            JPanel p = new JPanel(new BorderLayout(12, 12));
            p.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
            JLabel title = new JLabel("Code Distributor Tool");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
            title.setHorizontalAlignment(SwingConstants.CENTER);
            JProgressBar bar = new JProgressBar();
            bar.setIndeterminate(true);
            JLabel sub = new JLabel("Loading...");
            sub.setHorizontalAlignment(SwingConstants.CENTER);
            p.add(title, BorderLayout.NORTH);
            p.add(bar, BorderLayout.CENTER);
            p.add(sub, BorderLayout.SOUTH);
            setContentPane(p);
            pack();
            Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation((s.width - getWidth()) / 2, (s.height - getHeight()) / 2);
        }
    }

    static class MultiSelectDialog extends JDialog {
        private final JTable table;
        private final SelectTableModel model;
        private final JTextField search;
        private boolean ok = false;

        private MultiSelectDialog(Frame owner, String title, List<String> allIds, Collection<String> preselected) {
            super(owner, title, true);
            model = new SelectTableModel(allIds, preselected);
            table = new JTable(model);
            table.setFillsViewportHeight(true);
            table.setRowHeight(22);
            table.getColumnModel().getColumn(0).setMaxWidth(60);
            TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            search = new JTextField();
            search.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { filter(); }
                public void removeUpdate(DocumentEvent e) { filter(); }
                public void changedUpdate(DocumentEvent e) { filter(); }
                private void filter() {
                    String q = search.getText().trim();
                    if (q.isEmpty()) sorter.setRowFilter(null);
                    else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(q), 1));
                }
            });
            JButton all = new JButton("All");
            JButton none = new JButton("None");
            JButton invert = new JButton("Invert");
            all.addActionListener(e -> model.setAll(true));
            none.addActionListener(e -> model.setAll(false));
            invert.addActionListener(e -> model.invert());
            JButton okBtn = new JButton("OK");
            JButton cancelBtn = new JButton("Cancel");
            okBtn.addActionListener(e -> { ok = true; setVisible(false); });
            cancelBtn.addActionListener(e -> { ok = false; setVisible(false); });

            JPanel top = new JPanel(new BorderLayout(6,6));
            top.add(new JLabel("Search:"), BorderLayout.WEST);
            top.add(search, BorderLayout.CENTER);

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            controls.add(all); controls.add(none); controls.add(invert);

            JPanel north = new JPanel(new BorderLayout(6,6));
            north.add(top, BorderLayout.NORTH);
            north.add(controls, BorderLayout.SOUTH);

            JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            south.add(cancelBtn); south.add(okBtn);

            setLayout(new BorderLayout(8,8));
            add(north, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);
            add(south, BorderLayout.SOUTH);
            setSize(420, 520);
            setLocationRelativeTo(owner);
        }

        static LinkedHashSet<String> pick(Frame owner, String title, List<String> allIds, Collection<String> preselected) {
            MultiSelectDialog d = new MultiSelectDialog(owner, title, allIds, preselected);
            d.setVisible(true);
            if (!d.ok) return null;
            return d.model.getSelected();
        }

        static class SelectTableModel extends AbstractTableModel {
            private final List<Row> rows = new ArrayList<>();
            SelectTableModel(List<String> allIds, Collection<String> pre) {
                LinkedHashSet<String> preSet = new LinkedHashSet<>(pre == null ? Collections.emptyList() : pre);
                for (String id : allIds) rows.add(new Row(preSet.contains(id), id));
            }
            public int getRowCount() { return rows.size(); }
            public int getColumnCount() { return 2; }
            public String getColumnName(int c) { return c == 0 ? "✓" : "ACID"; }
            public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : String.class; }
            public boolean isCellEditable(int r, int c) { return c == 0; }
            public Object getValueAt(int r, int c) { Row row = rows.get(r); return c == 0 ? row.selected : row.id; }
            public void setValueAt(Object v, int r, int c) { if (c == 0) { rows.get(r).selected = (Boolean) v; fireTableRowsUpdated(r, r); } }
            LinkedHashSet<String> getSelected() { LinkedHashSet<String> out = new LinkedHashSet<>(); for (Row r : rows) if (r.selected) out.add(r.id); return out; }
            void setAll(boolean v) { for (Row r : rows) r.selected = v; fireTableDataChanged(); }
            void invert() { for (Row r : rows) r.selected = !r.selected; fireTableDataChanged(); }
            static class Row { boolean selected; String id; Row(boolean s, String i){ selected=s; id=i; } }
        }
    }
}
