package coop;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Cuenta de un socio. Las reglas de retiro dependen del tipo (polimorfismo). */
public abstract class Cuenta {
    private final String numero;
    private final String cedulaSocio;
    private BigDecimal saldo;
    private boolean activa;
    private final List<Movimiento> movimientos = new ArrayList<>();

    protected Cuenta(String numero, String cedulaSocio, BigDecimal saldo, boolean activa) {
        this.numero = numero;
        this.cedulaSocio = cedulaSocio;
        this.saldo = saldo.setScale(2, RoundingMode.HALF_UP);
        this.activa = activa;
    }

    public abstract String getTipo();

    /** @return null si el retiro está permitido; si no, el motivo del rechazo. */
    public abstract String motivoRechazoRetiro(BigDecimal monto);

    public String getNumero() { return numero; }
    public String getCedulaSocio() { return cedulaSocio; }
    public BigDecimal getSaldo() { return saldo; }
    public boolean isActiva() { return activa; }
    public List<Movimiento> getMovimientos() { return Collections.unmodifiableList(movimientos); }

    Movimiento registrar(int id, LocalDate fecha, TipoMovimiento tipo, BigDecimal monto, String desc) {
        BigDecimal nuevo = tipo.esEntrada() ? saldo.add(monto) : saldo.subtract(monto);
        Movimiento m = new Movimiento(id, numero, fecha, tipo, monto, nuevo, desc);
        movimientos.add(m);
        saldo = nuevo;
        return m;
    }

    void cargarMovimiento(Movimiento m) { movimientos.add(m); }

    void revertir(int cantidadMovimientos, BigDecimal saldoAnterior) {
        while (movimientos.size() > cantidadMovimientos) movimientos.remove(movimientos.size() - 1);
        saldo = saldoAnterior;
    }

    void setActiva(boolean activa) { this.activa = activa; }

    /** Solo para pruebas de auditoría: altera el saldo sin registrar movimiento. */
    void forzarSaldo(BigDecimal nuevo) { this.saldo = nuevo; }

    /** Saldo al cierre del mes, sin contar el interés ya registrado en ese mismo mes. */
    BigDecimal saldoBaseMes(YearMonth ym) {
        LocalDate fin = ym.atEndOfMonth();
        BigDecimal base = BigDecimal.ZERO.setScale(2);
        for (Movimiento m : movimientos) {
            boolean interesDelMes = m.getTipo() == TipoMovimiento.INTERES && YearMonth.from(m.getFecha()).equals(ym);
            if (!m.getFecha().isAfter(fin) && !interesDelMes) base = base.add(m.firmado());
        }
        return base;
    }

    /** Movimiento de interés ya registrado en el mes, o null. */
    Movimiento interesDelMes(YearMonth ym) {
        for (Movimiento m : movimientos) {
            if (m.getTipo() == TipoMovimiento.INTERES && YearMonth.from(m.getFecha()).equals(ym)) return m;
        }
        return null;
    }

    @Override
    public String toString() {
        return numero + " [" + getTipo() + "] saldo " + Formato.m(saldo) + (activa ? "" : " (INACTIVA)");
    }
}
