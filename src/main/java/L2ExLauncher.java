import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.tukaani.xz.LZMAInputStream;
import com.formdev.flatlaf.FlatDarkLaf;
import org.index.patchdownloader.EmbeddedNcSoftDownloader;
import org.index.patchdownloader.enums.CDNLink;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.Cursor;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import javax.imageio.ImageIO;
import java.io.File;
import javax.swing.JViewport;
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.GridLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.RadialGradientPaint;

public final class L2ExLauncher extends JFrame {
    private static final String FOURGAME_CONFIG_URL = "https://cdn.inn.ru/4game/config-live.xml";
    private static final String FOURGAME_PTS2_CONFIG_URL = "https://cdn.inn.ru/4game/config-pts2.xml";
    private static final DateTimeFormatter CATALOG_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);
    private static final Pattern NCSOFT_PROGRESS = Pattern.compile(
            "Progress\\s+(\\d{1,3})%\\s*/\\s*100%\\s*\\|\\s*(.*)", Pattern.CASE_INSENSITIVE);
    private final JComboBox<ClientConfig> clientBox = new JComboBox<>();
    private final JComboBox<Distribution> versionBox = new JComboBox<>();
    private final JTextField outputField = new JTextField();
    private final JTextArea logArea = new JTextArea();
    private final ModernProgressBar progress = new ModernProgressBar(Theme.GOLD1, Theme.GOLD2);
    private final ModernProgressBar fileProgress = new ModernProgressBar(new Color(0x9B72FF), new Color(0x60A5FA));
    private final JLabel fileLabel = new JLabel("No download in progress");
    private final StatusPill statusLabel = new StatusPill("Ready");
    private final JLabel heroTitle = new JLabel("Lineage II");
    private final JLabel brandName = new JLabel();
    private final JLabel brandTag = new JLabel();
    private final LogoMark logoMark = new LogoMark();
    private final Pill versionChip = new Pill(Theme.ACCENT_SOFT, "—");
    private final Pill typeChip = new Pill(Theme.GOLD1, "—");
    private javax.swing.Timer ncSoftFileAnimation;
    private volatile Process ncSoftProcess;
    private boolean extraGamesEnabled;
    private GradientPanel rootPanel;
    private final ModernButton refreshButton = new ModernButton("Refresh", ModernButton.Style.GHOST, ModernButton.Glyph.REFRESH);
    private final ModernButton downloadButton = new ModernButton("Download client", ModernButton.Style.PRIMARY, ModernButton.Glyph.DOWNLOAD);
    private final ModernButton launchButton = new ModernButton("Launch client", ModernButton.Style.PLAY, ModernButton.Glyph.PLAY);
    private final ModernButton cancelButton = new ModernButton("Cancel", ModernButton.Style.GHOST, ModernButton.Glyph.CLOSE);
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private SwingWorker<?, ?> activeWorker;

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        javax.swing.SwingUtilities.invokeLater(() -> new L2ExLauncher().setVisible(true));
    }

    private L2ExLauncher() {
        super("L2ExLauncher");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1120, 740);
        setMinimumSize(new Dimension(1000, 660));
        setLocationByPlatform(true);
        installTheme();
        buildUi();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                stopActiveWork();
                dispose();
                Thread exitGuard = new Thread(() -> {
                    try { Thread.sleep(2000L); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                    Process process = ncSoftProcess;
                    if (process != null && process.isAlive()) {
                        ProcessHandle handle = process.toHandle();
                        handle.descendants().forEach(ProcessHandle::destroyForcibly);
                        handle.destroyForcibly();
                    }
                    // Closing the window is an explicit request to terminate the launcher,
                    // including any worker that is blocked in network I/O.
                    System.exit(0);
                }, "l2exlauncher-exit-guard");
                exitGuard.setDaemon(true);
                exitGuard.start();
            }
        });
        loadClients();
        updateBranding();
    }

    // ------------------------------------------------------------------
    //  UI
    // ------------------------------------------------------------------

    /** Defaults globais do FlatLaf; precisa rodar antes de criar qualquer componente. */
    private static void installTheme() {
        UIManager.put("Panel.background", Theme.BG_BOTTOM);
        UIManager.put("OptionPane.background", Theme.BG_TOP);
        UIManager.put("Label.foreground", Theme.TEXT);
        UIManager.put("Component.arc", 14);
        UIManager.put("Button.arc", 14);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("ProgressBar.arc", 999);
        UIManager.put("Component.focusColor", Theme.ACCENT);
        UIManager.put("Component.focusedBorderColor", Theme.ACCENT);
        UIManager.put("Button.default.background", Theme.ACCENT);
        UIManager.put("TextComponent.selectionBackground", new Color(88, 64, 160));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.showButtons", false);
    }

    private void buildUi() {
        getRootPane().putClientProperty("JRootPane.titleBarBackground", Theme.TITLEBAR);
        getRootPane().putClientProperty("JRootPane.titleBarForeground", Theme.TEXT);
        setIconImage(LogoMark.render(64));

        // ---------- widgets ----------
        styleCombo(clientBox);
        styleCombo(versionBox);

        outputField.setOpaque(false);
        outputField.setBackground(new Color(0, 0, 0, 0));
        outputField.setForeground(Theme.TEXT);
        outputField.setCaretColor(Theme.ACCENT_SOFT);
        outputField.setSelectionColor(new Color(139, 92, 246, 130));
        outputField.setFont(Theme.font(Font.PLAIN, 13f));
        outputField.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 6));
        // tamanho fixo: um caminho longo não pode alterar o layout da barra lateral
        outputField.setPreferredSize(new Dimension(10, 44));
        outputField.setMinimumSize(new Dimension(10, 44));
        outputField.putClientProperty("JTextField.placeholderText", "Default: ~/NCSOFT_PTS/<client>");

        ModernButton browse = new ModernButton("", ModernButton.Style.SUBTLE, ModernButton.Glyph.FOLDER);
        browse.setToolTipText("Choose destination folder");

        fileLabel.setFont(Theme.font(Font.PLAIN, 12.5f));
        fileLabel.setForeground(Theme.TEXT);
        fileProgress.setPreferredSize(new Dimension(100, 22));
        progress.setPreferredSize(new Dimension(100, 22));
        cancelButton.setEnabled(false);

        logArea.setEditable(false);
        logArea.setLineWrap(false);
        logArea.setOpaque(false);
        logArea.setBackground(new Color(0, 0, 0, 0));
        logArea.setForeground(new Color(176, 183, 205));
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setMargin(new Insets(2, 2, 2, 2));

        // ---------- sidebar ----------
        JPanel brand = new JPanel(new BorderLayout(14, 0));
        brand.setOpaque(false);
        brand.add(logoMark, BorderLayout.WEST);
        brandName.setText("LINEAGE ][");
        brandName.setFont(Theme.tracked(Theme.font(Font.BOLD, 17f), 0.12f));
        brandName.setForeground(Theme.TEXT);
        brandTag.setText("LAUNCHER");
        brandTag.setFont(Theme.tracked(Theme.font(Font.BOLD, 10.5f), 0.2f));
        brandTag.setForeground(Theme.ACCENT_SOFT);
        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(Box.createVerticalGlue());
        names.add(brandName);
        names.add(Box.createVerticalStrut(2));
        names.add(brandTag);
        names.add(Box.createVerticalGlue());
        brand.add(names, BorderLayout.CENTER);

        JLabel footer = new JLabel("Experimental launcher");
        footer.setFont(Theme.font(Font.PLAIN, 11f));
        footer.setForeground(Theme.DIM);

        JPanel sidebar = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.SIDEBAR);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.BORDER);
                g.fillRect(getWidth() - 1, 0, 1, getHeight());
            }
        };
        sidebar.setOpaque(false);
        sidebar.setBorder(BorderFactory.createEmptyBorder(28, 26, 22, 26));
        sidebar.setPreferredSize(new Dimension(330, 0));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        int row = 0;
        g.gridy = row++; g.insets = new Insets(0, 0, 0, 0);   sidebar.add(brand, g);
        g.gridy = row++; g.insets = new Insets(38, 2, 9, 0);  sidebar.add(caption("CLIENT"), g);
        g.gridy = row++; g.insets = new Insets(0, 0, 0, 0);   sidebar.add(new FieldShell(clientBox, null), g);
        g.gridy = row++; g.insets = new Insets(22, 2, 9, 0);  sidebar.add(caption("DISTRIBUTION"), g);
        g.gridy = row++; g.insets = new Insets(0, 0, 0, 0);   sidebar.add(new FieldShell(versionBox, null), g);
        g.gridy = row++; g.insets = new Insets(22, 2, 9, 0);  sidebar.add(caption("DESTINATION"), g);
        g.gridy = row++; g.insets = new Insets(0, 0, 0, 0);   sidebar.add(new FieldShell(outputField, browse), g);
        g.gridy = row++; g.insets = new Insets(26, 0, 0, 0);  sidebar.add(refreshButton, g);
        g.gridy = row++; g.insets = new Insets(0, 0, 0, 0);   g.weighty = 1; g.fill = GridBagConstraints.BOTH;
        sidebar.add(Box.createGlue(), g);
        g.gridy = row;   g.weighty = 0; g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(0, 2, 0, 0);
        sidebar.add(footer, g);

        // ---------- hero ----------
        JLabel eyebrow = caption("SELECTED CLIENT");
        eyebrow.setForeground(Theme.ACCENT_SOFT);
        heroTitle.setFont(Theme.font(Font.BOLD, 30f));
        heroTitle.setForeground(Theme.TEXT);
        JLabel heroSub = new JLabel("Full + patches  ·  SHA-1 verification  ·  experimental launcher");
        heroSub.setFont(Theme.font(Font.PLAIN, 12.5f));
        heroSub.setForeground(Theme.MUTED);

        JPanel chips = new JPanel();
        chips.setOpaque(false);
        chips.setLayout(new BoxLayout(chips, BoxLayout.X_AXIS));
        chips.add(statusLabel);
        chips.add(Box.createHorizontalStrut(8));
        chips.add(versionChip);
        chips.add(Box.createHorizontalStrut(8));
        chips.add(typeChip);
        chips.add(Box.createHorizontalGlue());

        JPanel hero = new JPanel();
        hero.setOpaque(false);
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        for (JComponent c : List.of(eyebrow, heroTitle, heroSub, chips)) c.setAlignmentX(Component.LEFT_ALIGNMENT);
        hero.add(eyebrow);
        hero.add(Box.createVerticalStrut(6));
        hero.add(heroTitle);
        hero.add(Box.createVerticalStrut(4));
        hero.add(heroSub);
        hero.add(Box.createVerticalStrut(16));
        hero.add(chips);

        // ---------- log ----------
        ModernButton clearLog = new ModernButton("Clear", ModernButton.Style.SUBTLE, ModernButton.Glyph.NONE);
        clearLog.addActionListener(e -> logArea.setText(""));
        JPanel logHeader = new JPanel(new BorderLayout());
        logHeader.setOpaque(false);
        logHeader.add(caption("ACTIVITY"), BorderLayout.WEST);
        logHeader.add(clearLog, BorderLayout.EAST);

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(null);
        logScroll.setOpaque(false);
        logScroll.getViewport().setOpaque(false);
        logScroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        logScroll.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        logScroll.getHorizontalScrollBar().setUI(new ThinScrollBarUI());
        logScroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        logScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));

        Card logCard = new Card(Theme.CARD, Theme.BORDER, 18);
        logCard.setLayout(new BorderLayout(0, 6));
        logCard.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 14));
        logCard.add(logHeader, BorderLayout.NORTH);
        logCard.add(logScroll, BorderLayout.CENTER);

        // ---------- progresso + ações ----------
        JPanel fileRow = new JPanel(new BorderLayout(0, 8));
        fileRow.setOpaque(false);
        fileRow.add(fileLabel, BorderLayout.NORTH);
        fileRow.add(fileProgress, BorderLayout.CENTER);

        JPanel totalRow = new JPanel(new BorderLayout(0, 8));
        totalRow.setOpaque(false);
        totalRow.add(caption("TOTAL PROGRESS"), BorderLayout.NORTH);
        totalRow.add(progress, BorderLayout.CENTER);

        JPanel bars = new JPanel(new GridLayout(2, 1, 0, 16));
        bars.setOpaque(false);
        bars.add(fileRow);
        bars.add(totalRow);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(cancelButton);
        actions.add(downloadButton);
        actions.add(launchButton);

        Card bottomCard = new Card(Theme.CARD, Theme.BORDER, 18);
        bottomCard.setLayout(new BorderLayout(0, 14));
        bottomCard.setBorder(BorderFactory.createEmptyBorder(18, 22, 12, 16));
        bottomCard.add(bars, BorderLayout.NORTH);
        bottomCard.add(actions, BorderLayout.SOUTH);

        // ---------- montagem ----------
        JPanel main = new JPanel(new BorderLayout(0, 20)) {
            @Override protected void paintComponent(Graphics g) {
                // marca d'água "II" atrás do hero
                Graphics2D g2 = Theme.aa(g);
                g2.setFont(new Font(Font.SERIF, Font.BOLD, 250));
                FontMetrics fm = g2.getFontMetrics();
                String s = "][";
                int x = getWidth() - fm.stringWidth(s) - 10;
                int y = fm.getAscent() - 70;
                g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 26), 0, 260, new Color(139, 92, 246, 0)));
                g2.drawString(s, x, y);
                g2.dispose();
            }
        };
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(30, 34, 28, 34));
        main.add(hero, BorderLayout.NORTH);
        main.add(logCard, BorderLayout.CENTER);
        main.add(bottomCard, BorderLayout.SOUTH);

        rootPanel = new GradientPanel();
        rootPanel.setLayout(new BorderLayout());
        rootPanel.add(sidebar, BorderLayout.WEST);
        rootPanel.add(main, BorderLayout.CENTER);
        setContentPane(rootPanel);

        // ---------- eventos ----------
        launchButton.addActionListener(e -> launchClient());
        clientBox.addActionListener(e -> refreshVersions());
        clientBox.addActionListener(e -> updateHero());
        versionBox.addActionListener(e -> updateHero());
        refreshButton.addActionListener(e -> refreshVersions());
        browse.addActionListener(e -> chooseOutput());
        downloadButton.addActionListener(e -> startDownload());
        cancelButton.addActionListener(e -> stopActiveWork());
    }

    private void stopActiveWork() {
        SwingWorker<?, ?> worker = activeWorker;
        if (worker != null) worker.cancel(true);
        Process process = ncSoftProcess;
        if (process != null && process.isAlive()) terminateProcessTree(process);
    }

    private void terminateProcessTree(Process process) {
        ProcessHandle handle = process.toHandle();
        handle.descendants().forEach(ProcessHandle::destroy);
        handle.destroy();
        Thread forceKiller = new Thread(() -> {
            try { Thread.sleep(1500L); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            if (process.isAlive()) {
                handle.descendants().forEach(ProcessHandle::destroyForcibly);
                handle.destroyForcibly();
            }
        }, "l2exlauncher-process-cleanup");
        forceKiller.setDaemon(true);
        forceKiller.start();
    }

    private void updateHero() {
        ClientConfig client = (ClientConfig) clientBox.getSelectedItem();
        Distribution distribution = (Distribution) versionBox.getSelectedItem();
        heroTitle.setText(client == null ? "Lineage II" : client.displayName());
        versionChip.setText(distribution == null ? "—" : distribution.version);
        typeChip.setText(distribution == null || distribution.type().isBlank()
                ? "—" : distribution.type().toUpperCase(Locale.ROOT));
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.tracked(Theme.font(Font.BOLD, 10.5f), 0.14f));
        label.setForeground(Theme.MUTED);
        return label;
    }

    private static <T> void styleCombo(JComboBox<T> box) {
        box.setUI(new ModernComboUI());
        box.setRenderer(new ComboItemRenderer());
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder());
        box.setFont(Theme.font(Font.PLAIN, 13.5f));
        box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        box.setPreferredSize(new Dimension(10, 44));
        box.setMinimumSize(new Dimension(10, 44));
    }

    private void loadClients() {
        try {
            Path config = locateLauncherConfig();
            List<ClientConfig> clients;
            if (config == null) {
                clients = new ArrayList<>(fallbackClients());
            } else {
                String localXml = Files.readString(config);
                extraGamesEnabled = readExtraGamesSetting(localXml);
                clients = new ArrayList<>(readClients(localXml).stream()
                        .filter(ClientConfig::isNcSoft)
                        .filter(ClientConfig::enabled).toList());
                try {
                    clients.addAll(filterVisibleGames(updateAndReadFourGameCatalog()));
                    append("Loaded 4game catalogs from live + pts2 XML");
                } catch (Exception remoteEx) {
                    clients.addAll(filterVisibleGames(readClients(localXml, false).stream().filter(ClientConfig::enabled).toList()));
                    append("Warning: 4game catalog unavailable; using local catalog: " + rootMessage(remoteEx));
                }
            }
            append("Loaded " + clients.size() + " clients" + (config == null ? " from built-in defaults" : " from " + config));
            clientBox.setModel(new DefaultComboBoxModel<>(clients.toArray(ClientConfig[]::new)));
            if (!clients.isEmpty()) refreshVersions();
        } catch (Exception ex) {
            append("Error reading launcher-config.xml: " + ex.getMessage());
            clientBox.setModel(new DefaultComboBoxModel<>(fallbackClients().toArray(ClientConfig[]::new)));
        }
    }

    private boolean readExtraGamesSetting(String xml) {
        Matcher matcher = Pattern.compile("<config\\b[^>]*\\bEnableExtraGames\\s*=\\s*[\\\"']([^\\\"']+)", Pattern.CASE_INSENSITIVE).matcher(xml);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1).trim());
    }

    private List<ClientConfig> filterVisibleGames(List<ClientConfig> clients) {
        if (extraGamesEnabled) return clients;
        return clients.stream().filter(client -> client.displayName().toLowerCase(Locale.ROOT).contains("lineage")).toList();
    }

    private void updateBranding() {
        brandName.setText(extraGamesEnabled ? "MULTI GAME" : "LINEAGE ][");
        brandTag.setText("LAUNCHER");
        if (rootPanel != null) rootPanel.setMulti(extraGamesEnabled);
    }

    private Path locateLauncherConfig() {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Path.of("dist", "launcher-config.xml"));
        candidates.add(Path.of("launcher-config.xml"));
        try {
            Path jar = Path.of(L2ExLauncher.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(jar)) {
                candidates.add(jar.getParent().resolve("launcher-config.xml"));
                candidates.add(jar.getParent().getParent().resolve("launcher-config.xml"));
            } else {
                candidates.add(jar.resolve("dist/launcher-config.xml"));
                if (jar.getParent() != null) candidates.add(jar.getParent().resolve("dist/launcher-config.xml"));
                candidates.add(jar.resolve("launcher-config.xml"));
                for (Path parent = jar; parent != null; parent = parent.getParent()) {
                    candidates.add(parent.resolve("dist/launcher-config.xml"));
                }
            }
        } catch (Exception ignored) { }
        return candidates.stream().filter(Files::isRegularFile).findFirst().orElse(null);
    }

    private List<ClientConfig> readClients(String xml) throws Exception {
        return readClients(xml, true);
    }

    private List<ClientConfig> readClients(String xml, boolean includeNcSoft) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().parse(new InputSource(new java.io.StringReader(xml)));
        List<ClientConfig> result = new ArrayList<>();
        NodeList games = document.getElementsByTagName("game");
        for (int i = 0; i < games.getLength(); i++) {
            Element game = (Element) games.item(i);
            String name = game.getAttribute("display_name");
            String url = game.getAttribute("url");
            String source = game.getAttribute("source");
            String type = game.getAttribute("type").toLowerCase(Locale.ROOT);
            String patchVersion = game.getAttribute("patch_version");
            if (!includeNcSoft && "ncsoft".equalsIgnoreCase(source)) continue;
            if ("pts".equals(type) || url.matches(".*/pts/?$")) continue;
            if (!url.isBlank() || "ncsoft".equalsIgnoreCase(source)) {
                String executable = game.getAttribute("executable");
                if (executable.isBlank()) executable = executableFromXml(game.getAttribute("ex_name"));
                String environment = game.getAttribute("environment");
                if (environment.isBlank() && "pts2".equals(type)) environment = "pts2";
                if (environment.isBlank() && url.matches(".*/pts2/?$")) environment = "pts2";
                boolean line2Ini = name.toUpperCase(Locale.ROOT).contains("LINEAGE") && environment.isBlank();
                Integer patch = patchVersion.isBlank() ? null : Integer.valueOf(patchVersion);
                String displayName = game.getAttribute("display_name");
                if (displayName.isBlank()) displayName = name;
                if (isRetiredFourGame(displayName, url)) continue;
                if ("ncsoft".equalsIgnoreCase(source)) {
                    displayName = withNcSoftRegion(displayName);
                } else {
                    displayName = withFourGameRegion(displayName, url);
                }
                boolean enabled = !"false".equalsIgnoreCase(game.getAttribute("enabled"));
                if (isUnsupportedFourGameBackend(displayName, url)) enabled = false;
                if (isDisabledFourGame(displayName, url, environment)) enabled = false;
                result.add(new ClientConfig(displayName, stripContentDirectory(url), executable.isBlank() ? "l2.exe" : executable, line2Ini, environment, source, patch, enabled));
            }
        }
        return result;
    }

    private boolean isUnsupportedFourGameBackend(String displayName, String url) {
        String name = displayName == null ? "" : displayName.toLowerCase(Locale.ROOT);
        String lowerUrl = url == null ? "" : url.toLowerCase(Locale.ROOT);
        return name.contains("black desert") || lowerUrl.contains("playblackdesert.com")
                || (name.contains("ragnarok") && lowerUrl.contains("roteu"))
                || name.contains("royal quest")
                || name.contains("ram pressure");
    }

    private boolean isRetiredFourGame(String displayName, String url) {
        String name = displayName == null ? "" : displayName.toLowerCase(Locale.ROOT);
        String lowerUrl = url == null ? "" : url.toLowerCase(Locale.ROOT);
        return name.contains("black desert") || lowerUrl.contains("playblackdesert.com")
                || (name.contains("ragnarok") && lowerUrl.contains("roteu"))
                || name.contains("royal quest")
                || name.contains("ram pressure");
    }

    private boolean isDisabledFourGame(String displayName, String url, String environment) {
        String name = displayName == null ? "" : displayName.toLowerCase(Locale.ROOT);
        String lowerUrl = url == null ? "" : url.toLowerCase(Locale.ROOT);
        String env = environment == null ? "" : environment.toLowerCase(Locale.ROOT);
        if (name.contains("crowfall") || name.contains("mworu") || name.contains("mwo ru")) return true;
        if (env.equals("pts2") && name.contains("point blank")) return true;
        // RU Prime PTS2 still advertises an rcdn endpoint; keep the catalog entry
        // in JSON as disabled without allowing it into the version resolver.
        return env.equals("pts2") && name.contains("ragnarok") && name.contains("prime")
                && !lowerUrl.contains("roteu");
    }

    private String withFourGameRegion(String displayName, String url) {
        String name = displayName == null ? "" : displayName.trim();
        String upper = name.toUpperCase(Locale.ROOT);
        String region = "";
        if (name.matches("^\\[[A-Za-z]{2,3}].*")) region = name.substring(1, name.indexOf(']')).toUpperCase(Locale.ROOT);
        else if (upper.matches(".*(?:^|[\\s(-])RU(?:$|[\\s)-]).*")) region = "RU";
        else if (upper.matches(".*(?:^|[\\s(-])EU(?:$|[\\s)-]).*")) region = "EU";
        else if (upper.matches(".*(?:^|[\\s(-])LA(?:$|[\\s)-]).*")) region = "LA";

        String lowerUrl = url == null ? "" : url.toLowerCase(Locale.ROOT);
        if (region.isBlank()) {
            if (lowerUrl.matches(".*(?:roteu|lineage2eu|adeu|/eu(?:/|$)).*")) region = "EU";
            else if (lowerUrl.matches(".*(?:rqla|ramla|/la(?:/|$)).*")) region = "LA";
            else if (lowerUrl.contains("cdn.inn.ru")) region = "RU";
        }
        String labeled = region.isBlank() || name.isBlank() || name.startsWith("[" + region + "]")
                ? name : "[" + region + "] " + name;
        return labeled.endsWith("(Innova/4Game)") ? labeled : labeled + " (Innova/4Game)";
    }

    private String withNcSoftRegion(String displayName) {
        String name = displayName == null ? "" : displayName.trim();
        String upper = name.toUpperCase(Locale.ROOT);
        String label;
        if (upper.startsWith("[NA]")) label = "NCWest";
        else if (upper.startsWith("[JP]")) label = "NCJapan";
        else if (upper.startsWith("[TW]")) label = "NCTaiwan";
        else label = "NCSoft";
        name = name.replaceFirst("\\s+\\((?:NCSoft|NCWest|NCJapan|NCTaiwan)\\)$", "");
        return name + " (" + label + ")";
    }

    private String executableFromXml(String value) {
        if (value == null || value.isBlank()) return "";
        String executable = value.trim().split("\\s+", 2)[0].replace('\\', '/');
        while (executable.startsWith("../")) executable = executable.substring(3);
        return executable;
    }

    private List<ClientConfig> updateAndReadFourGameCatalog() throws Exception {
        byte[] liveBytes = request(URI.create(FOURGAME_CONFIG_URL), null);
        byte[] pts2Bytes = request(URI.create(FOURGAME_PTS2_CONFIG_URL), null);
        String liveXml = new String(liveBytes, StandardCharsets.UTF_8);
        String pts2Xml = new String(pts2Bytes, StandardCharsets.UTF_8);
        List<ClientConfig> current = new ArrayList<>(readClients(liveXml, false));
        current.addAll(readClients(pts2Xml, false));
        Map<String, ClientConfig> unique = new LinkedHashMap<>();
        for (ClientConfig client : current) unique.put(catalogKey(client), client);
        current = new ArrayList<>(unique.values());
        byte[] combinedXml = (liveXml + "\n<!-- PTS2 -->\n" + pts2Xml).getBytes(StandardCharsets.UTF_8);
        String xmlHash = sha256(combinedXml);
        Path root = Path.of(System.getProperty("user.home"), "NCSOFT_PTS", ".l2exlauncher", "4game-catalogs");
        Path currentGames = root.resolve("current-games");
        Path history = root.resolve("history");
        Files.createDirectories(currentGames);
        Files.createDirectories(history);

        Path previousXmlHash = root.resolve("current.sha256");
        String oldXmlHash = Files.isRegularFile(previousXmlHash) ? Files.readString(previousXmlHash).trim() : "";
        int changedGames = 0;
        for (ClientConfig client : current) {
            String key = catalogKey(client);
            String gameHash = sha256(clientFingerprint(client).getBytes(StandardCharsets.UTF_8));
            Path currentGame = currentGames.resolve(key + ".json");
            boolean hasCurrentGame = Files.isRegularFile(currentGame);
            String oldGameHash = hasCurrentGame ? jsonField(Files.readString(currentGame), "gameSha256") : "";
            if (!hasCurrentGame) {
                // Primeiro carregamento: cria somente o estado atual. Não é uma versão histórica.
                writeGameJson(currentGame, client, gameHash);
            } else if (!gameHash.equalsIgnoreCase(oldGameHash)) {
                // Mudança real: guarda a nova configuração no histórico e atualiza o estado atual.
                String stamp = CATALOG_STAMP.format(Instant.now());
                Path snapshot = history.resolve(stamp + "-" + key + "-" + gameHash.substring(0, 12));
                Files.createDirectories(snapshot);
                writeGameJson(snapshot.resolve("game.json"), client, gameHash);
                writeGameJson(currentGame, client, gameHash);
                changedGames++;
            } else {
                // Migra o JSON atual (por exemplo, adicionando enabled) sem criar histórico.
                writeGameJson(currentGame, client, gameHash);
            }
        }
        if (!xmlHash.equalsIgnoreCase(oldXmlHash)) {
            Files.writeString(previousXmlHash, xmlHash, StandardCharsets.UTF_8);
            Files.writeString(root.resolve("current-live.xml"), liveXml, StandardCharsets.UTF_8);
            Files.writeString(root.resolve("current-pts2.xml"), pts2Xml, StandardCharsets.UTF_8);
        }
        Path currentCatalog = root.resolve("current").resolve("catalog.json");
        Files.createDirectories(currentCatalog.getParent());
        writeCatalogJson(currentCatalog, current, xmlHash);
        if (changedGames > 0) append("4game catalog updated: " + changedGames + " game entr" + (changedGames == 1 ? "y" : "ies") + " changed (live + pts2).");

        List<ClientConfig> result = new ArrayList<>(current.stream().filter(ClientConfig::enabled).toList());
        try (var dirs = Files.list(history)) {
            dirs.filter(Files::isDirectory).sorted(Comparator.reverseOrder()).forEach(snapshot -> {
                Path oldGame = snapshot.resolve("game.json");
                if (!Files.isRegularFile(oldGame)) return;
                try {
                    ClientConfig client = readGameJson(oldGame);
                    if (isRetiredFourGame(client.displayName(), client.distrUrl())) return;
                    String key = catalogKey(client);
                    Path currentGame = currentGames.resolve(key + ".json");
                    String currentHash = Files.isRegularFile(currentGame)
                            ? jsonField(Files.readString(currentGame), "gameSha256") : "";
                    String historyHash = jsonField(Files.readString(oldGame), "gameSha256");
                    // Não exibe snapshots que representam exatamente a versão atualmente instalada.
                    // Isso também limpa visualmente históricos criados pelo comportamento antigo.
                    if (!currentHash.isBlank() && currentHash.equalsIgnoreCase(historyHash)) return;
                    String label = snapshot.getFileName().toString().split("-", 3)[0];
                    if (client.enabled()) result.add(withDisplayName(client, client.displayName() + " [" + label + "]"));
                } catch (Exception ignored) { }
            });
        }
        return result;
    }

    private String catalogKey(ClientConfig client) {
        return safeName(client.displayName() + (client.environment().isBlank() ? "" : "-" + client.environment())).toLowerCase(Locale.ROOT);
    }

    private String clientFingerprint(ClientConfig client) {
        return String.join("\n", client.displayName(), client.distrUrl(), client.executable(),
                Boolean.toString(client.downloadLiveIni()), client.environment(), client.source());
    }

    private ClientConfig readGameJson(Path file) throws IOException {
        String json = Files.readString(file);
        String enabledValue = jsonField(json, "enabled");
        boolean enabled = !"false".equalsIgnoreCase(enabledValue);
        return new ClientConfig(jsonField(json, "displayName"), jsonField(json, "url"), jsonField(json, "executable"),
                Boolean.parseBoolean(jsonField(json, "downloadLiveIni")), jsonField(json, "environment"),
                jsonField(json, "source"), null, enabled);
    }

    private String jsonField(String json, String field) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*(?:\\\"((?:\\\\.|[^\\\"])*)\\\"|(true|false|null))")
                .matcher(json);
        if (!matcher.find()) return "";
        String value = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        if ("null".equals(value)) return "";
        return value.replace("\\\\", "\\").replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r");
    }

    private ClientConfig withDisplayName(ClientConfig client, String displayName) {
        return new ClientConfig(displayName, client.distrUrl(), client.executable(), client.downloadLiveIni(),
                client.environment(), client.source(), client.patchVersion(), client.enabled());
    }

    private void writeCatalogJson(Path file, List<ClientConfig> clients, String hash) throws IOException {
        Path gamesDirectory = file.getParent().resolve("games");
        Files.createDirectories(gamesDirectory);
        StringBuilder json = new StringBuilder("{\n  \"sha256\": ")
                .append(jsonQuote(hash)).append(",\n  \"clients\": [\n");
        for (int i = 0; i < clients.size(); i++) {
            ClientConfig c = clients.get(i);
            Path gameFile = gamesDirectory.resolve(String.format(Locale.ROOT, "%03d-%s.json", i + 1, safeName(c.displayName())));
            writeGameJson(gameFile, c, hash);
            json.append("    {\"displayName\": ").append(jsonQuote(c.displayName()))
                    .append(", \"url\": ").append(jsonQuote(c.distrUrl()))
                    .append(", \"environment\": ").append(jsonQuote(c.environment()))
                    .append(", \"executable\": ").append(jsonQuote(c.executable()))
                    .append(", \"enabled\": ").append(c.enabled())
                    .append(", \"json\": ").append(jsonQuote(file.getParent().relativize(gameFile).toString())).append("}")
                    .append(i + 1 == clients.size() ? "\n" : ",\n");
        }
        json.append("  ]\n}\n");
        Files.writeString(file, json, StandardCharsets.UTF_8);
    }

    private void writeGameJson(Path file, ClientConfig client, String catalogHash) throws IOException {
        String json = "{\n"
                + "  \"gameSha256\": " + jsonQuote(catalogHash) + ",\n"
                + "  \"displayName\": " + jsonQuote(client.displayName()) + ",\n"
                + "  \"source\": " + jsonQuote(client.source()) + ",\n"
                + "  \"url\": " + jsonQuote(client.distrUrl()) + ",\n"
                + "  \"environment\": " + jsonQuote(client.environment()) + ",\n"
                + "  \"downloadLiveIni\": " + client.downloadLiveIni() + ",\n"
                + "  \"enabled\": " + client.enabled() + ",\n"
                + "  \"executable\": " + jsonQuote(client.executable()) + "\n"
                + "}\n";
        Files.writeString(file, json, StandardCharsets.UTF_8);
    }

    private static String jsonQuote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }

    private List<ClientConfig> fallbackClients() {
        return List.of(
                new ClientConfig("[EU] Lineage 2 EU (Innova/4Game)", "http://cdn.inn.ru/lineage2eu/patch/L2EU-p-260527-261006-1/distr", "l2.exe", true, "", "", null),
                new ClientConfig("[EU] Lineage 2 Essence EU (Innova/4Game)", "http://cdn.inn.ru/adeu/patch/adeu-p-260527-261006-1/distr", "l2.exe", true, "", "", null),
                new ClientConfig("[RU] Lineage 2 RU (Innova/4Game)", "http://cdn.inn.ru/lineage2/patch/L2RU-p-260527-261006-1/distr", "l2.exe", true, "", "", null),
                new ClientConfig("[RU] Lineage 2 Classic RU (Innova/4Game)", "http://cdn.inn.ru/clru/patch/CLRU-p-260527-261007-1/distr", "l2.exe", true, "", "", null),
                new ClientConfig("[RU] Lineage 2 Essence RU (Innova/4Game)", "http://cdn.inn.ru/l2adru/patch/adru-p-260527-261007-3/distr", "l2.exe", true, "", "", null),
                new ClientConfig("[KR] Lineage 2 Korea (NCSoft)", "", "l2.exe", false, "", "ncsoft", 283),
                new ClientConfig("[NA] Lineage 2 North America (NCWest)", "", "l2.exe", false, "", "ncsoft", 605),
                new ClientConfig("[JP] Lineage 2 Japan (NCJapan)", "", "l2.exe", false, "", "ncsoft", 227),
                new ClientConfig("[TW] Lineage 2 Taiwan (NCTaiwan)", "", "l2.exe", false, "", "ncsoft", 661)
        );
    }

    private static String stripContentDirectory(String url) {
        return url.replaceFirst("/(live|pts|pts2)/?$", "").replaceAll("/$", "");
    }

    private void refreshVersions() {
        ClientConfig client = (ClientConfig) clientBox.getSelectedItem();
        if (client == null || activeWorker != null) return;
        if (client.isNcSoft()) {
            List<Distribution> ncVersions = List.of(new Distribution(
                    Integer.toString(client.patchVersion()), "ncsoft", "NCSoft public manifest"));
            versionBox.setModel(new DefaultComboBoxModel<>(ncVersions.toArray(Distribution[]::new)));
            append("NCSoft " + client.displayName() + ": validated patch " + client.patchVersion() + ".");
            statusLabel.setText("NCSoft catalog ready");
            updateHero();
            return;
        }
        setBusy(true);
        append("Reading version-info.txt from " + client.distrUrl);
        statusLabel.setText("Checking versions...");
        activeWorker = new SwingWorker<List<Distribution>, Void>() {
            @Override protected List<Distribution> doInBackground() throws Exception {
                return resolveDistributions(client.distrUrl);
            }
            @Override protected void done() {
                try {
                    List<Distribution> list = get();
                    versionBox.setModel(new DefaultComboBoxModel<>(list.toArray(Distribution[]::new)));
                    selectFull(list);
                    append("Distributions found: " + list.size());
                } catch (Exception ex) { append("Error reading versions: " + rootMessage(ex)); }
                finally { activeWorker = null; setBusy(false); statusLabel.setText("Ready"); }
            }
        };
        activeWorker.execute();
    }

    private void selectFull(List<Distribution> list) {
        for (int i = 0; i < list.size(); i++) {
            if ("full".equalsIgnoreCase(list.get(i).type())) { versionBox.setSelectedIndex(i); return; }
        }
    }

    private List<Distribution> resolveDistributions(String distrUrl) throws Exception {
        byte[] info = request(URI.create(distrUrl + "/version-info.txt"), null);
        List<Distribution> result = parseDistributions(new String(info, StandardCharsets.UTF_8));
        for (Distribution d : result) d.resolvedUrl = URI.create(distrUrl + "/").resolve(d.files()).toString().replaceAll("/$", "");
        return result;
    }

    private List<Distribution> resolveEnvironment(String distrUrl, String environment) throws Exception {
        String base = distrUrl + "/" + environment;
        byte[] info = request(URI.create(base + "/version-info.txt"), null);
        List<Distribution> result = parseDistributions(new String(info, StandardCharsets.UTF_8));
        for (Distribution d : result) d.resolvedUrl = base;
        return result;
    }

    private void chooseOutput() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (!outputField.getText().isBlank()) chooser.setSelectedFile(Path.of(outputField.getText()).toFile());
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) outputField.setText(chooser.getSelectedFile().toPath().toString());
    }

    private void startDownload() {
        ClientConfig client = (ClientConfig) clientBox.getSelectedItem();
        Distribution distribution = (Distribution) versionBox.getSelectedItem();
        if (client == null || distribution == null) { append("Choose a client and a distribution."); return; }
        Path output = outputField.getText().isBlank()
                ? Path.of(System.getProperty("user.home"), "NCSOFT_PTS", safeName(client.displayName))
                : Path.of(outputField.getText());
        setBusy(true);
        progress.setValue(0);
        fileProgress.setValue(0);
        fileLabel.setText("Preparing effective manifest...");
        statusLabel.setText("Downloading...");
        append("Downloading " + distribution.version + " to " + output);
        activeWorker = new SwingWorker<Void, String>() {
            @Override protected Void doInBackground() throws Exception {
                if (client.isNcSoft()) runNcSoftDownload(client, distribution, output, this);
                else new Downloader(client.distrUrl, output, this, distribution, client.downloadLiveIni, client.environment).run();
                return null;
            }
            @Override protected void process(List<String> chunks) { chunks.forEach(L2ExLauncher.this::append); }
            @Override protected void done() {
                try {
                    get();
                    stopNcSoftFileAnimation();
                    append("Download complete and verified.");
                    statusLabel.setText("Ready");
                    fileLabel.setText("Client ready to launch");
                    fileProgress.setValue(100);
                    progress.setValue(100);
                }
                catch (CancellationException ex) { append("Download cancelled."); statusLabel.setText("Cancelled"); }
                catch (Exception ex) {
                    String message = rootMessage(ex);
                    append("Error: " + message);
                    statusLabel.setText("Error");
                    if (message.toLowerCase(Locale.ROOT).contains("aria2")) {
                        javax.swing.SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                                L2ExLauncher.this,
                                message + "\n\nInstall aria2 and restart the launcher.",
                                "aria2 required", JOptionPane.WARNING_MESSAGE));
                    }
                }
                finally { activeWorker = null; setBusy(false); }
            }
        };
        activeWorker.execute();
    }

    private void runNcSoftDownload(ClientConfig client, Distribution distribution, Path output,
                                   SwingWorker<?, String> worker) throws Exception {
        CDNLink cdn = ncCdnType(client.displayName());
        append("Using embedded NCSoft pipeline: " + cdn);
        append("NCSoft " + cdn + " v" + distribution.version + ": downloading through the validated generator...");
        int exit = EmbeddedNcSoftDownloader.download(cdn, Integer.parseInt(distribution.version), output,
                line -> handleNcSoftOutput(line));
        if (worker.isCancelled()) throw new CancellationException();
        if (exit != 0) throw new IOException("NCSoft downloader failed with code " + exit);
    }

    /** Legacy process path retained for one release as an emergency compatibility fallback. */
    private void runNcSoftDownloadExternal(ClientConfig client, Distribution distribution, Path output,
                                   SwingWorker<?, String> worker) throws Exception {
        Path backend = locateNcBackend();
        if (backend == null) throw new IOException("NCSoft backend not found. Put libs/l2patch-backend.jar in the launcher folder.");
        String cdn = ncCdn(client.displayName());
        append("Using legacy external NCSoft backend: " + backend.toAbsolutePath());
        append("NCSoft " + cdn + " v" + distribution.version + ": downloading through the validated generator...");
        // Keep the external NCSoft worker bounded: ALL_TEMP avoids retaining large decoded
        // payloads in heap, while the heap cap prevents a single update from consuming GBs.
        Process process = new ProcessBuilder(javaCommand(), "-Xmx768m", "-jar", backend.toString(), "-cdn", cdn,
                "-version", distribution.version, "-path", output.toAbsolutePath().toString(),
                "-include_filter", "*", "-exclude_filter", "*/*.dlt;*.torrent", "-hash", "-size", "-restore",
                "-download_mode", "ALL_TEMP")
                .directory(ncBackendRoot(backend).toFile()).redirectErrorStream(true).start();
        ncSoftProcess = process;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) {
                if (worker.isCancelled()) { terminateProcessTree(process); throw new CancellationException(); }
                handleNcSoftOutput(line);
            }
        } finally {
            if (worker.isCancelled() && process.isAlive()) terminateProcessTree(process);
            try { process.waitFor(2L, java.util.concurrent.TimeUnit.SECONDS); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            if (process.isAlive()) terminateProcessTree(process);
            if (ncSoftProcess == process) ncSoftProcess = null;
        }
        if (worker.isCancelled()) throw new CancellationException();
        if (process.isAlive()) throw new IOException("NCSoft backend did not terminate cleanly");
        int exit = process.exitValue();
        if (exit != 0) throw new IOException("NCSoft downloader exited with code " + exit);
    }

    private void handleNcSoftOutput(String line) {
        Matcher matcher = NCSOFT_PROGRESS.matcher(line);
        if (matcher.find()) {
            int percent = Math.max(0, Math.min(100, Integer.parseInt(matcher.group(1))));
            String item = matcher.group(2).trim();
            SwingUtilities.invokeLater(() -> {
                progress.setValue(percent);
                progress.setString(percent + "%");
                fileLabel.setText(item.isBlank() ? "NCSoft downloader" : item);
                statusLabel.setText("Downloading...");
                animateNcSoftFileProgress();
            });
            return;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.contains("[error]") || lower.contains("[warn]") || lower.contains("exception")
                || lower.contains("failed") || lower.contains("retry") || lower.contains("download list")
                || lower.contains("total available") || lower.startsWith("version:")
                || lower.startsWith("done:") || lower.contains("catalog")) {
            append(line);
        }
    }

    private void animateNcSoftFileProgress() {
        stopNcSoftFileAnimation();
        fileProgress.setValue(0);
        fileProgress.setString("0%");
        final int[] value = {0};
        ncSoftFileAnimation = new javax.swing.Timer(12, event -> {
            value[0] = Math.min(100, value[0] + 5);
            fileProgress.setValue(value[0]);
            fileProgress.setString(value[0] + "%");
            if (value[0] >= 100) stopNcSoftFileAnimation();
        });
        ncSoftFileAnimation.start();
    }

    private void stopNcSoftFileAnimation() {
        if (ncSoftFileAnimation != null) {
            ncSoftFileAnimation.stop();
            ncSoftFileAnimation = null;
        }
    }

    private Path locateNcBackend() {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Path.of("dist", "libs", "l2patch-backend.jar"));
        candidates.add(Path.of("libs", "l2patch-backend.jar"));
        candidates.add(Path.of("l2patch-backend.jar"));
        try {
            Path jar = Path.of(L2ExLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(jar)) {
                candidates.add(jar.getParent().resolve("libs").resolve("l2patch-backend.jar"));
                // Backward compatibility with packages that kept the backend beside the launcher.
                candidates.add(jar.getParent().resolve("l2patch-backend.jar"));
            } else {
                candidates.add(jar.resolve("dist/libs/l2patch-backend.jar"));
                if (jar.getParent() != null) candidates.add(jar.getParent().resolve("dist/libs/l2patch-backend.jar"));
                for (Path parent = jar; parent != null; parent = parent.getParent()) {
                    candidates.add(parent.resolve("dist/libs/l2patch-backend.jar"));
                    candidates.add(parent.resolve("libs/l2patch-backend.jar"));
                }
            }
        } catch (Exception ignored) { }
        return candidates.stream().map(Path::toAbsolutePath).filter(Files::isRegularFile).findFirst().orElse(null);
    }

    private Path ncBackendRoot(Path backend) {
        Path sibling = backend.getParent();
        if (Files.isRegularFile(sibling.resolve("work/config/Main.ini"))) return sibling;
        if (Files.isRegularFile(sibling.getParent().resolve("work/config/Main.ini"))) return sibling.getParent();
        return sibling;
    }

    private static String javaCommand() { return Path.of(System.getProperty("java.home"), "bin", "java").toString(); }
    private static String ncCdn(String displayName) {
        if (displayName.startsWith("[KR]")) return "NC_SOFT_KOREAN";
        if (displayName.startsWith("[NA]")) return "NC_SOFT_AMERICA";
        if (displayName.startsWith("[JP]")) return "NC_SOFT_JAPANESE";
        return "NC_SOFT_TAIWAN";
    }

    private static CDNLink ncCdnType(String displayName) {
        return CDNLink.valueOf(ncCdn(displayName));
    }

    private void setBusy(boolean busy) {
        clientBox.setEnabled(!busy); versionBox.setEnabled(!busy); refreshButton.setEnabled(!busy);
        downloadButton.setEnabled(!busy); launchButton.setEnabled(!busy); cancelButton.setEnabled(busy);
    }

    private void launchClient() {
        ClientConfig client = (ClientConfig) clientBox.getSelectedItem();
        if (client == null) return;
        Path root = outputField.getText().isBlank()
                ? Path.of(System.getProperty("user.home"), "NCSOFT_PTS", safeName(client.displayName))
                : Path.of(outputField.getText());
        Path absoluteRoot = root.toAbsolutePath().normalize();
        Path executable = absoluteRoot.resolve(client.executable).normalize();
        if (!executable.startsWith(absoluteRoot) || !Files.isRegularFile(executable)) {
            append("Warning: client executable not found: " + executable);
            JOptionPane.showMessageDialog(this,
                    "The client executable was not found:\n" + executable
                            + "\n\nDownload the client before launching it.",
                    "Client not installed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            new ProcessBuilder(executable.toString()).directory(root.toFile()).start();
            append("Client launched: " + executable.getFileName());
        } catch (IOException ex) {
            append("Error launching client: " + ex.getMessage());
        }
    }

    private void append(String message) { logArea.append(message + System.lineSeparator()); logArea.setCaretPosition(logArea.getDocument().getLength()); }
    private static String safeName(String value) {
        if (value == null || value.isBlank()) return "_";
        // Preserve Unicode letters/numbers (for example Cyrillic game names) instead
        // of reducing a valid name such as "Берсерк Герои Онлайн" to just "_".
        String safe = value.trim().replaceAll("[^\\p{L}\\p{N}._-]+", "_");
        safe = safe.replaceAll("^_+|_+$", "");
        return safe.isBlank() ? "_" : safe;
    }
    private static String rootMessage(Throwable t) { Throwable x = t; while (x.getCause() != null) x = x.getCause(); return Objects.toString(x.getMessage(), x.toString()); }

    private final class Downloader {
        private final String catalogUrl;
        private final Path root;
        private final SwingWorker<?, ?> worker;
        private final Distribution selected;
        private final boolean downloadLiveIni;
        private final String environment;

        Downloader(String catalogUrl, Path root, SwingWorker<?, ?> worker, Distribution selected,
                   boolean downloadLiveIni, String environment) {
            this.catalogUrl = catalogUrl.replaceAll("/$", "");
            this.root = root;
            this.worker = worker;
            this.selected = selected;
            this.downloadLiveIni = downloadLiveIni;
            this.environment = environment;
        }

        void run() throws Exception {
            List<Distribution> distributions = resolveDistributions(catalogUrl);
            List<Entry> entries = buildEffectiveManifest(distributions);
            if (entries.isEmpty()) throw new IOException("Manifest has no entries");
            Files.createDirectories(root);
            Path manifestTemp = Files.createTempDirectory(root, ".lineage-effective-");
            Path manifestFile = manifestTemp.resolve("manifest.json");
            writeEffectiveManifest(manifestFile, entries);
            publish("Effective manifest: " + entries.size() + " files; each path will be downloaded once.");
            publish("Temporary index: " + manifestFile);
            try {
                // Each entry may come from a different distribution, so ranges are
                // used to fetch only the final block of each file.
                downloadPartialEntries(entries);
                verifyEntries(entries);
                if (downloadLiveIni) downloadLiveIni();
                if (!environment.isBlank()) downloadEnvironmentFiles(environment);
                deleteTree(manifestTemp);
            } catch (Exception ex) {
                publish("Temporary manifest preserved at: " + manifestTemp);
                throw ex;
            }
        }

        private void downloadEnvironmentFiles(String environment) throws Exception {
            List<Entry> entries = new ArrayList<>();
            for (Distribution distribution : resolveEnvironment(catalogUrl, environment)) {
                for (Entry entry : parseEntries(loadControl(distribution.resolvedUrl))) {
                    entries.add(entry.withSourceBase(distribution.resolvedUrl));
                }
            }
            if (entries.isEmpty()) throw new IOException("Empty " + environment + " environment manifest");
            publish("Applying " + environment + " environment: " + entries.size() + " files.");
            downloadPartialEntries(entries);
            verifyEntries(entries);
        }

        private List<Entry> buildEnvironmentManifest(List<Distribution> distributions) throws Exception {
            Map<String, Entry> latest = new LinkedHashMap<>();
            for (Distribution distribution : distributions) {
                publish("Reading " + environment + " environment: " + distribution.version);
                for (Entry entry : parseEntries(loadControl(distribution.resolvedUrl))) {
                    latest.put(entry.path(), entry.withSourceBase(distribution.resolvedUrl));
                }
            }
            return new ArrayList<>(latest.values());
        }

        private List<Entry> buildEffectiveManifest(List<Distribution> distributions) throws Exception {
            int fullIndex = -1;
            int selectedIndex = -1;
            for (int i = 0; i < distributions.size(); i++) {
                Distribution d = distributions.get(i);
                if ("full".equalsIgnoreCase(d.type()) && fullIndex < 0) fullIndex = i;
                if (d.version.equals(selected.version)) selectedIndex = i;
            }
            if (fullIndex < 0) throw new IOException("No full distribution found");
            if (selectedIndex < 0) throw new IOException("Selected distribution is not in version-info.txt: " + selected.version);

            // Selecionar o full na GUI significa entregar o cliente completo e já
            // atualizado com todos os patches publicados depois dele.
            int end = "full".equalsIgnoreCase(selected.type()) ? distributions.size() - 1 : selectedIndex;
            if (end < fullIndex) throw new IOException("Invalid distribution order in version-info.txt");

            Map<String, Entry> latest = new LinkedHashMap<>();
            for (int i = fullIndex; i <= end; i++) {
                Distribution distribution = distributions.get(i);
                if (distribution.resolvedUrl == null || distribution.resolvedUrl.isBlank()) continue;
                publish("Merging " + distribution.version + " [" + distribution.type() + "]");
                for (Entry entry : parseEntries(loadControl(distribution.resolvedUrl))) {
                    latest.put(entry.path(), entry.withSourceBase(distribution.resolvedUrl));
                }
            }
            return new ArrayList<>(latest.values());
        }

        private void writeEffectiveManifest(Path file, List<Entry> entries) throws IOException {
            StringBuilder json = new StringBuilder("{\n  \"selected\": ")
                    .append(jsonString(selected.version)).append(",\n  \"files\": [\n");
            for (int i = 0; i < entries.size(); i++) {
                Entry e = entries.get(i);
                json.append("    {\"path\": ").append(jsonString(e.path()))
                        .append(", \"sha1\": ").append(jsonString(e.sha1()))
                        .append(", \"source\": ").append(jsonString(e.sourceBase()))
                        .append(", \"pack\": ").append(jsonString(e.pack()))
                        .append(", \"offset\": ").append(e.offset())
                        .append(", \"storedSize\": ").append(e.storedSize())
                        .append(", \"originalSize\": ").append(e.originalSize())
                        .append(", \"compressed\": ").append(e.compressed()).append("}")
                        .append(i + 1 == entries.size() ? "\n" : ",\n");
            }
            json.append("  ]\n}\n");
            Files.writeString(file, json, StandardCharsets.UTF_8);
        }

        private String jsonString(String value) {
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\r", "\\r").replace("\n", "\\n") + "\"";
        }

        private void downloadFullPacks(List<Entry> entries) throws Exception {
            String aria2 = ensureAria2();
            Path temp = Files.createTempDirectory(root, ".lineage-download-tmp-");
            Map<String, List<Entry>> byPack = new LinkedHashMap<>();
            for (Entry entry : entries) byPack.computeIfAbsent(entry.pack, ignored -> new ArrayList<>()).add(entry);
            try {
                int packIndex = 0;
                for (String pack : byPack.keySet()) {
                    checkCancelled();
                    long total = packSize(pack);
                    if (total <= 0) throw new IOException("Invalid size for " + pack);
                    Path target = temp.resolve(Path.of(pack).getFileName().toString());
                    if (Files.exists(target) && Files.size(target) == total) {
                        publish("Pack already complete, resuming: " + pack);
                    } else {
                        Files.deleteIfExists(target);
                        aria2Download(aria2, URI.create(entries.get(0).sourceBase() + "/" + pack), target, total);
                    }
                        publish("Pack downloaded " + (++packIndex) + "/" + byPack.size() + ": " + pack + " (" + total + " bytes)");
                }
                int completed = 0;
                for (Entry entry : entries) {
                    checkCancelled();
                    writeEntry(entry, readPackBlock(temp, entry));
                    int done = ++completed;
                    javax.swing.SwingUtilities.invokeLater(() -> progress.setValue(done * 100 / entries.size()));
                    publish("[" + done + "/" + entries.size() + "] " + entry.path);
                }
                deleteTree(temp);
            } catch (Exception ex) {
                publish("Temporary files preserved at: " + temp);
                throw ex;
            }
        }

        private void downloadPartialEntries(List<Entry> entries) throws Exception {
            for (int i = 0; i < entries.size(); i++) {
                checkCancelled();
                Entry entry = entries.get(i);
                if (existingEntryIsValid(entry)) {
                    updateFileProgress(entry.path(), entry.originalSize(), entry.originalSize());
                    int completed = i + 1;
                    SwingUtilities.invokeLater(() -> progress.setValue(completed * 100 / entries.size()));
                    continue;
                }
                long end = entry.offset + entry.storedSize - 1L;
                updateFileProgress(entry.path(), 0, entry.storedSize);
                writeEntry(entry, downloadRange(entry, new Range(entry.offset, end)));
                updateFileProgress(entry.path(), entry.storedSize, entry.storedSize);
                int completed = i + 1;
                javax.swing.SwingUtilities.invokeLater(() -> progress.setValue(completed * 100 / entries.size()));
            }
        }

        private boolean existingEntryIsValid(Entry entry) throws Exception {
            Path target = safePath(root, entry.path());
            if (!Files.isRegularFile(target) || Files.size(target) != entry.originalSize()) return false;
            try (InputStream in = Files.newInputStream(target)) {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                byte[] buffer = new byte[1024 * 1024];
                for (int n; (n = in.read(buffer)) >= 0; ) if (n > 0) digest.update(buffer, 0, n);
                return HexFormat.of().formatHex(digest.digest()).equalsIgnoreCase(entry.sha1());
            }
        }

        private byte[] downloadRange(Entry entry, Range range) throws Exception {
            Exception last = null;
            for (int attempt = 1; attempt <= 4; attempt++) {
                checkCancelled();
                try {
                    return downloadRangeOnce(entry, range);
                } catch (Exception ex) {
                    last = ex;
                    if (!retryable(ex) || attempt == 4) throw ex;
                    publish("Retrying " + entry.path() + " (" + attempt + "/3): " + ex.getMessage());
                    sleepBackoff(attempt);
                }
            }
            throw last;
        }

        private byte[] downloadRangeOnce(Entry entry, Range range) throws Exception {
            URI uri = URI.create(entry.sourceBase() + "/" + entry.pack);
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(10))
                    .header("Range", "bytes=" + range.start + "-" + range.end).GET().build();
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 206) {
                response.body().close();
                throw new HttpStatusException(response.statusCode(), "HTTP " + response.statusCode() + " para Range");
            }
            String cr = response.headers().firstValue("Content-Range").orElse("");
            if (!cr.startsWith("bytes " + range.start + "-" + range.end + "/")) {
                response.body().close();
                throw new IOException("Invalid Content-Range: " + cr);
            }
            long expected = range.end - range.start + 1;
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.toIntExact(expected));
            try (InputStream in = response.body()) {
                byte[] buffer = new byte[256 * 1024];
                long received = 0;
                for (int n; (n = in.read(buffer)) >= 0; ) {
                    if (n == 0) continue;
                    out.write(buffer, 0, n);
                    received += n;
                    updateFileProgress(entry.path(), received, expected);
                }
                if (received != expected) throw new IOException("Invalid Range size for " + entry.path());
            }
            return out.toByteArray();
        }

        private boolean retryable(Exception ex) {
            if (ex instanceof HttpStatusException status) {
                return status.status == 408 || status.status == 429 || status.status >= 500;
            }
            return !(ex instanceof java.nio.file.InvalidPathException);
        }

        private void sleepBackoff(int attempt) throws InterruptedException {
            Thread.sleep(Math.min(8000L, 750L << (attempt - 1)));
        }

        private void updateFileProgress(String path, long received, long total) {
            int percent = total <= 0 ? 0 : (int) Math.min(100, received * 100 / total);
            javax.swing.SwingUtilities.invokeLater(() -> {
                fileLabel.setText(path + "  (" + received + "/" + total + " bytes)");
                fileProgress.setValue(percent);
                fileProgress.setString(percent + "%");
            });
        }

        private void writeEntry(Entry entry, byte[] stored) throws Exception {
            byte[] content = entry.compressed ? decompressBlock(stored, entry.originalSize) : stored;
            if (content.length != entry.originalSize) throw new IOException("Invalid size: " + entry.path);
            String digest = sha1(content);
            if (!digest.equalsIgnoreCase(entry.sha1)) throw new IOException("Invalid SHA-1: " + entry.path);
            Path target = safePath(root, entry.path);
            Files.createDirectories(target.getParent());
            Path part = target.resolveSibling(target.getFileName() + ".part-" + UUID.randomUUID());
            try {
                Files.write(part, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                try {
                    Files.move(part, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(part);
            }
        }

        private void verifyEntries(List<Entry> entries) throws Exception {
            publish("Verifying " + entries.size() + " final files...");
            for (int i = 0; i < entries.size(); i++) {
                checkCancelled();
                Entry entry = entries.get(i);
                Path target = safePath(root, entry.path());
                if (!Files.isRegularFile(target)) throw new IOException("Missing file: " + entry.path());
                long expected = entry.originalSize();
                if (Files.size(target) != expected) throw new IOException("Invalid final size: " + entry.path());
                String digest;
                try (InputStream in = Files.newInputStream(target)) {
                    MessageDigest md = MessageDigest.getInstance("SHA-1");
                    byte[] buffer = new byte[1024 * 1024];
                    for (int n; (n = in.read(buffer)) >= 0; ) if (n > 0) md.update(buffer, 0, n);
                    digest = HexFormat.of().formatHex(md.digest());
                }
                if (!digest.equalsIgnoreCase(entry.sha1())) throw new IOException("Invalid final SHA-1: " + entry.path());
                int done = i + 1;
                javax.swing.SwingUtilities.invokeLater(() -> progress.setValue(done * 100 / entries.size()));
            }
            publish("Final verification complete: all files are present and valid.");
        }

        private byte[] readPackBlock(Path temp, Entry entry) throws Exception {
            Path pack = temp.resolve(Path.of(entry.pack).getFileName().toString());
            ByteBuffer buffer = ByteBuffer.allocate(entry.storedSize);
            try (FileChannel channel = FileChannel.open(pack, StandardOpenOption.READ)) {
                channel.position(entry.offset);
                while (buffer.hasRemaining() && channel.read(buffer) >= 0) { }
            }
            if (buffer.hasRemaining()) throw new IOException("Bloco incompleto em " + entry.path);
            return buffer.array();
        }

        private long packSize(String pack) throws Exception {
            URI uri = URI.create(catalogUrl + "/" + pack);
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(2)).header("Range", "bytes=0-0").GET().build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 206 || response.body().length != 1) throw new HttpStatusException(response.statusCode(), "Failed to query size of " + pack);
            String cr = response.headers().firstValue("Content-Range").orElse("");
            Matcher matcher = Pattern.compile("bytes 0-0/(\\d+)").matcher(cr);
            if (!matcher.find()) throw new IOException("Invalid Content-Range for " + pack + ": " + cr);
            return Long.parseLong(matcher.group(1));
        }

        private void downloadToFile(URI uri, Range range, Path target) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(10))
                    .header("Range", "bytes=" + range.start + "-" + range.end).GET().build();
            HttpResponse<Path> response = http.send(request, HttpResponse.BodyHandlers.ofFile(target));
            if (response.statusCode() != 206) throw new HttpStatusException(response.statusCode(), "HTTP " + response.statusCode() + " for " + uri);
            String cr = response.headers().firstValue("Content-Range").orElse("");
            if (!cr.startsWith("bytes " + range.start + "-" + range.end + "/")) throw new IOException("Invalid Content-Range: " + cr);
            long expected = range.end - range.start + 1;
            if (Files.size(target) != expected) throw new IOException("Invalid pack size: " + target);
        }

        private String ensureAria2() throws Exception {
            String executable = findExecutable("aria2c");
            if (executable != null) return executable;
            if (!isLinux()) throw new IOException("aria2c not found. Install aria2 and restart the launcher.");

            int answer = ask("aria2c was not found. Try to install aria2 now?", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) throw new IOException("Install aria2 and restart the launcher.");
            List<String> command = installCommand();
            if (command == null) throw new IOException("No supported package manager found. Install aria2 and restart the launcher.");
            publish("Installing aria2: " + String.join(" ", command));
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            try (InputStream output = process.getInputStream()) { output.transferTo(System.out); }
            if (process.waitFor() != 0 || (executable = findExecutable("aria2c")) == null) {
                throw new IOException("Could not install aria2. Install it manually and restart the launcher.");
            }
            return executable;
        }

        private void aria2Download(String aria2, URI uri, Path target, long expectedSize) throws Exception {
            List<String> command = List.of(
                    aria2, "--dir=" + target.getParent(), "--out=" + target.getFileName(),
                    "--continue=true", "--allow-overwrite=true", "--auto-file-renaming=false",
                    "--file-allocation=none", "--max-connection-per-server=8", "--split=8",
                    "--min-split-size=1M", "--console-log-level=warn", uri.toString());
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            try (InputStream output = process.getInputStream()) { output.transferTo(System.out); }
            if (process.waitFor() != 0) throw new IOException("aria2 failed to download " + uri);
            if (!Files.exists(target) || Files.size(target) != expectedSize) {
                throw new IOException("Invalid downloaded pack size: " + target);
            }
        }

        private int ask(String message, int options) throws Exception {
            final int[] result = new int[1];
            javax.swing.SwingUtilities.invokeAndWait(() -> result[0] = JOptionPane.showConfirmDialog(L2ExLauncher.this, message, "aria2", options));
            return result[0];
        }

        private List<String> installCommand() {
            String privilege = findExecutable("pkexec");
            if (privilege == null) privilege = findExecutable("sudo");
            if (privilege == null) return null;
            if (findExecutable("apt-get") != null) return List.of(privilege, "apt-get", "install", "-y", "aria2");
            if (findExecutable("dnf") != null) return List.of(privilege, "dnf", "install", "-y", "aria2");
            if (findExecutable("pacman") != null) return List.of(privilege, "pacman", "-S", "--noconfirm", "aria2");
            if (findExecutable("zypper") != null) return List.of(privilege, "zypper", "--non-interactive", "install", "aria2");
            return null;
        }

        private boolean isLinux() { return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux"); }

        private String findExecutable(String name) {
            try {
                Process process = new ProcessBuilder("sh", "-c", "command -v " + name).redirectErrorStream(true).start();
                String result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                return process.waitFor() == 0 && !result.isBlank() ? result : null;
            } catch (Exception ignored) { return null; }
        }

        private void downloadLiveIni() throws Exception {
            try {
                byte[] live = request(URI.create(catalogUrl + "/live/l2.ini"), null);
                Path target = root.resolve("live/l2.ini");
                Files.createDirectories(target.getParent());
                Files.write(target, live);
                publish("live/l2.ini");
            } catch (HttpStatusException ex) {
                if (ex.status != 404) throw ex;
                publish("Warning: live/l2.ini does not exist in this distribution.");
            }
        }

        private void deleteTree(Path directory) throws IOException {
            try (var paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException ex) { throw new RuntimeException(ex); }
                });
            } catch (RuntimeException ex) {
                if (ex.getCause() instanceof IOException io) throw io;
                throw ex;
            }
        }

        private void publish(String s) {
            if (!importantMessage(s)) return;
            javax.swing.SwingUtilities.invokeLater(() -> append(s));
        }

        private boolean importantMessage(String message) {
            String lower = message.toLowerCase(Locale.ROOT);
            return lower.startsWith("error") || lower.startsWith("warning") || lower.startsWith("installing")
                    || lower.startsWith("effective manifest") || lower.startsWith("temporary index")
                    || lower.startsWith("verifying") || lower.startsWith("final verification")
                    || lower.startsWith("temporary manifest preserved") || lower.startsWith("pack already complete")
                    || lower.startsWith("pack downloaded");
        }
        private void checkCancelled() { if (worker.isCancelled()) throw new CancellationException(); }
    }

    private String loadControl(String baseUrl) throws Exception {
        try { return new String(decompressLzma(request(URI.create(baseUrl + "/control.txt.7z"), null)), StandardCharsets.UTF_8); }
        catch (HttpStatusException ex) { if (ex.status != 404) throw ex; return new String(request(URI.create(baseUrl + "/control.txt"), null), StandardCharsets.UTF_8); }
    }

    private byte[] decompressLzma(byte[] input) throws IOException {
        try (LZMAInputStream in = new LZMAInputStream(new ByteArrayInputStream(input)); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // CDN blocks are raw LZMA streams whose end marker may be omitted;
            // the manifest's original_size is the authoritative boundary.
            in.enableRelaxedEndCondition();
            in.transferTo(out);
            return out.toByteArray();
        }
    }

    private byte[] decompressBlock(byte[] input, int expected) throws IOException {
        try { byte[] result = decompressLzma(input); if (result.length == expected) return result; } catch (IOException ignored) { }
        throw new IOException("Compressed block is not LZMA or has an unexpected size");
    }

    private byte[] request(URI uri, Range range) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(2)).GET();
        if (range != null) builder.header("Range", "bytes=" + range.start + "-" + range.end);
        HttpResponse<byte[]> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        int status = response.statusCode();
        if (range != null) {
            if (status != 206) throw new HttpStatusException(status, "HTTP " + status + " para Range");
            String cr = response.headers().firstValue("Content-Range").orElse("");
            if (!cr.startsWith("bytes " + range.start + "-" + range.end + "/")) throw new IOException("Invalid Content-Range: " + cr);
            if (response.body().length != range.end - range.start + 1) throw new IOException("Invalid Range size");
        } else if (status != 200) throw new HttpStatusException(status, "HTTP " + status + " em " + uri);
        return response.body();
    }

    private static List<Distribution> parseDistributions(String text) {
        List<Distribution> out = new ArrayList<>(); boolean in = false; Distribution current = null;
        for (String line : text.split("\\R")) {
            String s = line.trim(); if (s.startsWith("distribution ")) { in = true; current = null; continue; }
            if (in && s.startsWith("delta ")) break; if (!in) continue;
            if (s.startsWith("version=")) { if (current != null) out.add(current); current = new Distribution(s.substring(8)); continue; }
            if (current != null && s.contains("=")) { int p = s.indexOf('='); current.fields.put(s.substring(0, p), s.substring(p + 1)); }
        }
        if (current != null) out.add(current); return out;
    }

    private static List<Entry> parseEntries(String text) {
        List<Entry> out = new ArrayList<>();
        for (String line : text.split("\\R")) { if (line.isBlank() || !line.contains("|")) continue; String[] f = line.split("\\|", -1); if (f.length < 8) continue;
            long stored = Long.parseLong(f[6]); int original = f[7].isBlank() ? Math.toIntExact(stored) : Integer.parseInt(f[7]);
            out.add(new Entry(f[0].equals("1"), f[1].replace('\\', '/'), f[2], f[4], Long.parseLong(f[5]), Math.toIntExact(stored), original)); }
        return out;
    }

    private static Path safePath(Path root, String relative) throws IOException { Path r = root.toAbsolutePath().normalize(); Path p = r.resolve(relative).normalize(); if (!p.startsWith(r)) throw new IOException("Caminho inseguro: " + relative); return p; }
    private static String sha1(byte[] data) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data)); }
    private static String sha256(byte[] data) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }

    // ------------------------------------------------------------------
    //  Componentes visuais
    // ------------------------------------------------------------------

    private static final class Theme {
        static final Color BG_TOP = new Color(0x0F1019);
        static final Color BG_BOTTOM = new Color(0x08090D);
        static final Color TITLEBAR = new Color(0x0C0D14);
        static final Color SIDEBAR = new Color(6, 7, 11, 150);
        static final Color CARD = new Color(18, 20, 31, 215);
        static final Color BORDER = new Color(255, 255, 255, 22);
        static final Color BORDER_STRONG = new Color(255, 255, 255, 46);
        static final Color TEXT = new Color(0xECEEF6);
        static final Color MUTED = new Color(0x8D93AB);
        static final Color DIM = new Color(0x5B6178);
        static final Color ACCENT = new Color(0x8B5CF6);
        static final Color ACCENT_SOFT = new Color(0xB79CFF);
        static final Color POPUP = new Color(0x161827);
        static final Color POPUP_SELECTED = new Color(0x2E2457);
        static final Color GOLD1 = new Color(0xFFD978);
        static final Color GOLD2 = new Color(0xEE9A35);
        static final Font BASE = pickFont();

        private static Font pickFont() {
            Set<String> families = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            for (String name : new String[]{"Inter", "Segoe UI", "SF Pro Text", "Helvetica Neue", "Roboto",
                    "Noto Sans", "Ubuntu", "Cantarell"}) {
                if (families.contains(name)) return new Font(name, Font.PLAIN, 13);
            }
            return new Font(Font.SANS_SERIF, Font.PLAIN, 13);
        }

        static Font font(int style, float size) { return BASE.deriveFont(style, size); }

        static Font tracked(Font font, float tracking) {
            return font.deriveFont(Map.of(TextAttribute.TRACKING, tracking));
        }

        static Graphics2D aa(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            return g2;
        }

        static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

        static Color mix(Color c, Color other, float t) {
            return new Color(
                    Math.round(c.getRed() + (other.getRed() - c.getRed()) * t),
                    Math.round(c.getGreen() + (other.getGreen() - c.getGreen()) * t),
                    Math.round(c.getBlue() + (other.getBlue() - c.getBlue()) * t));
        }

        static String ellipsize(String text, FontMetrics fm, int maxWidth) {
            if (fm.stringWidth(text) <= maxWidth) return text;
            String dots = "…";
            int end = text.length();
            while (end > 0 && fm.stringWidth(text.substring(0, end) + dots) > maxWidth) end--;
            return text.substring(0, end) + dots;
        }
    }

    /** Fundo do launcher: seleciona a arte L2 ou Multi conforme a configuração ativa. */
    private static final class GradientPanel extends JPanel {
        private boolean multi;
        private BufferedImage background;
        private BufferedImage scaled;

        GradientPanel() {
            setOpaque(true);
            background = loadBackground(false);
        }

        void setMulti(boolean multi) {
            if (this.multi != multi) {
                this.multi = multi;
                background = loadBackground(multi);
                scaled = null;
                repaint();
            }
        }

        private static BufferedImage loadBackground(boolean multi) {
            String fileName = multi ? "launcher-multi.jpg" : "launcher-l2.jpg";
            List<Path> candidates = new ArrayList<>();
            candidates.add(Path.of("dist", "images").resolve(fileName));
            candidates.add(Path.of("images").resolve(fileName));
            candidates.add(Path.of(fileName));
            try {
                Path codeSource = Path.of(L2ExLauncher.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI());
                Path base = Files.isRegularFile(codeSource) ? codeSource.getParent() : codeSource;
                if (base != null) {
                    candidates.add(base.resolve("dist/images").resolve(fileName));
                    candidates.add(base.resolve("images").resolve(fileName));
                    candidates.add(base.resolve(fileName));
                    if (base.getParent() != null) {
                        candidates.add(base.getParent().resolve("dist/images").resolve(fileName));
                        candidates.add(base.getParent().resolve(fileName));
                    }
                    for (Path parent = base; parent != null; parent = parent.getParent()) {
                        candidates.add(parent.resolve("dist/images").resolve(fileName));
                        candidates.add(parent.resolve("images").resolve(fileName));
                    }
                }
            } catch (Exception ignored) { }
            for (Path candidate : candidates) {
                if (!Files.isRegularFile(candidate)) continue;
                try {
                    BufferedImage image = ImageIO.read(candidate.toFile());
                    if (image != null) return image;
                } catch (IOException ignored) { }
            }
            // Compatibility with the old optional background name.
            for (String name : new String[]{"launcher-bg.png", "launcher-bg.jpg"}) {
                File file = new File(name);
                if (file.isFile()) {
                    try { return ImageIO.read(file); } catch (IOException ignored) { }
                }
            }
            return null;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            int w = getWidth(), h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, Theme.BG_TOP, 0, h, Theme.BG_BOTTOM));
            g2.fillRect(0, 0, w, h);
            if (background != null) {
                if (scaled == null || scaled.getWidth() != w || scaled.getHeight() != h) scaled = cover(w, h);
                g2.drawImage(scaled, 0, 0, null);
                g2.setPaint(new GradientPaint(0, 0, new Color(8, 9, 14, 150), 0, h, new Color(8, 9, 14, 240)));
                g2.fillRect(0, 0, w, h);
            }
            float radius = Math.max(w, h) * 0.6f;
            g2.setPaint(new RadialGradientPaint(new Point2D.Float(w * 0.85f, h * 0.02f), radius,
                    new float[]{0f, 1f}, new Color[]{new Color(124, 77, 255, 80), new Color(124, 77, 255, 0)}));
            g2.fillRect(0, 0, w, h);
            g2.setPaint(new RadialGradientPaint(new Point2D.Float(w * 0.98f, h * 1.0f), radius * 0.8f,
                    new float[]{0f, 1f}, new Color[]{new Color(40, 100, 220, 48), new Color(40, 100, 220, 0)}));
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }

        private BufferedImage cover(int w, int h) {
            double scale = Math.max((double) w / background.getWidth(), (double) h / background.getHeight());
            int dw = (int) Math.ceil(background.getWidth() * scale), dh = (int) Math.ceil(background.getHeight() * scale);
            BufferedImage out = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = out.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(background, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
            g2.dispose();
            return out;
        }
    }

    /** Painel translúcido com cantos arredondados. */
    private static final class Card extends JPanel {
        private final Color fill, border;
        private final int arc;

        Card(Color fill, Color border, int arc) {
            this.fill = fill;
            this.border = border;
            this.arc = arc;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(fill);
            g2.fill(shape);
            g2.setColor(border);
            g2.draw(shape);
            g2.dispose();
        }
    }

    /** Moldura arredondada para combos e campos de texto, com borda de foco. */
    private static final class FieldShell extends JPanel {
        private boolean focused;
        private final PropertyChangeListener focusListener = e -> {
            boolean now = focusWithin();
            if (now != focused) { focused = now; repaint(); }
        };

        FieldShell(JComponent inner, JComponent trailing) {
            super(new BorderLayout());
            setOpaque(false);
            add(inner, BorderLayout.CENTER);
            if (trailing != null) {
                JPanel wrap = new JPanel(new GridBagLayout());
                wrap.setOpaque(false);
                wrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
                wrap.add(trailing);
                add(wrap, BorderLayout.EAST);
            }
        }

        private boolean focusWithin() {
            Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
            return owner != null && SwingUtilities.isDescendingFrom(owner, this);
        }

        @Override public void addNotify() {
            super.addNotify();
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("permanentFocusOwner", focusListener);
        }

        @Override public void removeNotify() {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("permanentFocusOwner", focusListener);
            super.removeNotify();
        }

        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.height = Math.max(d.height, 44);
            return d;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(new Color(255, 255, 255, isEnabled() ? 12 : 6));
            g2.fill(shape);
            if (focused) {
                g2.setColor(Theme.alpha(Theme.ACCENT, 60));
                g2.setStroke(new BasicStroke(3f));
                g2.draw(shape);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(Theme.alpha(Theme.ACCENT_SOFT, 220));
            } else {
                g2.setColor(new Color(255, 255, 255, 28));
            }
            g2.draw(shape);
            g2.dispose();
        }
    }

    /** Botões: PRIMARY (roxo), PLAY (dourado), GHOST (contorno) e SUBTLE (sem fundo). */
    private static final class ModernButton extends JButton {
        enum Style { PRIMARY, PLAY, GHOST, SUBTLE }
        enum Glyph { NONE, DOWNLOAD, PLAY, REFRESH, FOLDER, CLOSE }

        private final Style style;
        private final Glyph glyph;

        ModernButton(String text, Style style, Glyph glyph) {
            super(text);
            this.style = style;
            this.glyph = glyph;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFont(Theme.font(Font.BOLD, 13f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        private boolean glows() { return style == Style.PRIMARY || style == Style.PLAY; }

        @Override public Dimension getPreferredSize() {
            if (style == null) return super.getPreferredSize();
            FontMetrics fm = getFontMetrics(getFont());
            String text = getText() == null ? "" : getText();
            int content = fm.stringWidth(text) + (glyph == Glyph.NONE ? 0 : 16 + (text.isEmpty() ? 0 : 9));
            int padX = style == Style.SUBTLE ? 14 : 24;
            int h = style == Style.SUBTLE ? 34 : 46;
            int glow = glows() ? 16 : 0;
            return new Dimension(content + padX * 2 + glow, h + glow);
        }

        @Override public Dimension getMinimumSize() { return getPreferredSize(); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            boolean enabled = isEnabled();
            boolean hover = enabled && getModel().isRollover();
            boolean pressed = enabled && getModel().isArmed() && getModel().isPressed();
            int pad = glows() ? 8 : 0;
            int x = pad, y = pad, w = getWidth() - pad * 2, h = getHeight() - pad * 2;
            float arc = 14f;
            if (!enabled) g2.setComposite(AlphaComposite.SrcOver.derive(0.42f));
            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(x, y, w, h, arc, arc);
            RoundRectangle2D.Float inner = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1, arc, arc);
            Color fg;

            if (style == Style.PRIMARY || style == Style.PLAY) {
                boolean play = style == Style.PLAY;
                Color top = play ? new Color(0xFFDC82) : new Color(0x9B72FF);
                Color bottom = play ? new Color(0xEE9A35) : new Color(0x6D46E8);
                if (hover) { top = Theme.mix(top, Color.WHITE, 0.14f); bottom = Theme.mix(bottom, Color.WHITE, 0.14f); }
                if (pressed) { top = Theme.mix(top, Color.BLACK, 0.18f); bottom = Theme.mix(bottom, Color.BLACK, 0.18f); }
                if (enabled) {
                    Color glow = play ? Theme.GOLD2 : Theme.ACCENT;
                    for (int i = 6; i >= 1; i--) {
                        g2.setColor(Theme.alpha(glow, hover ? 16 : 10));
                        g2.fill(new RoundRectangle2D.Float(x - i, y - i + 2, w + 2 * i, h + 2 * i, arc + 2 * i, arc + 2 * i));
                    }
                }
                g2.setPaint(new GradientPaint(0, y, top, 0, y + h, bottom));
                g2.fill(shape);
                g2.setColor(new Color(255, 255, 255, play ? 80 : 50));
                g2.draw(inner);
                fg = play ? new Color(0x2A1A05) : Color.WHITE;
            } else if (style == Style.GHOST) {
                g2.setColor(new Color(255, 255, 255, pressed ? 34 : hover ? 24 : 12));
                g2.fill(shape);
                g2.setColor(new Color(255, 255, 255, hover ? 70 : 40));
                g2.draw(inner);
                fg = Theme.TEXT;
            } else {
                if (hover || pressed) {
                    g2.setColor(new Color(255, 255, 255, pressed ? 28 : 16));
                    g2.fill(shape);
                }
                fg = hover ? Theme.TEXT : Theme.MUTED;
            }

            String text = getText() == null ? "" : getText();
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int glyphW = glyph == Glyph.NONE ? 0 : 16 + (text.isEmpty() ? 0 : 9);
            int contentW = fm.stringWidth(text) + glyphW;
            int startX = x + (w - contentW) / 2;
            g2.setColor(fg);
            if (glyph != Glyph.NONE) drawGlyph(g2, glyph, startX, y + (h - 16) / 2f, fg);
            if (!text.isEmpty()) {
                g2.setColor(fg);
                g2.drawString(text, startX + glyphW, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }

        private static void drawGlyph(Graphics2D src, Glyph glyph, float x, float y, Color color) {
            Graphics2D g2 = (Graphics2D) src.create();
            g2.translate(x, y);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (glyph) {
                case DOWNLOAD -> {
                    g2.draw(new Line2D.Float(8, 2, 8, 10));
                    Path2D.Float head = new Path2D.Float();
                    head.moveTo(4.5, 6.5); head.lineTo(8, 10); head.lineTo(11.5, 6.5);
                    g2.draw(head);
                    g2.draw(new Line2D.Float(3, 13.5f, 13, 13.5f));
                }
                case PLAY -> {
                    Path2D.Float tri = new Path2D.Float();
                    tri.moveTo(4.5, 2.5); tri.lineTo(13, 8); tri.lineTo(4.5, 13.5); tri.closePath();
                    g2.fill(tri);
                }
                case REFRESH -> {
                    g2.draw(new Arc2D.Float(2.5f, 2.5f, 11, 11, 50, 270, Arc2D.OPEN));
                    double th = Math.toRadians(320);
                    float ex = 8 + (float) (5.5 * Math.cos(th)), ey = 8 - (float) (5.5 * Math.sin(th));
                    float tx = (float) -Math.sin(th), ty = (float) -Math.cos(th);
                    float px = -ty, py = tx;
                    Path2D.Float head = new Path2D.Float();
                    head.moveTo(ex + tx * 4.2f, ey + ty * 4.2f);
                    head.lineTo(ex + px * 3.4f, ey + py * 3.4f);
                    head.lineTo(ex - px * 3.4f, ey - py * 3.4f);
                    head.closePath();
                    g2.fill(head);
                }
                case FOLDER -> {
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D.Float f = new Path2D.Float();
                    f.moveTo(2, 4.5); f.lineTo(6, 4.5); f.lineTo(7.6, 6.2); f.lineTo(14, 6.2);
                    f.lineTo(14, 13); f.lineTo(2, 13); f.closePath();
                    g2.draw(f);
                }
                case CLOSE -> {
                    g2.draw(new Line2D.Float(4, 4, 12, 12));
                    g2.draw(new Line2D.Float(12, 4, 4, 12));
                }
                default -> { }
            }
            g2.dispose();
        }
    }

    /** Barra de progresso fina com gradiente, animação suave e percentual à direita. */
    private static final class ModernProgressBar extends JProgressBar {
        private final Color from, to;
        private final javax.swing.Timer animation;
        private double shown;

        ModernProgressBar(Color from, Color to) {
            super(0, 100);
            this.from = from;
            this.to = to;
            setOpaque(false);
            setBorder(null);
            animation = new javax.swing.Timer(15, e -> step());
            animation.setCoalesce(true);
            addChangeListener(e -> { if (!animation.isRunning()) animation.start(); });
        }

        private void step() {
            double target = getValue() * 100.0 / Math.max(1, getMaximum());
            shown += (target - shown) * 0.22;
            if (Math.abs(target - shown) < 0.15) { shown = target; animation.stop(); }
            repaint();
        }

        @Override public String getString() { return Math.round(getPercentComplete() * 100) + "%"; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            g2.setFont(Theme.font(Font.BOLD, 12f));
            FontMetrics fm = g2.getFontMetrics();
            String text = getString();
            int reserve = fm.stringWidth("100%") + 14;
            int barW = Math.max(10, getWidth() - reserve), barH = 8;
            int y = (getHeight() - barH) / 2;
            g2.setColor(new Color(255, 255, 255, 20));
            g2.fill(new RoundRectangle2D.Float(0, y, barW, barH, barH, barH));
            int fillW = (int) Math.round(barW * Math.max(0, Math.min(100, shown)) / 100.0);
            if (fillW > 0) {
                fillW = Math.max(fillW, barH);
                g2.setPaint(new GradientPaint(0, 0, from, barW, 0, to));
                g2.fill(new RoundRectangle2D.Float(0, y, fillW, barH, barH, barH));
            }
            g2.setColor(getValue() > 0 ? Theme.TEXT : Theme.MUTED);
            g2.drawString(text, getWidth() - fm.stringWidth(text), (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Selo de status: muda de cor conforme o texto (Pronto / Baixando… / Erro / Cancelado). */
    private static final class StatusPill extends JLabel {
        private javax.swing.Timer pulse;

        StatusPill(String text) {
            setText(text);
            setFont(Theme.font(Font.BOLD, 11.5f));
            setOpaque(false);
        }

        @Override public void setText(String text) {
            super.setText(text);
            boolean busy = tone() == 1;
            if (busy && pulse == null) {
                pulse = new javax.swing.Timer(50, e -> repaint());
                pulse.start();
            } else if (!busy && pulse != null) {
                pulse.stop();
                pulse = null;
            }
        }

        private int tone() {
            String t = getText() == null ? "" : getText().toLowerCase(Locale.ROOT);
            if (t.startsWith("pronto") || t.startsWith("ready") || t.isEmpty()) return 0;
            if (t.startsWith("erro") || t.startsWith("error")) return 2;
            if (t.startsWith("cancel")) return 3;
            return 1;
        }

        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(getText() == null ? "" : getText()) + 44, 30);
        }

        @Override public Dimension getMinimumSize() { return getPreferredSize(); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            int tone = tone();
            Color tint = switch (tone) {
                case 0 -> new Color(0x4ADE80);
                case 2 -> new Color(0xF87171);
                case 3 -> new Color(0xFBBF24);
                default -> new Color(0xA78BFA);
            };
            int w = getWidth(), h = getHeight();
            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h);
            g2.setColor(Theme.alpha(tint, 34));
            g2.fill(shape);
            g2.setColor(Theme.alpha(tint, 100));
            g2.draw(shape);
            double wave = tone == 1 ? 0.55 + 0.45 * Math.sin(System.currentTimeMillis() / 220.0) : 1.0;
            g2.setColor(Theme.alpha(tint, (int) (255 * wave)));
            g2.fill(new Ellipse2D.Float(13, h / 2f - 4, 8, 8));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Theme.mix(tint, Color.WHITE, 0.35f));
            g2.drawString(getText(), 28, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Etiqueta pequena (versão, tipo). */
    private static final class Pill extends JLabel {
        private final Color tint;

        Pill(Color tint, String text) {
            super(text, CENTER);
            this.tint = tint;
            setFont(Theme.font(Font.BOLD, 11.5f));
            setOpaque(false);
        }

        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(getText() == null ? "" : getText()) + 28, 30);
        }

        @Override public Dimension getMinimumSize() { return getPreferredSize(); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override protected void paintComponent(Graphics g) {
            if (tint == null) return;
            Graphics2D g2 = Theme.aa(g);
            int w = getWidth(), h = getHeight();
            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h, h);
            g2.setColor(Theme.alpha(tint, 28));
            g2.fill(shape);
            g2.setColor(Theme.alpha(tint, 80));
            g2.draw(shape);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(tint);
            String text = getText() == null ? "" : getText();
            g2.drawString(text, (w - fm.stringWidth(text)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Logo do launcher, carregado da arte correspondente ao modo ativo. */
    private static final class LogoMark extends JComponent {
        LogoMark() { setPreferredSize(new Dimension(54, 54)); }

        static BufferedImage render(int size) {
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = Theme.aa(image.getGraphics());
            paintLogo(g2, 2, 2, size - 4);
            g2.dispose();
            return image;
        }

        private static void paintLogo(Graphics2D g2, int x, int y, int s) {
            float arc = s * 0.32f;
            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(x, y, s, s, arc, arc);
            g2.setPaint(new GradientPaint(x, y, new Color(0xB79CFF), x + s, y + s, new Color(0x5B3FD9)));
            g2.fill(shape);
            g2.setColor(new Color(255, 255, 255, 70));
            g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, s - 1, s - 1, arc, arc));
            g2.setFont(Theme.font(Font.BOLD, s * 0.42f));
            FontMetrics fm = g2.getFontMetrics();
            String text = "L2";
            g2.setColor(Color.WHITE);
            g2.drawString(text, x + (s - fm.stringWidth(text)) / 2f, y + (s - fm.getHeight()) / 2f + fm.getAscent());
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            for (int i = 5; i >= 1; i--) {
                g2.setColor(Theme.alpha(Theme.ACCENT, 14));
                g2.fill(new RoundRectangle2D.Float(5 - i, 5 - i + 2, 44 + 2 * i, 44 + 2 * i, 14 + 2 * i, 14 + 2 * i));
            }
            paintLogo(g2, 5, 5, 44);
            g2.dispose();
        }
    }

    /** Combo sem fundo/borda própria (a moldura vem do FieldShell), com seta e lista customizadas. */
    private static final class ModernComboUI extends BasicComboBoxUI {
        @Override protected JButton createArrowButton() {
            JButton button = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = Theme.aa(g);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setColor(comboBox.isEnabled() ? Theme.MUTED : Theme.DIM);
                    float cx = getWidth() / 2f, cy = getHeight() / 2f;
                    float dir = comboBox.isPopupVisible() ? -1f : 1f;
                    Path2D.Float chevron = new Path2D.Float();
                    chevron.moveTo(cx - 4, cy - 2 * dir);
                    chevron.lineTo(cx, cy + 2 * dir);
                    chevron.lineTo(cx + 4, cy - 2 * dir);
                    g2.draw(chevron);
                    g2.dispose();
                }
            };
            button.setBorder(BorderFactory.createEmptyBorder());
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setFocusable(false);
            button.setPreferredSize(new Dimension(38, 44));
            return button;
        }

        @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) { }

        @Override public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            Object value = comboBox.getSelectedItem();
            if (value == null) return;
            Graphics2D g2 = Theme.aa(g);
            g2.setFont(comboBox.getFont());
            g2.setColor(comboBox.isEnabled() ? Theme.TEXT : Theme.DIM);
            FontMetrics fm = g2.getFontMetrics();
            String text = Theme.ellipsize(value.toString(), fm, bounds.width - 16);
            g2.drawString(text, bounds.x + 14, bounds.y + (bounds.height - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }

        @Override protected ComboPopup createPopup() {
            BasicComboPopup popup = new BasicComboPopup(comboBox) {
                @Override protected JScrollPane createScroller() {
                    JScrollPane scroller = super.createScroller();
                    scroller.setBorder(BorderFactory.createEmptyBorder());
                    scroller.setBackground(Theme.POPUP);
                    scroller.getViewport().setBackground(Theme.POPUP);
                    scroller.getVerticalScrollBar().setUI(new ThinScrollBarUI());
                    scroller.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
                    return scroller;
                }
            };
            popup.setBorder(BorderFactory.createLineBorder(Theme.BORDER_STRONG));
            popup.setBackground(Theme.POPUP);
            popup.getList().setBackground(Theme.POPUP);
            popup.getList().setFixedCellHeight(36);
            return popup;
        }
    }

    private static final class ComboItemRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                boolean selected, boolean focused) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
            label.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            label.setFont(Theme.font(Font.PLAIN, 13.5f));
            label.setOpaque(true);
            label.setBackground(selected ? Theme.POPUP_SELECTED : Theme.POPUP);
            label.setForeground(Theme.TEXT);
            label.setPreferredSize(new Dimension(10, 36));
            return label;
        }
    }

    /** Scrollbar fina, sem setas, estilo overlay. */
    private static final class ThinScrollBarUI extends BasicScrollBarUI {
        @Override public void installUI(JComponent c) {
            super.installUI(c);
            c.setOpaque(false);
        }

        private JButton zeroButton() {
            JButton button = new JButton();
            Dimension zero = new Dimension(0, 0);
            button.setPreferredSize(zero);
            button.setMinimumSize(zero);
            button.setMaximumSize(zero);
            return button;
        }

        @Override protected JButton createDecreaseButton(int orientation) { return zeroButton(); }
        @Override protected JButton createIncreaseButton(int orientation) { return zeroButton(); }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) { }

        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = Theme.aa(g);
            int alpha = isDragging ? 100 : isThumbRollover() ? 75 : 46;
            g2.setColor(new Color(255, 255, 255, alpha));
            g2.fill(new RoundRectangle2D.Float(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8));
            g2.dispose();
        }
    }

    private record ClientConfig(String displayName, String distrUrl, String executable,
                                boolean downloadLiveIni, String environment, String source,
                                Integer patchVersion, boolean enabled) {
        ClientConfig(String displayName, String distrUrl, String executable,
                     boolean downloadLiveIni, String environment, String source,
                     Integer patchVersion) {
            this(displayName, distrUrl, executable, downloadLiveIni, environment, source, patchVersion, true);
        }
        boolean isNcSoft() { return "ncsoft".equalsIgnoreCase(source); }
        @Override public String toString() { return displayName; }
    }
    private static final class Distribution { final String version; final java.util.Map<String, String> fields = new java.util.HashMap<>(); String resolvedUrl; Distribution(String version) { this.version = version; } Distribution(String version, String type, String label) { this.version = version; fields.put("type", type); fields.put("label", label); } String type() { return fields.getOrDefault("type", ""); } String files() { return fields.getOrDefault("files", ""); } @Override public String toString() { return version + " [" + type() + "]"; } }
    private record Entry(boolean compressed, String path, String sha1, String pack, long offset, int storedSize,
                         int originalSize, String sourceBase) {
        Entry(boolean compressed, String path, String sha1, String pack, long offset, int storedSize, int originalSize) {
            this(compressed, path, sha1, pack, offset, storedSize, originalSize, "");
        }
        Entry withSourceBase(String base) {
            return new Entry(compressed, path, sha1, pack, offset, storedSize, originalSize, base);
        }
    }
    private record Range(long start, long end) { }
    private static final class HttpStatusException extends IOException { final int status; HttpStatusException(int status, String message) { super(message); this.status = status; } }

    private static final class LauncherComboRenderer<T> extends JLabel implements ListCellRenderer<T> {
        private static final Color NORMAL = new Color(35, 38, 44);
        private static final Color SELECTED = new Color(85, 58, 125);
        private static final Color TEXT = new Color(232, 235, 240);

        LauncherComboRenderer() {
            setOpaque(true);
            setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends T> list, T value, int index,
                                                       boolean selected, boolean focused) {
            list.setFixedCellHeight(36);
            setText(value == null ? "" : value.toString());
            setBackground(selected ? SELECTED : NORMAL);
            setForeground(TEXT);
            return this;
        }
    }

    private static final class JPanelBuilder { final javax.swing.JPanel panel = new javax.swing.JPanel(new GridBagLayout()); GridBagConstraints constraints() { GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(4, 6, 4, 6); c.anchor = GridBagConstraints.WEST; return c; } void add(java.awt.Component component, GridBagConstraints c, int x, int y) { add(component, c, x, y, 1); } void add(java.awt.Component component, GridBagConstraints c, int x, int y, int width) { c.gridx = x; c.gridy = y; c.gridwidth = width; panel.add(component, (GridBagConstraints) c.clone()); } }
}
