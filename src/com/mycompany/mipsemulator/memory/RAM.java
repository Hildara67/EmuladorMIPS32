package com.mycompany.mipsemulator.memory;

/**
 * Memoria de datos RAM organizada como una matriz de 10 segmentos (columnas)
 * por 10 direcciones (filas): 100 palabras de 32 bits.
 * Base 0x00000000, rango de bytes 0x00000000 - 0x0000018F.
 * Cada segmento ocupa 10 direcciones (40 bytes); la dirección física de una
 * celda se obtiene como BASE + (segmento * 10 + direccion) * 4.
 * Permite lectura/escritura de palabras (LW/SW) y de bytes (LB/SB).
 * El orden de bytes dentro de una palabra es little-endian.
 */
public class RAM {

    public static final int BASE = 0x00000000;
    public static final int SEGMENTOS = 10;
    public static final int DIRECCIONES = 10;
    public static final int TAMANO = SEGMENTOS * DIRECCIONES;
    public static final int TAMANO_BYTES = TAMANO * 4;
    public static final int TAMANO_SEGMENTO_BYTES = DIRECCIONES * 4;

    private final int[][] celdas = new int[SEGMENTOS][DIRECCIONES];

    public void reiniciar() {
        for (int segmento = 0; segmento < SEGMENTOS; segmento++) {
            for (int direccion = 0; direccion < DIRECCIONES; direccion++) {
                celdas[segmento][direccion] = 0;
            }
        }
    }

    public int getTamano() {
        return TAMANO;
    }

    /** Devuelve una palabra por índice lineal (0..99), útil para la interfaz. */
    public int getCelda(int indice) {
        if (indice < 0 || indice >= TAMANO) {
            throw new IllegalArgumentException("Índice RAM fuera de rango: " + indice);
        }
        return celdas[indice / DIRECCIONES][indice % DIRECCIONES];
    }

    /**
     * Dirección física de una celda identificada por segmento (columna) y
     * dirección (fila).
     */
    public int direccionFisica(int segmento, int direccion) {
        validarCelda(segmento, direccion);
        return BASE + (segmento * DIRECCIONES + direccion) * 4;
    }

    /** Segmento (columna) al que pertenece una dirección física. */
    public int getSegmento(int direccion) {
        validarPalabra(direccion);
        return indiceSegmento(direccion);
    }

    /** Dirección (fila) dentro del segmento para una dirección física. */
    public int getDireccion(int direccion) {
        validarPalabra(direccion);
        return indiceDireccion(direccion);
    }

    /** Lee una palabra directamente por segmento y dirección. */
    public int leerPalabra(int segmento, int direccion) {
        validarCelda(segmento, direccion);
        return celdas[segmento][direccion];
    }

    /** Escribe una palabra directamente por segmento y dirección. */
    public void escribirPalabra(int segmento, int direccion, int valor) {
        validarCelda(segmento, direccion);
        celdas[segmento][direccion] = valor;
    }

    private int indiceSegmento(int direccion) {
        return (direccion - BASE) / TAMANO_SEGMENTO_BYTES;
    }

    private int indiceDireccion(int direccion) {
        return ((direccion - BASE) % TAMANO_SEGMENTO_BYTES) / 4;
    }

    private void validarCelda(int segmento, int direccion) {
        if (segmento < 0 || segmento >= SEGMENTOS) {
            throw new IllegalArgumentException(
                    "Segmento RAM fuera de rango: " + segmento);
        }
        if (direccion < 0 || direccion >= DIRECCIONES) {
            throw new IllegalArgumentException(
                    "Dirección RAM fuera de rango: " + direccion);
        }
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
        return celdas[indiceSegmento(direccion)][indiceDireccion(direccion)];
    }

    public void escribirPalabra(int direccion, int valor) {
        validarPalabra(direccion);
        celdas[indiceSegmento(direccion)][indiceDireccion(direccion)] = valor;
    }

    public int leerByte(int direccion) {
        validarByte(direccion);
        int segmento = indiceSegmento(direccion);
        int fila = indiceDireccion(direccion);
        int desplazamiento = ((direccion - BASE) & 0x3) * 8;
        return (byte) ((celdas[segmento][fila] >>> desplazamiento) & 0xFF);
    }

    public void escribirByte(int direccion, int valor) {
        validarByte(direccion);
        int segmento = indiceSegmento(direccion);
        int fila = indiceDireccion(direccion);
        int desplazamiento = ((direccion - BASE) & 0x3) * 8;
        int mascara = 0xFF << desplazamiento;
        celdas[segmento][fila] = (celdas[segmento][fila] & ~mascara)
                | ((valor & 0xFF) << desplazamiento);
    }
}
