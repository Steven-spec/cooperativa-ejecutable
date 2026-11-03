package coop;

public enum TipoMovimiento {
    DEPOSITO, RETIRO, INTERES;

    /** true si suma al saldo (depósito o interés), false si resta (retiro). */
    public boolean esEntrada() {
        return this != RETIRO;
    }
}
