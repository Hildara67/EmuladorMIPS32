package com.mycompany.mipsemulator.instruction;

import com.mycompany.mipsemulator.instruction.Instruccion.Formato;

/**
 * Interpreta los campos de una palabra de instrucción de 32 bits y determina
 * su operación y formato (R, I o J). Las constantes de opcode y función
 * residen aquí, tal como lo indica la especificación.
 */
public class Decodificador {

    // Opcodes (6 bits)
    public static final int OP_R = 0x00;
    public static final int OP_J = 0x02;
    public static final int OP_JAL = 0x03;
    public static final int OP_BEQ = 0x04;
    public static final int OP_BNE = 0x05;
    public static final int OP_ADDI = 0x08;
    public static final int OP_SLTI = 0x0A;
    public static final int OP_ANDI = 0x0C;
    public static final int OP_ORI = 0x0D;
    public static final int OP_LUI = 0x0F;
    public static final int OP_LB = 0x20;
    public static final int OP_LW = 0x23;
    public static final int OP_SB = 0x28;
    public static final int OP_SW = 0x2B;

    // Campos funct (6 bits) para el opcode R
    public static final int F_SLL = 0x00;
    public static final int F_SRL = 0x02;
    public static final int F_SRA = 0x03;
    public static final int F_JR = 0x08;
    public static final int F_SYSCALL = 0x0C;
    public static final int F_MFHI = 0x10;
    public static final int F_MFLO = 0x12;
    public static final int F_MULT = 0x18;
    public static final int F_DIV = 0x1A;
    public static final int F_ADD = 0x20;
    public static final int F_SUB = 0x22;
    public static final int F_AND = 0x24;
    public static final int F_OR = 0x25;
    public static final int F_XOR = 0x26;
    public static final int F_NOR = 0x27;
    public static final int F_SLT = 0x2A;
    public static final int F_SLTU = 0x2B;

    /**
     * Decodifica una palabra de 32 bits en una {@link Instruccion}.
     * Una palabra 0x00000000 se reconoce como NOP.
     */
    public Instruccion decodificar(int palabra) {
        Instruccion ins = new Instruccion(palabra);
        int opcode = (palabra >>> 26) & 0x3F;
        int rs = (palabra >>> 21) & 0x1F;
        int rt = (palabra >>> 16) & 0x1F;
        int rd = (palabra >>> 11) & 0x1F;
        int shamt = (palabra >>> 6) & 0x1F;
        int funct = palabra & 0x3F;

        ins.setOpcode(opcode);
        ins.setRs(rs);
        ins.setRt(rt);
        ins.setRd(rd);
        ins.setShamt(shamt);
        ins.setFunct(funct);
        ins.setImm((short) (palabra & 0xFFFF));
        ins.setDireccion(palabra & 0x03FFFFFF);

        if (palabra == 0) {
            ins.setFormato(Formato.R);
            ins.setMnemonico("NOP");
            ins.setSoportada(true);
            return ins;
        }

        if (opcode == OP_R) {
            ins.setFormato(Formato.R);
            switch (funct) {
                case F_ADD: ins.setMnemonico("ADD"); ins.setSoportada(true); break;
                case F_SUB: ins.setMnemonico("SUB"); ins.setSoportada(true); break;
                case F_AND: ins.setMnemonico("AND"); ins.setSoportada(true); break;
                case F_OR: ins.setMnemonico("OR"); ins.setSoportada(true); break;
                case F_XOR: ins.setMnemonico("XOR"); ins.setSoportada(true); break;
                case F_NOR: ins.setMnemonico("NOR"); ins.setSoportada(true); break;
                case F_SLT: ins.setMnemonico("SLT"); ins.setSoportada(true); break;
                case F_SLTU: ins.setMnemonico("SLTU"); ins.setSoportada(true); break;
                case F_SLL: ins.setMnemonico("SLL"); ins.setSoportada(true); break;
                case F_SRL: ins.setMnemonico("SRL"); ins.setSoportada(true); break;
                case F_SRA: ins.setMnemonico("SRA"); ins.setSoportada(true); break;
                case F_MULT: ins.setMnemonico("MULT"); ins.setSoportada(true); break;
                case F_DIV: ins.setMnemonico("DIV"); ins.setSoportada(true); break;
                case F_MFHI: ins.setMnemonico("MFHI"); ins.setSoportada(true); break;
                case F_MFLO: ins.setMnemonico("MFLO"); ins.setSoportada(true); break;
                case F_JR: ins.setMnemonico("JR"); ins.setSoportada(true); break;
                case F_SYSCALL: ins.setMnemonico("SYSCALL"); ins.setSoportada(true); break;
                default:
                    ins.setMnemonico("DESCONOCIDA");
                    ins.setSoportada(false);
                    break;
            }
            return ins;
        }

        ins.setFormato(Formato.I);
        switch (opcode) {
            case OP_ADDI: ins.setMnemonico("ADDI"); ins.setSoportada(true); break;
            case OP_ANDI: ins.setMnemonico("ANDI"); ins.setSoportada(true); break;
            case OP_ORI: ins.setMnemonico("ORI"); ins.setSoportada(true); break;
            case OP_SLTI: ins.setMnemonico("SLTI"); ins.setSoportada(true); break;
            case OP_LUI: ins.setMnemonico("LUI"); ins.setSoportada(true); break;
            case OP_LW: ins.setMnemonico("LW"); ins.setSoportada(true); break;
            case OP_SW: ins.setMnemonico("SW"); ins.setSoportada(true); break;
            case OP_LB: ins.setMnemonico("LB"); ins.setSoportada(true); break;
            case OP_SB: ins.setMnemonico("SB"); ins.setSoportada(true); break;
            case OP_BEQ: ins.setMnemonico("BEQ"); ins.setSoportada(true); break;
            case OP_BNE: ins.setMnemonico("BNE"); ins.setSoportada(true); break;
            case OP_J: ins.setFormato(Formato.J); ins.setMnemonico("J"); ins.setSoportada(true); break;
            case OP_JAL: ins.setFormato(Formato.J); ins.setMnemonico("JAL"); ins.setSoportada(true); break;
            default:
                ins.setFormato(Formato.DESCONOCIDO);
                ins.setMnemonico("DESCONOCIDA");
                ins.setSoportada(false);
                break;
        }
        return ins;
    }
}
