package com.mycompany.mipsemulator.memory;

/**
 * Memoria ROM de 100 palabras de 32 bits.
 * Base 0x00000200, rango de bytes 0x00000200 - 0x0000038F.
 * Es de solo lectura durante la ejecución normal; cualquier escritura se
 * rechaza de forma segura lanzando una excepción controlada.
 */
public class ROM {

    public static final int BASE = 0x00000200;
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

    /**
     * Carga inicial de la ROM (fuera de la ejecución normal de instrucciones).
     */
    public void cargarPalabra(int indice, int valor) {
        if (indice < 0 || indice >= TAMANO) {
            throw new IllegalArgumentException("Índice ROM fuera de rango: " + indice);
        }
        celdas[indice] = valor;
    }

    private void validarPalabra(int direccion) {
        if (direccion < BASE || direccion >= BASE + TAMANO_BYTES) {
            throw new IllegalArgumentException(
                    "Dirección ROM fuera de rango: 0x" + Integer.toHexString(direccion));
        }
        if ((direccion & 0x3) != 0) {
            throw new IllegalArgumentException(
                    "Acceso no alineado (palabra) en ROM: 0x" + Integer.toHexString(direccion));
        }
    }

    private void validarByte(int direccion) {
        if (direccion < BASE || direccion >= BASE + TAMANO_BYTES) {
            throw new IllegalArgumentException(
                    "Dirección ROM fuera de rango: 0x" + Integer.toHexString(direccion));
        }
    }

    public int leerPalabra(int direccion) {
        validarPalabra(direccion);
        return celdas[(direccion - BASE) >>> 2];
    }

    public int leerByte(int direccion) {
        validarByte(direccion);
        int palabra = celdas[(direccion - BASE) >>> 2];
        int desplazamiento = ((direccion - BASE) & 0x3) * 8;
        return (byte) ((palabra >>> desplazamiento) & 0xFF);
    }

    public void escribirPalabra(int direccion, int valor) {
        throw new UnsupportedOperationException(
                "Escritura rechazada: la ROM es de solo lectura (0x" + Integer.toHexString(direccion) + ")");
    }
}
