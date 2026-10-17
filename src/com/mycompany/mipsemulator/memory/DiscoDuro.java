package com.mycompany.mipsemulator.memory;

/**
 * Almacenamiento secundario (disco duro) de 100 palabras de 32 bits.
 * Base 0x00000400, rango de bytes 0x00000400 - 0x0000058F.
 * Clase independiente de 100 palabras solicitada por la práctica.
 */
public class DiscoDuro {

    public static final int BASE = 0x00000400;
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
                    "Dirección de disco fuera de rango: 0x" + Integer.toHexString(direccion));
        }
        if ((direccion & 0x3) != 0) {
            throw new IllegalArgumentException(
                    "Acceso no alineado (palabra) en disco: 0x" + Integer.toHexString(direccion));
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
}
