package com.mycompany.mipsemulator.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Memoria de instrucciones separada de la RAM de datos.
 * Carga un archivo binario real (.bin) con instrucciones de 32 bits.
 * Convención fijada: little-endian (byte menos significativo primero).
 */
public class MemoriaInstrucciones {

    private final List<Integer> palabras = new ArrayList<>();

    /**
     * Carga las instrucciones desde un archivo .bin little-endian.
     *
     * @param ruta ruta del archivo binario.
     * @throws IOException si el archivo no puede leerse.
     * @throws IllegalArgumentException si el archivo está vacío o su longitud
     *         no es múltiplo de cuatro bytes.
     */
    public void cargar(String ruta) throws IOException {
        byte[] datos = Files.readAllBytes(Paths.get(ruta));
        if (datos.length == 0) {
            throw new IllegalArgumentException("El archivo .bin está vacío.");
        }
        if (datos.length % 4 != 0) {
            throw new IllegalArgumentException("Longitud inválida: " + datos.length
                    + " bytes. Debe ser múltiplo de 4 (cada instrucción ocupa 4 bytes).");
        }
        palabras.clear();
        for (int i = 0; i < datos.length; i += 4) {
            int b0 = datos[i] & 0xFF;
            int b1 = datos[i + 1] & 0xFF;
            int b2 = datos[i + 2] & 0xFF;
            int b3 = datos[i + 3] & 0xFF;
            guardarPalabra(b0 | (b1 << 8) | (b2 << 16) | (b3 << 24));
        }
    }

    private void guardarPalabra(int palabra) {
        palabras.add(palabra);
    }

    /**
     * Carga las instrucciones directamente desde un arreglo de palabras de 32 bits.
     * Se usa como apoyo de pruebas para no depender de archivos externos.
     */
    public void cargarPalabras(int[] palabras32) {
        palabras.clear();
        for (int p : palabras32) {
            palabras.add(p);
        }
    }

    public int getCantidad() {
        return palabras.size();
    }

    public boolean estaVacia() {
        return palabras.isEmpty();
    }

    public int getPalabra(int indice) {
        return palabras.get(indice);
    }

    /**
     * Devuelve la instrucción apuntada por un PC expresado en bytes.
     * Valida que el PC esté alineado y dentro del programa cargado.
     */
    public int leerPalabra(int direccionByte) {
        if (direccionByte < 0 || (direccionByte & 0x3) != 0) {
            throw new IllegalArgumentException(
                    "PC no alineado o negativo: 0x" + Integer.toHexString(direccionByte));
        }
        int indice = direccionByte >>> 2;
        if (indice >= palabras.size()) {
            throw new IllegalArgumentException(
                    "PC fuera del programa: 0x" + Integer.toHexString(direccionByte));
        }
        return palabras.get(indice);
    }

    public void limpiar() {
        palabras.clear();
    }
}
