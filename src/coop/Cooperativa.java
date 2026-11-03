package coop;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Servicio principal: aplica todas las reglas de negocio de la cooperativa. 
public class Cooperativa {
    private final Map<String, Socio> socios = new LinkedHashMap<>();
    private final Map<String, Cuenta> cuentas = new LinkedHashMap<>();
    private int siguienteMovimiento = 1;

    // ---------- consultas ----------
    public Collection<Socio> getSocios() { return Collections.unmodifiableCollection(socios.values()); }
    public Collection<Cuenta> getCuentas() { return Collections.unmodifiableCollection(cuentas.values()); }
    public Socio getSocio(String cedula) { return socios.get(cedula); }
    public Cuenta getCuenta(String numero) { return cuentas.get(numero); }

    public Cuenta obtener(String numero) {
        Cuenta c = cuentas.get(numero);
        if (c == null) throw new OperacionInvalidaException("La cuenta " + numero + " no existe");
        return c;
    }

    // ---------- socios y cuentas ----------
    public Socio registrarSocio(String cedula, String nombre) {
        if (cedula == null || cedula.trim().isEmpty() || nombre == null || nombre.trim().isEmpty())
            throw new OperacionInvalidaException("Cédula y nombre son obligatorios");
        cedula = cedula.trim();
        if (socios.containsKey(cedula)) throw new OperacionInvalidaException("Ya existe un socio con la cédula " + cedula);
        Socio s = new Socio(cedula, nombre.trim());
        socios.put(cedula, s);
        return s;
    }

    public Cuenta abrirCuenta(String cedula, String tipo, BigDecimal depositoInicial, LocalDate fecha) {
        if (!socios.containsKey(cedula)) throw new OperacionInvalidaException("El socio " + cedula + " no existe");
        BigDecimal inicial = normal(depositoInicial);
        if (inicial.signum() < 0) throw new OperacionInvalidaException("El depósito inicial no puede ser negativo");
        String prefijo;
        if ("AHORROS".equalsIgnoreCase(tipo)) {
            prefijo = "AH-";
            if (inicial.compareTo(CuentaAhorros.SALDO_MINIMO) < 0)
                throw new OperacionInvalidaException("Una cuenta de ahorros requiere un depósito inicial de al menos "
                        + Formato.m(CuentaAhorros.SALDO_MINIMO));
        } else if ("CORRIENTE".equalsIgnoreCase(tipo)) {
            prefijo = "CC-";
        } else {
            throw new OperacionInvalidaException("Tipo de cuenta inválido: " + tipo);
        }
        int n = 1;
        while (cuentas.containsKey(prefijo + String.format("%04d", n))) n++;
        String numero = prefijo + String.format("%04d", n);
        Cuenta c = prefijo.equals("AH-") ? new CuentaAhorros(numero, cedula, BigDecimal.ZERO, true)
                                          : new CuentaCorriente(numero, cedula, BigDecimal.ZERO, true);
        cuentas.put(numero, c);
        if (inicial.signum() > 0) c.registrar(siguienteMovimiento++, fecha, TipoMovimiento.DEPOSITO, inicial, "Depósito inicial");
        return c;
    }

    // ---------- depósitos y retiros ----------
    public Movimiento depositar(String numero, BigDecimal monto, LocalDate fecha, String desc) {
        BigDecimal m = normal(monto);
        validarMonto(m);
        Cuenta c = obtener(numero);
        validarActiva(c);
        return c.registrar(siguienteMovimiento++, fecha, TipoMovimiento.DEPOSITO, m, vacio(desc, "Depósito"));
    }

    public Movimiento retirar(String numero, BigDecimal monto, LocalDate fecha, String desc) {
        BigDecimal m = normal(monto);
        validarMonto(m);
        Cuenta c = obtener(numero);
        validarActiva(c);
        String motivo = c.motivoRechazoRetiro(m);
        if (motivo != null) throw new OperacionInvalidaException(motivo);
        return c.registrar(siguienteMovimiento++, fecha, TipoMovimiento.RETIRO, m, vacio(desc, "Retiro"));
    }

    /**
     * Transferencia atómica: primero se validan TODAS las condiciones y recién entonces se aplican
     * el retiro y el depósito. Si algo fallara a mitad, se revierte el estado de ambas cuentas.
     */
    public Movimiento[] transferir(String origen, String destino, BigDecimal monto, LocalDate fecha) {
        BigDecimal m = normal(monto);
        validarMonto(m);
        if (origen.equals(destino)) throw new OperacionInvalidaException("No se puede transferir a la misma cuenta");
        Cuenta o = obtener(origen);
        Cuenta d = obtener(destino);
        validarActiva(o);
        validarActiva(d);
        String motivo = o.motivoRechazoRetiro(m);
        if (motivo != null) throw new OperacionInvalidaException("Retiro en origen rechazado: " + motivo);

        BigDecimal saldoO = o.getSaldo(), saldoD = d.getSaldo();
        int movO = o.getMovimientos().size(), movD = d.getMovimientos().size();
        int idInicial = siguienteMovimiento;
        try {
            Movimiento r = o.registrar(siguienteMovimiento++, fecha, TipoMovimiento.RETIRO, m, "Transferencia a " + destino);
            Movimiento dep = d.registrar(siguienteMovimiento++, fecha, TipoMovimiento.DEPOSITO, m, "Transferencia desde " + origen);
            return new Movimiento[]{r, dep};
        } catch (RuntimeException e) {
            o.revertir(movO, saldoO);
            d.revertir(movD, saldoD);
            siguienteMovimiento = idInicial;
            throw e;
        }
    }

    // ---------- cierre y eliminación ----------
    public void cerrarCuenta(String numero) {
        Cuenta c = obtener(numero);
        if (!c.isActiva()) throw new OperacionInvalidaException("La cuenta " + numero + " ya está inactiva");
        if (c.getSaldo().signum() != 0)
            throw new OperacionInvalidaException("No se puede cerrar: el saldo es " + Formato.m(c.getSaldo()) + " (debe ser $0.00)");
        c.setActiva(false);
    }

    /** @return true si se eliminó; false si, por tener movimientos, solo se marcó como inactiva. */
    public boolean eliminarCuenta(String numero) {
        Cuenta c = obtener(numero);
        if (c.getMovimientos().isEmpty()) {
            cuentas.remove(numero);
            return true;
        }
        cerrarCuenta(numero); // exige saldo cero y la deja inactiva
        return false;
    }

    // ---------- intereses ----------
    /** Registra el interés del mes (último día del mes) en cada cuenta de ahorros activa que aún no lo tenga. */
    public List<Movimiento> aplicarIntereses(YearMonth ym) {
        List<Movimiento> aplicados = new ArrayList<>();
        for (Cuenta c : cuentas.values()) {
            if (!(c instanceof CuentaAhorros) || !c.isActiva() || c.interesDelMes(ym) != null) continue;
            BigDecimal interes = ((CuentaAhorros) c).calcularInteresMensual(ym);
            if (interes.signum() > 0)
                aplicados.add(c.registrar(siguienteMovimiento++, ym.atEndOfMonth(), TipoMovimiento.INTERES, interes,
                        "Interés " + ym));
        }
        return aplicados;
    }

    // ---------- utilidades ----------
    private static BigDecimal normal(BigDecimal v) {
        if (v == null) throw new OperacionInvalidaException("Monto requerido");
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static void validarMonto(BigDecimal m) {
        if (m.signum() <= 0) throw new OperacionInvalidaException("El monto debe ser mayor que cero");
    }

    private static void validarActiva(Cuenta c) {
        if (!c.isActiva()) throw new OperacionInvalidaException("La cuenta " + c.getNumero() + " está inactiva");
    }

    private static String vacio(String s, String porDefecto) {
        return (s == null || s.trim().isEmpty()) ? porDefecto : s.trim();
    }

    // ---------- soporte para el repositorio (mismo paquete) ----------
    void agregarSocio(Socio s) { socios.put(s.getCedula(), s); }
    void agregarCuenta(Cuenta c) { cuentas.put(c.getNumero(), c); }

    void ajustarContador() {
        int max = 0;
        for (Cuenta c : cuentas.values())
            for (Movimiento m : c.getMovimientos()) max = Math.max(max, m.getId());
        siguienteMovimiento = max + 1;
    }

    static Cuenta crearCuenta(String tipo, String numero, String cedula, BigDecimal saldo, boolean activa) {
        return "AHORROS".equals(tipo) ? new CuentaAhorros(numero, cedula, saldo, activa)
                                       : new CuentaCorriente(numero, cedula, saldo, activa);
    }
}
