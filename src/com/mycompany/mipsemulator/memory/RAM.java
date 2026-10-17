package com.mycompany.mipsemulator.memory;

/**
 * Memoria de datos RAM de 100 palabras de 32 bits.
 * Base 0x00000000, rango de bytes 0x00000000 - 0x0000018F.
 * Permite lectura/escritura de palabras (LW/SW) y de bytes (LB/SB).
 * El orden de bytes dentro de una palabra es little-endian.
 */
public class RAM {

    public static final int BASE = 0x00000000;
    public static final int TAMANO = 100;
    public static final int TAMANO_BYTES = TAMANO * 4;

    private final int[] celdas = new int[TAMANO];

    public void reiniciar() {
        for (int i = 0; i < TAMANO; i++) {
            celdas[i] = 0;
        }
    }

    public int getTamano() {
        return TAMANO;
    }

    public int getCelda(int indice) {
        return celdas[indice];
    }

    private void validarPalabra(int direccion) {
        if (direccion < BASE || direccion >= BASE + TAMANO_BYTES) {
            throw new IllegalArgumentException(
                    "Dirección RAM fuera de rango: 0x" + Integer.toHexString(direccion));
        }
        if ((direccion & 0x3) != 0) {
            throw new IllegalArgumentException(
                    "Acceso no alineado (palabra) en RAM: 0x" + Integer.toHexString(direccion));
        }
    }

    private void validarByte(int direccion) {
        if (direccion < BASE || direccion >= BASE + TAMANO_BYTES) {
            throw new IllegalArgumentException(
                    "Dirección RAM fuera de rango: 0x" + Integer.toHexString(direccion));
        }
    }

    public int leerPalabra(int direccion) {
        validarPalabra(direccion);
        return celdas[(direccion - BASE) >>> 2];
    }

    public void escribirPalabra(int direccion, int valor) {
        validarPalabra(direccion);
        celdas[(direccion - BASE) >>> 2] = valor;
    }

    public int leerByte(int direccion) {
        validarByte(direccion);
        int palabra = celdas[(direccion - BASE) >>> 2];
        int desplazamiento = ((direccion - BASE) & 0x3) * 8;
        return (byte) ((palabra >>> desplazamiento) & 0xFF);
    }

    public void escribirByte(int direccion, int valor) {
        validarByte(direccion);
        int indice = (direccion - BASE) >>> 2;
        int desplazamiento = ((direccion - BASE) & 0x3) * 8;
        int mascara = 0xFF << desplazamiento;
        celdas[indice] = (celdas[indice] & ~mascara) | ((valor & 0xFF) << desplazamiento);
    }
}
