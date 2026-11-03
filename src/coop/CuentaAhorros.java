package coop;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

public class CuentaAhorros extends Cuenta {
    public static final BigDecimal SALDO_MINIMO = new BigDecimal("5.00");
    public static final BigDecimal TASA_ANUAL = new BigDecimal("0.04");

    public CuentaAhorros(String numero, String cedulaSocio, BigDecimal saldo, boolean activa) {
        super(numero, cedulaSocio, saldo, activa);
    }

    @Override
    public String getTipo() { return "AHORROS"; }

    /**
     * El saldo resultante debe ser >= $5. Se permite retirar todo (saldo 0) para poder cerrar
     * la cuenta, ya que el cierre exige saldo cero.
     */
    @Override
    public String motivoRechazoRetiro(BigDecimal monto) {
        BigDecimal resultado = getSaldo().subtract(monto);
        if (resultado.signum() < 0) return "Saldo insuficiente (saldo " + Formato.m(getSaldo()) + ")";
        if (resultado.signum() == 0) return null;
        if (resultado.compareTo(SALDO_MINIMO) < 0)
            return "La cuenta de ahorros debe mantener un saldo mínimo de " + Formato.m(SALDO_MINIMO)
                    + " (o retirarse por completo para cerrarla)";
        return null;
    }

    /** Interés mensual = saldo base del mes * 4 % / 12. */
    public BigDecimal calcularInteresMensual(YearMonth ym) {
        BigDecimal base = saldoBaseMes(ym);
        if (base.signum() <= 0) return BigDecimal.ZERO.setScale(2);
        return base.multiply(TASA_ANUAL).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }
}
