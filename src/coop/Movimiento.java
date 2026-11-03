package coop;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Movimiento {
    private final int id;
    private final String cuenta;
    private final LocalDate fecha;
    private final TipoMovimiento tipo;
    private final BigDecimal monto;
    private final BigDecimal saldoPosterior;
    private final String descripcion;

    public Movimiento(int id, String cuenta, LocalDate fecha, TipoMovimiento tipo,
                      BigDecimal monto, BigDecimal saldoPosterior, String descripcion) {
        this.id = id;
        this.cuenta = cuenta;
        this.fecha = fecha;
        this.tipo = tipo;
        this.monto = monto;
        this.saldoPosterior = saldoPosterior;
        this.descripcion = descripcion == null ? "" : descripcion;
    }

    public int getId() { return id; }
    public String getCuenta() { return cuenta; }
    public LocalDate getFecha() { return fecha; }
    public TipoMovimiento getTipo() { return tipo; }
    public BigDecimal getMonto() { return monto; }
    public BigDecimal getSaldoPosterior() { return saldoPosterior; }
    public String getDescripcion() { return descripcion; }

    /** Monto con signo: positivo si entra dinero, negativo si sale. */
    public BigDecimal firmado() {
        return tipo.esEntrada() ? monto : monto.negate();
    }

    public String aCsv() {
        return id + ";" + cuenta + ";" + fecha + ";" + tipo + ";" + monto.toPlainString() + ";"
                + saldoPosterior.toPlainString() + ";" + descripcion.replace(';', ',');
    }

    public static Movimiento desdeCsv(String linea) {
        String[] p = linea.split(";", 7);
        return new Movimiento(Integer.parseInt(p[0]), p[1], LocalDate.parse(p[2]),
                TipoMovimiento.valueOf(p[3]), new BigDecimal(p[4]), new BigDecimal(p[5]),
                p.length > 6 ? p[6] : "");
    }
}
