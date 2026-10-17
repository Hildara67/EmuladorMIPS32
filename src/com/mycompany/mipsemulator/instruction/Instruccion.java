package com.mycompany.mipsemulator.instruction;

/**
 * Representa la instrucción actual y sus campos relevantes.
 * Es un contenedor de datos que el {@link Decodificador} llena tras
 * interpretar la palabra de 32 bits.
 */
public class Instruccion {

    public enum Formato {
        R, I, J, DESCONOCIDO
    }

    private final int palabra;
    private Formato formato = Formato.DESCONOCIDO;
    private String mnemonico = "DESCONOCIDA";
    private boolean soportada;
    private int opcode;
    private int rs;
    private int rt;
    private int rd;
    private int shamt;
    private int funct;
    private int imm;        // inmediato con extensión de signo
    private int immSinSigno; // inmediato sin extensión de signo (16 bits)
    private int direccion;   // campo de dirección de 26 bits (formato J)

    public Instruccion(int palabra) {
        this.palabra = palabra;
    }

    public int getPalabra() {
        return palabra;
    }

    public Formato getFormato() {
        return formato;
    }

    public void setFormato(Formato formato) {
        this.formato = formato;
    }

    public String getMnemonico() {
        return mnemonico;
    }

    public void setMnemonico(String mnemonico) {
        this.mnemonico = mnemonico;
    }

    public boolean isSoportada() {
        return soportada;
    }

    public void setSoportada(boolean soportada) {
        this.soportada = soportada;
    }

    public int getOpcode() {
        return opcode;
    }

    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    public int getRs() {
        return rs;
    }

    public void setRs(int rs) {
        this.rs = rs;
    }

    public int getRt() {
        return rt;
    }

    public void setRt(int rt) {
        this.rt = rt;
    }

    public int getRd() {
        return rd;
    }

    public void setRd(int rd) {
        this.rd = rd;
    }

    public int getShamt() {
        return shamt;
    }

    public void setShamt(int shamt) {
        this.shamt = shamt;
    }

    public int getFunct() {
        return funct;
    }

    public void setFunct(int funct) {
        this.funct = funct;
    }

    public int getImm() {
        return imm;
    }

    public void setImm(int imm) {
        this.imm = imm;
        this.immSinSigno = imm & 0xFFFF;
    }

    public int getImmSinSigno() {
        return immSinSigno;
    }

    public int getDireccion() {
        return direccion;
    }

    public void setDireccion(int direccion) {
        this.direccion = direccion;
    }

    public String toBinario() {
        return String.format("%32s", Integer.toBinaryString(palabra)).replace(' ', '0');
    }
}
