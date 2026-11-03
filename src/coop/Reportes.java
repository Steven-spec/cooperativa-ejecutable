package coop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Los 5 reportes que requiere el gerente. */
public final class Reportes {
    private Reportes() {}

    private static String nombreSocio(Cooperativa c, String cedula) {
        Socio s = c.getSocio(cedula);
        return s == null ? "(socio inexistente)" : s.getNombre();
    }

    /** 1. Estado de cuenta con el saldo después de cada movimiento. */
    public static String estadoCuenta(Cooperativa coop, String numero) {
        Cuenta cu = coop.obtener(numero);
        StringBuilder sb = new StringBuilder();
        sb.append(Formato.linea(92)).append('\n');
        sb.append("ESTADO DE CUENTA ").append(numero).append(" (").append(cu.getTipo()).append(")")
          .append(cu.isActiva() ? "" : " - INACTIVA").append('\n');
        sb.append("Socio: ").append(nombreSocio(coop, cu.getCedulaSocio())).append('\n');
        sb.append(Formato.linea(92)).append('\n');
        sb.append(String.format("%-4s %-11s %-9s %-34s %12s %13s%n", "ID", "Fecha", "Tipo", "Descripción", "Monto", "Saldo"));
        for (Movimiento m : cu.getMovimientos())
            sb.append(String.format("%-4d %-11s %-9s %-34s %12s %13s%n", m.getId(), m.getFecha(), m.getTipo(),
                    m.getDescripcion(), Formato.m(m.firmado()), Formato.m(m.getSaldoPosterior())));
        sb.append("Saldo actual: ").append(Formato.m(cu.getSaldo())).append('\n');
        return sb.toString();
    }

    /** 2. Intereses del mes de todas las cuentas de ahorro. */
    public static String interesesMes(Cooperativa coop, YearMonth ym) {
        StringBuilder sb = new StringBuilder();
        sb.append(Formato.linea(92)).append('\n');
        sb.append("INTERESES DEL MES ").append(ym).append(" (4 % anual / 12 sobre el saldo al cierre del mes)\n");
        sb.append(Formato.linea(92)).append('\n');
        sb.append(String.format("%-9s %-28s %13s %10s  %s%n", "Cuenta", "Socio", "Saldo base", "Interés", "Estado"));
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Cuenta c : coop.getCuentas()) {
            if (!(c instanceof CuentaAhorros) || !c.isActiva()) continue;
            Movimiento reg = c.interesDelMes(ym);
            BigDecimal interes = reg != null ? reg.getMonto() : ((CuentaAhorros) c).calcularInteresMensual(ym);
            total = total.add(interes);
            sb.append(String.format("%-9s %-28s %13s %10s  %s%n", c.getNumero(), nombreSocio(coop, c.getCedulaSocio()),
                    Formato.m(c.saldoBaseMes(ym)), Formato.m(interes), reg != null ? "Aplicado" : "Por aplicar"));
        }
        sb.append("Total intereses del mes: ").append(Formato.m(total)).append('\n');
        return sb.toString();
    }

    /** 3. Cuentas en sobregiro. */
    public static String cuentasSobregiro(Cooperativa coop) {
        StringBuilder sb = new StringBuilder();
        sb.append(Formato.linea(92)).append('\n');
        sb.append("CUENTAS EN SOBREGIRO (saldo negativo, límite " + Formato.m(CuentaCorriente.SOBREGIRO_MAXIMO) + ")\n");
        sb.append(Formato.linea(92)).append('\n');
        sb.append(String.format("%-9s %-28s %13s %20s%n", "Cuenta", "Socio", "Saldo", "Sobregiro disponible"));
        int n = 0;
        for (Cuenta c : coop.getCuentas()) {
            if (c instanceof CuentaCorriente && c.getSaldo().signum() < 0) {
                n++;
                sb.append(String.format("%-9s %-28s %13s %20s%n", c.getNumero(), nombreSocio(coop, c.getCedulaSocio()),
                        Formato.m(c.getSaldo()), Formato.m(CuentaCorriente.SOBREGIRO_MAXIMO.add(c.getSaldo()))));
            }
        }
        if (n == 0) sb.append("No hay cuentas en sobregiro.\n");
        return sb.toString();
    }

    /** 4. Total depositado y retirado en un período (las transferencias cuentan como retiro + depósito). */
    public static String totalPeriodo(Cooperativa coop, LocalDate desde, LocalDate hasta) {
        BigDecimal dep = BigDecimal.ZERO.setScale(2), ret = dep, inte = dep;
        int nDep = 0, nRet = 0, nInt = 0;
        for (Cuenta c : coop.getCuentas()) {
            for (Movimiento m : c.getMovimientos()) {
                if (m.getFecha().isBefore(desde) || m.getFecha().isAfter(hasta)) continue;
                switch (m.getTipo()) {
                    case DEPOSITO: dep = dep.add(m.getMonto()); nDep++; break;
                    case RETIRO: ret = ret.add(m.getMonto()); nRet++; break;
                    default: inte = inte.add(m.getMonto()); nInt++; break;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(Formato.linea(60)).append('\n');
        sb.append("TOTAL DEPOSITADO Y RETIRADO del ").append(desde).append(" al ").append(hasta).append('\n');
        sb.append(Formato.linea(60)).append('\n');
        sb.append(String.format("Depósitos (%d):        %12s%n", nDep, Formato.m(dep)));
        sb.append(String.format("Retiros (%d):           %12s%n", nRet, Formato.m(ret)));
        sb.append(String.format("Intereses acreditados (%d): %8s%n", nInt, Formato.m(inte)));
        sb.append(String.format("Flujo neto (dep. + int. - ret.): %s%n", Formato.m(dep.add(inte).subtract(ret))));
        return sb.toString();
    }

    /** Detecta inconsistencias: recalcula cada saldo desde los movimientos y revisa las reglas. */
    public static List<String> inconsistencias(Cooperativa coop) {
        List<String> r = new ArrayList<>();
        for (Cuenta c : coop.getCuentas()) {
            BigDecimal acum = BigDecimal.ZERO.setScale(2);
            boolean cadenaRota = false;
            for (Movimiento m : c.getMovimientos()) {
                acum = acum.add(m.firmado());
                if (!cadenaRota && acum.compareTo(m.getSaldoPosterior()) != 0) {
                    r.add(c.getNumero() + ": el movimiento " + m.getId() + " registra saldo " + Formato.m(m.getSaldoPosterior())
                            + " pero el recalculado es " + Formato.m(acum));
                    cadenaRota = true;
                }
            }
            if (acum.compareTo(c.getSaldo()) != 0)
                r.add(c.getNumero() + ": saldo guardado " + Formato.m(c.getSaldo()) + " ≠ saldo recalculado " + Formato.m(acum));
            if (coop.getSocio(c.getCedulaSocio()) == null)
                r.add(c.getNumero() + ": pertenece a un socio inexistente (" + c.getCedulaSocio() + ")");
            if (c instanceof CuentaAhorros && c.isActiva() && c.getSaldo().compareTo(CuentaAhorros.SALDO_MINIMO) < 0)
                r.add(c.getNumero() + ": cuenta de ahorros activa con saldo bajo el mínimo (" + Formato.m(c.getSaldo()) + ")");
            if (c instanceof CuentaCorriente && c.getSaldo().compareTo(CuentaCorriente.SOBREGIRO_MAXIMO.negate()) < 0)
                r.add(c.getNumero() + ": sobregiro excede el límite (" + Formato.m(c.getSaldo()) + ")");
            if (!c.isActiva() && c.getSaldo().signum() != 0)
                r.add(c.getNumero() + ": cuenta inactiva con saldo distinto de cero");
        }
        return r;
    }

    /** 5. Auditoría de saldos. */
    public static String auditoriaSaldos(Cooperativa coop) {
        StringBuilder sb = new StringBuilder();
        sb.append(Formato.linea(92)).append('\n');
        sb.append("AUDITORÍA DE SALDOS\n");
        sb.append(Formato.linea(92)).append('\n');
        sb.append(String.format("%-9s %-10s %-9s %13s %13s  %s%n", "Cuenta", "Tipo", "Estado", "Guardado", "Recalculado", "Resultado"));
        List<String> problemas = inconsistencias(coop);
        for (Cuenta c : coop.getCuentas()) {
            BigDecimal calc = BigDecimal.ZERO.setScale(2);
            for (Movimiento m : c.getMovimientos()) calc = calc.add(m.firmado());
            boolean ok = true;
            for (String p : problemas) if (p.startsWith(c.getNumero() + ":")) ok = false;
            sb.append(String.format("%-9s %-10s %-9s %13s %13s  %s%n", c.getNumero(), c.getTipo(),
                    c.isActiva() ? "Activa" : "Inactiva", Formato.m(c.getSaldo()), Formato.m(calc), ok ? "OK" : "INCONSISTENTE"));
        }
        sb.append('\n');
        if (problemas.isEmpty()) sb.append("Resultado: todos los saldos son consistentes.\n");
        else {
            sb.append("Resultado: ").append(problemas.size()).append(" inconsistencia(s):\n");
            for (String p : problemas) sb.append("  - ").append(p).append('\n');
        }
        return sb.toString();
    }

    public static String listadoCuentas(Cooperativa coop) {
        StringBuilder sb = new StringBuilder();
        for (Socio s : coop.getSocios()) {
            sb.append(s).append('\n');
            for (Cuenta c : coop.getCuentas())
                if (c.getCedulaSocio().equals(s.getCedula())) sb.append("    ").append(c).append('\n');
        }
        if (coop.getSocios().isEmpty()) sb.append("No hay socios registrados.\n");
        return sb.toString();
    }
}
