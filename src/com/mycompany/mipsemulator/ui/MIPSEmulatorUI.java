package com.mycompany.mipsemulator.ui;

import com.mycompany.mipsemulator.alu.ALU;
import com.mycompany.mipsemulator.cpu.CPU;
import com.mycompany.mipsemulator.cpu.Registros;
import com.mycompany.mipsemulator.execution.UnidadControl;
import com.mycompany.mipsemulator.instruction.Instruccion;
import com.mycompany.mipsemulator.memory.DiscoDuro;
import com.mycompany.mipsemulator.memory.RAM;
import com.mycompany.mipsemulator.memory.ROM;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Interfaz Swing del emulador.
 * Solo muestra el estado y solicita acciones a {@link CPU}; no decodifica ni
 * ejecuta instrucciones por sí misma. La memoria de registros y la RAM
 * permanecen visibles de forma simultánea; el resto va en pestañas secundarias.
 *
 * <p>Apariencia: tema <b>"Vintage Paper"</b> — paleta terrosa (plum, terracota,
 * mostaza, sage y café), tipografía serif en los títulos y monoespaciada en los
 * datos. Los fondos claros con texto oscuro mantienen la legibilidad incluso con
 * luz ambiental o proyector.</p>
 */
public class MIPSEmulatorUI extends JFrame {

    private static final int LIMITE_EJECUCION_TOTAL = 100000;
    private static final int ALTO_FILA_REGISTRO = 18;
    private static final int ALTO_FILA_RAM = 20;

    // --------------------------------------------------------------
    // Tema "Vintage Paper": papel, tinta y marcos
    // --------------------------------------------------------------
    private static final Color COLOR_FONDO_ARRIBA = new Color(0xF7EFE0);  // papel (arriba)
    private static final Color COLOR_FONDO_ABAJO = new Color(0xE3D2B8);   // papel (abajo)
    private static final Color COLOR_PANEL = new Color(0xFBF5E9);         // tarjetas (crema)
    private static final Color COLOR_PANEL_ALT = new Color(0xF3E8D5);     // filas alternas (zebra)
    private static final Color COLOR_BORDE = new Color(0xBFA37E);         // marcos
    private static final Color COLOR_REJA = new Color(0xDCCCB4);          // rejas de tabla
    private static final Color COLOR_TEXTO = new Color(0x3B3028);         // tinta principal
    private static final Color COLOR_TEXTO_SEC = new Color(0x6B5B4A);     // tinta secundaria

    // Acentos vintage
    private static final Color COLOR_PLUM = new Color(0x5C4A6E);          // ciruela
    private static final Color COLOR_PLUM_OSCURO = new Color(0x4A3B5C);
    private static final Color COLOR_TERRACOTA = new Color(0xB85C38);
    private static final Color COLOR_TERRACOTA_OSCURO = new Color(0x9A4A2C);
    private static final Color COLOR_MOSTAZA = new Color(0xD9A441);
    private static final Color COLOR_MOSTAZA_OSCURO = new Color(0xA97F2C);
    private static final Color COLOR_SAGE = new Color(0x6E8B63);
    private static final Color COLOR_SAGE_OSCURO = new Color(0x556E4C);
    private static final Color COLOR_CAFE = new Color(0x7A6248);
    private static final Color COLOR_CAFE_OSCURO = new Color(0x5E4A34);
    private static final Color COLOR_CREMA_TEXTO = new Color(0xF8F1E2);   // texto sobre acento oscuro
    private static final Color COLOR_TINTA_OSCURA = new Color(0x3A2B12);  // texto sobre mostaza

    // Títulos de sección (un acento por tarjeta)
    private static final Color COLOR_TITULO_REGISTROS = new Color(0x5C4A6E);
    private static final Color COLOR_TITULO_RAM = new Color(0xA9542F);
    private static final Color COLOR_TITULO_PESTANIAS = new Color(0x556E4C);
    private static final Color COLOR_TITULO_INFERIOR = new Color(0x8A6A2F);

    // Etapas y badges de estado
    private static final Color COLOR_ACTIVO = new Color(0x6E5A8C);         // etapa activa (plum)
    private static final Color COLOR_ACTIVO_TEXTO = new Color(0xF8F1E2);
    private static final Color COLOR_INACTIVO = new Color(0xE4D5BE);       // etapa inactiva (papel)
    private static final Color COLOR_INACTIVO_TEXTO = new Color(0x7A6A55);
    private static final Color COLOR_FIN = new Color(0x6E8B63);            // badge FIN (sage)
    private static final Color COLOR_FIN_TEXTO = new Color(0xF8F1E2);
    private static final Color COLOR_ERROR = new Color(0xB85C38);          // badge ERROR (terracota)
    private static final Color COLOR_ERROR_TEXTO = new Color(0xFBF1E6);
    private static final Color COLOR_LISTO_TEXTO = new Color(0x8A6A2F);

    // Resaltados de datos (un tono distinto por tipo)
    private static final Color COLOR_REG_MOD = new Color(0xF2D08A);        // registro modificado (mostaza)
    private static final Color COLOR_RAM_MOD = new Color(0xE8B08A);        // RAM modificada (terracota suave)
    private static final Color COLOR_RAM_CONSULTA = new Color(0xA9C7D9);   // RAM consultada (azul empolvado)
    private static final Color COLOR_FILA_CERO = new Color(0xEDE2CF);      // fila de $zero

    // Encabezados de tabla
    private static final Color COLOR_ENCABEZADO = new Color(0x6B5B4A);
    private static final Color COLOR_ENCABEZADO_TEXTO = new Color(0xF8F1E2);

    // --------------------------------------------------------------
    // Tipografías (con respaldo si la fuente no está instalada)
    // --------------------------------------------------------------
    private static final Font FUENTE_TITULO = fuente(
            new String[]{"Georgia", "Cambria", "Palatino Linotype", "Book Antiqua"}, Font.BOLD, 13);
    private static final Font FUENTE_TITULO_GRANDE = fuente(
            new String[]{"Georgia", "Cambria", "Palatino Linotype"}, Font.BOLD, 22);
    private static final Font FUENTE_SUBTITULO = fuente(
            new String[]{"Georgia", "Cambria", "Palatino Linotype"}, Font.ITALIC, 12);
    private static final Font FUENTE_UI = fuente(
            new String[]{"Segoe UI", "Trebuchet MS", "Verdana", "Tahoma"}, Font.PLAIN, 12);
    private static final Font FUENTE_UI_NEGRITA = fuente(
            new String[]{"Segoe UI", "Trebuchet MS", "Verdana", "Tahoma"}, Font.BOLD, 12);
    private static final Font FUENTE_DATOS = fuente(
            new String[]{"Consolas", "Cascadia Mono", "Lucida Console", "Monospaced"}, Font.PLAIN, 12);
    private static final Font FUENTE_DATOS_NEGRITA = fuente(
            new String[]{"Consolas", "Cascadia Mono", "Lucida Console", "Monospaced"}, Font.BOLD, 12);

    private final transient CPU cpu = new CPU();

    private final JLabel lblArchivo = new JLabel("(ninguno)");
    private final JLabel lblEstado = new JLabel("SIN_PROGRAMA");
    private final JLabel lblPC = new JLabel("-");
    private final JLabel lblInstruccion = new JLabel("-");
    private final JLabel lblBinario = new JLabel("-");
    private final JLabel lblContador = new JLabel("0");
    private final JLabel lblPrograma = new JLabel("0");
    private final JLabel lblBadgeFin = new JLabel("FIN");
    private final JLabel lblUltimaEtapa = new JLabel(" ");
    private final JLabel lblHI = new JLabel("0x00000000");
    private final JLabel lblLO = new JLabel("0x00000000");
    private final JLabel lblALUResumen = new JLabel("-");
    private final JLabel lblMensaje = new JLabel(" ");
    private final JLabel[] labelsEtapa = new JLabel[UnidadControl.ETAPAS.length];

    private final JButton btnCargar = new JButton("Cargar .bin");
    private final JButton btnEjecutar = new JButton("Ejecutar instrucción");
    private final JButton btnEtapa = new JButton("Siguiente etapa");
    private final JButton btnTodo = new JButton("Ejecutar hasta el final");
    private final JButton btnReiniciar = new JButton("Reiniciar");

    private final DefaultTableModel modeloRegistros;
    private final DefaultTableModel modeloInstrucciones;
    private final DefaultTableModel modeloROM;
    private final DefaultTableModel modeloDisco;
    private final RAMTableModel modeloRAM;

    private final JTable tablaRegistros = new JTable();
    private final JTable tablaRAM = new JTable();

    private final JTextArea areaALU = new JTextArea();
    private final JTextArea areaLog = new JTextArea();

    private final int[] regPrev = new int[Registros.CANTIDAD];
    private final int[] ramPrev = new int[RAM.TAMANO];
    private final Set<Integer> regsModificados = new HashSet<>();
    private final Set<Integer> celdasModificadas = new HashSet<>();
    private int celdaConsultada = -1;

    private boolean programaCargado = false;

    public MIPSEmulatorUI() {
        setTitle("Emulador monociclo MIPS32 - EmuladorMIPS32");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 860);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        PanelDegradado fondo = new PanelDegradado(COLOR_FONDO_ARRIBA, COLOR_FONDO_ABAJO);
        fondo.setLayout(new BorderLayout(8, 8));
        fondo.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
        setContentPane(fondo);

        modeloRegistros = modeloNoEditable("Nombre", "Índice", "Hexadecimal", "Decimal");
        modeloInstrucciones = modeloNoEditable("Índice", "Dirección", "Binario (32 bits)", "Mnemónico");
        modeloROM = modeloNoEditable("Índice", "Dirección", "Hexadecimal", "Decimal");
        modeloDisco = modeloNoEditable("Índice", "Dirección", "Hexadecimal", "Decimal");
        modeloRAM = new RAMTableModel();

        add(crearBanner(), BorderLayout.NORTH);
        add(crearPanelCentral(), BorderLayout.CENTER);
        add(crearPanelInferior(), BorderLayout.SOUTH);

        configurarEventos();
        inicializarTablas();
        habilitarControles(false);
        actualizarVista();
    }

    // ------------------------------------------------------------------
    // Construcción de la interfaz
    // ------------------------------------------------------------------
    private JPanel crearBanner() {
        JPanel banner = new JPanel(new BorderLayout(12, 0));
        banner.setBackground(COLOR_PLUM);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, COLOR_MOSTAZA),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JLabel titulo = new JLabel("Emulador MIPS32");
        titulo.setFont(FUENTE_TITULO_GRANDE);
        titulo.setForeground(COLOR_CREMA_TEXTO);

        JLabel subtitulo = new JLabel("Emulador monociclo  ·  ciclo IF → ID → EX → MEM → WB");
        subtitulo.setFont(FUENTE_SUBTITULO);
        subtitulo.setForeground(new Color(0xD8C9B0));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        textos.add(titulo);
        textos.add(subtitulo);

        banner.add(textos, BorderLayout.WEST);
        return banner;
    }

    private JPanel crearPanelCentral() {
        JSplitPane derecha = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                crearPanelRAM(), crearPanelPestanias());
        derecha.setResizeWeight(0.45);
        derecha.setContinuousLayout(true);
        derecha.setDividerLocation(300);
        derecha.setBackground(COLOR_PANEL_ALT);

        JSplitPane principal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                crearPanelRegistros(), derecha);
        principal.setResizeWeight(0.0);
        principal.setContinuousLayout(true);
        principal.setDividerLocation(430);
        principal.setBackground(COLOR_PANEL_ALT);

        JPanel panel = panelTransparente(new BorderLayout());
        panel.add(crearPanelSuperior(), BorderLayout.NORTH);
        panel.add(principal, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelSuperior() {
        JPanel panel = panelTransparente(new BorderLayout(8, 6));

        estilizarBoton(btnCargar, COLOR_SAGE, COLOR_CREMA_TEXTO, COLOR_SAGE_OSCURO);
        estilizarBoton(btnEjecutar, COLOR_TERRACOTA, COLOR_CREMA_TEXTO, COLOR_TERRACOTA_OSCURO);
        estilizarBoton(btnEtapa, COLOR_MOSTAZA, COLOR_TINTA_OSCURA, COLOR_MOSTAZA_OSCURO);
        estilizarBoton(btnTodo, COLOR_PLUM, COLOR_CREMA_TEXTO, COLOR_PLUM_OSCURO);
        estilizarBoton(btnReiniciar, COLOR_CAFE, COLOR_CREMA_TEXTO, COLOR_CAFE_OSCURO);

        JPanel fila1 = panelTransparente(new FlowLayout(FlowLayout.LEFT, 10, 4));
        fila1.add(btnCargar);
        fila1.add(new JLabel("Archivo:"));
        fila1.add(lblArchivo);
        fila1.add(Box.createHorizontalStrut(24));
        fila1.add(new JLabel("Estado:"));
        lblEstado.setFont(FUENTE_UI_NEGRITA);
        fila1.add(lblEstado);

        JPanel fila2 = panelTransparente(new FlowLayout(FlowLayout.LEFT, 6, 0));
        fila2.add(btnEjecutar);
        fila2.add(btnEtapa);
        fila2.add(btnTodo);
        fila2.add(btnReiniciar);

        JPanel filas = panelTransparente(new BorderLayout());
        filas.add(fila1, BorderLayout.NORTH);
        filas.add(fila2, BorderLayout.CENTER);

        panel.add(filas, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelRegistros() {
        JPanel panel = tarjeta(new BorderLayout(0, 4));
        panel.setBorder(bordeTarjeta("Memoria de registros (32 registros)", COLOR_TITULO_REGISTROS));

        tablaRegistros.setModel(modeloRegistros);
        estilizarTabla(tablaRegistros);
        tablaRegistros.setRowHeight(ALTO_FILA_REGISTRO);
        tablaRegistros.setFont(FUENTE_DATOS);
        tablaRegistros.setDefaultRenderer(Object.class, new RegistroRenderer());
        JScrollPane scroll = estilizarScroll(new JScrollPane(tablaRegistros));
        scroll.getVerticalScrollBar().setUnitIncrement(ALTO_FILA_REGISTRO);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel especiales = tarjeta(new FlowLayout(FlowLayout.LEFT, 12, 4));
        especiales.setBorder(bordeTarjeta("Registros especiales", COLOR_TITULO_REGISTROS));
        lblHI.setFont(FUENTE_DATOS_NEGRITA);
        lblLO.setFont(FUENTE_DATOS_NEGRITA);
        especiales.add(new JLabel("HI:"));
        especiales.add(lblHI);
        especiales.add(Box.createHorizontalStrut(16));
        especiales.add(new JLabel("LO:"));
        especiales.add(lblLO);
        panel.add(especiales, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearPanelRAM() {
        JPanel panel = tarjeta(new BorderLayout(0, 4));
        panel.setBorder(bordeTarjeta("RAM", COLOR_TITULO_RAM));

        tablaRAM.setModel(modeloRAM);
        estilizarTabla(tablaRAM);
        tablaRAM.setRowHeight(ALTO_FILA_RAM);
        tablaRAM.setFont(FUENTE_DATOS);
        tablaRAM.setDefaultRenderer(Object.class, new RAMRenderer());
        tablaRAM.setRowSelectionAllowed(false);
        tablaRAM.setFillsViewportHeight(false);
        tablaRAM.getTableHeader().setReorderingAllowed(false);
        tablaRAM.getColumnModel().getColumn(0).setPreferredWidth(150);
        tablaRAM.getColumnModel().getColumn(1).setPreferredWidth(150);

        JScrollPane scroll = estilizarScroll(new JScrollPane(tablaRAM));
        scroll.getVerticalScrollBar().setUnitIncrement(ALTO_FILA_RAM);
        panel.add(scroll, BorderLayout.CENTER);

        JLabel leyenda = new JLabel("Mostaza = registro modificado.  "
                + "Terracota = celda de RAM modificada.  "
                + "Azul = última celda consultada.");
        leyenda.setFont(FUENTE_UI);
        leyenda.setForeground(COLOR_TEXTO_SEC);
        panel.add(leyenda, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearPanelPestanias() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(COLOR_PANEL);
        tabs.setForeground(COLOR_TEXTO);
        tabs.setFont(FUENTE_UI_NEGRITA);

        tabs.addTab("Memoria de instrucciones", estilizarScroll(new JScrollPane(tablaCon(modeloInstrucciones))));
        tabs.addTab("ROM", estilizarScroll(new JScrollPane(tablaCon(modeloROM))));
        tabs.addTab("Disco duro", estilizarScroll(new JScrollPane(tablaCon(modeloDisco))));

        areaALU.setEditable(false);
        areaALU.setFont(FUENTE_DATOS);
        areaALU.setBackground(COLOR_PANEL);
        areaALU.setForeground(COLOR_TEXTO);
        areaALU.setCaretColor(COLOR_TEXTO);
        tabs.addTab("ALU / HI-LO", estilizarScroll(new JScrollPane(areaALU)));

        areaLog.setEditable(false);
        areaLog.setFont(FUENTE_DATOS);
        areaLog.setBackground(COLOR_PANEL);
        areaLog.setForeground(COLOR_TEXTO);
        areaLog.setCaretColor(COLOR_TEXTO);
        tabs.addTab("Log de etapas", estilizarScroll(new JScrollPane(areaLog)));

        JPanel panel = tarjeta(new BorderLayout());
        panel.setBorder(bordeTarjeta("Vistas secundarias", COLOR_TITULO_PESTANIAS));
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelInferior() {
        JPanel panel = tarjeta(new BorderLayout(6, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(3, 0, 0, 0, COLOR_BORDE),
                BorderFactory.createEmptyBorder(8, 12, 6, 12)));

        JPanel info = panelTransparente(new GridLayout(3, 1, 4, 2));

        JPanel filaPC = panelTransparente(new FlowLayout(FlowLayout.LEFT, 10, 0));
        lblPC.setFont(FUENTE_DATOS_NEGRITA);
        lblInstruccion.setFont(FUENTE_DATOS_NEGRITA);
        lblBinario.setFont(FUENTE_DATOS);
        lblContador.setFont(FUENTE_DATOS_NEGRITA);
        lblPrograma.setFont(FUENTE_DATOS_NEGRITA);
        filaPC.add(new JLabel("PC:"));
        filaPC.add(lblPC);
        filaPC.add(Box.createHorizontalStrut(16));
        filaPC.add(new JLabel("Instrucción actual:"));
        filaPC.add(lblInstruccion);
        filaPC.add(Box.createHorizontalStrut(16));
        filaPC.add(lblBinario);
        filaPC.add(Box.createHorizontalStrut(16));
        filaPC.add(new JLabel("Instrucciones ejecutadas (acumuladas):"));
        filaPC.add(lblContador);
        filaPC.add(Box.createHorizontalStrut(16));
        filaPC.add(new JLabel("Programa cargado:"));
        filaPC.add(lblPrograma);
        filaPC.add(new JLabel("instr."));

        JPanel filaEtapas = panelTransparente(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JLabel lblEtapas = new JLabel("Etapas:");
        lblEtapas.setFont(FUENTE_UI_NEGRITA);
        filaEtapas.add(lblEtapas);
        for (int i = 0; i < labelsEtapa.length; i++) {
            JLabel etapa = new JLabel(UnidadControl.ETAPAS[i], SwingConstants.CENTER);
            etapa.setOpaque(true);
            etapa.setBackground(COLOR_INACTIVO);
            etapa.setForeground(COLOR_INACTIVO_TEXTO);
            etapa.setBorder(BorderFactory.createLineBorder(COLOR_BORDE));
            etapa.setPreferredSize(new Dimension(48, 23));
            etapa.setFont(FUENTE_UI_NEGRITA);
            labelsEtapa[i] = etapa;
            filaEtapas.add(etapa);
        }
        filaEtapas.add(Box.createHorizontalStrut(10));
        lblBadgeFin.setOpaque(true);
        lblBadgeFin.setHorizontalAlignment(SwingConstants.CENTER);
        lblBadgeFin.setPreferredSize(new Dimension(76, 23));
        lblBadgeFin.setFont(FUENTE_UI_NEGRITA);
        lblBadgeFin.setBackground(COLOR_FIN);
        lblBadgeFin.setForeground(COLOR_FIN_TEXTO);
        lblBadgeFin.setBorder(BorderFactory.createLineBorder(COLOR_SAGE_OSCURO));
        lblBadgeFin.setVisible(false);
        filaEtapas.add(lblBadgeFin);

        lblUltimaEtapa.setFont(FUENTE_UI);
        lblUltimaEtapa.setForeground(COLOR_TEXTO_SEC);
        filaEtapas.add(lblUltimaEtapa);

        filaEtapas.add(Box.createHorizontalStrut(16));
        JLabel lblALU = new JLabel("ALU:");
        lblALU.setFont(FUENTE_UI_NEGRITA);
        lblALUResumen.setFont(FUENTE_DATOS);
        filaEtapas.add(lblALU);
        filaEtapas.add(lblALUResumen);

        JPanel filaMensaje = panelTransparente(new BorderLayout());
        lblMensaje.setFont(FUENTE_UI);
        lblMensaje.setForeground(COLOR_TEXTO);
        filaMensaje.add(lblMensaje, BorderLayout.CENTER);

        info.add(filaPC);
        info.add(filaEtapas);
        info.add(filaMensaje);

        panel.add(info, BorderLayout.CENTER);
        return panel;
    }

    // ------------------------------------------------------------------
    // Estilos del tema "Vintage Paper"
    // ------------------------------------------------------------------
    private static JPanel tarjeta(LayoutManager diseño) {
        JPanel panel = new JPanel(diseño);
        panel.setBackground(COLOR_PANEL);
        return panel;
    }

    private static JPanel panelTransparente(LayoutManager diseño) {
        JPanel panel = new JPanel(diseño);
        panel.setOpaque(false);
        return panel;
    }

    private static TitledBorder bordeTitulo(String texto, Color color) {
        TitledBorder borde = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDE), texto);
        borde.setTitleFont(FUENTE_TITULO);
        borde.setTitleColor(color);
        return borde;
    }

    /** Borde de tarjeta: marco + título de sección + relleno interior. */
    private static javax.swing.border.Border bordeTarjeta(String texto, Color color) {
        return BorderFactory.createCompoundBorder(
                bordeTitulo(texto, color),
                BorderFactory.createEmptyBorder(2, 6, 6, 6));
    }

    private static void estilizarBoton(JButton boton, Color fondo, Color texto, Color borde) {
        boton.setBackground(fondo);
        boton.setForeground(texto);
        boton.setFont(FUENTE_UI_NEGRITA);
        boton.setOpaque(true);
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borde),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        boton.setFocusPainted(false);
        // Efecto al pasar el mouse: un tono más claro del mismo color.
        final Color claro = aclarar(fondo, 0.12);
        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (boton.isEnabled()) {
                    boton.setBackground(claro);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                boton.setBackground(fondo);
            }
        });
    }

    private static void estilizarTabla(JTable tabla) {
        tabla.setBackground(COLOR_PANEL);
        tabla.setForeground(COLOR_TEXTO);
        tabla.setGridColor(COLOR_REJA);
        tabla.setSelectionBackground(COLOR_MOSTAZA);
        tabla.setSelectionForeground(COLOR_TINTA_OSCURA);
        tabla.getTableHeader().setBackground(COLOR_ENCABEZADO);
        tabla.getTableHeader().setForeground(COLOR_ENCABEZADO_TEXTO);
        tabla.getTableHeader().setFont(FUENTE_UI_NEGRITA);
        tabla.getTableHeader().setOpaque(true);
    }

    private static JScrollPane estilizarScroll(JScrollPane scroll) {
        scroll.setBackground(COLOR_PANEL);
        scroll.getViewport().setBackground(COLOR_PANEL);
        scroll.setBorder(BorderFactory.createLineBorder(COLOR_BORDE));
        return scroll;
    }

    private static JTable tablaCon(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);
        estilizarTabla(tabla);
        tabla.setRowHeight(20);
        tabla.setFont(FUENTE_DATOS);
        tabla.setDefaultRenderer(Object.class, new FilaZebra());
        return tabla;
    }

    /** Primer nombre de fuente disponible de la lista; si no hay, usa la última. */
    private static Font fuente(String[] candidatas, int estilo, int tamano) {
        try {
            Set<String> disponibles = new HashSet<>();
            for (String nombre : GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames()) {
                disponibles.add(nombre);
            }
            for (String candidata : candidatas) {
                if (disponibles.contains(candidata)) {
                    return new Font(candidata, estilo, tamano);
                }
            }
        } catch (Exception ex) {
            // Sin entorno gráfico: se usa la fuente lógica de respaldo.
        }
        return new Font(candidatas[candidatas.length - 1], estilo, tamano);
    }

    /** Aclara un color acercándolo al blanco (factor 0..1). */
    private static Color aclarar(Color color, double factor) {
        int r = (int) Math.round(color.getRed() + (255 - color.getRed()) * factor);
        int g = (int) Math.round(color.getGreen() + (255 - color.getGreen()) * factor);
        int b = (int) Math.round(color.getBlue() + (255 - color.getBlue()) * factor);
        return new Color(r, g, b);
    }

    /** Fondo de papel con un degradado vertical suave. */
    private static class PanelDegradado extends JPanel {
        private final Color arriba;
        private final Color abajo;

        PanelDegradado(Color arriba, Color abajo) {
            this.arriba = arriba;
            this.abajo = abajo;
            setBackground(arriba);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setPaint(new GradientPaint(0, 0, arriba, 0, getHeight(), abajo));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    /** Renderer de filas alternas (zebra) para las tablas secundarias. */
    private static class FilaZebra extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccion,
                boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(tabla, valor, seleccion, foco, fila, columna);
            if (seleccion) {
                c.setBackground(tabla.getSelectionBackground());
                c.setForeground(tabla.getSelectionForeground());
            } else {
                c.setBackground(fila % 2 == 0 ? COLOR_PANEL : COLOR_PANEL_ALT);
                c.setForeground(COLOR_TEXTO);
            }
            return c;
        }
    }

    // ------------------------------------------------------------------
    // Eventos
    // ------------------------------------------------------------------
    private void configurarEventos() {
        btnCargar.addActionListener(e -> cargarArchivo());
        btnEjecutar.addActionListener(e -> {
            cpu.ejecutarInstruccion();
            registrarEnLog();
            actualizarVista();
        });
        btnEtapa.addActionListener(e -> {
            cpu.ejecutarEtapa();
            registrarEnLog();
            actualizarVista();
        });
        btnTodo.addActionListener(e -> {
            int n = cpu.ejecutarHastaElFinal(LIMITE_EJECUCION_TOTAL);
            areaLog.append("[SISTEMA] Ejecutadas " + n + " instrucciones en esta corrida. "
                    + "Total acumulado: " + cpu.getContadorInstrucciones() + ".\n");
            actualizarVista();
        });
        btnReiniciar.addActionListener(e -> {
            if (!programaCargado) {
                return;
            }
            cpu.reiniciarEstado();
            limpiarResaltados();
            areaLog.append("[SISTEMA] Estado reiniciado.\n");
            actualizarVista();
        });
    }

    private void cargarArchivo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos binarios (*.bin)", "bin"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = chooser.getSelectedFile();
        try {
            cpu.cargarPrograma(archivo.getAbsolutePath());
            programaCargado = true;
            lblArchivo.setText(archivo.getName() + " (" + archivo.length() + " bytes)");
            areaLog.setText("");
            areaLog.append("[SISTEMA] Programa cargado: " + archivo.getAbsolutePath() + "\n");
            cargarTablaInstrucciones();
            limpiarResaltados();
            habilitarControles(true);
            actualizarVista();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo cargar el archivo:\n" + ex.getMessage(),
                    "Error de carga", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void habilitarControles(boolean habilitado) {
        btnEjecutar.setEnabled(habilitado);
        btnEtapa.setEnabled(habilitado);
        btnTodo.setEnabled(habilitado);
        btnReiniciar.setEnabled(habilitado);
    }

    // ------------------------------------------------------------------
    // Inicialización y actualización de tablas
    // ------------------------------------------------------------------
    private void inicializarTablas() {
        for (int i = 0; i < Registros.CANTIDAD; i++) {
            modeloRegistros.addRow(new Object[]{cpu.getRegistros().getNombre(i), i, "0x00000000", 0});
        }
        for (int i = 0; i < ROM.TAMANO; i++) {
            modeloROM.addRow(new Object[]{i, direccionHex(ROM.BASE, i), "0x00000000", 0});
        }
        for (int i = 0; i < DiscoDuro.TAMANO; i++) {
            modeloDisco.addRow(new Object[]{i, direccionHex(DiscoDuro.BASE, i), "0x00000000", 0});
        }
    }

    private void cargarTablaInstrucciones() {
        modeloInstrucciones.setRowCount(0);
        int cantidad = cpu.getMemoriaInstrucciones().getCantidad();
        for (int i = 0; i < cantidad; i++) {
            int palabra = cpu.getMemoriaInstrucciones().getPalabra(i);
            Instruccion ins = cpu.getDecodificador().decodificar(palabra);
            modeloInstrucciones.addRow(new Object[]{
                i,
                String.format("0x%08X", i * 4),
                binario(palabra),
                ins.getMnemonico()
            });
        }
    }

    private void actualizarVista() {
        String estado = cpu.getEstado();
        lblEstado.setText(estado);
        lblEstado.setForeground(colorEstado(estado));

        lblPC.setText(String.format("0x%08X", cpu.getPC().get()));

        Instruccion ins = cpu.getInstruccionActual();
        if (ins != null) {
            lblInstruccion.setText(ins.getMnemonico() + "  @ 0x"
                    + String.format("%08X", cpu.getPCInstruccionActual()));
            lblBinario.setText(ins.toBinario());
        } else {
            lblInstruccion.setText("-");
            lblBinario.setText("-");
        }

        boolean terminado = "FINALIZADO".equals(estado) || "ERROR".equals(estado);
        int etapa = cpu.getUnidadControl().getEtapaIndice();
        for (int i = 0; i < labelsEtapa.length; i++) {
            boolean activa = !terminado && (etapa >= 1 && etapa <= labelsEtapa.length && i == etapa - 1);
            labelsEtapa[i].setBackground(activa ? COLOR_ACTIVO : COLOR_INACTIVO);
            labelsEtapa[i].setForeground(activa ? COLOR_ACTIVO_TEXTO : COLOR_INACTIVO_TEXTO);
            labelsEtapa[i].setBorder(BorderFactory.createLineBorder(activa ? COLOR_PLUM_OSCURO : COLOR_BORDE));
        }
        if (terminado) {
            boolean esError = "ERROR".equals(estado);
            lblBadgeFin.setText(esError ? "ERROR" : "FIN");
            lblBadgeFin.setBackground(esError ? COLOR_ERROR : COLOR_FIN);
            lblBadgeFin.setForeground(esError ? COLOR_ERROR_TEXTO : COLOR_FIN_TEXTO);
            lblBadgeFin.setBorder(BorderFactory.createLineBorder(
                    esError ? COLOR_TERRACOTA_OSCURO : COLOR_SAGE_OSCURO));
            lblBadgeFin.setVisible(true);
            lblUltimaEtapa.setText("Última etapa procesada: "
                    + cpu.getUnidadControl().getEtapaNombre() + " (" + etapa + ")");
        } else {
            lblBadgeFin.setVisible(false);
            lblUltimaEtapa.setText(" ");
        }

        lblContador.setText(String.valueOf(cpu.getContadorInstrucciones()));
        lblPrograma.setText(String.valueOf(cpu.getMemoriaInstrucciones().getCantidad()));

        // Registros: valores + detección de cambios.
        for (int i = 0; i < Registros.CANTIDAD; i++) {
            int v = cpu.getRegistros().read(i);
            if (v != regPrev[i]) {
                regsModificados.add(i);
            }
            modeloRegistros.setValueAt(String.format("0x%08X", v), i, 2);
            modeloRegistros.setValueAt(v, i, 3);
            regPrev[i] = v;
        }

        ALU alu = cpu.getALU();
        lblHI.setText(String.format("0x%08X", alu.getHi()));
        lblLO.setText(String.format("0x%08X", alu.getLo()));

        // RAM: valores + detección de cambios y celda consultada.
        celdasModificadas.clear();
        for (int i = 0; i < RAM.TAMANO; i++) {
            int v = cpu.getRAM().getCelda(i);
            if (v != ramPrev[i]) {
                celdasModificadas.add(i);
            }
            modeloRAM.setValor(i, String.format("0x%08X", v));
            ramPrev[i] = v;
        }
        celdaConsultada = indiceRAM(cpu.getUltimaDireccionDatos());

        actualizarMemoria(modeloROM, cpu.getROM(), ROM.BASE, ROM.TAMANO);
        actualizarMemoria(modeloDisco, cpu.getDisco(), DiscoDuro.BASE, DiscoDuro.TAMANO);

        actualizarALU();
        lblMensaje.setText(cpu.getMensaje());

        tablaRegistros.repaint();
        tablaRAM.repaint();
    }

    private void actualizarALU() {
        ALU alu = cpu.getALU();
        lblALUResumen.setText((alu.getUltimaOperacion() == null ? "-" : alu.getUltimaOperacion())
                + "  A=0x" + String.format("%08X", alu.getOperandoA())
                + "  B=0x" + String.format("%08X", alu.getOperandoB())
                + "  res=0x" + String.format("%08X", alu.getResultado())
                + "  Z=" + (alu.isZero() ? 1 : 0));

        StringBuilder sb = new StringBuilder();
        sb.append("Operación: ").append(alu.getUltimaOperacion() == null ? "-" : alu.getUltimaOperacion()).append("\n");
        sb.append("Operando A: 0x").append(String.format("%08X", alu.getOperandoA()))
                .append("  (").append(alu.getOperandoA()).append(")\n");
        sb.append("Operando B: 0x").append(String.format("%08X", alu.getOperandoB()))
                .append("  (").append(alu.getOperandoB()).append(")\n");
        sb.append("Resultado : 0x").append(String.format("%08X", alu.getResultado()))
                .append("  (").append(alu.getResultado()).append(")\n");
        sb.append("Bandera Z : ").append(alu.isZero() ? 1 : 0).append("\n\n");
        sb.append("HI: 0x").append(String.format("%08X", alu.getHi())).append("\n");
        sb.append("LO: 0x").append(String.format("%08X", alu.getLo())).append("\n");
        areaALU.setText(sb.toString());
    }

    private void actualizarMemoria(DefaultTableModel modelo, Object memoria, int base, int tamano) {
        for (int i = 0; i < tamano; i++) {
            int v;
            if (memoria instanceof ROM) {
                v = ((ROM) memoria).getCelda(i);
            } else {
                v = ((DiscoDuro) memoria).getCelda(i);
            }
            modelo.setValueAt(String.format("0x%08X", v), i, 2);
            modelo.setValueAt(v, i, 3);
        }
    }

    private void registrarEnLog() {
        areaLog.append("[" + cpu.getEstado() + " | " + cpu.getEtapaNombre() + "] "
                + cpu.getMensaje() + "\n");
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------
    /**
     * Limpia los resaltados de la última ejecución (registros y RAM) y
     * sincroniza los valores previos, para que al reiniciar o cargar un
     * programa la vista deje de marcar cambios de la corrida anterior.
     * No modifica el estado del procesador.
     */
    private void limpiarResaltados() {
        regsModificados.clear();
        celdasModificadas.clear();
        celdaConsultada = -1;
        for (int i = 0; i < Registros.CANTIDAD; i++) {
            regPrev[i] = cpu.getRegistros().read(i);
        }
        for (int i = 0; i < RAM.TAMANO; i++) {
            ramPrev[i] = cpu.getRAM().getCelda(i);
        }
    }

    private static int indiceRAM(int direccion) {
        if (direccion < RAM.BASE || direccion >= RAM.BASE + RAM.TAMANO_BYTES || (direccion & 0x3) != 0) {
            return -1;
        }
        return (direccion - RAM.BASE) >>> 2;
    }

    private static String direccionRAM(int indicePalabra) {
        return String.format("0x%08X", RAM.BASE + indicePalabra * 4);
    }

    private static String direccionHex(int base, int indice) {
        return String.format("0x%08X", base + indice * 4);
    }

    private static String binario(int valor) {
        return String.format("%32s", Integer.toBinaryString(valor)).replace(' ', '0');
    }

    private static Color colorEstado(String estado) {
        switch (estado) {
            case "ERROR":
                return COLOR_TERRACOTA_OSCURO;
            case "FINALIZADO":
                return COLOR_SAGE_OSCURO;
            case "EJECUTANDO":
                return COLOR_PLUM;
            case "LISTO":
                return COLOR_TITULO_INFERIOR;
            default:
                return COLOR_TEXTO_SEC;
        }
    }

    private static DefaultTableModel modeloNoEditable(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    // ------------------------------------------------------------------
    // Modelos y renderers
    // ------------------------------------------------------------------
    private class RegistroRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccion,
                boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(tabla, valor, seleccion, foco, fila, columna);
            if (seleccion) {
                c.setBackground(tabla.getSelectionBackground());
                c.setForeground(tabla.getSelectionForeground());
                return c;
            }
            if (regsModificados.contains(fila)) {
                c.setBackground(COLOR_REG_MOD);
            } else if (fila == 0) {
                c.setBackground(COLOR_FILA_CERO);
            } else {
                c.setBackground(fila % 2 == 0 ? COLOR_PANEL : COLOR_PANEL_ALT);
            }
            c.setForeground(COLOR_TEXTO);
            return c;
        }
    }

    private class RAMRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccion,
                boolean foco, int fila, int columna) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(tabla, valor, seleccion, foco, fila, columna);
            c.setHorizontalAlignment(SwingConstants.LEFT);
            c.setForeground(COLOR_TEXTO);
            int indice = fila;
            if (celdasModificadas.contains(indice)) {
                c.setBackground(COLOR_RAM_MOD);
            } else if (indice == celdaConsultada) {
                c.setBackground(COLOR_RAM_CONSULTA);
            } else {
                c.setBackground(indice % 2 == 0 ? COLOR_PANEL : COLOR_PANEL_ALT);
            }
            c.setToolTipText("Palabra " + indice + "  ->  " + direccionRAM(indice));
            return c;
        }
    }

    private static class RAMTableModel extends AbstractTableModel {
        private final String[] contenido = new String[RAM.TAMANO];

        RAMTableModel() {
            for (int i = 0; i < RAM.TAMANO; i++) {
                contenido[i] = "0x00000000";
            }
        }

        @Override
        public int getRowCount() {
            return RAM.TAMANO;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int columna) {
            return columna == 0 ? "Dirección" : "Contenido";
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            return columna == 0 ? direccionRAM(fila) : contenido[fila];
        }

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }

        void setValor(int fila, String valor) {
            contenido[fila] = valor;
            fireTableCellUpdated(fila, 1);
        }
    }
}
