package com.mycompany.mipsemulator.execution;

import com.mycompany.mipsemulator.alu.ALU;
import com.mycompany.mipsemulator.alu.ALU.Operacion;
import com.mycompany.mipsemulator.cpu.PC;
import com.mycompany.mipsemulator.cpu.Registros;
import com.mycompany.mipsemulator.instruction.Decodificador;
import com.mycompany.mipsemulator.instruction.Instruccion;
import com.mycompany.mipsemulator.memory.DiscoDuro;
import com.mycompany.mipsemulator.memory.MemoriaInstrucciones;
import com.mycompany.mipsemulator.memory.RAM;
import com.mycompany.mipsemulator.memory.ROM;

/**
 * Unidad de control monociclo. Contiene como métodos las cinco etapas
 * IF, ID, EX, MEM y WB, además del coordinador de una instrucción.
 * Nunca hay dos instrucciones solapadas: una instrucción completa sus
 * cinco etapas antes de iniciar la siguiente.
 *
 * Nota: las cinco etapas son fases lógicas ejecutadas en orden; no son
 * ciclos de reloj independientes ni implican pipeline.
 */
public class UnidadControl {

    public static final String[] ETAPAS = {"IF", "ID", "EX", "MEM", "WB"};

    private final PC pc;
    private final Registros registros;
    private final MemoriaInstrucciones memoriaInstrucciones;
    private final RAM ram;
    private final ROM rom;
    private final DiscoDuro disco;
    private final ALU alu;
    private final Decodificador decodificador;

    // Variables de paso entre etapas (latches internos).
    private Instruccion instruccionActual;
    private int valorRs;
    private int valorRt;
    private int direccionEfectiva;
    private int datoMemoria;
    private int pcSiguiente;

    // Estado de control.
    private int etapaActual;              // 0 = sin instrucción en curso; 1..5 tras cada etapa
    private boolean detenida;
    private boolean error;                // true solo si la detención fue por una anomalía real
    private String mensaje = "";
    private int contadorInstrucciones;
    private int limiteInstrucciones = 100000;

    // Observabilidad para la GUI (solo se expone, no contiene lógica de control).
    private int pcInstruccionActual;      // dirección de la instrucción que se está ejecutando
    private int ultimaDireccionDatos = -1; // última dirección de datos accedida (LW/LB/SW/SB)
    private boolean accesoEscritura;       // true si el último acceso de datos fue escritura

    public UnidadControl(PC pc, Registros registros, MemoriaInstrucciones memoriaInstrucciones,
            RAM ram, ROM rom, DiscoDuro disco, ALU alu, Decodificador decodificador) {
        this.pc = pc;
        this.registros = registros;
        this.memoriaInstrucciones = memoriaInstrucciones;
        this.ram = ram;
        this.rom = rom;
        this.disco = disco;
        this.alu = alu;
        this.decodificador = decodificador;
    }

    public void reiniciar() {
        instruccionActual = null;
        valorRs = 0;
        valorRt = 0;
        direccionEfectiva = 0;
        datoMemoria = 0;
        pcSiguiente = 0;
        etapaActual = 0;
        detenida = false;
        error = false;
        mensaje = "";
        contadorInstrucciones = 0;
        pcInstruccionActual = 0;
        ultimaDireccionDatos = -1;
        accesoEscritura = false;
    }

    // ------------------------------------------------------------------
    // IF — Instruction Fetch
    // ------------------------------------------------------------------
    public void instructionFetch() {
        if (detenida) {
            return;
        }
        int pcActual = pc.get();
        int finPrograma = memoriaInstrucciones.getCantidad() * 4;
        // Fin normal: el PC cayó justo tras la última instrucción.
        if (pcActual == finPrograma) {
            detener("Fin del programa: PC en 0x" + hex(pcActual)
                    + ", inmediatamente después de la última instrucción.", false);
            return;
        }
        // Salto inválido o ejecución fuera del programa.
        if (pcActual < 0 || pcActual > finPrograma || (pcActual & 0x3) != 0) {
            detener("IF: PC fuera del programa: 0x" + hex(pcActual)
                    + " (el programa ocupa " + finPrograma + " bytes).", true);
            return;
        }
        try {
            int palabra = memoriaInstrucciones.leerPalabra(pcActual);
            instruccionActual = decodificador.decodificar(palabra);
            pcSiguiente = pcActual + 4;
            pc.set(pcSiguiente); // avance normal en 4 bytes
            pcInstruccionActual = pcActual;
            mensaje = "IF: 0x" + hex(pcActual) + ": " + formatoInstruccion();
        } catch (IllegalArgumentException e) {
            detener("IF: " + e.getMessage(), true);
        }
    }

    // ------------------------------------------------------------------
    // ID — Instruction Decode
    // ------------------------------------------------------------------
    public void decode() {
        if (detenida) {
            return;
        }
        if (instruccionActual == null) {
            detener("ID: no hay instrucción activa.");
            return;
        }
        if (!instruccionActual.isSoportada()) {
            detener("ID: instrucción no soportada (0x"
                    + Integer.toHexString(instruccionActual.getPalabra()) + ").");
            return;
        }
        valorRs = registros.read(instruccionActual.getRs());
        valorRt = registros.read(instruccionActual.getRt());
        String valores = valoresRegistros();
        mensaje = "ID: " + formatoInstruccion()
                + (valores.isEmpty() ? "" : " | " + valores);
    }

    // ------------------------------------------------------------------
    // EX — Execute
    // ------------------------------------------------------------------
    public void execute() {
        if (detenida) {
            return;
        }
        Instruccion ins = instruccionActual;
        String m = ins.getMnemonico();
        int rs = valorRs;
        int rt = valorRt;
        int imm = ins.getImm();
        int immU = ins.getImmSinSigno();
        boolean errorALU = false;

        try {
            switch (m) {
                case "ADD": alu.ejecutar(Operacion.ADD, rs, rt); break;
                case "SUB": alu.ejecutar(Operacion.SUB, rs, rt); break;
                case "AND": alu.ejecutar(Operacion.AND, rs, rt); break;
                case "OR": alu.ejecutar(Operacion.OR, rs, rt); break;
                case "XOR": alu.ejecutar(Operacion.XOR, rs, rt); break;
                case "NOR": alu.ejecutar(Operacion.NOR, rs, rt); break;
                case "SLT": alu.ejecutar(Operacion.SLT, rs, rt); break;
                case "SLTU": alu.ejecutar(Operacion.SLTU, rs, rt); break;
                case "SLL": alu.ejecutar(Operacion.SLL, rt, ins.getShamt()); break;
                case "SRL": alu.ejecutar(Operacion.SRL, rt, ins.getShamt()); break;
                case "SRA": alu.ejecutar(Operacion.SRA, rt, ins.getShamt()); break;
                case "ADDI": alu.ejecutar(Operacion.ADDI, rs, imm); break;
                case "ANDI": alu.ejecutar(Operacion.ANDI, rs, immU); break;
                case "ORI": alu.ejecutar(Operacion.ORI, rs, immU); break;
                case "SLTI": alu.ejecutar(Operacion.SLTI, rs, imm); break;
                case "LUI": alu.ejecutar(Operacion.LUI, 0, immU); break;
                case "MULT": alu.ejecutar(Operacion.MULT, rs, rt); break;
                case "DIV":
                    try {
                        alu.ejecutar(Operacion.DIV, rs, rt);
                    } catch (ArithmeticException ex) {
                        mensaje = "EX: " + ex.getMessage() + " (resultado indefinido; LO=0, HI=0).";
                        errorALU = true;
                    }
                    break;
                case "MFHI":
                case "MFLO":
                    break; // el valor se toma de HI/LO en WB
                case "LW":
                case "SW":
                case "LB":
                case "SB":
                    direccionEfectiva = rs + imm;
                    break;
                case "BEQ":
                    if (rs == rt) {
                        int objetivo = pcSiguiente + (imm << 2);
                        validarDestino(objetivo, "BEQ");
                        pc.set(objetivo);
                    }
                    break;
                case "BNE":
                    if (rs != rt) {
                        int objetivo = pcSiguiente + (imm << 2);
                        validarDestino(objetivo, "BNE");
                        pc.set(objetivo);
                    }
                    break;
                case "J": {
                    int objetivo = calcularObjetivoSalto();
                    validarDestino(objetivo, "J");
                    pc.set(objetivo);
                    break;
                }
                case "JAL": {
                    int objetivo = calcularObjetivoSalto();
                    validarDestino(objetivo, "JAL");
                    pc.set(objetivo);
                    break;
                }
                case "JR":
                    validarDestino(rs, "JR");
                    pc.set(rs);
                    break;
                case "SYSCALL":
                case "NOP":
                    break;
                default:
                    break;
            }
        } catch (IllegalArgumentException e) {
            detener("EX: " + e.getMessage());
            return;
        }
        if (!errorALU) {
            mensaje = describirEX(m);
        }
    }

    private int calcularObjetivoSalto() {
        int bitsAltos = pcSiguiente & 0xF0000000;
        int destino = (instruccionActual.getDireccion() << 2) & 0x0FFFFFFF;
        return bitsAltos | destino;
    }

    private void validarDestino(int objetivo, String tipo) {
        if ((objetivo & 0x3) != 0) {
            throw new IllegalArgumentException(tipo + ": destino no alineado 0x" + hex(objetivo));
        }
        int finPrograma = memoriaInstrucciones.getCantidad() * 4;
        if (objetivo < 0 || objetivo > finPrograma) {
            throw new IllegalArgumentException(tipo + ": destino fuera del programa 0x" + hex(objetivo));
        }
    }

    // ------------------------------------------------------------------
    // MEM — Memory Access
    // ------------------------------------------------------------------
    public void memoryAccess() {
        if (detenida) {
            return;
        }
        String m = instruccionActual.getMnemonico();
        try {
            switch (m) {
                case "LW":
                    ultimaDireccionDatos = direccionEfectiva;
                    accesoEscritura = false;
                    datoMemoria = leerPalabra(direccionEfectiva);
                    mensaje = "MEM: " + formatoInstruccion() + ": lee 0x"
                            + hex(datoMemoria) + " de 0x" + hex(direccionEfectiva)
                            + ubicacionRAM(direccionEfectiva);
                    break;
                case "LB":
                    ultimaDireccionDatos = direccionEfectiva;
                    accesoEscritura = false;
                    datoMemoria = leerByte(direccionEfectiva);
                    mensaje = "MEM: " + formatoInstruccion() + ": lee 0x"
                            + hex(datoMemoria) + " de 0x" + hex(direccionEfectiva)
                            + ubicacionRAM(direccionEfectiva);
                    break;
                case "SW":
                    ultimaDireccionDatos = direccionEfectiva;
                    accesoEscritura = true;
                    escribirPalabra(direccionEfectiva, valorRt);
                    mensaje = "MEM: " + formatoInstruccion() + ": escribe 0x"
                            + hex(valorRt) + " en 0x" + hex(direccionEfectiva);
                    break;
                case "SB":
                    ultimaDireccionDatos = direccionEfectiva;
                    accesoEscritura = true;
                    escribirByte(direccionEfectiva, valorRt);
                    mensaje = "MEM: " + formatoInstruccion() + ": escribe 0x"
                            + hex(valorRt & 0xFF) + " en 0x" + hex(direccionEfectiva);
                    break;
                default:
                    mensaje = "MEM: sin acceso a memoria.";
                    break;
            }
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            detener("MEM: " + e.getMessage());
        }
    }

    private boolean enRango(int direccion, int base, int tamanoBytes) {
        return direccion >= base && direccion < base + tamanoBytes;
    }

    private int leerPalabra(int direccion) {
        if (enRango(direccion, RAM.BASE, RAM.TAMANO_BYTES)) {
            return ram.leerPalabra(direccion);
        }
        if (enRango(direccion, ROM.BASE, ROM.TAMANO_BYTES)) {
            return rom.leerPalabra(direccion);
        }
        if (enRango(direccion, DiscoDuro.BASE, DiscoDuro.TAMANO_BYTES)) {
            return disco.leerPalabra(direccion);
        }
        throw new IllegalArgumentException("Dirección de datos fuera de rango: 0x" + hex(direccion));
    }

    private int leerByte(int direccion) {
        if (enRango(direccion, RAM.BASE, RAM.TAMANO_BYTES)) {
            return ram.leerByte(direccion);
        }
        if (enRango(direccion, ROM.BASE, ROM.TAMANO_BYTES)) {
            return rom.leerByte(direccion);
        }
        throw new IllegalArgumentException("Dirección de datos fuera de rango: 0x" + hex(direccion));
    }

    private void escribirPalabra(int direccion, int valor) {
        if (enRango(direccion, RAM.BASE, RAM.TAMANO_BYTES)) {
            ram.escribirPalabra(direccion, valor);
            return;
        }
        if (enRango(direccion, ROM.BASE, ROM.TAMANO_BYTES)) {
            rom.escribirPalabra(direccion, valor); // lanza: ROM de solo lectura
            return;
        }
        if (enRango(direccion, DiscoDuro.BASE, DiscoDuro.TAMANO_BYTES)) {
            disco.escribirPalabra(direccion, valor);
            return;
        }
        throw new IllegalArgumentException("Dirección de datos fuera de rango: 0x" + hex(direccion));
    }

    private void escribirByte(int direccion, int valor) {
        if (enRango(direccion, RAM.BASE, RAM.TAMANO_BYTES)) {
            ram.escribirByte(direccion, valor);
            return;
        }
        if (enRango(direccion, ROM.BASE, ROM.TAMANO_BYTES)) {
            throw new UnsupportedOperationException(
                    "Escritura rechazada: la ROM es de solo lectura (0x" + hex(direccion) + ")");
        }
        throw new IllegalArgumentException("Dirección de datos fuera de rango: 0x" + hex(direccion));
    }

    // ------------------------------------------------------------------
    // WB — Write Back
    // ------------------------------------------------------------------
    public void writeBack() {
        if (detenida) {
            return;
        }
        Instruccion ins = instruccionActual;
        String m = ins.getMnemonico();

        // Destino del WB, solo para describir el resultado en el log.
        int indiceDestino = -1;
        int valorNuevo = 0;
        switch (m) {
            case "ADD":
            case "SUB":
            case "AND":
            case "OR":
            case "XOR":
            case "NOR":
            case "SLT":
            case "SLTU":
            case "SLL":
            case "SRL":
            case "SRA":
                indiceDestino = ins.getRd();
                valorNuevo = alu.getResultado();
                break;
            case "MFHI":
                indiceDestino = ins.getRd();
                valorNuevo = alu.getHi();
                break;
            case "MFLO":
                indiceDestino = ins.getRd();
                valorNuevo = alu.getLo();
                break;
            case "ADDI":
            case "ANDI":
            case "ORI":
            case "SLTI":
            case "LUI":
                indiceDestino = ins.getRt();
                valorNuevo = alu.getResultado();
                break;
            case "LW":
            case "LB":
                indiceDestino = ins.getRt();
                valorNuevo = datoMemoria;
                break;
            case "JAL":
                indiceDestino = 31;
                valorNuevo = pcSiguiente;
                break;
            default:
                break;
        }
        int valorAnterior = indiceDestino >= 0
                ? registros.read(indiceDestino)
                : 0;

        switch (m) {
            case "ADD":
            case "SUB":
            case "AND":
            case "OR":
            case "XOR":
            case "NOR":
            case "SLT":
            case "SLTU":
            case "SLL":
            case "SRL":
            case "SRA":
                registros.write(ins.getRd(), alu.getResultado());
                break;
            case "MFHI":
                registros.write(ins.getRd(), alu.getHi());
                break;
            case "MFLO":
                registros.write(ins.getRd(), alu.getLo());
                break;
            case "ADDI":
            case "ANDI":
            case "ORI":
            case "SLTI":
            case "LUI":
                registros.write(ins.getRt(), alu.getResultado());
                break;
            case "LW":
            case "LB":
                registros.write(ins.getRt(), datoMemoria);
                break;
            case "JAL":
                registros.write(31, pcSiguiente); // $ra = PC + 4
                break;
            case "SYSCALL":
                ejecutarSyscall();
                break;
            default:
                break;
        }
        if (!detenida) {
            if ("SYSCALL".equals(m)) {
                // Conserva el mensaje del servicio ejecutado.
            } else if (indiceDestino >= 0) {
                boolean esDireccion = "JAL".equals(m);
                String valor = esDireccion
                        ? "0x" + hex(valorNuevo)
                        : String.valueOf(valorNuevo);
                String antes = esDireccion
                        ? "0x" + hex(valorAnterior)
                        : String.valueOf(valorAnterior);
                mensaje = "WB: " + nombre(indiceDestino) + " = " + valor
                        + " (antes " + antes + ")";
            } else {
                mensaje = "WB: " + m + " sin escritura de registro.";
            }
        }
        contadorInstrucciones++;
        if (!detenida && contadorInstrucciones >= limiteInstrucciones) {
            detener("Límite de instrucciones alcanzado (" + limiteInstrucciones
                    + "). Posible bucle infinito.");
        }
    }

    /**
     * SYSCALL de comportamiento limitado y documentado.
     * $v0 = 1  -> imprimir entero en $a0.
     * $v0 = 11 -> imprimir carácter (byte bajo de $a0).
     * $v0 = 34 -> imprimir $a0 en hexadecimal.
     * $v0 = 10 -> terminar la ejecución.
     * Otro valor -> se informa que no está soportado.
     */
    private void ejecutarSyscall() {
        int servicio = registros.read(2);  // $v0
        int argumento = registros.read(4); // $a0
        switch (servicio) {
            case 1:
                mensaje = "SYSCALL: entero = " + argumento;
                break;
            case 11:
                mensaje = "SYSCALL: carácter = '" + (char) (argumento & 0xFF) + "'";
                break;
            case 34:
                mensaje = "SYSCALL: hex = 0x" + Integer.toHexString(argumento);
                break;
            case 10:
                detener("SYSCALL: fin de programa (exit).", false);
                break;
            default:
                mensaje = "SYSCALL: servicio no soportado ($v0=" + servicio + ").";
                break;
        }
    }

    // ------------------------------------------------------------------
    // Coordinadores
    // ------------------------------------------------------------------

    /**
     * Ejecuta las cinco etapas en orden para una instrucción.
     * @return 1 si el procesador debe detenerse, 0 si puede continuar.
     */
    public int cicloDeComputo() {
        if (detenida) {
            return 1;
        }
        // Si no hay una instrucción a medio ejecutar, iniciarla (IF).
        if (!instruccionEnCurso()) {
            siguienteEtapa();
            if (detenida) {
                return 1;
            }
        }
        // Continuar desde la etapa pendiente hasta completar ESTA instrucción,
        // sin iniciar otra ni repetir etapas.
        while (instruccionEnCurso() && !detenida) {
            siguienteEtapa();
        }
        return detenida ? 1 : 0;
    }

    /**
     * Indica si hay una instrucción con etapas pendientes (1..4). Tras WB
     * (etapa 5) o antes del IF (etapa 0) no hay instrucción en curso.
     */
    public boolean instruccionEnCurso() {
        return etapaActual >= 1 && etapaActual < ETAPAS.length;
    }

    /**
     * Variante de depuración: ejecuta las cinco etapas imprimiendo el estado
     * después de cada una, sin iniciar otra instrucción antes de terminar
     * la actual.
     * @return 1 si el procesador debe detenerse, 0 si puede continuar.
     */
    public int cicloDeComputoDEBUG() {
        System.out.println("INICIA---------------------");
        System.out.println("-- IF  --");
        instructionFetch();
        imprimirEstado();
        if (detenida) return 1;

        System.out.println("-- ID  --");
        decode();
        imprimirEstado();
        if (detenida) return 1;

        System.out.println("-- EX  --");
        execute();
        imprimirEstado();
        if (detenida) return 1;

        System.out.println("-- MEM --");
        memoryAccess();
        imprimirEstado();
        if (detenida) return 1;

        System.out.println("-- WB  --");
        writeBack();
        imprimirEstado();
        etapaActual = ETAPAS.length; // instrucción completada (WB realizada)
        System.out.println("FINALIZA---------------------");
        return detenida ? 1 : 0;
    }

    /**
     * Avanza exactamente una etapa lógica (para la GUI). Mantiene la
     * instrucción activa y su estado intermedio entre llamadas.
     */
    public void siguienteEtapa() {
        if (detenida) {
            return;
        }
        switch (etapaActual) {
            case 0:
            case 5:
                // Sin instrucción en curso (o recién terminada): iniciar una nueva.
                instructionFetch();
                if (!detenida) etapaActual = 1;
                break;
            case 1:
                decode();
                if (!detenida) etapaActual = 2;
                break;
            case 2:
                execute();
                if (!detenida) etapaActual = 3;
                break;
            case 3:
                memoryAccess();
                if (!detenida) etapaActual = 4;
                break;
            case 4:
                writeBack();
                etapaActual = ETAPAS.length; // WB completado, aunque la detención ocurra aquí
                break;
            default:
                etapaActual = 0;
                break;
        }
    }

    // ------------------------------------------------------------------
    // Descripción legible para el log de etapas
    // ------------------------------------------------------------------
    private String nombre(int indice) {
        return registros.getNombre(indice);
    }

    private void agregarValor(StringBuilder sb, int indice, int valor) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(nombre(indice)).append("=").append(valor);
    }

    private String formatoInstruccion() {
        Instruccion ins = instruccionActual;
        if (ins == null) {
            return "";
        }
        String m = ins.getMnemonico();
        int rs = ins.getRs();
        int rt = ins.getRt();
        int rd = ins.getRd();
        int imm = ins.getImm();
        switch (m) {
            case "ADD":
            case "SUB":
            case "AND":
            case "OR":
            case "XOR":
            case "NOR":
            case "SLT":
            case "SLTU":
                return m + " " + nombre(rd) + ", " + nombre(rs) + ", " + nombre(rt);
            case "SLL":
            case "SRL":
            case "SRA":
                return m + " " + nombre(rd) + ", " + nombre(rt) + ", " + ins.getShamt();
            case "MULT":
            case "DIV":
                return m + " " + nombre(rs) + ", " + nombre(rt);
            case "MFHI":
            case "MFLO":
                return m + " " + nombre(rd);
            case "JR":
                return "JR " + nombre(rs);
            case "SYSCALL":
            case "NOP":
                return m;
            case "ADDI":
            case "ANDI":
            case "ORI":
            case "SLTI":
                return m + " " + nombre(rt) + ", " + nombre(rs) + ", " + imm;
            case "LUI":
                return "LUI " + nombre(rt) + ", 0x"
                        + Integer.toHexString(ins.getImmSinSigno());
            case "LW":
            case "SW":
            case "LB":
            case "SB":
                return m + " " + nombre(rt) + ", " + imm + "(" + nombre(rs) + ")";
            case "BEQ":
            case "BNE":
                return m + " " + nombre(rs) + ", " + nombre(rt) + ", 0x"
                        + hex(pcSiguiente + (imm << 2));
            case "J":
            case "JAL":
                return m + " 0x" + hex(calcularObjetivoSalto());
            default:
                return m;
        }
    }

    private String valoresRegistros() {
        Instruccion ins = instruccionActual;
        if (ins == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        switch (ins.getMnemonico()) {
            case "ADD":
            case "SUB":
            case "AND":
            case "OR":
            case "XOR":
            case "NOR":
            case "SLT":
            case "SLTU":
            case "SLL":
            case "SRL":
            case "SRA":
            case "MULT":
            case "DIV":
            case "BEQ":
            case "BNE":
                agregarValor(sb, ins.getRs(), valorRs);
                agregarValor(sb, ins.getRt(), valorRt);
                break;
            case "MFHI":
            case "MFLO":
                sb.append("HI=").append(alu.getHi()).append(", LO=").append(alu.getLo());
                break;
            case "JR":
            case "ADDI":
            case "ANDI":
            case "ORI":
            case "SLTI":
            case "LW":
            case "LB":
                agregarValor(sb, ins.getRs(), valorRs);
                break;
            case "SW":
            case "SB":
                agregarValor(sb, ins.getRs(), valorRs);
                agregarValor(sb, ins.getRt(), valorRt);
                break;
            case "SYSCALL":
                agregarValor(sb, 2, registros.read(2));
                agregarValor(sb, 4, registros.read(4));
                break;
            default:
                break;
        }
        return sb.toString();
    }

    private String ubicacionRAM(int direccion) {
        if (!enRango(direccion, RAM.BASE, RAM.TAMANO_BYTES)) {
            return "";
        }
        int desplazamiento = direccion - RAM.BASE;
        int segmento = desplazamiento / RAM.TAMANO_SEGMENTO_BYTES;
        int fila = (desplazamiento % RAM.TAMANO_SEGMENTO_BYTES) / 4;
        int byteEnPalabra = desplazamiento % 4;
        String ubicacion = " [segmento " + segmento + ", dirección " + fila;
        if (byteEnPalabra != 0) {
            ubicacion += ", byte " + byteEnPalabra;
        }
        return ubicacion + "]";
    }

    private String describirEX(String m) {
        Instruccion ins = instruccionActual;
        switch (m) {
            case "ADD":
            case "SUB":
            case "AND":
            case "OR":
            case "XOR":
            case "NOR":
            case "SLT":
            case "SLTU":
            case "SLL":
            case "SRL":
            case "SRA":
                return "EX: " + formatoInstruccion() + " -> " + nombre(ins.getRd())
                        + " = " + alu.getResultado();
            case "ADDI":
            case "ANDI":
            case "ORI":
            case "SLTI":
                return "EX: " + formatoInstruccion() + " -> " + nombre(ins.getRt())
                        + " = " + alu.getResultado();
            case "LUI":
                return "EX: " + formatoInstruccion() + " -> 0x"
                        + hex(alu.getResultado());
            case "MULT":
            case "DIV":
            case "MFHI":
            case "MFLO":
                return "EX: " + formatoInstruccion() + " -> HI=" + alu.getHi()
                        + ", LO=" + alu.getLo();
            case "LW":
            case "SW":
            case "LB":
            case "SB":
                return "EX: " + formatoInstruccion() + " -> dirección 0x"
                        + hex(direccionEfectiva);
            case "BEQ":
            case "BNE": {
                boolean iguales = valorRs == valorRt;
                boolean salta = "BEQ".equals(m) ? iguales : !iguales;
                return "EX: " + formatoInstruccion() + " (" + nombre(ins.getRs())
                        + "=" + valorRs + (iguales ? " == " : " != ") + nombre(ins.getRt())
                        + "=" + valorRt + ") -> " + (salta ? "salta" : "no salta");
            }
            case "J":
            case "JAL":
                return "EX: " + formatoInstruccion() + " -> 0x"
                        + hex(calcularObjetivoSalto());
            case "JR":
                return "EX: JR " + nombre(ins.getRs()) + " (0x" + hex(valorRs)
                        + ") -> 0x" + hex(pc.get());
            case "SYSCALL":
                return "EX: SYSCALL (servicio $v0=" + registros.read(2) + ")";
            case "NOP":
                return "EX: NOP";
            default:
                return "EX: " + m;
        }
    }

    private void imprimirEstado() {
        System.out.println("   PC=0x" + hex(pc.get())
                + " ALU op=" + (alu.getUltimaOperacion() == null ? "-" : alu.getUltimaOperacion())
                + " A=" + alu.getOperandoA() + " B=" + alu.getOperandoB()
                + " res=" + alu.getResultado()
                + " HI=" + alu.getHi() + " LO=" + alu.getLo());
        System.out.println("   " + mensaje);
    }

    private void detener(String motivo) {
        detener(motivo, true);
    }

    private void detener(String motivo, boolean esError) {
        detenida = true;
        error = esError;
        mensaje = motivo;
    }

    // ------------------------------------------------------------------
    // Consulta de estado
    // ------------------------------------------------------------------
    public boolean estaDetenida() {
        return detenida;
    }

    /** true solo cuando la detención se debió a una anomalía real. */
    public boolean isError() {
        return error;
    }

    public String getMensaje() {
        return mensaje;
    }

    /** Dirección de la instrucción que se está ejecutando (fijada en IF). */
    public int getPCInstruccionActual() {
        return pcInstruccionActual;
    }

    /** Última dirección de datos accedida por LW/LB/SW/SB, o -1 si no hubo. */
    public int getUltimaDireccionDatos() {
        return ultimaDireccionDatos;
    }

    /** true si el último acceso de datos fue una escritura. */
    public boolean isAccesoEscritura() {
        return accesoEscritura;
    }

    public Instruccion getInstruccionActual() {
        return instruccionActual;
    }

    public int getEtapaIndice() {
        return etapaActual;
    }

    public String getEtapaNombre() {
        if (etapaActual >= 1 && etapaActual <= ETAPAS.length) {
            return ETAPAS[etapaActual - 1];
        }
        return "-";
    }

    public int getContadorInstrucciones() {
        return contadorInstrucciones;
    }

    public int getLimiteInstrucciones() {
        return limiteInstrucciones;
    }

    public void setLimiteInstrucciones(int limiteInstrucciones) {
        this.limiteInstrucciones = limiteInstrucciones;
    }

    private static String hex(int valor) {
        return String.format("%08X", valor);
    }
}
