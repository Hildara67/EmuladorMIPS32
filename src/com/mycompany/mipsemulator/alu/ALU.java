package com.mycompany.mipsemulator.alu;

/**
 * Unidad aritmética lógica independiente de Swing.
 * La unidad de control selecciona la operación por medio de {@link Operacion}
 * y la ALU la ejecuta mediante una selección (switch).
 *
 * Comportamiento documentado:
 * - Trabaja con enteros de 32 bits en complemento a dos.
 * - El desbordamiento aritmético se envuelve (wrap-around) silenciosamente,
 *   tal como ocurre en un registro de 32 bits.
 * - MULT coloca el producto de 64 bits en HI (parte alta) y LO (parte baja).
 * - DIV coloca el cociente en LO y el residuo en HI; si el divisor es cero
 *   lanza ArithmeticException para que el control lo reporte sin cerrar la app.
 */
public class ALU {

    public enum Operacion {
        ADD, SUB, ADDI, MULT, DIV,
        AND, OR, XOR, NOR, ANDI, ORI,
        SLT, SLTU, SLTI,
        SLL, SRL, SRA,
        LUI
    }

    private int operandoA;
    private int operandoB;
    private int resultado;
    private int hi;
    private int lo;
    private boolean zero;
    private Operacion ultimaOperacion;

    public void ejecutar(Operacion op, int a, int b) {
        this.operandoA = a;
        this.operandoB = b;
        this.ultimaOperacion = op;

        switch (op) {
            case ADD:
            case ADDI:
                resultado = a + b;
                break;
            case SUB:
                resultado = a - b;
                break;
            case AND:
            case ANDI:
                resultado = a & b;
                break;
            case OR:
            case ORI:
                resultado = a | b;
                break;
            case XOR:
                resultado = a ^ b;
                break;
            case NOR:
                resultado = ~(a | b);
                break;
            case SLT:
            case SLTI:
                resultado = (a < b) ? 1 : 0;
                break;
            case SLTU:
                resultado = (Integer.compareUnsigned(a, b) < 0) ? 1 : 0;
                break;
            case SLL:
                resultado = a << (b & 0x1F);
                break;
            case SRL:
                resultado = a >>> (b & 0x1F);
                break;
            case SRA:
                resultado = a >> (b & 0x1F);
                break;
            case LUI:
                resultado = b << 16;
                break;
            case MULT: {
                long producto = (long) a * (long) b;
                hi = (int) (producto >> 32);
                lo = (int) producto;
                resultado = lo;
                break;
            }
            case DIV:
                if (b == 0) {
                    throw new ArithmeticException("División entre cero");
                }
                lo = a / b;
                hi = a % b;
                resultado = lo;
                break;
            default:
                resultado = 0;
                break;
        }
        this.zero = (resultado == 0);
    }

    public int getOperandoA() {
        return operandoA;
    }

    public int getOperandoB() {
        return operandoB;
    }

    public int getResultado() {
        return resultado;
    }

    public int getHi() {
        return hi;
    }

    public int getLo() {
        return lo;
    }

    public boolean isZero() {
        return zero;
    }

    public Operacion getUltimaOperacion() {
        return ultimaOperacion;
    }

    public void reiniciar() {
        operandoA = 0;
        operandoB = 0;
        resultado = 0;
        hi = 0;
        lo = 0;
        zero = false;
        ultimaOperacion = null;
    }
}
