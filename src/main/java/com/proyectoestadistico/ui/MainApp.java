package com.proyectoestadistico.ui;

import com.proyectoestadistico.i18n.UiMessages;
import com.proyectoestadistico.model.TablaDatos;
import com.proyectoestadistico.service.ActualizadorINEGI;
import com.proyectoestadistico.service.GeminiIntroduccionService;
import com.proyectoestadistico.service.GeneradorGraficas;
import com.proyectoestadistico.service.GeneradorPDF;
import com.proyectoestadistico.service.LectorCSV;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;

public class MainApp extends JFrame {

    private final LectorCSV lectorCSV = new LectorCSV();
    private final ActualizadorINEGI actualizadorINEGI = new ActualizadorINEGI();
    private final GeminiIntroduccionService geminiIntroduccionService = new GeminiIntroduccionService();
    private final GeneradorGraficas generadorGraficas = new GeneradorGraficas();
    private final GeneradorPDF generadorPDF = new GeneradorPDF();

    private TablaDatos tablaDatosOriginal;
    private java.util.List<TablaDatos> tablasDerivadas = java.util.Collections.emptyList();
    private java.util.Map<String, Path> csvPorCategoria = java.util.Collections.emptyMap();

    private JFreeChart graficaBarras;
    private JFreeChart graficaLineas;
    private JFreeChart graficaDispersion;

    private final UITheme theme;
    private JButton btnActualizarDatos;
    private JButton btnGenerarPdf;
    private JComboBox<String> comboCategoria;
    private JCheckBox chkPersistirBD;
    private JLabel badgeEstadoCsv;
    private JLabel lblTitulo;
    private JLabel lblSubtitulo;
    private JLabel lblEtiquetaCategoria;
    private JButton btnIdioma;

    private enum BadgeKind {
        NOT_UPDATED, DOWNLOADING, OK_WITH_DB, OK_CSV, UPDATE_ERROR, NEED_REFRESH_FIRST, SHOWING, LOAD_ERROR
    }

    private BadgeKind badgeKind = BadgeKind.NOT_UPDATED;
    private String badgeCategoriaEs;
    /** Coincide con la última acción "Actualizar datos (INEGI)"; para badge OK_* tras sync sin pisar SHOWING. */
    private boolean ultimaActualizacionInegiPersistioBD;

    public MainApp() {
        super();
        setTitle(UiMessages.get("window.title"));
        theme = UITheme.apply();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);

        getContentPane().setBackground(theme.bg0);
        initUI();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, theme.border),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));
        topPanel.setBackground(theme.bg1);

        lblTitulo = new JLabel(UiMessages.get("header.title"));
        lblTitulo.setFont(theme.titleFont(lblTitulo));
        lblTitulo.setForeground(theme.text);

        lblSubtitulo = new JLabel(UiMessages.get("header.subtitle"));
        lblSubtitulo.setFont(theme.subtitleFont(lblSubtitulo));
        lblSubtitulo.setForeground(theme.muted);

        JPanel tituloPanel = new JPanel();
        tituloPanel.setLayout(new BoxLayout(tituloPanel, BoxLayout.Y_AXIS));
        tituloPanel.setOpaque(false);
        tituloPanel.add(lblTitulo);
        tituloPanel.add(Box.createVerticalStrut(2));
        tituloPanel.add(lblSubtitulo);

        comboCategoria = new JComboBox<>(new String[]{"Educación", "Población", "Seguridad"});
        comboCategoria.setEnabled(false);
        comboCategoria.setBackground(theme.bg1);
        comboCategoria.setForeground(theme.text);
        comboCategoria.setOpaque(true);
        comboCategoria.setBorder(theme.roundedBorder(theme.border));
        comboCategoria.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String s) {
                    setText(etiquetaCategoriaVisible(s));
                }
                return this;
            }
        });
        comboCategoria.addActionListener(ev -> cargarVisualizacionDesdeCategoriaSeleccionada());

        JPanel categoriaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        categoriaPanel.setOpaque(false);
        lblEtiquetaCategoria = new JLabel(UiMessages.get("category.label"));
        lblEtiquetaCategoria.setForeground(theme.muted);
        lblEtiquetaCategoria.setFont(lblEtiquetaCategoria.getFont().deriveFont(Font.BOLD, 12f));
        categoriaPanel.add(lblEtiquetaCategoria);
        categoriaPanel.add(comboCategoria);

        tituloPanel.add(Box.createVerticalStrut(10));
        tituloPanel.add(categoriaPanel);
        tituloPanel.add(Box.createVerticalStrut(2));

        JPanel botonesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botonesPanel.setOpaque(false);
        badgeEstadoCsv = new JLabel();
        badgeEstadoCsv.setOpaque(true);
        badgeEstadoCsv.setBorder(BorderFactory.createCompoundBorder(
                theme.roundedBorder(theme.border),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));

        chkPersistirBD = new JCheckBox(UiMessages.get("chk.persist_db"));
        chkPersistirBD.setSelected(true);
        chkPersistirBD.setOpaque(false);
        chkPersistirBD.setForeground(theme.text);

        btnIdioma = crearBotonSecundario(UiMessages.get("btn.lang_to_en"));
        btnIdioma.setToolTipText(UiMessages.get("btn.language_tip"));
        btnIdioma.addActionListener(this::accionCambiarIdioma);

        btnActualizarDatos = crearBotonPrimario(UiMessages.get("btn.update"), theme.neonCyan, theme.neonCyan.darker());
        btnActualizarDatos.setToolTipText(UiMessages.get("btn.update.tip"));
        btnGenerarPdf = crearBotonSecundario(UiMessages.get("btn.pdf"));
        btnGenerarPdf.setToolTipText(UiMessages.get("btn.pdf.tip"));
        btnGenerarPdf.setEnabled(false);

        btnActualizarDatos.addActionListener(this::accionCargarCsv);
        btnGenerarPdf.addActionListener(this::accionGenerarPdf);

        botonesPanel.add(badgeEstadoCsv);
        botonesPanel.add(Box.createHorizontalStrut(6));
        botonesPanel.add(chkPersistirBD);
        botonesPanel.add(btnIdioma);
        botonesPanel.add(btnActualizarDatos);
        botonesPanel.add(btnGenerarPdf);

        topPanel.add(tituloPanel, BorderLayout.WEST);
        topPanel.add(botonesPanel, BorderLayout.EAST);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.45);
        splitPane.setBorder(null);
        splitPane.setBackground(theme.bg0);
        splitPane.setDividerSize(8);

        JTabbedPane tabsTablas = new JTabbedPane();
        tabsTablas.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tabsTablas.setBackground(theme.bg0);
        tabsTablas.setOpaque(true);
        tabsTablas.setForeground(theme.text);
        tabsTablas.addTab(UiMessages.get("table.numbered", 1), crearPlaceholderTabla(1));
        tabsTablas.addTab(UiMessages.get("table.numbered", 2), crearPlaceholderTabla(2));
        tabsTablas.addTab(UiMessages.get("table.numbered", 3), crearPlaceholderTabla(3));
        splitPane.setTopComponent(crearCard(tabsTablas));
        estilizarTabs(tabsTablas);

        JTabbedPane tabsGraficas = new JTabbedPane();
        tabsGraficas.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tabsGraficas.setBackground(theme.bg0);
        tabsGraficas.setOpaque(true);
        tabsGraficas.setForeground(theme.text);
        tabsGraficas.addTab(UiMessages.get("chart.bars"), crearPlaceholderGraficas(0));
        tabsGraficas.setToolTipTextAt(0, UiMessages.get("chart.tab.tip.bars"));
        tabsGraficas.addTab(UiMessages.get("chart.lines"), crearPlaceholderGraficas(1));
        tabsGraficas.setToolTipTextAt(1, UiMessages.get("chart.tab.tip.lines"));
        tabsGraficas.addTab(UiMessages.get("chart.scatter"), crearPlaceholderGraficas(2));
        tabsGraficas.setToolTipTextAt(2, UiMessages.get("chart.tab.tip.scatter"));
        splitPane.setBottomComponent(crearCard(tabsGraficas));
        estilizarTabs(tabsGraficas);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(splitPane, BorderLayout.CENTER);

        getRootPane().putClientProperty("tabsGraficas", tabsGraficas);
        getRootPane().putClientProperty("tabsTablas", tabsTablas);
        actualizarEstadoBotonPdf(false);
        setBadge(BadgeKind.NOT_UPDATED, null);
    }

    private String etiquetaCategoriaVisible(String claveEs) {
        if (claveEs == null || claveEs.isBlank()) {
            return "";
        }
        return switch (claveEs) {
            case "Educación" -> UiMessages.get("category.education");
            case "Población" -> UiMessages.get("category.population");
            case "Seguridad" -> UiMessages.get("category.security");
            default -> claveEs;
        };
    }

    private void setBadge(BadgeKind kind, String categoriaEs) {
        this.badgeKind = kind;
        this.badgeCategoriaEs = categoriaEs;
        pintarBadge();
    }

    private void setBadgeOkTrasSyncInegi() {
        setBadge(ultimaActualizacionInegiPersistioBD ? BadgeKind.OK_WITH_DB : BadgeKind.OK_CSV, null);
    }

    private void pintarBadge() {
        if (badgeEstadoCsv == null) {
            return;
        }
        String text = switch (badgeKind) {
            case NOT_UPDATED -> UiMessages.get("badge.not_updated");
            case DOWNLOADING -> UiMessages.get("badge.downloading");
            case OK_WITH_DB -> UiMessages.get("badge.ok_db");
            case OK_CSV -> UiMessages.get("badge.ok_csv");
            case UPDATE_ERROR -> UiMessages.get("badge.error_update");
            case NEED_REFRESH_FIRST -> UiMessages.get("badge.need_refresh_first");
            case SHOWING -> UiMessages.get("badge.showing", etiquetaCategoriaVisible(badgeCategoriaEs));
            case LOAD_ERROR -> UiMessages.get("badge.load_error");
        };
        Color bg;
        Color fg;
        switch (badgeKind) {
            case NOT_UPDATED -> {
                bg = theme.bg2;
                fg = theme.muted;
            }
            case DOWNLOADING, NEED_REFRESH_FIRST -> {
                bg = theme.neonMagenta;
                fg = theme.bg0;
            }
            case OK_WITH_DB, OK_CSV, SHOWING -> {
                bg = theme.neonGreen;
                fg = theme.bg0;
            }
            case UPDATE_ERROR, LOAD_ERROR -> {
                bg = theme.danger;
                fg = theme.bg0;
            }
            default -> {
                bg = theme.bg2;
                fg = theme.muted;
            }
        }
        actualizarBadgeCsv(text, bg, fg);
    }

    private void accionCambiarIdioma(ActionEvent e) {
        UiMessages.setEnglish(!UiMessages.isEnglish());
        aplicarIdiomaEnPantalla();
    }

    private void aplicarIdiomaEnPantalla() {
        setTitle(UiMessages.get("window.title"));
        if (lblTitulo != null) {
            lblTitulo.setText(UiMessages.get("header.title"));
        }
        if (lblSubtitulo != null) {
            lblSubtitulo.setText(UiMessages.get("header.subtitle"));
        }
        if (lblEtiquetaCategoria != null) {
            lblEtiquetaCategoria.setText(UiMessages.get("category.label"));
        }
        if (chkPersistirBD != null) {
            chkPersistirBD.setText(UiMessages.get("chk.persist_db"));
        }
        if (btnIdioma != null) {
            btnIdioma.setText(UiMessages.isEnglish() ? UiMessages.get("btn.lang_to_es") : UiMessages.get("btn.lang_to_en"));
            btnIdioma.setToolTipText(UiMessages.get("btn.language_tip"));
        }
        if (btnActualizarDatos != null) {
            btnActualizarDatos.setText(UiMessages.get("btn.update"));
            btnActualizarDatos.setToolTipText(UiMessages.get("btn.update.tip"));
        }
        if (btnGenerarPdf != null) {
            btnGenerarPdf.setText(UiMessages.get("btn.pdf"));
            btnGenerarPdf.setToolTipText(UiMessages.get("btn.pdf.tip"));
        }
        pintarBadge();
        if (csvPorCategoria == null || csvPorCategoria.isEmpty() || tablasDerivadas == null || tablasDerivadas.isEmpty()) {
            restaurarPlaceholdersTablas();
            restaurarPlaceholdersGraficas();
        } else {
            JTabbedPane tabsGraficas = (JTabbedPane) getRootPane().getClientProperty("tabsGraficas");
            if (tabsGraficas != null) {
                for (int i = 0; i < 3; i++) {
                    String shortTitle = switch (i) {
                        case 0 -> UiMessages.get("chart.bars");
                        case 1 -> UiMessages.get("chart.lines");
                        default -> UiMessages.get("chart.scatter");
                    };
                    String tip = switch (i) {
                        case 0 -> UiMessages.get("chart.tab.tip.bars");
                        case 1 -> UiMessages.get("chart.tab.tip.lines");
                        default -> UiMessages.get("chart.tab.tip.scatter");
                    };
                    tabsGraficas.setTitleAt(i, shortTitle);
                    tabsGraficas.setToolTipTextAt(i, tip);
                    actualizarTextoTab(tabsGraficas, i, shortTitle);
                }
            }
            poblarTablasSwing();
            generarGraficas();
        }
        if (comboCategoria != null) {
            comboCategoria.repaint();
        }
        revalidate();
        repaint();
    }

    private void restaurarPlaceholdersTablas() {
        JTabbedPane tabsTablas = (JTabbedPane) getRootPane().getClientProperty("tabsTablas");
        if (tabsTablas == null) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            String title = UiMessages.get("table.numbered", i + 1);
            tabsTablas.setTitleAt(i, title);
            tabsTablas.setToolTipTextAt(i, title);
            actualizarTextoTab(tabsTablas, i, title);
            tabsTablas.setComponentAt(i, crearPlaceholderTabla(i + 1));
        }
    }

    private void restaurarPlaceholdersGraficas() {
        JTabbedPane tabsGraficas = (JTabbedPane) getRootPane().getClientProperty("tabsGraficas");
        if (tabsGraficas == null) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            String shortTitle = switch (i) {
                case 0 -> UiMessages.get("chart.bars");
                case 1 -> UiMessages.get("chart.lines");
                default -> UiMessages.get("chart.scatter");
            };
            String tip = switch (i) {
                case 0 -> UiMessages.get("chart.tab.tip.bars");
                case 1 -> UiMessages.get("chart.tab.tip.lines");
                default -> UiMessages.get("chart.tab.tip.scatter");
            };
            tabsGraficas.setTitleAt(i, shortTitle);
            tabsGraficas.setToolTipTextAt(i, tip);
            actualizarTextoTab(tabsGraficas, i, shortTitle);
            tabsGraficas.setComponentAt(i, crearPlaceholderGraficas(i));
        }
    }

    private JButton crearBotonPrimario(String texto, Color base, Color hover) {
        JButton b = new JButton(texto);
        b.setForeground(theme.bg0);
        b.setBackground(base);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setBorder(theme.roundedBorder(base.darker()));
        b.setBorderPainted(true);
        b.setRolloverEnabled(true);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12f));
        b.setMargin(new Insets(8, 18, 8, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(hover);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(base);
            }
        });
        return b;
    }

    private JButton crearBotonSecundario(String texto) {
        JButton b = new JButton(texto);
        b.setForeground(theme.text);
        b.setBackground(theme.bg2);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setBorder(theme.roundedBorder(theme.border));
        b.setBorderPainted(true);
        b.setRolloverEnabled(true);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12f));
        b.setMargin(new Insets(8, 18, 8, 18));
        b.setForeground(theme.text);
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(theme.bg1);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(theme.bg2);
            }
        });
        return b;
    }

    private void actualizarEstadoBotonPdf(boolean habilitado) {
        if (btnGenerarPdf == null) return;
        btnGenerarPdf.setEnabled(habilitado);
        btnGenerarPdf.setBackground(habilitado ? theme.bg2 : new Color(24, 31, 56));
        btnGenerarPdf.setForeground(habilitado ? theme.text : new Color(100, 114, 150));
        btnGenerarPdf.setBorder(theme.roundedBorder(habilitado ? theme.border : new Color(44, 56, 92)));
    }

    private JPanel crearPlaceholderGraficas(int chartIndex) {
        String chartTitle = switch (chartIndex) {
            case 0 -> UiMessages.get("chart.label.bar");
            case 1 -> UiMessages.get("chart.label.line");
            default -> UiMessages.get("chart.label.scatter");
        };
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(theme.bg1);
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(new PlaceholderIcon(theme.neonCyan));
        stack.add(Box.createVerticalStrut(10));
        JLabel label = new JLabel(UiMessages.get("placeholder.chart.prompt", chartTitle), SwingConstants.CENTER);
        label.setForeground(theme.muted);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        stack.add(label);
        JLabel hint = new JLabel(UiMessages.get("placeholder.chart.hint"), SwingConstants.CENTER);
        hint.setForeground(new Color(120, 140, 190));
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));
        stack.add(Box.createVerticalStrut(4));
        stack.add(hint);
        p.add(stack);
        return p;
    }

    private JPanel crearPlaceholderTabla(int tableNumber) {
        String tableTitle = UiMessages.get("table.numbered", tableNumber);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(theme.bg1);
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(new PlaceholderIcon(theme.neonPurple));
        stack.add(Box.createVerticalStrut(10));
        JLabel label = new JLabel(UiMessages.get("placeholder.table.prompt", tableTitle), SwingConstants.CENTER);
        label.setForeground(theme.muted);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        stack.add(label);
        JLabel hint = new JLabel(UiMessages.get("placeholder.table.hint"), SwingConstants.CENTER);
        hint.setForeground(new Color(120, 140, 190));
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));
        stack.add(Box.createVerticalStrut(4));
        stack.add(hint);
        p.add(stack);
        return p;
    }

    private void accionCargarCsv(ActionEvent e) {
        if (btnActualizarDatos == null) return;

        boolean persistirBD = chkPersistirBD != null && chkPersistirBD.isSelected();
        ultimaActualizacionInegiPersistioBD = persistirBD;
        Path carpetaSalida = Path.of(System.getProperty("user.home"), "ProyectoEstadistico", "inegi_csv");

        btnActualizarDatos.setEnabled(false);
        actualizarEstadoBotonPdf(false);
        if (comboCategoria != null) comboCategoria.setEnabled(false);
        setBadge(BadgeKind.DOWNLOADING, null);

        SwingWorker<ActualizadorINEGI.Resultado, Void> worker = new SwingWorker<>() {
            @Override
            protected ActualizadorINEGI.Resultado doInBackground() throws Exception {
                return actualizadorINEGI.actualizar(persistirBD, carpetaSalida);
            }

            @Override
            protected void done() {
                try {
                    ActualizadorINEGI.Resultado resultado = get();
                    csvPorCategoria = resultado.csvPorCategoria();

                    if (comboCategoria != null) {
                        comboCategoria.setEnabled(true);
                        cargarVisualizacionDesdeCategoriaSeleccionada();
                        if (badgeKind == BadgeKind.DOWNLOADING) {
                            setBadgeOkTrasSyncInegi();
                        }
                    } else {
                        setBadgeOkTrasSyncInegi();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    setBadge(BadgeKind.UPDATE_ERROR, null);
                    String msg = ex.getMessage();
                    mostrarError(UiMessages.get("dialog.error.update.title"), msg == null || msg.isBlank()
                            ? UiMessages.get("dialog.error.update.body") : msg);
                    actualizarEstadoBotonPdf(false);
                } finally {
                    btnActualizarDatos.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void cargarVisualizacionDesdeCategoriaSeleccionada() {
        if (comboCategoria == null) return;
        Object item = comboCategoria.getSelectedItem();
        if (item == null) return;
        cargarVisualizacionDesdeCategoria(item.toString());
    }

    private void cargarVisualizacionDesdeCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) return;
        if (csvPorCategoria == null || csvPorCategoria.isEmpty()) {
            setBadge(BadgeKind.NEED_REFRESH_FIRST, null);
            actualizarEstadoBotonPdf(false);
            return;
        }

        Path csv = csvPorCategoria.get(categoria);
        if (csv == null || !Files.exists(csv)) {
            mostrarWarn(UiMessages.get("dialog.csv.notfound.title"),
                    UiMessages.get("dialog.csv.notfound.body", escapar(etiquetaCategoriaVisible(categoria))));
            actualizarEstadoBotonPdf(false);
            setBadgeOkTrasSyncInegi();
            return;
        }

        try {
            tablaDatosOriginal = lectorCSV.leer(csv);
            if (tablaDatosOriginal == null || tablaDatosOriginal.getNumeroColumnas() == 0) {
                mostrarError(UiMessages.get("dialog.invalid.title"), UiMessages.get("dialog.invalid.body"));
                actualizarEstadoBotonPdf(false);
                setBadgeOkTrasSyncInegi();
                return;
            }
            if (tablaDatosOriginal.getNumeroFilas() == 0) {
                mostrarWarn(UiMessages.get("dialog.csv.empty.title"), UiMessages.get("dialog.csv.empty.body"));
            }

            tablasDerivadas = derivarTresTablas(tablaDatosOriginal);

            if (tablasDerivadas == null || tablasDerivadas.isEmpty()) {
                actualizarEstadoBotonPdf(false);
                mostrarWarn(UiMessages.get("dialog.derive.fail.title"), UiMessages.get("dialog.derive.fail.body"));
            } else {
                poblarTablasSwing();
                generarGraficas();
                actualizarEstadoBotonPdf(true);
            }

            if (tablaDatosOriginal.getNumeroColumnas() < 4) {
                int posibles = Math.max(0, Math.min(3, tablaDatosOriginal.getNumeroColumnas() - 1));
                mostrarWarn(UiMessages.get("dialog.cols.title"),
                        UiMessages.get("dialog.cols.body", posibles));
            }

            setBadge(BadgeKind.SHOWING, categoria);
        } catch (IOException ex) {
            ex.printStackTrace();
            mostrarError(UiMessages.get("dialog.read.error.title"),
                    ex.getMessage() == null ? UiMessages.get("dialog.read.error.body") : escapar(ex.getMessage()));
            setBadge(BadgeKind.LOAD_ERROR, null);
            actualizarEstadoBotonPdf(false);
        }
    }

    private void actualizarBadgeCsv(String texto, Color bg, Color fg) {
        if (badgeEstadoCsv == null) return;
        badgeEstadoCsv.setText(texto);
        badgeEstadoCsv.setBackground(bg);
        badgeEstadoCsv.setForeground(fg);
        badgeEstadoCsv.repaint();
    }

    private void poblarTablasSwing() {
        JTabbedPane tabsTablas = (JTabbedPane) getRootPane().getClientProperty("tabsTablas");
        if (tabsTablas == null) return;

        java.util.List<TablaDatos> aMostrar = (tablasDerivadas == null || tablasDerivadas.isEmpty())
                ? java.util.List.of(tablaDatosOriginal)
                : tablasDerivadas;

        for (int i = 0; i < 3; i++) {
            if (i >= aMostrar.size() || aMostrar.get(i) == null) {
                String placeholderTitle = UiMessages.get("table.numbered", i + 1);
                tabsTablas.setTitleAt(i, placeholderTitle);
                actualizarTextoTab(tabsTablas, i, placeholderTitle);
                tabsTablas.setToolTipTextAt(i, placeholderTitle);
                tabsTablas.setComponentAt(i, crearPlaceholderTabla(i + 1));
                continue;
            }
            TablaDatos t = aMostrar.get(i);
            String titulo = t.getNumeroColumnas() > 1 ? (t.getEncabezados().get(0) + " vs " + t.getEncabezados().get(1))
                    : UiMessages.get("table.numbered", i + 1);
            tabsTablas.setTitleAt(i, titulo);
            actualizarTextoTab(tabsTablas, i, titulo);
            tabsTablas.setToolTipTextAt(i, titulo);
            tabsTablas.setComponentAt(i, crearScrollTablaPara(t));
        }
    }

    private JScrollPane crearScrollTablaPara(TablaDatos tabla) {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);
        table.setBackground(theme.bg1);
        table.setForeground(theme.text);
        table.setGridColor(theme.border);
        table.setSelectionBackground(theme.neonPurple.darker());
        table.setSelectionForeground(theme.text);
        table.setShowGrid(true);
        table.setAutoCreateRowSorter(true);
        table.setDefaultEditor(Object.class, null);
        table.setDefaultRenderer(Object.class, crearRendererCeldas());

        var tableHeader = table.getTableHeader();
        tableHeader.setBackground(theme.bg2);
        tableHeader.setForeground(theme.text);
        tableHeader.setReorderingAllowed(false);
        tableHeader.setResizingAllowed(true);
        tableHeader.setDefaultRenderer(crearHeaderRenderer());

        if (tabla != null && tabla.getNumeroColumnas() > 0) {
            for (String header : tabla.getEncabezados()) {
                model.addColumn(header);
            }
            for (var fila : tabla.getFilas()) {
                model.addRow(fila.toArray());
            }
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(theme.bg1);
        scroll.setBackground(theme.bg1);
        scroll.setBorder(theme.roundedContainerBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        estilizarScrollBars(scroll);
        return scroll;
    }

    private DefaultTableCellRenderer crearHeaderRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setOpaque(true);
                c.setBackground(theme.bg2);
                c.setForeground(theme.text);
                c.setFont(c.getFont().deriveFont(Font.BOLD, 12f));
                c.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, theme.border));
                c.setHorizontalAlignment(LEFT);
                return c;
            }
        };
    }

    private DefaultTableCellRenderer crearRendererCeldas() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setOpaque(true);
                if (isSelected) {
                    c.setBackground(theme.neonPurple.darker());
                    c.setForeground(theme.text);
                } else {
                    c.setBackground(row % 2 == 0 ? theme.bg1 : theme.bg2);
                    c.setForeground(theme.text);
                }

                String text = value == null ? "" : value.toString();
                if (esNumero(text)) {
                    c.setHorizontalAlignment(RIGHT);
                    c.setText(formatearNumero(text));
                } else {
                    c.setHorizontalAlignment(LEFT);
                    c.setText(text);
                }
                return c;
            }
        };
    }

    private java.util.List<TablaDatos> derivarTresTablas(TablaDatos original) {
        if (original == null || original.getNumeroColumnas() < 2) {
            return java.util.Collections.emptyList();
        }

        int xCol = 0;
        int[] yCols = new int[]{1, 2, 3};
        java.util.List<TablaDatos> out = new java.util.ArrayList<>();

        for (int yCol : yCols) {
            if (yCol >= original.getNumeroColumnas()) {
                continue;
            }
            TablaDatos t = new TablaDatos();
            t.getEncabezados().add(original.getEncabezados().get(xCol));
            t.getEncabezados().add(original.getEncabezados().get(yCol));
            for (var fila : original.getFilas()) {
                if (fila.size() <= yCol) continue;
                var nuevaFila = new java.util.ArrayList<String>(2);
                nuevaFila.add(fila.get(xCol));
                nuevaFila.add(fila.get(yCol));
                t.getFilas().add(nuevaFila);
            }
            out.add(t);
        }

        return out;
    }

    private void generarGraficas() {
        if (tablasDerivadas == null || tablasDerivadas.isEmpty()) {
            return;
        }

        graficaBarras = generadorGraficas.crearGraficaBarras(tablasDerivadas);
        graficaLineas = generadorGraficas.crearGraficaLineas(tablasDerivadas);
        graficaDispersion = generadorGraficas.crearGraficaDispersión(tablasDerivadas);

        JTabbedPane tabsGraficas = (JTabbedPane) getRootPane().getClientProperty("tabsGraficas");
        if (tabsGraficas == null) return;

        tabsGraficas.setComponentAt(0, crearPanelGrafica(graficaBarras));
        tabsGraficas.setComponentAt(1, crearPanelGrafica(graficaLineas));
        tabsGraficas.setComponentAt(2, crearPanelGrafica(graficaDispersion));
    }

    private JPanel crearPanelGrafica(JFreeChart chart) {
        ChartPanel panel = new ChartPanel(chart);
        panel.setBackground(theme.bg1);
        panel.setOpaque(true);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(theme.bg1);
        wrap.add(panel, BorderLayout.CENTER);
        wrap.setBorder(theme.roundedContainerBorder());
        return wrap;
    }

    private void accionGenerarPdf(ActionEvent e) {
        if (tablasDerivadas == null || tablasDerivadas.isEmpty()) {
            mostrarInfo(UiMessages.get("dialog.info.refresh.first.title"), UiMessages.get("dialog.info.refresh.first.body"));
            return;
        }

        String nombreDefault = UiMessages.isEnglish() ? "statistical_report.pdf" : "reporte_estadistico.pdf";
        Path destino = mostrarDialogoGuardarArchivoNativo(UiMessages.get("dialog.save.pdf.title"), nombreDefault);
        if (destino == null) return;

        try {
            if (Files.exists(destino)) {
                int res = mostrarConfirmacion(UiMessages.get("dialog.overwrite.title"),
                        UiMessages.get("dialog.overwrite.body", escapar(destino.toString())));
                if (res != JOptionPane.YES_OPTION) return;
            }
        } catch (Exception ignored) {
        }

        btnGenerarPdf.setEnabled(false);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    TablaDatos tablaSeleccionada = obtenerTablaSeleccionadaParaPdf();
                    String categoria = obtenerCategoriaSeleccionada();
                    String indicador = obtenerIndicadorSeleccionadoParaPdf();

                    String introduccion = geminiIntroduccionService.generarIntroduccion(categoria, indicador);

                    JFreeChart barrasSel = generadorGraficas.crearGraficaBarras(tablaSeleccionada);
                    JFreeChart lineasSel = generadorGraficas.crearGraficaLineas(tablaSeleccionada);
                    JFreeChart dispSel = generadorGraficas.crearGraficaDispersión(tablaSeleccionada);

                    generadorPDF.generarReporteConGraficas(destino, tablaSeleccionada,
                            introduccion, barrasSel, lineasSel, dispSel);
                } catch (Exception ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    if (error != null) {
                        error.printStackTrace();
                        String msg = error.getMessage() == null ? "" : error.getMessage();
                        mostrarError(UiMessages.get("dialog.pdf.error.title"),
                                error.getClass().getSimpleName() + (msg.isBlank() ? "" : (" - " + msg)));
                        return;
                    }
                    mostrarInfo(UiMessages.get("dialog.success.title"),
                            UiMessages.get("dialog.success.body", escapar(destino.toString())));
                } finally {
                    btnGenerarPdf.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private String obtenerCategoriaSeleccionada() {
        if (comboCategoria == null) return "";
        Object item = comboCategoria.getSelectedItem();
        return item == null ? "" : item.toString();
    }

    private TablaDatos obtenerTablaSeleccionadaParaPdf() {
        int idx = 0;
        try {
            JTabbedPane tabsTablas = (JTabbedPane) getRootPane().getClientProperty("tabsTablas");
            if (tabsTablas != null) idx = Math.max(0, tabsTablas.getSelectedIndex());
        } catch (Exception ignored) {
        }
        if (tablasDerivadas == null || tablasDerivadas.isEmpty()) {
            return new TablaDatos();
        }
        if (idx >= 0 && idx < tablasDerivadas.size()) {
            return tablasDerivadas.get(idx);
        }
        return tablasDerivadas.get(0);
    }

    private String obtenerIndicadorSeleccionadoParaPdf() {
        try {
            TablaDatos t = obtenerTablaSeleccionadaParaPdf();
            if (t == null || t.getEncabezados() == null || t.getEncabezados().size() < 2) return "";
            return t.getEncabezados().get(1);
        } catch (Exception ignored) {
            return "";
        }
    }

    private void mostrarInfo(String titulo, String mensajeHtml) {
        mostrarDialogoSimple(titulo, mensajeHtml, JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarWarn(String titulo, String mensajeHtml) {
        mostrarDialogoSimple(titulo, mensajeHtml, JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarError(String titulo, String mensajeHtml) {
        mostrarDialogoSimple(titulo, mensajeHtml, JOptionPane.ERROR_MESSAGE);
    }

    private int mostrarConfirmacion(String titulo, String mensajeHtml) {
        Object[] options = {UiMessages.get("dialog.yes"), UiMessages.get("dialog.no")};
        return mostrarDialogoOpciones(titulo, mensajeHtml, JOptionPane.QUESTION_MESSAGE, options, options[1]);
    }

    private JComponent crearContenidoDialogo(String mensajeHtml, int messageType) {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(theme.bg0);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(theme.bg1);
        card.setBorder(new UITheme.RoundedLineBorder(theme.border, 12, 1));

        JLabel iconLabel = new JLabel();
        iconLabel.setOpaque(true);
        iconLabel.setBackground(theme.bg1);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.TOP);
        Icon icon;
        switch (messageType) {
            case JOptionPane.ERROR_MESSAGE -> icon = UIManager.getIcon("OptionPane.errorIcon");
            case JOptionPane.WARNING_MESSAGE -> icon = UIManager.getIcon("OptionPane.warningIcon");
            case JOptionPane.QUESTION_MESSAGE -> icon = UIManager.getIcon("OptionPane.questionIcon");
            default -> icon = UIManager.getIcon("OptionPane.informationIcon");
        }
        iconLabel.setIcon(icon);
        card.add(iconLabel, BorderLayout.WEST);

        JLabel msg = new JLabel("<html><div style='width:380px; color:#E6F1FF;'>" + mensajeHtml + "</div></html>");
        msg.setOpaque(true);
        msg.setBackground(theme.bg1);
        msg.setForeground(theme.text);
        msg.setFont(msg.getFont().deriveFont(Font.PLAIN, 12f));
        card.add(msg, BorderLayout.CENTER);

        root.add(card, BorderLayout.CENTER);
        return root;
    }

    private void mostrarDialogoSimple(String titulo, String mensajeHtml, int messageType) {
        JOptionPane pane = new JOptionPane(
                crearContenidoDialogo(mensajeHtml, messageType),
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION
        );
        JDialog dialog = pane.createDialog(this, titulo);
        aplicarTemaDialogo(dialog);
        dialog.setVisible(true);
    }

    private int mostrarDialogoOpciones(String titulo, String mensajeHtml, int messageType, Object[] options, Object initialValue) {
        JOptionPane pane = new JOptionPane(
                crearContenidoDialogo(mensajeHtml, messageType),
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                options,
                initialValue
        );
        JDialog dialog = pane.createDialog(this, titulo);
        aplicarTemaDialogo(dialog);
        dialog.setVisible(true);
        Object val = pane.getValue();
        if (val == null) return JOptionPane.CLOSED_OPTION;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(val)) return i;
        }
        return JOptionPane.CLOSED_OPTION;
    }

    private void aplicarTemaDialogo(JDialog dialog) {
        if (dialog == null) return;
        dialog.setBackground(theme.bg0);
        if (dialog.getContentPane() != null) {
            dialog.getContentPane().setBackground(theme.bg0);
            aplicarTemaRecursivo(dialog.getContentPane());
        }
        dialog.pack();
        dialog.setResizable(false);
    }

    private void aplicarTemaRecursivo(Component c) {
        if (c instanceof JComponent jc) {
            jc.setOpaque(true);
        }

        if (c instanceof JPanel p) {
            p.setBackground(theme.bg0);
        } else if (c instanceof JLabel l) {
            l.setForeground(theme.text);
        } else if (c instanceof JButton b) {
            b.setFocusPainted(false);
            b.setFocusable(false);
            b.setOpaque(true);
            b.setContentAreaFilled(true);
            b.setBorder(theme.roundedBorder(theme.border));
            b.setBackground(b.isDefaultButton() ? theme.neonCyan : theme.bg2);
            b.setForeground(b.isDefaultButton() ? theme.bg0 : theme.text);
            b.setMargin(new Insets(8, 18, 8, 18));
        } else if (c instanceof JSeparator s) {
            s.setForeground(theme.border);
            s.setBackground(theme.border);
        }

        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                aplicarTemaRecursivo(child);
            }
        }
    }

    private static String escapar(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private JPanel crearCard(JComponent content) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(theme.bg1);
        card.setBorder(theme.roundedContainerBorder());
        card.add(content, BorderLayout.CENTER);
        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private void estilizarTabs(JTabbedPane tabs) {
        ChangeListener prev = (ChangeListener) tabs.getClientProperty("tabChipListener");
        if (prev != null) {
            tabs.removeChangeListener(prev);
        }
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        for (int i = 0; i < tabs.getTabCount(); i++) {
            tabs.setTabComponentAt(i, crearTabChip(tabs, i, tabs.getTitleAt(i), i == tabs.getSelectedIndex()));
        }
        ChangeListener listener = e -> {
            for (int i = 0; i < tabs.getTabCount(); i++) {
                var comp = tabs.getTabComponentAt(i);
                if (comp instanceof JLabel lab) {
                    boolean selected = i == tabs.getSelectedIndex();
                    lab.setBackground(selected ? theme.neonCyan : theme.bg2);
                    lab.setForeground(selected ? theme.bg0 : theme.text);
                    lab.setBorder(theme.roundedBorder(selected ? theme.neonCyan : theme.border));
                }
            }
        };
        tabs.putClientProperty("tabChipListener", listener);
        tabs.addChangeListener(listener);
        listener.stateChanged(null);
    }

    private JLabel crearTabChip(JTabbedPane tabs, int index, String text, boolean selected) {
        String safe = truncarTextoTab(text);
        JLabel lab = new JLabel(" " + safe + " ");
        lab.setOpaque(true);
        lab.setHorizontalAlignment(SwingConstants.CENTER);
        lab.setFont(lab.getFont().deriveFont(Font.BOLD, 12f));
        lab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lab.setBorder(theme.roundedBorder(selected ? theme.neonCyan : theme.border));
        lab.setBackground(selected ? theme.neonCyan : theme.bg2);
        lab.setForeground(selected ? theme.bg0 : theme.text);
        lab.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                tabs.setSelectedIndex(index);
            }
        });
        return lab;
    }

    private void actualizarTextoTab(JTabbedPane tabs, int idx, String text) {
        Component comp = tabs.getTabComponentAt(idx);
        if (comp instanceof JLabel lab) {
            lab.setText(" " + truncarTextoTab(text) + " ");
            lab.setToolTipText(text);
        }
    }

    private String truncarTextoTab(String t) {
        if (t == null) return "";
        return t.length() > 22 ? t.substring(0, 21) + "…" : t;
    }

    private boolean esNumero(String s) {
        if (s == null || s.isBlank()) return false;
        try {
            Double.parseDouble(s.trim());
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String formatearNumero(String s) {
        try {
            double d = Double.parseDouble(s.trim());
            return String.format(java.util.Locale.US, "%.3f", d);
        } catch (Exception ex) {
            return s;
        }
    }

    private static class PlaceholderIcon extends JComponent {
        private final Color color;
        PlaceholderIcon(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(38, 38));
            setMinimumSize(new Dimension(38, 38));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
                g2.fillOval(2, 2, w - 4, h - 4);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(4, 4, w - 8, h - 8);
                g2.drawLine(w / 2, 10, w / 2, h - 10);
                g2.drawLine(10, h / 2, w - 10, h / 2);
            } finally {
                g2.dispose();
            }
        }
    }

    private void estilizarScrollBars(JScrollPane scroll) {
        scroll.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 10));
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(10, 10));
    }

    private class DarkScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(70, 95, 150);
            this.trackColor = theme.bg1;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return crearBotonScroll();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return crearBotonScroll();
        }

        private JButton crearBotonScroll() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            b.setVisible(false);
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(theme.bg1);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(70, 95, 150));
            g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 2, thumbBounds.width - 2, thumbBounds.height - 4, 8, 8);
            g2.dispose();
        }
    }

    private Path mostrarDialogoAbrirArchivoNativo(String titulo, String archivoPattern) {
        try {
            FileDialog dialog = new FileDialog(this, titulo, FileDialog.LOAD);
            dialog.setFile(archivoPattern);
            dialog.setVisible(true);
            String file = dialog.getFile();
            String dir = dialog.getDirectory();
            if (file == null || dir == null) return null;
            return Path.of(dir, file);
        } catch (Exception ex) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(titulo);
            int result = chooser.showOpenDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) return null;
            return chooser.getSelectedFile().toPath();
        }
    }

    private Path mostrarDialogoGuardarArchivoNativo(String titulo, String nombreSugerido) {
        try {
            FileDialog dialog = new FileDialog(this, titulo, FileDialog.SAVE);
            dialog.setFile(nombreSugerido);
            dialog.setVisible(true);
            String file = dialog.getFile();
            String dir = dialog.getDirectory();
            if (file == null || dir == null) return null;
            Path p = Path.of(dir, file);
            if (!p.getFileName().toString().toLowerCase().endsWith(".pdf")) {
                p = Path.of(dir, p.getFileName().toString() + ".pdf");
            }
            return p;
        } catch (Exception ex) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(titulo);
            chooser.setSelectedFile(new java.io.File(nombreSugerido));
            int result = chooser.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) return null;
            return chooser.getSelectedFile().toPath();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
            new MainApp().setVisible(true);
        });
    }
}

