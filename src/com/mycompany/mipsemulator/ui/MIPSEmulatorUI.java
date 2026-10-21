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
import javax.swing.UIManager;
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
 * <p>Apariencia: tema <b>"Cyber Neon Purple"</b> — fondo púrpura con
 * profundidad (degradados sutiles), superficies moradas diferenciadas,
 * bordes violetas con resplandor sutil, títulos en lavanda y violeta claro,
 * detalles cian para los indicadores técnicos y tipografía monoespaciada
 * en los títulos y en los datos.</p>
 */
public class MIPSEmulatorUI extends JFrame {

    private static final int LIMITE_EJECUCION_TOTAL = 100000;
    private static final int ALTO_FILA_REGISTRO = 18;
    private static final int ALTO_FILA_RAM = 20;

    // --------------------------------------------------------------
    // Tema "Cyber Neon Purple": fondo púrpura con profundidad + acentos neón
    // --------------------------------------------------------------
    private static final Color COLOR_FONDO_ARRIBA = new Color(0x1D1038);  // púrpura profundo (arriba)
    private static final Color COLOR_FONDO_ABAJO = new Color(0x170D2B);   // púrpura oscuro (abajo)
    private static final Color COLOR_PANEL = new Color(0x21113C);         // superficie de tarjetas
    private static final Color COLOR_PANEL_ALT = new Color(0x271344);     // superficie alterna (zebra)
    private static final Color COLOR_BORDE = new Color(0x7629D9);         // marcos violeta finos
    private static final Color COLOR_BORDE_RESPLANDO = new Color(0x8B36F2); // resplandor violeta sutil
    private static final Color COLOR_PESTANIA_ACTIVA = new Color(0x4A2490); // pestaña seleccionada
    private static final Color COLOR_REJA = new Color(0x3A2360);          // rejas de tabla (discretas)
    private static final Color COLOR_TEXTO = new Color(0xF0E5FF);         // texto principal (lavanda)
    private static final Color COLOR_TEXTO_SEC = new Color(0xCDB6F5);     // texto secundario (más legible)

    // Acentos y estados
    private static final Color COLOR_MOSTAZA = new Color(0xFFC857);       // neón ámbar
    private static final Color COLOR_SAGE_OSCURO = new Color(0x249B79);   // borde verde neón
    private static final Color COLOR_CREMA_TEXTO = new Color(0xFFFFFF);   // texto claro sobre los botones
    private static final Color COLOR_TINTA_OSCURA = new Color(0x241333);  // texto oscuro sobre ámbar

    // Botones (misma familia violeta/magenta, con jerarquía visual)
    private static final Color BOTON_BASE = new Color(0xA44FE5);          // violeta neón (base)
    private static final Color BOTON_BASE_OSCURO = new Color(0x7131B0);
    private static final Color BOTON_ROSA = new Color(0xDF36D4);          // rosa neón (principal)
    private static final Color BOTON_ROSA_OSCURO = new Color(0xA923A1);
    private static final Color BOTON_AZUL = new Color(0x713BD1);          // azul neón
    private static final Color BOTON_AZUL_OSCURO = new Color(0x49238F);
    private static final Color BOTON_VERDE = new Color(0x813DE0);         // verde neón
    private static final Color BOTON_VERDE_OSCURO = new Color(0x5724AB);
    private static final Color BOTON_AMARILLO = new Color(0x513988);      // amarillo neón
    private static final Color BOTON_AMARILLO_OSCURO = new Color(0x39255F);

    // Títulos de sección (lavanda y violeta muy brillantes; cian en lo técnico)
    private static final Color COLOR_TITULO_REGISTROS = new Color(0xE0D0FF);   // lavanda claro brillante
    private static final Color COLOR_TITULO_RAM = new Color(0xF2B8FF);         // violeta claro
    private static final Color COLOR_TITULO_PESTANIAS = new Color(0xC3A8FF);   // violeta medio claro
    private static final Color COLOR_TITULO_INFERIOR = new Color(0x9CEBFF);    // cian brillante (indicadores)

    // Etapas y badges de estado
    private static final Color COLOR_ACTIVO = new Color(0xFF4FDE);         // etapa activa (rosa neón brillante)
    private static final Color COLOR_ACTIVO_BORDE = new Color(0xFFA8F4);
    private static final Color COLOR_ACTIVO_TEXTO = new Color(0x2E0530);   // texto oscuro sobre rosa neón
    private static final Color COLOR_INACTIVO = new Color(0x140930);       // etapa inactiva (fondo bien oscuro)
    private static final Color COLOR_INACTIVO_TEXTO = new Color(0xDCCEFF); // texto lavanda muy claro
    private static final Color COLOR_FIN = new Color(0x3DFF88);            // badge FIN (verde neón)
    private static final Color COLOR_FIN_TEXTO = new Color(0x05220F);
    private static final Color COLOR_ERROR = new Color(0xFF3B5C);          // badge ERROR (rojo neón)
    private static final Color COLOR_ERROR_TEXTO = new Color(0xFFF0F3);
    private static final Color COLOR_LISTO_TEXTO = new Color(0x8FE3FF);    // LISTO en cian discreto

    // Resaltados de datos (dos tonos en la RAM; se conservan)
    private static final Color COLOR_REG_MOD = new Color(0x5A4A12);        // registro modificado (ámbar tenue)
    private static final Color COLOR_RAM_MOD = new Color(0x59291A);        // RAM modificada (naranja tenue)
    private static final Color COLOR_RAM_CONSULTA = new Color(0x10425A);   // RAM consultada (cian tenue)
    private static final Color COLOR_FILA_CERO = new Color(0x2C1A52);      // fila de $zero

    // Encabezados de tabla
    private static final Color COLOR_ENCABEZADO = new Color(0x33195C);    // azul-violeta oscuro
    private static final Color COLOR_ENCABEZADO_TEXTO = new Color(0x8FE3FF); // cian claro

    // --------------------------------------------------------------
    // Tipografías (con respaldo si la fuente no está instalada)
    // --------------------------------------------------------------
    private static final Font FUENTE_TITULO = fuente(
            new String[]{"Cascadia Mono", "Consolas", "Lucida Console", "Monospaced"}, Font.BOLD, 13);
    private static final Font FUENTE_TITULO_GRANDE = fuente(
            new String[]{"Cascadia Mono", "Consolas", "Lucida Console", "Monospaced"}, Font.BOLD, 24);
    private static final Font FUENTE_UI = fuente(
            new String[]{"Segoe UI", "Trebuchet MS", "Verdana", "Tahoma"}, Font.PLAIN, 12);
    private static final Font FUENTE_UI_NEGRITA = fuente(
            new String[]{"Segoe UI", "Trebuchet MS", "Verdana", "Tahoma"}, Font.BOLD, 12);
    private static final Font FUENTE_DATOS = fuente(
            new String[]{"Consolas", "Cascadia Mono", "Lucida Console", "Monospaced"}, Font.PLAIN, 12);
    private static final Font FUENTE_DATOS_NEGRITA = fuente(
            new String[]{"Consolas", "Cascadia Mono", "Lucida Console", "Monospaced"}, Font.BOLD, 12);

    // --------------------------------------------------------------
    // Defaults de Swing: también oscuros (scrollbars, pestañas,
    // divisores, etiquetas y diálogos creados por el propio Swing)
    // --------------------------------------------------------------
    static {
        Color texto = COLOR_TEXTO;
        UIManager.put("Panel.background", COLOR_PANEL);
        UIManager.put("Viewport.background", COLOR_PANEL);
        UIManager.put("Label.foreground", texto);
        UIManager.put("Label.disabledForeground", COLOR_INACTIVO_TEXTO);
        UIManager.put("control", COLOR_PANEL);
        UIManager.put("info", COLOR_PANEL);
        UIManager.put("text", texto);
        UIManager.put("controlText", texto);
        UIManager.put("infoText", texto);
        UIManager.put("controlTextColor", texto);
        UIManager.put("inactiveControlTextColor", COLOR_INACTIVO_TEXTO);
        UIManager.put("controlShadow", COLOR_BORDE);
        UIManager.put("controlDarkShadow", COLOR_FONDO_ARRIBA);
        UIManager.put("controlHighlight", COLOR_PANEL_ALT);
        UIManager.put("primaryControl", COLOR_ACTIVO_BORDE);
        UIManager.put("primaryControlShadow", COLOR_ACTIVO);
        UIManager.put("primaryControlDarkShadow", COLOR_ACTIVO_BORDE);
        UIManager.put("Button.background", COLOR_PANEL);
        UIManager.put("Button.foreground", texto);
        UIManager.put("Button.disabledText", COLOR_INACTIVO_TEXTO);
        UIManager.put("Table.background", COLOR_PANEL);
        UIManager.put("Table.foreground", texto);
        UIManager.put("Table.selectionBackground", COLOR_MOSTAZA);
        UIManager.put("Table.selectionForeground", COLOR_TINTA_OSCURA);
        UIManager.put("Table.gridColor", COLOR_REJA);
        UIManager.put("TableHeader.background", COLOR_ENCABEZADO);
        UIManager.put("TableHeader.foreground", COLOR_ENCABEZADO_TEXTO);
        UIManager.put("ScrollPane.background", COLOR_PANEL);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumb", COLOR_BORDE);
        UIManager.put("ScrollBar.thumbHighlight", COLOR_ACTIVO_BORDE);
        UIManager.put("ScrollBar.thumbDarkShadow", COLOR_FONDO_ARRIBA);
        UIManager.put("ScrollBar.shadow", COLOR_PANEL_ALT);
        UIManager.put("ScrollBar.highlight", COLOR_PANEL_ALT);
        UIManager.put("ScrollBar.track", COLOR_PANEL_ALT);
        UIManager.put("TabbedPane.background", COLOR_PANEL);
        UIManager.put("TabbedPane.foreground", COLOR_TEXTO_SEC);
        UIManager.put("TabbedPane.selected", COLOR_PESTANIA_ACTIVA);
        UIManager.put("TabbedPane.selectHighlight", COLOR_BORDE_RESPLANDO);
        UIManager.put("TabbedPane.borderHightlightColor", COLOR_BORDE_RESPLANDO);
        UIManager.put("SplitPane.background", COLOR_PANEL_ALT);
        UIManager.put("SplitPaneDivider.background", COLOR_BORDE);
        UIManager.put("SplitPaneDivider.border", BorderFactory.createEmptyBorder());
        UIManager.put("SplitPaneDivider.focusSize", 0);
        UIManager.put("TextField.background", COLOR_PANEL_ALT);
        UIManager.put("TextField.foreground", texto);
        UIManager.put("TextField.caretForeground", COLOR_TITULO_REGISTROS);
        UIManager.put("OptionPane.background", COLOR_PANEL);
        UIManager.put("OptionPane.messageForeground", texto);
        UIManager.put("FileChooser.background", COLOR_PANEL);
        UIManager.put("FileChooser.foreground", texto);
    }

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

    private final NeonButton btnCargar = new NeonButton("Cargar .bin");
    private final NeonButton btnEjecutar = new NeonButton("Ejecutar instrucción");
    private final NeonButton btnEtapa = new NeonButton("Siguiente etapa");
    private final NeonButton btnTodo = new NeonButton("Ejecutar hasta el final");
    private final NeonButton btnReiniciar = new NeonButton("Reiniciar");

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
        JPanel banner = new PanelDegradado(new Color(0x2B1452), COLOR_FONDO_ARRIBA);
        banner.setLayout(new BorderLayout(12, 0));
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, COLOR_BORDE_RESPLANDO),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JLabel titulo = new TextoConResplandor(">_ Emulador MIPS32",
                COLOR_TITULO_REGISTROS, COLOR_BORDE_RESPLANDO);
        titulo.setFont(FUENTE_TITULO_GRANDE);
        titulo.setForeground(COLOR_TITULO_REGISTROS);

        banner.add(titulo, BorderLayout.WEST);
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
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        panel.add(crearPanelSuperior(), BorderLayout.NORTH);
        panel.add(principal, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelSuperior() {
        JPanel panel = panelTransparente(new BorderLayout(8, 6));

        estilizarBoton(btnCargar, BOTON_BASE, COLOR_CREMA_TEXTO, BOTON_BASE_OSCURO);
        estilizarBoton(btnEjecutar, BOTON_ROSA, COLOR_CREMA_TEXTO, BOTON_ROSA_OSCURO);
        estilizarBoton(btnEtapa, BOTON_AZUL, COLOR_CREMA_TEXTO, BOTON_AZUL_OSCURO);
        estilizarBoton(btnTodo, BOTON_VERDE, COLOR_CREMA_TEXTO, BOTON_VERDE_OSCURO);
        estilizarBoton(btnReiniciar, BOTON_AMARILLO, COLOR_CREMA_TEXTO, BOTON_AMARILLO_OSCURO);

        JPanel fila1 = panelTransparente(new FlowLayout(FlowLayout.LEFT, 10, 6));
        fila1.add(btnCargar);
        fila1.add(etiqueta("Archivo:"));
        lblArchivo.setFont(FUENTE_DATOS_NEGRITA);
        fila1.add(lblArchivo);
        fila1.add(Box.createHorizontalStrut(24));
        fila1.add(etiqueta("Estado:"));
        lblEstado.setFont(FUENTE_DATOS_NEGRITA);
        fila1.add(lblEstado);

        JPanel fila2 = panelTransparente(new FlowLayout(FlowLayout.LEFT, 6, 4));
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
        tabs.setForeground(COLOR_TEXTO_SEC);
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
                BorderFactory.createMatteBorder(3, 0, 0, 0, COLOR_BORDE_RESPLANDO),
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
    // Estilos del tema "Cyber Neon"
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

    /**
     * Etiqueta descriptiva con la misma tipografía (y por lo tanto la misma
     * altura de línea) que los valores en monoespaciada, de modo que texto y
     * número quedan a la par en la misma fila.
     */
    private static JLabel etiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(FUENTE_DATOS);
        etiqueta.setForeground(COLOR_TEXTO_SEC);
        return etiqueta;
    }

    private static TitledBorder bordeTitulo(String texto, Color color) {
        TitledBorder borde = BorderFactory.createTitledBorder(
                new BordeNeon(COLOR_BORDE, COLOR_BORDE_RESPLANDO), texto);
        borde.setTitleFont(FUENTE_TITULO);
        borde.setTitleColor(color);
        return borde;
    }

    /**
     * Borde de tarjeta: línea violeta fina con un resplandor interior muy
     * sutil (trazos translúcidos por dentro del marco). No modifica el tamaño
     * ni la disposición: solo pinta alrededor del panel.
     */
    private static final class BordeNeon extends javax.swing.border.AbstractBorder {
        private final Color base;
        private final Color resplandor;

        BordeNeon(Color base, Color resplandor) {
            this.base = base;
            this.resplandor = resplandor;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int radio = 10;
            // Resplandor interior: se desvanece hacia dentro del panel.
            for (int i = 3; i >= 1; i--) {
                int alpha = 46 - i * 12;
                g2.setColor(new Color(resplandor.getRed(), resplandor.getGreen(),
                        resplandor.getBlue(), alpha));
                g2.setStroke(new java.awt.BasicStroke(2.2f));
                g2.drawRoundRect(x + i, y + i, w - 1 - 2 * i, h - 1 - 2 * i,
                        Math.max(2, radio - i), Math.max(2, radio - i));
            }
            // Marco principal: línea violeta fina.
            g2.setStroke(new java.awt.BasicStroke(1f));
            g2.setColor(base);
            g2.drawRoundRect(x, y, w - 1, h - 1, radio, radio);
            g2.dispose();
        }

        @Override
        public java.awt.Insets getBorderInsets(Component c) {
            return new java.awt.Insets(2, 2, 2, 2);
        }

        @Override
        public java.awt.Insets getBorderInsets(Component c, java.awt.Insets insets) {
            insets.set(2, 2, 2, 2);
            return insets;
        }
    }

    /**
     * Título con un ligero resplandor violeta: dibuja el texto dos veces más
     * en los laterales con transparencia y encima el texto sólido.
     */
    private static final class TextoConResplandor extends JLabel {
        private final Color brillo;

        TextoConResplandor(String texto, Color color, Color brillo) {
            super(texto);
            setForeground(color);
            this.brillo = brillo;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            java.awt.FontMetrics fm = g2.getFontMetrics();
            int x = getInsets().left;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            for (int i = 2; i >= 1; i--) {
                g2.setColor(new Color(brillo.getRed(), brillo.getGreen(),
                        brillo.getBlue(), 55 - i * 20));
                g2.drawString(getText(), x - i, y);
                g2.drawString(getText(), x + i, y);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Borde de tarjeta: marco + título de sección + relleno interior. */
    private static javax.swing.border.Border bordeTarjeta(String texto, Color color) {
        return BorderFactory.createCompoundBorder(
                bordeTitulo(texto, color),
                BorderFactory.createEmptyBorder(2, 6, 6, 6));
    }

    private void estilizarBoton(JButton boton, Color fondo, Color texto, Color borde) {
        boton.setForeground(texto);
        boton.setFont(FUENTE_UI_NEGRITA);
        boton.setFocusPainted(false);
        boton.setFocusable(false);
        if (boton instanceof NeonButton neon) {
            // El cuerpo se dibuja con borde brillante (resplandor violeta);
            // la variante oscura de cada botón se reserva para el pulsado.
            Color claro = boton == btnReiniciar
                    ? mezclar(fondo, BOTON_ROSA, 0.45)
                    : aclarar(fondo, 0.24);
            neon.configurarColores(fondo, claro, texto, aclarar(fondo, 0.32));
            neon.setBaseOscura(borde);
            // Jerarquía: halo permanente en las dos acciones clave y más
            // notorio en el botón principal (Ejecutar instrucción).
            neon.setHaloPermanente(boton == btnEjecutar || boton == btnCargar);
            neon.setHaloFuerte(boton == btnEjecutar);
        } else {
            boton.setBackground(fondo);
            boton.setOpaque(true);
            boton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borde),
                    BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        }
        boton.addPropertyChangeListener("enabled", evt -> {
            boolean habilitado = boton.isEnabled();
            boton.setForeground(habilitado ? texto : COLOR_INACTIVO_TEXTO);
            boton.repaint();
        });
    }

    /** Botón Swing dibujado a mano: degradado, esquinas redondeadas y halo neón. */
    private static final class NeonButton extends JButton {
        private Color colorBase = BOTON_BASE;
        private Color colorClaro = aclarar(BOTON_BASE, 0.24);
        private Color colorOscuro = BOTON_BASE_OSCURO;
        private Color colorTexto = COLOR_TEXTO;
        private Color colorBorde = COLOR_BORDE;
        private boolean hover;
        private boolean haloPermanente;
        private boolean haloFuerte;

        NeonButton(String texto) {
            super(texto);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            // El tamaño lo controla getPreferredSize(): sin depender del borde
            // ni del margin del Look and Feel, el texto queda siempre con aire
            // y dentro del cuerpo redondeado.
            setBorder(BorderFactory.createEmptyBorder());
            setMargin(new java.awt.Insets(0, 0, 0, 0));
            setForeground(colorTexto);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
            // Repinta al pulsar/soltar para mostrar el estado "pulsado".
            getModel().addChangeListener(evt -> repaint());
        }

        /** Tamaño = texto + margen interno fijo (30 px de ancho, 18 de alto). */
        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.width += 30;
            d.height += 18;
            return d;
        }

        void configurarColores(Color base, Color claro, Color texto, Color borde) {
            colorBase = base;
            colorClaro = claro;
            colorTexto = texto;
            colorBorde = borde;
            setForeground(texto);
            repaint();
        }

        void setBaseOscura(Color valor) {
            colorOscuro = valor;
            repaint();
        }

        void setHaloPermanente(boolean valor) {
            haloPermanente = valor;
            repaint();
        }

        void setHaloFuerte(boolean valor) {
            haloFuerte = valor;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            boolean activo = isEnabled();
            boolean pulsado = activo && getModel().isArmed();
            boolean brillo = activo && (hover || haloPermanente);
            int inset = 2;

            // 1) Cuerpo del botón: degradado con esquinas redondeadas.
            Color arriba = activo
                    ? (pulsado ? colorBase : (hover ? colorClaro : aclarar(colorBase, 0.08)))
                    : COLOR_PANEL;
            Color abajo = activo ? (pulsado ? colorOscuro : colorBase) : COLOR_PANEL_ALT;
            g2.setPaint(new GradientPaint(0, inset, arriba, 0, h - inset, abajo));
            g2.fillRoundRect(inset, inset, w - inset * 2, h - inset * 2, 13, 13);

            // 2) Halo neón: anillos translúcidos alrededor del cuerpo. Se dibujan
            //    DESPUÉS del degradado para que el resplandor realmente se vea.
            if (brillo) {
                int alphaBase = haloFuerte ? 55 : 38;
                int alpha0 = hover ? alphaBase + 35 : alphaBase;
                for (int i = 3; i >= 0; i--) {
                    int alpha = alpha0 - i * 9;
                    if (alpha <= 0) {
                        continue;
                    }
                    g2.setColor(new Color(colorClaro.getRed(), colorClaro.getGreen(), colorClaro.getBlue(), alpha));
                    g2.setStroke(new java.awt.BasicStroke(1.6f + i));
                    int margen = i;
                    g2.drawRoundRect(inset - margen, inset - margen,
                            w - (inset - margen) * 2 - 1, h - (inset - margen) * 2 - 1,
                            15 + 2 * margen, 15 + 2 * margen);
                }
            }

            // 3) Borde del cuerpo.
            g2.setStroke(new java.awt.BasicStroke(1.2f));
            g2.setColor(activo ? (hover ? colorClaro : colorBorde) : COLOR_REJA);
            g2.drawRoundRect(inset, inset, w - inset * 2 - 1, h - inset * 2 - 1, 13, 13);
            g2.dispose();

            setForeground(activo ? colorTexto : COLOR_INACTIVO_TEXTO);
            super.paintComponent(g);
        }
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

    /** Mezcla dos colores (factor 0..1 = peso del segundo color). */
    private static Color mezclar(Color a, Color b, double factor) {
        int r = (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * factor);
        int g = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * factor);
        int b2 = (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * factor);
        return new Color(Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b2)));
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
            labelsEtapa[i].setBorder(BorderFactory.createLineBorder(activa ? COLOR_ACTIVO_BORDE : COLOR_BORDE));
        }
        if (terminado) {
            boolean esError = "ERROR".equals(estado);
            lblBadgeFin.setText(esError ? "ERROR" : "FIN");
            lblBadgeFin.setBackground(esError ? COLOR_ERROR : COLOR_FIN);
            lblBadgeFin.setForeground(esError ? COLOR_ERROR_TEXTO : COLOR_FIN_TEXTO);
            lblBadgeFin.setBorder(BorderFactory.createLineBorder(
                    esError ? new Color(0xB32842) : new Color(0x19B264)));
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

    private static String direccionRAM(int indicePalabra) {
        return String.format("0x%08X", RAM.BASE + indicePalabra * 4);
    }

    /** Índice de palabra de RAM para una dirección, o -1 si no es válida. */
    private static int indiceRAM(int direccion) {
        if (direccion < RAM.BASE || direccion >= RAM.BASE + RAM.TAMANO_BYTES || (direccion & 0x3) != 0) {
            return -1;
        }
        return (direccion - RAM.BASE) >>> 2;
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
                return COLOR_ERROR;
            case "FINALIZADO":
                return COLOR_FIN;
            case "EJECUTANDO":
                return COLOR_ACTIVO;
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
