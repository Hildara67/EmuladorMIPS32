package com.mycompany.mipsemulator.cpu;

/**
 * Contador de programa (PC) de 32 bits.
 * Avanza normalmente 4 bytes por instrucción; la unidad de control puede
 * sobrescribirlo para ramas y saltos.
 */
public class PC {

    private int valor;

    public int get() {
        return valor;
    }

    public void set(int nuevoValor) {
        this.valor = nuevoValor;
    }

    public void incrementar() {
        this.valor += 4;
    }

    public void reiniciar() {
        this.valor = 0;
    }
}
