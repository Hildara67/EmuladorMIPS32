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
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Interfaz Swing del emulador.
 * Solo muestra el estado y solicita acciones a {@link CPU}; no decodifica ni
 * ejecuta instrucciones por sí misma. La memoria de registros y la RAM
 * permanecen visibles de forma simultánea; el resto va en pestañas secundarias.
 */
public class MIPSEmulatorUI extends JFrame {

    private static final int LIMITE_EJECUCION_TOTAL = 100000;
    private static final int ALTO_FILA_REGISTRO = 18;
    private static final int ALTO_FILA_RAM = 20;

    // --------------------------------------------------------------
    // Paleta "Miel, Latte & Lavanda" (diseñada para verse bien incluso
    // con luz ambiental / proyector: fondos pastel + texto oscuro).
    // --------------------------------------------------------------
    // Base
    private static final Color COLOR_FONDO = new Color(0xFBF7F0);      // fondo general cálido
    private static final Color COLOR_PANEL = new Color(0xFFFFFF);      // tablas y áreas de texto
    private static final Color COLOR_BORDE = new Color(0xE8DFD3);      // bordes y rejas latte
    private static final Color COLOR_TEXTO = new Color(0x3A2E27);      // texto principal (café)
    private static final Color COLOR_TEXTO_SEC = new Color(0x7A6A5F);  // texto secundario
    private static final Color COLOR_TITULO = new Color(0x5C4A3F);     // títulos de panel

    // Etapas y badges de estado
    private static final Color COLOR_ACTIVO = new Color(0xD8CCF7);        // etapa activa (lavanda)
    private static final Color COLOR_ACTIVO_TEXTO = new Color(0x4A3173);  // texto lavanda oscuro
    private static final Color COLOR_INACTIVO = new Color(0xF1EBE2);      // etapa inactiva (beige)
    private static final Color COLOR_INACTIVO_TEXTO = new Color(0x8A7C6E);
    private static final Color COLOR_FIN = new Color(0xCDEFD6);           // badge FIN (menta)
    private static final Color COLOR_FIN_TEXTO = new Color(0x1E6B34);
    private static final Color COLOR_ERROR = new Color(0xFFC9D6);         // badge ERROR (rosa)
    private static final Color COLOR_ERROR_TEXTO = new Color(0xA32C4A);
    private static final Color COLOR_LISTO_TEXTO = new Color(0x6B5400);   // estado LISTO (miel)

    // Resaltados de datos
    private static final Color COLOR_REG_MOD = new Color(0xFFF3B0);       // registro modificado (amarillo)
    private static final Color COLOR_RAM_MOD = new Color(0xF5DCC0);       // RAM modificada (durazno)
    private static final Color COLOR_RAM_CONSULTA = new Color(0xDCCCFA);  // RAM consultada (lavanda)
    private static final Color COLOR_FILA_CERO = new Color(0xF3EEE6);     // fila de $zero

    // Controles
    private static final Color COLOR_BOTON = new Color(0xE4DAFB);         // botones (lavanda)
    private static final Color COLOR_BOTON_TEXTO = new Color(0x3D2B63);
    private static final Color COLOR_BOTON_BORDE = new Color(0xB39DDB);

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
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(COLOR_FONDO);

        modeloRegistros = modeloNoEditable("Nombre", "Índice", "Hexadecimal", "Decimal");
        modeloInstrucciones = modeloNoEditable("Índice", "Dirección", "Binario (32 bits)", "Mnemónico");
        modeloROM = modeloNoEditable("Índice", "Dirección", "Hexadecimal", "Decimal");
        modeloDisco = modeloNoEditable("Índice", "Dirección", "Hexadecimal", "Decimal");
        modeloRAM = new RAMTableModel();

        add(crearPanelSuperior(), BorderLayout.NORTH);
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
    private JPanel crearPanelSuperior() {
        JPanel panel = nuevoPanel(new BorderLayout(8, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        estilizarBoton(btnCargar);
        estilizarBoton(btnEjecutar);
        estilizarBoton(btnEtapa);
        estilizarBoton(btnTodo);
        estilizarBoton(btnReiniciar);

        JPanel fila1 = nuevoPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        fila1.add(btnCargar);
        fila1.add(new JLabel("Archivo:"));
        fila1.add(lblArchivo);
        fila1.add(Box.createHorizontalStrut(24));
        fila1.add(new JLabel("Estado:"));
        lblEstado.setFont(new Font("Monospaced", Font.BOLD, 13));
        fila1.add(lblEstado);

        JPanel fila2 = nuevoPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        fila2.add(btnEjecutar);
        fila2.add(btnEtapa);
        fila2.add(btnTodo);
        fila2.add(btnReiniciar);

        JPanel filas = nuevoPanel();
        filas.setLayout(new BoxLayout(filas, BoxLayout.Y_AXIS));
        fila1.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila2.setAlignmentX(Component.LEFT_ALIGNMENT);
        filas.add(fila1);
        filas.add(fila2);

        panel.add(filas, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelCentral() {
        JSplitPane derecha = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                crearPanelRAM(), crearPanelPestanias());
        derecha.setResizeWeight(0.45);
        derecha.setContinuousLayout(true);
        derecha.setDividerLocation(300);
        derecha.setBackground(COLOR_FONDO);

        JSplitPane principal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                crearPanelRegistros(), derecha);
        principal.setResizeWeight(0.0);
        principal.setContinuousLayout(true);
        principal.setDividerLocation(430);
        principal.setBackground(COLOR_FONDO);

        JPanel panel = nuevoPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        panel.add(principal, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelRegistros() {
        JPanel panel = nuevoPanel(new BorderLayout(0, 4));
        panel.setBorder(bordeTitulo("Memoria de registros (32 registros)"));

        tablaRegistros.setModel(modeloRegistros);
        estilizarTabla(tablaRegistros);
        tablaRegistros.setRowHeight(ALTO_FILA_REGISTRO);
        tablaRegistros.setFont(new Font("Monospaced", Font.PLAIN, 11));
        tablaRegistros.setDefaultRenderer(Object.class, new RegistroRenderer());
        JScrollPane scroll = estilizarScroll(new JScrollPane(tablaRegistros));
        scroll.getVerticalScrollBar().setUnitIncrement(ALTO_FILA_REGISTRO);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel especiales = nuevoPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        especiales.setBorder(bordeTitulo("Registros especiales"));
        Font mono = new Font("Monospaced", Font.BOLD, 13);
        lblHI.setFont(mono);
        lblLO.setFont(mono);
        especiales.add(new JLabel("HI:"));
        especiales.add(lblHI);
        especiales.add(Box.createHorizontalStrut(16));
        especiales.add(new JLabel("LO:"));
        especiales.add(lblLO);
        panel.add(especiales, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearPanelRAM() {
        JPanel panel = nuevoPanel(new BorderLayout(0, 4));
        panel.setBorder(bordeTitulo(
                "RAM")); // (100 palabras de 32 bits; direcciones 0x00000000 - 0x0000018C)

        tablaRAM.setModel(modeloRAM);
        estilizarTabla(tablaRAM);
        tablaRAM.setRowHeight(ALTO_FILA_RAM);
        tablaRAM.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tablaRAM.setDefaultRenderer(Object.class, new RAMRenderer());
        tablaRAM.setRowSelectionAllowed(false);
        tablaRAM.setFillsViewportHeight(false);
        tablaRAM.getTableHeader().setReorderingAllowed(false);
        tablaRAM.getColumnModel().getColumn(0).setPreferredWidth(150);
        tablaRAM.getColumnModel().getColumn(1).setPreferredWidth(150);

        JScrollPane scroll = estilizarScroll(new JScrollPane(tablaRAM));
        scroll.getVerticalScrollBar().setUnitIncrement(ALTO_FILA_RAM);
        panel.add(scroll, BorderLayout.CENTER);

        JLabel leyenda = new JLabel("Durazno = última celda modificada.  "
                + "Lavanda = última celda consultada.");
        leyenda.setFont(new Font("SansSerif", Font.PLAIN, 11));
        leyenda.setForeground(COLOR_TEXTO_SEC);
        panel.add(leyenda, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearPanelPestanias() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(COLOR_FONDO);
        tabs.setForeground(COLOR_TEXTO);

        tabs.addTab("Memoria de instrucciones", estilizarScroll(new JScrollPane(tablaCon(modeloInstrucciones))));
        tabs.addTab("ROM", estilizarScroll(new JScrollPane(tablaCon(modeloROM))));
        tabs.addTab("Disco duro", estilizarScroll(new JScrollPane(tablaCon(modeloDisco))));

        areaALU.setEditable(false);
        areaALU.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaALU.setBackground(COLOR_PANEL);
        areaALU.setForeground(COLOR_TEXTO);
        areaALU.setCaretColor(COLOR_TEXTO);
        tabs.addTab("ALU / HI-LO", estilizarScroll(new JScrollPane(areaALU)));

        areaLog.setEditable(false);
        areaLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaLog.setBackground(COLOR_PANEL);
        areaLog.setForeground(COLOR_TEXTO);
        areaLog.setCaretColor(COLOR_TEXTO);
        tabs.addTab("Log de etapas", estilizarScroll(new JScrollPane(areaLog)));

        JPanel panel = nuevoPanel(new BorderLayout());
        panel.setBorder(bordeTitulo("Vistas secundarias"));
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelInferior() {
        JPanel panel = nuevoPanel(new BorderLayout(6, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        JPanel info = nuevoPanel(new GridLayout(3, 1, 4, 2));

        JPanel filaPC = nuevoPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        Font mono = new Font("Monospaced", Font.BOLD, 13);
        lblPC.setFont(mono);
        lblInstruccion.setFont(mono);
        lblBinario.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lblContador.setFont(mono);
        lblPrograma.setFont(mono);
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

        JPanel filaEtapas = nuevoPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filaEtapas.add(new JLabel("Etapas:"));
        for (int i = 0; i < labelsEtapa.length; i++) {
            JLabel etapa = new JLabel(UnidadControl.ETAPAS[i], SwingConstants.CENTER);
            etapa.setOpaque(true);
            etapa.setBackground(COLOR_INACTIVO);
            etapa.setPreferredSize(new Dimension(46, 22));
            etapa.setFont(mono);
            labelsEtapa[i] = etapa;
            filaEtapas.add(etapa);
        }
        filaEtapas.add(Box.createHorizontalStrut(10));
        lblBadgeFin.setOpaque(true);
        lblBadgeFin.setHorizontalAlignment(SwingConstants.CENTER);
        lblBadgeFin.setPreferredSize(new Dimension(74, 22));
        lblBadgeFin.setFont(mono);
        lblBadgeFin.setBackground(COLOR_FIN);
        lblBadgeFin.setForeground(COLOR_FIN_TEXTO);
        lblBadgeFin.setVisible(false);
        filaEtapas.add(lblBadgeFin);

        lblUltimaEtapa.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lblUltimaEtapa.setForeground(COLOR_TEXTO_SEC);
        filaEtapas.add(lblUltimaEtapa);

        filaEtapas.add(Box.createHorizontalStrut(16));
        lblALUResumen.setFont(new Font("Monospaced", Font.PLAIN, 12));
        filaEtapas.add(new JLabel("ALU:"));
        filaEtapas.add(lblALUResumen);

        JPanel filaMensaje = nuevoPanel(new BorderLayout());
        lblMensaje.setFont(new Font("Monospaced", Font.PLAIN, 12));
        filaMensaje.add(lblMensaje, BorderLayout.CENTER);

        info.add(filaPC);
        info.add(filaEtapas);
        info.add(filaMensaje);

        panel.add(info, BorderLayout.CENTER);
        return panel;
    }

    // ------------------------------------------------------------------
    // Estilos de la paleta "Miel, Latte & Lavanda"
    // ------------------------------------------------------------------
    private static JPanel nuevoPanel(LayoutManager diseño) {
        JPanel panel = new JPanel(diseño);
        panel.setBackground(COLOR_FONDO);
        return panel;
    }

    private static JPanel nuevoPanel() {
        return nuevoPanel(new FlowLayout());
    }

    private static TitledBorder bordeTitulo(String texto) {
        TitledBorder borde = BorderFactory.createTitledBorder(texto);
        borde.setTitleColor(COLOR_TITULO);
        return borde;
    }

    private static void estilizarBoton(JButton boton) {
        boton.setBackground(COLOR_BOTON);
        boton.setForeground(COLOR_BOTON_TEXTO);
        boton.setOpaque(true);
        boton.setBorder(BorderFactory.createLineBorder(COLOR_BOTON_BORDE));
        boton.setFocusPainted(false);
    }

    private static void estilizarTabla(JTable tabla) {
        tabla.setBackground(COLOR_PANEL);
        tabla.setForeground(COLOR_TEXTO);
        tabla.setGridColor(COLOR_BORDE);
        tabla.setSelectionBackground(COLOR_BOTON);
        tabla.setSelectionForeground(COLOR_BOTON_TEXTO);
        tabla.getTableHeader().setBackground(COLOR_BOTON);
        tabla.getTableHeader().setForeground(COLOR_BOTON_TEXTO);
    }

    private static JScrollPane estilizarScroll(JScrollPane scroll) {
        scroll.setBackground(COLOR_FONDO);
        scroll.getViewport().setBackground(COLOR_PANEL);
        return scroll;
    }

    private static JTable tablaCon(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);
        estilizarTabla(tabla);
        tabla.setRowHeight(20);
        tabla.setFont(new Font("Monospaced", Font.PLAIN, 11));
        return tabla;
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
        }
        if (terminado) {
            boolean esError = "ERROR".equals(estado);
            lblBadgeFin.setText(esError ? "ERROR" : "FIN");
            lblBadgeFin.setBackground(esError ? COLOR_ERROR : COLOR_FIN);
            lblBadgeFin.setForeground(esError ? COLOR_ERROR_TEXTO : COLOR_FIN_TEXTO);
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
                return COLOR_ERROR_TEXTO;
            case "FINALIZADO":
                return COLOR_FIN_TEXTO;
            case "EJECUTANDO":
                return COLOR_ACTIVO_TEXTO;
            case "LISTO":
                return COLOR_LISTO_TEXTO;
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
            if (regsModificados.contains(fila)) {
                c.setBackground(COLOR_REG_MOD);
            } else if (fila == 0) {
                c.setBackground(COLOR_FILA_CERO);
            } else {
                c.setBackground(COLOR_PANEL);
            }
            return c;
        }
    }

    private class RAMRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccion,
                boolean foco, int fila, int columna) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(tabla, valor, seleccion, foco, fila, columna);
            c.setHorizontalAlignment(SwingConstants.LEFT);
            int indice = fila;
            if (celdasModificadas.contains(indice)) {
                c.setBackground(COLOR_RAM_MOD);
            } else if (indice == celdaConsultada) {
                c.setBackground(COLOR_RAM_CONSULTA);
            } else {
                c.setBackground(COLOR_PANEL);
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
