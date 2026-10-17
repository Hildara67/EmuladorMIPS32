package com.mycompany.mipsemulator;

import com.mycompany.mipsemulator.alu.ALU;
import com.mycompany.mipsemulator.alu.ALU.Operacion;
import com.mycompany.mipsemulator.cpu.CPU;
import com.mycompany.mipsemulator.cpu.PC;
import com.mycompany.mipsemulator.cpu.Registros;
import com.mycompany.mipsemulator.instruction.Decodificador;
import com.mycompany.mipsemulator.instruction.Instruccion;
import com.mycompany.mipsemulator.memory.DiscoDuro;
import com.mycompany.mipsemulator.memory.MemoriaInstrucciones;
import com.mycompany.mipsemulator.memory.RAM;
import com.mycompany.mipsemulator.memory.ROM;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Pruebas técnicas y de integración del emulador, en Java puro (sin JUnit).
 * Imprime PASS/FAIL y devuelve código de salida 0 si todo pasa.
 */
public final class PruebasEmulador {

    private static int total;
    private static int correctas;

    private PruebasEmulador() {
    }

    private static void check(String nombre, boolean condicion) {
        total++;
        if (condicion) {
            correctas++;
            System.out.println("[PASS] " + nombre);
        } else {
            System.out.println("[FAIL] " + nombre);
        }
    }

    public static void main(String[] args) throws Exception {
        probarALU();
        probarRegistros();
        probarPC();
        probarMemorias();
        probarDecodificador();
        probarIntegracion();
        probarEtapasYEstado();
        probarCapacidadYLimitesMemorias();
        probarFinEtapasYReinicio();
        probarContadoresPasosYDireccionamiento();

        System.out.println();
        System.out.println("RESUMEN: " + correctas + "/" + total + " pruebas correctas.");
        System.exit(correctas == total ? 0 : 1);
    }

    // ------------------------------------------------------------------
    private static void probarALU() {
        ALU alu = new ALU();

        alu.ejecutar(Operacion.ADD, 5, 3);
        check("ALU ADD 5+3=8", alu.getResultado() == 8);

        alu.ejecutar(Operacion.SUB, 5, 8);
        check("ALU SUB 5-8=-3", alu.getResultado() == -3);

        alu.ejecutar(Operacion.AND, 0b1100, 0b1010);
        check("ALU AND = 8", alu.getResultado() == 8);

        alu.ejecutar(Operacion.OR, 0b1100, 0b1010);
        check("ALU OR = 14", alu.getResultado() == 14);

        alu.ejecutar(Operacion.XOR, 0b1100, 0b1010);
        check("ALU XOR = 6", alu.getResultado() == 6);

        alu.ejecutar(Operacion.NOR, 0b1100, 0b1010);
        check("ALU NOR = ~14", alu.getResultado() == ~14);

        alu.ejecutar(Operacion.SLT, -1, 1);
        check("ALU SLT con signo (-1 < 1)", alu.getResultado() == 1);

        alu.ejecutar(Operacion.SLTU, 0xFFFFFFFF, 1);
        check("ALU SLTU sin signo (0xFFFFFFFF < 1 es falso)", alu.getResultado() == 0);

        alu.ejecutar(Operacion.SLL, 1, 4);
        check("ALU SLL 1<<4=16", alu.getResultado() == 16);

        alu.ejecutar(Operacion.SRL, 0x80000000, 4);
        check("ALU SRL lógico = 0x08000000", alu.getResultado() == 0x08000000);

        alu.ejecutar(Operacion.SRA, 0x80000000, 4);
        check("ALU SRA aritmético = 0xF8000000", alu.getResultado() == 0xF8000000);

        alu.ejecutar(Operacion.MULT, 3, 4);
        check("ALU MULT 3*4=12 en LO con HI=0", alu.getLo() == 12 && alu.getHi() == 0);

        alu.ejecutar(Operacion.MULT, 0x00010000, 0x00010000);
        check("ALU MULT 64 bits HI=1 LO=0", alu.getHi() == 1 && alu.getLo() == 0);

        alu.ejecutar(Operacion.DIV, 20, 6);
        check("ALU DIV 20/6 -> LO=3 HI=2", alu.getLo() == 3 && alu.getHi() == 2);

        boolean lanzo = false;
        try {
            alu.ejecutar(Operacion.DIV, 10, 0);
        } catch (ArithmeticException e) {
            lanzo = true;
        }
        check("ALU DIV entre cero lanza excepción controlada", lanzo);
    }

    // ------------------------------------------------------------------
    private static void probarRegistros() {
        Registros regs = new Registros();
        regs.write(5, 123);
        check("Registros escritura/lectura", regs.read(5) == 123);

        regs.write(0, 999);
        check("$zero inmutable", regs.read(0) == 0);

        check("Nombre de $zero", "$zero".equals(regs.getNombre(0)));
        check("Nombre de $ra", "$ra".equals(regs.getNombre(31)));

        regs.setHi(7);
        regs.setLo(9);
        check("HI/LO", regs.getHi() == 7 && regs.getLo() == 9);
    }

    // ------------------------------------------------------------------
    private static void probarPC() {
        PC pc = new PC();
        pc.set(100);
        check("PC set/get", pc.get() == 100);
        pc.incrementar();
        check("PC incrementa 4", pc.get() == 104);
        pc.reiniciar();
        check("PC reinicia a 0", pc.get() == 0);
    }

    // ------------------------------------------------------------------
    private static void probarMemorias() throws IOException {
        RAM ram = new RAM();
        ram.escribirPalabra(0, 0x11223344);
        check("RAM palabra", ram.leerPalabra(0) == 0x11223344);
        check("RAM byte 0 (LSB)", ram.leerByte(0) == 0x44);
        check("RAM byte 1", ram.leerByte(1) == 0x33);

        ram.escribirByte(1, 0xAB);
        check("RAM escribirByte con extensión", ram.leerByte(1) == (byte) 0xAB);
        check("RAM palabra tras escribirByte", ram.leerPalabra(0) == 0x1122AB44);

        boolean noAlineado = false;
        try {
            ram.leerPalabra(2);
        } catch (IllegalArgumentException e) {
            noAlineado = true;
        }
        check("RAM acceso no alineado rechazado", noAlineado);

        boolean fueraRango = false;
        try {
            ram.leerPalabra(400);
        } catch (IllegalArgumentException e) {
            fueraRango = true;
        }
        check("RAM dirección fuera de rango rechazada", fueraRango);

        ROM rom = new ROM();
        boolean romProtegida = false;
        try {
            rom.escribirPalabra(ROM.BASE, 1);
        } catch (UnsupportedOperationException e) {
            romProtegida = true;
        }
        check("ROM rechaza escritura", romProtegida);

        Path temp = Files.createTempFile("prueba", ".bin");
        try (FileOutputStream out = new FileOutputStream(temp.toFile())) {
            for (int i = 0; i < 10; i++) {
                out.write(0);
            }
        }
        MemoriaInstrucciones mem = new MemoriaInstrucciones();
        boolean longInvalida = false;
        try {
            mem.cargar(temp.toString());
        } catch (IllegalArgumentException e) {
            longInvalida = true;
        }
        check("Cargador rechaza longitud no múltiplo de 4", longInvalida);
        Files.deleteIfExists(temp);

        Path vacio = Files.createTempFile("vacio", ".bin");
        boolean vacioRechazado = false;
        try {
            mem.cargar(vacio.toString());
        } catch (IllegalArgumentException e) {
            vacioRechazado = true;
        }
        check("Cargador rechaza archivo vacío", vacioRechazado);
        Files.deleteIfExists(vacio);
    }

    // ------------------------------------------------------------------
    private static void probarDecodificador() {
        Decodificador dec = new Decodificador();

        Instruccion addi = dec.decodificar(GeneradorBinario.addi(8, 0, 5));
        check("Decodifica ADDI", "ADDI".equals(addi.getMnemonico()) && addi.isSoportada());
        check("ADDI campos rs/rt/imm", addi.getRs() == 0 && addi.getRt() == 8 && addi.getImm() == 5);

        Instruccion nop = dec.decodificar(0);
        check("Decodifica NOP", "NOP".equals(nop.getMnemonico()));

        Instruccion jota = dec.decodificar(GeneradorBinario.j(3));
        check("Decodifica J (formato J)", "J".equals(jota.getMnemonico())
                && jota.getFormato() == Instruccion.Formato.J);

        Instruccion add = dec.decodificar(GeneradorBinario.add(10, 8, 9));
        check("Decodifica ADD (formato R)", "ADD".equals(add.getMnemonico())
                && add.getFormato() == Instruccion.Formato.R);

        Instruccion invalida = dec.decodificar(0xFC000000);
        check("Instrucción no soportada detectada", !invalida.isSoportada());

        Instruccion neg = dec.decodificar(GeneradorBinario.addi(8, 0, -5));
        check("Inmediato con extensión de signo", neg.getImm() == -5);
    }

    // ------------------------------------------------------------------
    private static void probarIntegracion() throws IOException {
        File dir = Files.createTempDirectory("mipsprogramas").toFile();

        // Sumatoria -> 55 en $t2 y RAM[0]
        GeneradorBinario.escribirBin(new File(dir, "sumatoria.bin"), GeneradorBinario.sumatoria());
        CPU cpu = cargarYEjecutar(new File(dir, "sumatoria.bin"));
        check("Integración sumatoria 1..10 = 55 (registro)",
                cpu.getRegistros().read(10) == 55);
        check("Integración sumatoria RAM[0]=55", cpu.getRAM().leerPalabra(0) == 55);

        // Factorial -> 120 en $t2 y RAM[1]
        GeneradorBinario.escribirBin(new File(dir, "factorial.bin"), GeneradorBinario.factorial());
        cpu = cargarYEjecutar(new File(dir, "factorial.bin"));
        check("Integración factorial 5! = 120 (registro)", cpu.getRegistros().read(10) == 120);
        check("Integración factorial RAM[1]=120", cpu.getRAM().leerPalabra(4) == 120);

        // Integral -> 50 en $t6 y RAM[2]
        GeneradorBinario.escribirBin(new File(dir, "integral.bin"), GeneradorBinario.integral());
        cpu = cargarYEjecutar(new File(dir, "integral.bin"));
        check("Integración integral definida = 50 (registro)", cpu.getRegistros().read(14) == 50);
        check("Integración integral RAM[2]=50", cpu.getRAM().leerPalabra(8) == 50);

        // Promedio -> 30 en $s0 y RAM[5]
        GeneradorBinario.escribirBin(new File(dir, "promedio.bin"), GeneradorBinario.promedio());
        cpu = cargarYEjecutar(new File(dir, "promedio.bin"));
        check("Integración promedio = 30 (registro)", cpu.getRegistros().read(16) == 30);
        check("Integración promedio RAM[5]=30", cpu.getRAM().leerPalabra(20) == 30);
        check("Integración vector RAM[0..4]",
                cpu.getRAM().leerPalabra(0) == 10
                && cpu.getRAM().leerPalabra(4) == 20
                && cpu.getRAM().leerPalabra(8) == 30
                && cpu.getRAM().leerPalabra(12) == 40
                && cpu.getRAM().leerPalabra(16) == 50);

        // Perceptrón -> 1 en $t6 y RAM[0]
        GeneradorBinario.escribirBin(new File(dir, "perceptron.bin"), GeneradorBinario.perceptron());
        cpu = cargarYEjecutar(new File(dir, "perceptron.bin"));
        check("Integración perceptrón salida=1 (registro)", cpu.getRegistros().read(14) == 1);
        check("Integración perceptrón RAM[0]=1", cpu.getRAM().leerPalabra(0) == 1);

        // Ejecución por etapas: IF -> ID -> EX -> MEM -> WB en orden
        cpu = new CPU();
        cpu.cargarPrograma(new File(dir, "sumatoria.bin").getAbsolutePath());
        cpu.ejecutarEtapa();
        check("Etapa 1 es IF", "IF".equals(cpu.getEtapaNombre()));
        cpu.ejecutarEtapa();
        check("Etapa 2 es ID", "ID".equals(cpu.getEtapaNombre()));
        cpu.ejecutarEtapa();
        check("Etapa 3 es EX", "EX".equals(cpu.getEtapaNombre()));
        cpu.ejecutarEtapa();
        check("Etapa 4 es MEM", "MEM".equals(cpu.getEtapaNombre()));
        cpu.ejecutarEtapa();
        check("Etapa 5 es WB", "WB".equals(cpu.getEtapaNombre()));
        check("Tras WB la instrucción se completa (PC=4)", cpu.getPC().get() == 4);

        // Error controlado: PC fuera de programa no cierra la aplicación
        CPU cpuError = new CPU();
        cpuError.cargarPrograma(new File(dir, "factorial.bin").getAbsolutePath());
        for (int i = 0; i < 100; i++) {
            cpuError.ejecutarInstruccion();
        }
        check("Ejecución termina de forma controlada (detenida)", cpuError.estaDetenida());
    }

    // ------------------------------------------------------------------
    private static void probarEtapasYEstado() {
        // Estado inicial: sin programa.
        CPU sinPrograma = new CPU();
        check("Estado inicial SIN_PROGRAMA", "SIN_PROGRAMA".equals(sinPrograma.getEstado()));

        // LISTO tras cargar, EJECUTANDO a media instrucción, LISTO al completarla.
        CPU cpu = new CPU();
        cpu.cargarPalabras(GeneradorBinario.sumatoria());
        check("Estado LISTO tras cargar", "LISTO".equals(cpu.getEstado()));
        cpu.ejecutarEtapa(); // IF
        cpu.ejecutarEtapa(); // ID
        check("instruccionEnCurso tras 2 etapas", cpu.getUnidadControl().instruccionEnCurso());
        check("Estado EJECUTANDO a media instrucción", "EJECUTANDO".equals(cpu.getEstado()));
        cpu.ejecutarInstruccion(); // completa la MISMA instrucción (EX, MEM, WB)
        check("sin instrucción en curso tras completar", !cpu.getUnidadControl().instruccionEnCurso());
        check("Estado LISTO tras completar", "LISTO".equals(cpu.getEstado()));
        check("PC=4 tras completar la primera instrucción", cpu.getPC().get() == 4);
        check("Contador de instrucciones = 1", cpu.getContadorInstrucciones() == 1);
        check("Primera instrucción aplicada ($t0=1)", cpu.getRegistros().read(8) == 1);

        // FINALIZADO por SYSCALL exit.
        CPU cpuExit = new CPU();
        cpuExit.cargarPalabras(GeneradorBinario.sumatoria());
        cpuExit.ejecutarHastaElFinal(100000);
        check("Estado FINALIZADO tras SYSCALL exit", "FINALIZADO".equals(cpuExit.getEstado()));
        check("SYSCALL exit no marca error", !cpuExit.isError());

        // FINALIZADO por caída natural (PC tras la última instrucción, sin exit).
        CPU cpuFin = new CPU();
        cpuFin.cargarPalabras(new int[]{
            GeneradorBinario.addi(8, 0, 1),
            GeneradorBinario.addi(9, 0, 2)
        });
        cpuFin.ejecutarHastaElFinal(100000);
        check("Estado FINALIZADO por caída natural", "FINALIZADO".equals(cpuFin.getEstado()));
        check("Caída natural no marca error", !cpuFin.isError());

        // ERROR por salto fuera del programa.
        CPU cpuSalto = new CPU();
        cpuSalto.cargarPalabras(new int[]{GeneradorBinario.j(100)});
        cpuSalto.ejecutarHastaElFinal(100000);
        check("Estado ERROR por salto inválido", "ERROR".equals(cpuSalto.getEstado()));
        check("Salto inválido marca error", cpuSalto.isError());

        // ERROR por instrucción no soportada.
        CPU cpuNoSop = new CPU();
        cpuNoSop.cargarPalabras(new int[]{0xFC000000});
        cpuNoSop.ejecutarHastaElFinal(100000);
        check("Estado ERROR por instrucción no soportada", "ERROR".equals(cpuNoSop.getEstado()));

        // Observabilidad de acceso a datos (SW) y de la instrucción actual.
        CPU cpuMem = new CPU();
        cpuMem.cargarPalabras(new int[]{
            GeneradorBinario.addi(8, 0, 7),
            GeneradorBinario.sw(8, 0, 0)
        });
        cpuMem.ejecutarHastaElFinal(100000);
        check("RAM[0]=7 tras SW", cpuMem.getRAM().leerPalabra(0) == 7);
        check("ultimaDireccionDatos = 0x0", cpuMem.getUltimaDireccionDatos() == 0);
        check("accesoEscritura = true", cpuMem.isAccesoEscritura());
        check("PC de la instrucción actual = 0x4", cpuMem.getPCInstruccionActual() == 4);

        // División entre cero: se informa pero no detiene ni marca error.
        CPU cpuDiv = new CPU();
        cpuDiv.cargarPalabras(new int[]{
            GeneradorBinario.addi(8, 0, 10),
            GeneradorBinario.addi(9, 0, 0),
            GeneradorBinario.div(8, 9)
        });
        cpuDiv.ejecutarInstruccion();
        cpuDiv.ejecutarInstruccion();
        cpuDiv.ejecutarInstruccion();
        check("División entre cero no detiene la ejecución", !cpuDiv.estaDetenida());
        check("División entre cero no marca error", !cpuDiv.isError());
    }

    // ------------------------------------------------------------------
    private static void probarCapacidadYLimitesMemorias() {
        // RAM: 100 palabras de 32 bits, base 0x00000000, última palabra 0x0000018C.
        check("RAM tiene 100 palabras de 32 bits (400 bytes)",
                RAM.TAMANO == 100 && RAM.TAMANO_BYTES == 400);
        RAM ram = new RAM();
        check("RAM getTamano()==100", ram.getTamano() == 100);
        check("RAM primera palabra en 0x00000000", RAM.BASE == 0x00000000);
        check("RAM última palabra en 0x0000018C",
                RAM.BASE + (RAM.TAMANO - 1) * 4 == 0x0000018C);
        ram.escribirPalabra(0x0000018C, 0x7FFFFFFF);
        check("RAM escritura/lectura en la última palabra",
                ram.leerPalabra(0x0000018C) == 0x7FFFFFFF);
        boolean ramFuera = false;
        try {
            ram.leerPalabra(0x00000190);
        } catch (IllegalArgumentException e) {
            ramFuera = true;
        }
        check("RAM rechaza dirección siguiente (0x00000190)", ramFuera);

        // ROM: 100 palabras de 32 bits, base 0x00000200, última palabra 0x0000038C.
        check("ROM tiene 100 palabras de 32 bits (400 bytes)",
                ROM.TAMANO == 100 && ROM.TAMANO_BYTES == 400);
        ROM rom = new ROM();
        check("ROM getTamano()==100", rom.getTamano() == 100);
        check("ROM primera palabra en 0x00000200", ROM.BASE == 0x00000200);
        check("ROM última palabra en 0x0000038C",
                ROM.BASE + (ROM.TAMANO - 1) * 4 == 0x0000038C);
        rom.cargarPalabra(99, 0x0BADF00D);
        check("ROM permite carga inicial con cargarPalabra()",
                rom.getCelda(99) == 0x0BADF00D);
        check("ROM lectura en la última palabra",
                rom.leerPalabra(0x0000038C) == 0x0BADF00D);
        boolean romEscritura = false;
        try {
            rom.escribirPalabra(ROM.BASE, 1);
        } catch (UnsupportedOperationException e) {
            romEscritura = true;
        }
        check("ROM rechaza la escritura normal (solo lectura)", romEscritura);
        boolean romFuera = false;
        try {
            rom.leerPalabra(0x00000390);
        } catch (IllegalArgumentException e) {
            romFuera = true;
        }
        check("ROM rechaza dirección siguiente (0x00000390)", romFuera);

        // Disco duro: 100 palabras de 32 bits, base 0x00000400, última palabra 0x0000058C.
        check("Disco tiene 100 palabras de 32 bits (400 bytes)",
                DiscoDuro.TAMANO == 100 && DiscoDuro.TAMANO_BYTES == 400);
        DiscoDuro disco = new DiscoDuro();
        check("Disco getTamano()==100", disco.getTamano() == 100);
        check("Disco primera palabra en 0x00000400", DiscoDuro.BASE == 0x00000400);
        check("Disco última palabra en 0x0000058C",
                DiscoDuro.BASE + (DiscoDuro.TAMANO - 1) * 4 == 0x0000058C);
        disco.escribirPalabra(0x0000058C, 0x12345678);
        check("Disco escritura/lectura en la última palabra",
                disco.leerPalabra(0x0000058C) == 0x12345678);
        boolean discoFuera = false;
        try {
            disco.leerPalabra(0x00000590);
        } catch (IllegalArgumentException e) {
            discoFuera = true;
        }
        check("Disco rechaza dirección siguiente (0x00000590)", discoFuera);
        boolean discoNoAlineado = false;
        try {
            disco.escribirPalabra(0x00000402, 1);
        } catch (IllegalArgumentException e) {
            discoNoAlineado = true;
        }
        check("Disco rechaza acceso no alineado", discoNoAlineado);
    }

    // ------------------------------------------------------------------
    private static void probarFinEtapasYReinicio() {
        CPU cpu = new CPU();
        cpu.cargarPalabras(GeneradorBinario.sumatoria());
        check("Programa cargado con 10 instrucciones",
                cpu.getMemoriaInstrucciones().getCantidad() == 10);

        cpu.ejecutarHastaElFinal(100000);
        check("Sumatoria termina FINALIZADO", "FINALIZADO".equals(cpu.getEstado()));
        check("Sumatoria sin error", !cpu.isError());
        check("Instrucciones ejecutadas == 46", cpu.getContadorInstrucciones() == 46);
        check("PC final == 40 (0x28)", cpu.getPC().get() == 40);
        check("Tras terminar en WB, etapa final es WB (índice 5)",
                cpu.getUnidadControl().getEtapaIndice() == 5);
        check("getEtapaNombre() devuelve WB sin desbordar", "WB".equals(cpu.getEtapaNombre()));
        check("Mensaje final de SYSCALL no sobrescrito",
                cpu.getMensaje() != null && cpu.getMensaje().contains("fin de programa"));

        cpu.reiniciarEstado();
        check("Reinicio: contador ejecutadas = 0", cpu.getContadorInstrucciones() == 0);
        check("Reinicio: etapa = 0", cpu.getUnidadControl().getEtapaIndice() == 0);
        check("Reinicio: estado LISTO", "LISTO".equals(cpu.getEstado()));
        check("Reinicio: PC = 0", cpu.getPC().get() == 0);
        check("Reinicio: sin error", !cpu.isError());
    }

    // ------------------------------------------------------------------
    private static void probarContadoresPasosYDireccionamiento() {
        // Contadores: ejecutarHastaElFinal devuelve las completadas en la corrida
        // y coincide con el contador acumulado cuando no hubo pasos previos.
        CPU cpu = new CPU();
        cpu.cargarPalabras(GeneradorBinario.sumatoria());
        int n = cpu.ejecutarHastaElFinal(100000);
        check("ejecutarHastaElFinal devuelve instrucciones completadas (46)", n == 46);
        check("La corrida coincide con el contador acumulado",
                n == cpu.getContadorInstrucciones());

        // Programa que cae al final sin SYSCALL: la comprobación de fin de programa
        // no debe contarse como instrucción ejecutada.
        CPU cpuFin = new CPU();
        cpuFin.cargarPalabras(new int[]{
            GeneradorBinario.addi(8, 0, 1),
            GeneradorBinario.addi(9, 0, 2)
        });
        int nFin = cpuFin.ejecutarHastaElFinal(100000);
        check("Caída natural: la comprobación de fin no se cuenta",
                nFin == 2 && cpuFin.getContadorInstrucciones() == 2);

        // Avance por etapas: tras 2 etapas, "Ejecutar instrucción" completa la
        // MISMA instrucción (EX, MEM, WB) sin reiniciarla ni avanzar dos veces.
        CPU cpuPasos = new CPU();
        cpuPasos.cargarPalabras(GeneradorBinario.sumatoria());
        cpuPasos.ejecutarEtapa(); // IF
        cpuPasos.ejecutarEtapa(); // ID
        check("A media instrucción: EJECUTANDO", "EJECUTANDO".equals(cpuPasos.getEstado()));
        check("A media instrucción: instruccionEnCurso",
                cpuPasos.getUnidadControl().instruccionEnCurso());
        cpuPasos.ejecutarInstruccion(); // completa EX, MEM, WB de la misma
        check("Misma instrucción completada en WB (etapa 5)",
                cpuPasos.getUnidadControl().getEtapaIndice() == 5);
        check("PC no se duplica (PC=4)", cpuPasos.getPC().get() == 4);
        check("Contador=1 (una sola instrucción)", cpuPasos.getContadorInstrucciones() == 1);
        check("Primera instrucción aplicada ($t0=1)", cpuPasos.getRegistros().read(8) == 1);
        check("Sin instrucción en curso tras completar",
                !cpuPasos.getUnidadControl().instruccionEnCurso());

        // Direccionamiento de RAM coherente con el backend: SW a 8 ($t0=7).
        CPU cpuMem = new CPU();
        cpuMem.cargarPalabras(new int[]{
            GeneradorBinario.addi(8, 0, 7),
            GeneradorBinario.sw(8, 0, 8)
        });
        cpuMem.ejecutarHastaElFinal(100000);
        check("Última dirección de datos = 0x8 (offset 8)",
                cpuMem.getUltimaDireccionDatos() == 8);
        check("Dirección 0x8 = RAM.BASE + índice 2 * 4",
                RAM.BASE + 2 * 4 == 8);
        check("RAM en dirección 0x8 actualizada a 7",
                cpuMem.getRAM().leerPalabra(8) == 7);
    }

    private static CPU cargarYEjecutar(File archivo) throws IOException {
        CPU cpu = new CPU();
        cpu.cargarPrograma(archivo.getAbsolutePath());
        cpu.ejecutarHastaElFinal(100000);
        return cpu;
    }
}
