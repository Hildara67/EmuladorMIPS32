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

    private static final Color COLOR_ACTIVO = new Color(0x1E88E5);
    private static final Color COLOR_INACTIVO = new Color(0xE0E0E0);
    private static final Color COLOR_FIN = new Color(0x2E7D32);
    private static final Color COLOR_ERROR = new Color(0xC62828);
    private static final Color COLOR_REG_MOD = new Color(0xDDF5DD);
    private static final Color COLOR_RAM_MOD = new Color(0xFFE0B2);
    private static final Color COLOR_RAM_CONSULTA = new Color(0xBBE1FA);

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
        JPanel panel = new JPanel(new BorderLayout(8, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        fila1.add(btnCargar);
        fila1.add(new JLabel("Archivo:"));
        fila1.add(lblArchivo);
        fila1.add(Box.createHorizontalStrut(24));
        fila1.add(new JLabel("Estado:"));
        lblEstado.setFont(new Font("Monospaced", Font.BOLD, 13));
        fila1.add(lblEstado);

        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        fila2.add(btnEjecutar);
        fila2.add(btnEtapa);
        fila2.add(btnTodo);
        fila2.add(btnReiniciar);

        JPanel filas = new JPanel();
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

        JSplitPane principal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                crearPanelRegistros(), derecha);
        principal.setResizeWeight(0.0);
        principal.setContinuousLayout(true);
        principal.setDividerLocation(430);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        panel.add(principal, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelRegistros() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Memoria de registros (32 registros)"));

        tablaRegistros.setModel(modeloRegistros);
        tablaRegistros.setRowHeight(ALTO_FILA_REGISTRO);
        tablaRegistros.setFont(new Font("Monospaced", Font.PLAIN, 11));
        tablaRegistros.setDefaultRenderer(Object.class, new RegistroRenderer());
        JScrollPane scroll = new JScrollPane(tablaRegistros);
        scroll.getVerticalScrollBar().setUnitIncrement(ALTO_FILA_REGISTRO);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel especiales = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        especiales.setBorder(BorderFactory.createTitledBorder("Registros especiales"));
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
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder(
                "RAM")); // (100 palabras de 32 bits; direcciones 0x00000000 - 0x0000018C)

        tablaRAM.setModel(modeloRAM);
        tablaRAM.setRowHeight(ALTO_FILA_RAM);
        tablaRAM.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tablaRAM.setDefaultRenderer(Object.class, new RAMRenderer());
        tablaRAM.setRowSelectionAllowed(false);
        tablaRAM.setFillsViewportHeight(false);
        tablaRAM.getTableHeader().setReorderingAllowed(false);
        tablaRAM.getColumnModel().getColumn(0).setPreferredWidth(150);
        tablaRAM.getColumnModel().getColumn(1).setPreferredWidth(150);

        JScrollPane scroll = new JScrollPane(tablaRAM);
        scroll.getVerticalScrollBar().setUnitIncrement(ALTO_FILA_RAM);
        panel.add(scroll, BorderLayout.CENTER);

        JLabel leyenda = new JLabel("Naranja = última celda modificada.  "
                + "Azul = última celda consultada.");
        leyenda.setFont(new Font("SansSerif", Font.PLAIN, 11));
        panel.add(leyenda, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearPanelPestanias() {
        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Memoria de instrucciones", new JScrollPane(tablaCon(modeloInstrucciones)));
        tabs.addTab("ROM", new JScrollPane(tablaCon(modeloROM)));
        tabs.addTab("Disco duro", new JScrollPane(tablaCon(modeloDisco)));

        areaALU.setEditable(false);
        areaALU.setFont(new Font("Monospaced", Font.PLAIN, 13));
        tabs.addTab("ALU / HI-LO", new JScrollPane(areaALU));

        areaLog.setEditable(false);
        areaLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tabs.addTab("Log de etapas", new JScrollPane(areaLog));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Vistas secundarias"));
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelInferior() {
        JPanel panel = new JPanel(new BorderLayout(6, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        JPanel info = new JPanel(new GridLayout(3, 1, 4, 2));

        JPanel filaPC = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
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

        JPanel filaEtapas = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
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
        lblBadgeFin.setForeground(Color.WHITE);
        lblBadgeFin.setVisible(false);
        filaEtapas.add(lblBadgeFin);

        lblUltimaEtapa.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lblUltimaEtapa.setForeground(Color.GRAY);
        filaEtapas.add(lblUltimaEtapa);

        filaEtapas.add(Box.createHorizontalStrut(16));
        lblALUResumen.setFont(new Font("Monospaced", Font.PLAIN, 12));
        filaEtapas.add(new JLabel("ALU:"));
        filaEtapas.add(lblALUResumen);

        JPanel filaMensaje = new JPanel(new BorderLayout());
        lblMensaje.setFont(new Font("Monospaced", Font.PLAIN, 12));
        filaMensaje.add(lblMensaje, BorderLayout.CENTER);

        info.add(filaPC);
        info.add(filaEtapas);
        info.add(filaMensaje);

        panel.add(info, BorderLayout.CENTER);
        return panel;
    }

    private static JTable tablaCon(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);
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
            labelsEtapa[i].setForeground(activa ? Color.WHITE : Color.DARK_GRAY);
        }
        if (terminado) {
            boolean esError = "ERROR".equals(estado);
            lblBadgeFin.setText(esError ? "ERROR" : "FIN");
            lblBadgeFin.setBackground(esError ? COLOR_ERROR : COLOR_FIN);
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
                return new Color(0xC62828);
            case "FINALIZADO":
                return new Color(0x2E7D32);
            case "EJECUTANDO":
                return new Color(0x1E88E5);
            case "LISTO":
                return new Color(0x1565C0);
            default:
                return Color.GRAY;
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
                c.setBackground(new Color(0xF0F0F0));
            } else {
                c.setBackground(Color.WHITE);
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
                c.setBackground(Color.WHITE);
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
