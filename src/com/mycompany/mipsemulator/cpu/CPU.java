package com.mycompany.mipsemulator.cpu;

import com.mycompany.mipsemulator.alu.ALU;
import com.mycompany.mipsemulator.execution.UnidadControl;
import com.mycompany.mipsemulator.instruction.Decodificador;
import com.mycompany.mipsemulator.instruction.Instruccion;
import com.mycompany.mipsemulator.memory.DiscoDuro;
import com.mycompany.mipsemulator.memory.MemoriaInstrucciones;
import com.mycompany.mipsemulator.memory.RAM;
import com.mycompany.mipsemulator.memory.ROM;

import java.io.IOException;

/**
 * Coordina el PC, los registros, la unidad de control y las memorias.
 * Ofrece operaciones de ejecución y consulta del estado. No depende de Swing.
 */
public class CPU {

    private final PC pc = new PC();
    private final Registros registros = new Registros();
    private final MemoriaInstrucciones memoriaInstrucciones = new MemoriaInstrucciones();
    private final RAM ram = new RAM();
    private final ROM rom = new ROM();
    private final DiscoDuro disco = new DiscoDuro();
    private final ALU alu = new ALU();
    private final Decodificador decodificador = new Decodificador();
    private final UnidadControl unidadControl;

    private boolean programaCargado;

    public CPU() {
        this.unidadControl = new UnidadControl(pc, registros, memoriaInstrucciones, ram, rom, disco, alu, decodificador);
    }

    /**
     * Carga un programa desde un archivo .bin y reinicia el estado del procesador.
     */
    public void cargarPrograma(String ruta) throws IOException {
        memoriaInstrucciones.cargar(ruta);
        programaCargado = true;
        reiniciarEstado();
    }

    /**
     * Carga un programa desde un arreglo de palabras de 32 bits (apoyo de pruebas).
     */
    public void cargarPalabras(int[] palabras) {
        memoriaInstrucciones.cargarPalabras(palabras);
        programaCargado = true;
        reiniciarEstado();
    }

    /**
     * Reinicia PC, registros, memorias de datos, ALU y unidad de control.
     * No borra la memoria de instrucciones cargada.
     */
    public void reiniciarEstado() {
        pc.reiniciar();
        registros.reiniciar();
        ram.reiniciar();
        rom.reiniciar();
        disco.reiniciar();
        alu.reiniciar();
        unidadControl.reiniciar();
    }

    /**
     * Ejecuta una instrucción completa (IF -> ID -> EX -> MEM -> WB).
     * @return 1 si el procesador se detuvo, 0 si puede continuar.
     */
    public int ejecutarInstruccion() {
        return unidadControl.cicloDeComputo();
    }

    /**
     * Avanza exactamente una etapa lógica de la instrucción en curso.
     */
    public void ejecutarEtapa() {
        unidadControl.siguienteEtapa();
    }

    /**
     * Ejecuta instrucciones completas hasta detenerse o alcanzar el límite.
     * @return número de instrucciones realmente completadas en esta llamada
     *         (no incluye la comprobación de fin de programa ni lo ya ejecutado).
     */
    public int ejecutarHastaElFinal(int limite) {
        int antes = unidadControl.getContadorInstrucciones();
        int intentos = 0;
        while (!unidadControl.estaDetenida() && intentos < limite) {
            ejecutarInstruccion();
            intentos++;
        }
        return unidadControl.getContadorInstrucciones() - antes;
    }

    public boolean estaDetenida() {
        return unidadControl.estaDetenida();
    }

    /** true si hay una instrucción con etapas pendientes. */
    public boolean hayInstruccionEnCurso() {
        return unidadControl.instruccionEnCurso();
    }

    /**
     * Estado real del emulador para la GUI:
     * SIN_PROGRAMA, LISTO, EJECUTANDO, FINALIZADO o ERROR.
     */
    public String getEstado() {
        if (!programaCargado) {
            return "SIN_PROGRAMA";
        }
        if (unidadControl.estaDetenida()) {
            return unidadControl.isError() ? "ERROR" : "FINALIZADO";
        }
        if (unidadControl.instruccionEnCurso()) {
            return "EJECUTANDO";
        }
        return "LISTO";
    }

    public String getMensaje() {
        return unidadControl.getMensaje();
    }

    public int getPCInstruccionActual() {
        return unidadControl.getPCInstruccionActual();
    }

    public int getUltimaDireccionDatos() {
        return unidadControl.getUltimaDireccionDatos();
    }

    public boolean isAccesoEscritura() {
        return unidadControl.isAccesoEscritura();
    }

    public boolean isError() {
        return unidadControl.isError();
    }

    public Instruccion getInstruccionActual() {
        return unidadControl.getInstruccionActual();
    }

    public String getEtapaNombre() {
        return unidadControl.getEtapaNombre();
    }

    public int getContadorInstrucciones() {
        return unidadControl.getContadorInstrucciones();
    }

    public void setLimiteInstrucciones(int limite) {
        unidadControl.setLimiteInstrucciones(limite);
    }

    public PC getPC() {
        return pc;
    }

    public Registros getRegistros() {
        return registros;
    }

    public ALU getALU() {
        return alu;
    }

    public RAM getRAM() {
        return ram;
    }

    public ROM getROM() {
        return rom;
    }

    public DiscoDuro getDisco() {
        return disco;
    }

    public MemoriaInstrucciones getMemoriaInstrucciones() {
        return memoriaInstrucciones;
    }

    public UnidadControl getUnidadControl() {
        return unidadControl;
    }

    public Decodificador getDecodificador() {
        return decodificador;
    }

    /**
     * Devuelve un listado legible de la memoria de instrucciones para la GUI:
     * "índice | dirección | palabra binaria | mnemónico".
     * La decodificación se hace aquí, de modo que la GUI solo muestra texto.
     */
    public String[] getListadoInstrucciones() {
        int cantidad = memoriaInstrucciones.getCantidad();
        String[] lineas = new String[cantidad];
        for (int i = 0; i < cantidad; i++) {
            int palabra = memoriaInstrucciones.getPalabra(i);
            Instruccion ins = decodificador.decodificar(palabra);
            lineas[i] = String.format("[%d] 0x%08X  %s  %s",
                    i, i * 4, ins.toBinario(), ins.getMnemonico());
        }
        return lineas;
    }
}
