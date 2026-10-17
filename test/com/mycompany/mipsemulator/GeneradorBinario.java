package com.mycompany.mipsemulator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Herramienta auxiliar (NO forma parte del emulador) que codifica instrucciones
 * MIPS y escribe archivos .bin con la misma convención little-endian que usa el
 * cargador. El emulador nunca incorpora un ensamblador: solo consume el .bin.
 *
 * Contiene además cinco programas de validación:
 * sumatoria, factorial, integral definida, promedio de un vector y perceptrón.
 */
public final class GeneradorBinario {

    // Registros
    private static final int ZERO = 0;
    private static final int V0 = 2;
    private static final int T0 = 8;
    private static final int T1 = 9;
    private static final int T2 = 10;
    private static final int T3 = 11;
    private static final int T4 = 12;
    private static final int T5 = 13;
    private static final int T6 = 14;
    private static final int T7 = 15;
    private static final int S0 = 16;

    // Opcodes
    private static final int OP_J = 0x02;
    private static final int OP_JAL = 0x03;
    private static final int OP_BEQ = 0x04;
    private static final int OP_BNE = 0x05;
    private static final int OP_ADDI = 0x08;
    private static final int OP_SLTI = 0x0A;
    private static final int OP_ANDI = 0x0C;
    private static final int OP_ORI = 0x0D;
    private static final int OP_LUI = 0x0F;
    private static final int OP_LW = 0x23;
    private static final int OP_SW = 0x2B;

    // Funct
    private static final int F_SLL = 0x00;
    private static final int F_SRL = 0x02;
    private static final int F_SRA = 0x03;
    private static final int F_JR = 0x08;
    private static final int F_SYSCALL = 0x0C;
    private static final int F_MFHI = 0x10;
    private static final int F_MFLO = 0x12;
    private static final int F_MULT = 0x18;
    private static final int F_DIV = 0x1A;
    private static final int F_ADD = 0x20;
    private static final int F_SUB = 0x22;
    private static final int F_AND = 0x24;
    private static final int F_OR = 0x25;
    private static final int F_XOR = 0x26;
    private static final int F_NOR = 0x27;
    private static final int F_SLT = 0x2A;
    private static final int F_SLTU = 0x2B;

    private GeneradorBinario() {
    }

    // ------------------------------------------------------------------
    // Codificadores
    // ------------------------------------------------------------------
    static int rType(int rs, int rt, int rd, int shamt, int funct) {
        return ((rs & 0x1F) << 21) | ((rt & 0x1F) << 16) | ((rd & 0x1F) << 11)
                | ((shamt & 0x1F) << 6) | (funct & 0x3F);
    }

    static int iType(int opcode, int rs, int rt, int imm) {
        return ((opcode & 0x3F) << 26) | ((rs & 0x1F) << 21) | ((rt & 0x1F) << 16)
                | (imm & 0xFFFF);
    }

    static int jType(int opcode, int direccion) {
        return ((opcode & 0x3F) << 26) | (direccion & 0x03FFFFFF);
    }

    static int addi(int rt, int rs, int imm) { return iType(OP_ADDI, rs, rt, imm); }
    static int add(int rd, int rs, int rt) { return rType(rs, rt, rd, 0, F_ADD); }
    static int sub(int rd, int rs, int rt) { return rType(rs, rt, rd, 0, F_SUB); }
    static int and(int rd, int rs, int rt) { return rType(rs, rt, rd, 0, F_AND); }
    static int or(int rd, int rs, int rt) { return rType(rs, rt, rd, 0, F_OR); }
    static int xor(int rd, int rs, int rt) { return rType(rs, rt, rd, 0, F_XOR); }
    static int nor(int rd, int rs, int rt) { return rType(rs, rt, rd, 0, F_NOR); }
    static int slt(int rd, int rs, int rt) { return rType(rs, rt, rd, 0, F_SLT); }
    static int sltu(int rd, int rs, int rt) { return rType(rs, rt, rd, 0, F_SLTU); }
    static int sll(int rd, int rt, int shamt) { return rType(0, rt, rd, shamt, F_SLL); }
    static int srl(int rd, int rt, int shamt) { return rType(0, rt, rd, shamt, F_SRL); }
    static int sra(int rd, int rt, int shamt) { return rType(0, rt, rd, shamt, F_SRA); }
    static int mult(int rs, int rt) { return rType(rs, rt, 0, 0, F_MULT); }
    static int div(int rs, int rt) { return rType(rs, rt, 0, 0, F_DIV); }
    static int mfhi(int rd) { return rType(0, 0, rd, 0, F_MFHI); }
    static int mflo(int rd) { return rType(0, 0, rd, 0, F_MFLO); }
    static int jr(int rs) { return rType(rs, 0, 0, 0, F_JR); }
    static int syscall() { return rType(0, 0, 0, 0, F_SYSCALL); }
    static int lui(int rt, int imm) { return iType(OP_LUI, 0, rt, imm); }
    static int andi(int rt, int rs, int imm) { return iType(OP_ANDI, rs, rt, imm); }
    static int ori(int rt, int rs, int imm) { return iType(OP_ORI, rs, rt, imm); }
    static int slti(int rt, int rs, int imm) { return iType(OP_SLTI, rs, rt, imm); }
    static int lw(int rt, int rs, int offset) { return iType(OP_LW, rs, rt, offset); }
    static int sw(int rt, int rs, int offset) { return iType(OP_SW, rs, rt, offset); }
    static int beq(int rs, int rt, int offset) { return iType(OP_BEQ, rs, rt, offset); }
    static int bne(int rs, int rt, int offset) { return iType(OP_BNE, rs, rt, offset); }
    static int j(int direccionPalabra) { return jType(OP_J, direccionPalabra); }
    static int jal(int direccionPalabra) { return jType(OP_JAL, direccionPalabra); }
    static int nop() { return 0; }

    // ------------------------------------------------------------------
    // Programas de validación
    // ------------------------------------------------------------------

    /** Sumatoria de 1 a 10. Resultado 55 en $t2 y RAM[0]. */
    public static int[] sumatoria() {
        return new int[]{
            addi(T0, ZERO, 1),    // 0: i = 1
            addi(T1, ZERO, 10),   // 1: N = 10
            addi(T2, ZERO, 0),    // 2: suma = 0
            add(T2, T2, T0),      // 3: suma += i          <- bucle
            addi(T0, T0, 1),      // 4: i++
            slt(T3, T1, T0),      // 5: t3 = (N < i)
            beq(T3, ZERO, -4),    // 6: si no (N<i) repetir
            sw(T2, ZERO, 0),      // 7: RAM[0] = suma
            addi(V0, ZERO, 10),   // 8: servicio exit
            syscall()             // 9
        };
    }

    /** Factorial de 5 = 120 en $t2 y RAM[1]. */
    public static int[] factorial() {
        return new int[]{
            addi(T0, ZERO, 1),    // 0: i = 1
            addi(T1, ZERO, 5),    // 1: N = 5
            addi(T2, ZERO, 1),    // 2: res = 1
            mult(T2, T0),         // 3: res *= i           <- bucle
            mflo(T2),             // 4: res = LO
            addi(T0, T0, 1),      // 5: i++
            slt(T3, T1, T0),      // 6: t3 = (N < i)
            beq(T3, ZERO, -5),    // 7: si no (N<i) repetir
            sw(T2, ZERO, 4),      // 8: RAM[1] = 120
            addi(V0, ZERO, 10),   // 9: exit
            syscall()             // 10
        };
    }

    /**
     * Integral definida de x entre 0 y 10 con regla del punto medio.
     * Punto fijo con escala 2: cada término se representa como 2*(i+0.5)=2i+1.
     * La suma vale 100 en escala 2; al dividir entre 2 se obtiene 50.
     * Resultado 50 en $t6 y RAM[2].
     */
    public static int[] integral() {
        return new int[]{
            addi(T0, ZERO, 0),    // 0: i = 0
            addi(T1, ZERO, 10),   // 1: N = 10
            addi(T2, ZERO, 0),    // 2: suma = 0
            add(T3, T0, T0),      // 3: t3 = 2i            <- bucle
            addi(T3, T3, 1),      // 4: t3 = 2i + 1
            add(T2, T2, T3),      // 5: suma += t3
            addi(T0, T0, 1),      // 6: i++
            slt(T4, T0, T1),      // 7: t4 = (i < N)
            bne(T4, ZERO, -6),    // 8: repetir mientras i < N
            addi(T5, ZERO, 2),    // 9: t5 = 2
            div(T2, T5),          // 10: LO = suma / 2
            mflo(T6),             // 11: t6 = 50
            sw(T6, ZERO, 8),      // 12: RAM[2] = 50
            addi(V0, ZERO, 10),   // 13: exit
            syscall()             // 14
        };
    }

    /** Promedio del vector fijo [10,20,30,40,50]. Resultado 30 en $s0 y RAM[5]. */
    public static int[] promedio() {
        return new int[]{
            addi(T0, ZERO, 10),   // 0
            sw(T0, ZERO, 0),      // 1: v[0] = 10
            addi(T0, ZERO, 20),   // 2
            sw(T0, ZERO, 4),      // 3: v[1] = 20
            addi(T0, ZERO, 30),   // 4
            sw(T0, ZERO, 8),      // 5: v[2] = 30
            addi(T0, ZERO, 40),   // 6
            sw(T0, ZERO, 12),     // 7: v[3] = 40
            addi(T0, ZERO, 50),   // 8
            sw(T0, ZERO, 16),     // 9: v[4] = 50
            addi(T1, ZERO, 0),    // 10: i = 0
            addi(T2, ZERO, 0),    // 11: suma = 0
            addi(T3, ZERO, 5),    // 12: n = 5
            addi(T4, ZERO, 0),    // 13: offset = 0
            lw(T5, T4, 0),        // 14: t5 = v[i]         <- bucle
            add(T2, T2, T5),      // 15: suma += t5
            addi(T1, T1, 1),      // 16: i++
            addi(T4, T4, 4),      // 17: offset += 4
            slt(T6, T1, T3),      // 18: t6 = (i < n)
            bne(T6, ZERO, -6),    // 19: repetir
            addi(T7, ZERO, 5),    // 20: t7 = 5
            div(T2, T7),          // 21: LO = suma / 5
            mflo(S0),             // 22: s0 = 30
            sw(S0, ZERO, 20),     // 23: RAM[5] = 30
            addi(V0, ZERO, 10),   // 24: exit
            syscall()             // 25
        };
    }

    /**
     * Perceptrón con entradas fijas: x1=3, x2=4, w1=2, w2=3, sesgo=-5.
     * Salida = 1 si (w1*x1 + w2*x2 + sesgo) > 0, si no 0.
     * Escala entera (escala 1). Suma = 13 > 0 -> salida 1 en $t6 y RAM[0].
     */
    public static int[] perceptron() {
        return new int[]{
            addi(T0, ZERO, 3),    // 0: x1 = 3
            addi(T1, ZERO, 4),    // 1: x2 = 4
            addi(T2, ZERO, 2),    // 2: w1 = 2
            addi(T3, ZERO, 3),    // 3: w2 = 3
            mult(T2, T0),         // 4: w1*x1
            mflo(T4),             // 5: t4 = 6
            mult(T3, T1),         // 6: w2*x2
            mflo(T5),             // 7: t5 = 12
            add(T4, T4, T5),      // 8: t4 = 18
            addi(T4, T4, -5),     // 9: t4 = 13 (+ sesgo)
            addi(T6, ZERO, 0),    // 10: salida = 0
            slt(T7, ZERO, T4),    // 11: t7 = (0 < suma)
            beq(T7, ZERO, 2),     // 12: si suma <= 0 saltar
            addi(T6, ZERO, 1),    // 13: salida = 1
            sw(T6, ZERO, 0),      // 14: RAM[0] = 1
            addi(V0, ZERO, 10),   // 15: exit
            syscall()             // 16
        };
    }

    // ------------------------------------------------------------------
    // Escritura little-endian
    // ------------------------------------------------------------------
    public static void escribirBin(File destino, int[] palabras) throws IOException {
        try (FileOutputStream out = new FileOutputStream(destino)) {
            for (int palabra : palabras) {
                out.write(palabra & 0xFF);
                out.write((palabra >>> 8) & 0xFF);
                out.write((palabra >>> 16) & 0xFF);
                out.write((palabra >>> 24) & 0xFF);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String directorio = args.length > 0 ? args[0] : "programas";
        File dir = new File(directorio);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear el directorio: " + dir.getAbsolutePath());
        }
        escribirBin(new File(dir, "sumatoria.bin"), sumatoria());
        escribirBin(new File(dir, "factorial.bin"), factorial());
        escribirBin(new File(dir, "integral.bin"), integral());
        escribirBin(new File(dir, "promedio.bin"), promedio());
        escribirBin(new File(dir, "perceptron.bin"), perceptron());
        escribirBin(new File("programa.bin"), sumatoria());
        System.out.println("Archivos .bin generados en: " + dir.getAbsolutePath());
    }
}
