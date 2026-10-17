package com.mycompany.mipsemulator;

import com.mycompany.mipsemulator.ui.MIPSEmulatorUI;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada del emulador. Inicia la interfaz gráfica; no contiene la
 * lógica del procesador.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MIPSEmulatorUI().setVisible(true));
    }
}
