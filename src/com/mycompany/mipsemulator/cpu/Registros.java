package com.mycompany.mipsemulator.cpu;

/**
 * Banco de 32 registros generales de 32 bits, más los registros especiales
 * HI y LO. Garantiza que $zero (índice 0) siempre valga cero e ignore
 * cualquier escritura.
 */
public class Registros {

    public static final int CANTIDAD = 32;

    private static final String[] NOMBRES = {
        "$zero", "$at", "$v0", "$v1", "$a0", "$a1", "$a2", "$a3",
        "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7",
        "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
        "$t8", "$t9", "$k0", "$k1", "$gp", "$sp", "$fp", "$ra"
    };

    private final int[] gpr = new int[CANTIDAD];
    private int hi;
    private int lo;

    public void reiniciar() {
        for (int i = 0; i < CANTIDAD; i++) {
            gpr[i] = 0;
        }
        hi = 0;
        lo = 0;
    }

    public String getNombre(int indice) {
        if (indice < 0 || indice >= CANTIDAD) {
            return "?";
        }
        return NOMBRES[indice];
    }

    public int read(int indice) {
        if (indice < 0 || indice >= CANTIDAD) {
            return 0;
        }
        return gpr[indice];
    }

    public void write(int indice, int valor) {
        // $zero es inmutable y nunca se modifica.
        if (indice > 0 && indice < CANTIDAD) {
            gpr[indice] = valor;
        }
    }

    public int getHi() {
        return hi;
    }

    public void setHi(int hi) {
        this.hi = hi;
    }

    public int getLo() {
        return lo;
    }

    public void setLo(int lo) {
        this.lo = lo;
    }
}
