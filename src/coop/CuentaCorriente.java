package coop;

import java.math.BigDecimal;

public class CuentaCorriente extends Cuenta {
    public static final BigDecimal SOBREGIRO_MAXIMO = new BigDecimal("200.00");

    public CuentaCorriente(String numero, String cedulaSocio, BigDecimal saldo, boolean activa) {
        super(numero, cedulaSocio, saldo, activa);
    }

    @Override
    public String getTipo() { return "CORRIENTE"; }

    /** El saldo resultante no puede ser menor a -$200. */
    @Override
    public String motivoRechazoRetiro(BigDecimal monto) {
        BigDecimal resultado = getSaldo().subtract(monto);
        if (resultado.compareTo(SOBREGIRO_MAXIMO.negate()) < 0)
            return "Excede el sobregiro máximo de " + Formato.m(SOBREGIRO_MAXIMO);
        return null;
    }
}
