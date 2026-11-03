package coop;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Supplier;

/** Casos de prueba automáticos con resultados reales. Uso: java -cp out coop.Pruebas */
public class Pruebas {
    private static int total = 0, aprobados = 0;
    private static final LocalDate F = LocalDate.of(2026, 10, 1);

    static BigDecimal b(String v) { return new BigDecimal(v); }

    /** Escenario base: AH-0001 ($100) y CC-0001 ($0) de socio 1; AH-0002 ($300) de socio 2. */
    static Cooperativa base() {
        Cooperativa c = new Cooperativa();
        c.registrarSocio("1000000001", "Ana");
        c.registrarSocio("1000000002", "Luis");
        c.abrirCuenta("1000000001", "AHORROS", b("100"), LocalDate.of(2026, 9, 1));
        c.abrirCuenta("1000000001", "CORRIENTE", b("0"), LocalDate.of(2026, 9, 1));
        c.abrirCuenta("1000000002", "AHORROS", b("300"), LocalDate.of(2026, 9, 1));
        return c;
    }

    static String saldo(Cooperativa c, String n) { return c.getCuenta(n).getSaldo().toPlainString(); }

    static void caso(String desc, String esperado, Supplier<String> accion) {
        String obtenido;
        try {
            obtenido = accion.get();
        } catch (OperacionInvalidaException e) {
            obtenido = "RECHAZADO: " + e.getMessage();
        } catch (Exception e) {
            obtenido = "ERROR: " + e;
        }
        boolean ok = obtenido.startsWith(esperado);
        total++;
        if (ok) aprobados++;
        System.out.printf("| %2d | %s | %s | %s | %s |%n", total, desc, esperado, obtenido, ok ? "OK" : "FALLA");
    }

    static String intenta(Runnable r) {
        r.run();
        return "ACEPTADO";
    }

    public static void main(String[] args) throws Exception {
        System.out.println("| N° | Caso de prueba | Resultado esperado | Resultado obtenido | Estado |");
        System.out.println("|----|----------------|--------------------|--------------------|--------|");

        caso("Depósito de $0", "RECHAZADO", () -> intenta(() -> base().depositar("AH-0001", b("0"), F, "")));
        caso("Depósito de monto negativo (-$10)", "RECHAZADO", () -> intenta(() -> base().depositar("AH-0001", b("-10"), F, "")));
        caso("Depósito válido de $50 en AH-0001 ($100)", "ACEPTADO saldo=150.00", () -> {
            Cooperativa c = base(); c.depositar("AH-0001", b("50"), F, ""); return "ACEPTADO saldo=" + saldo(c, "AH-0001"); });
        caso("Retiro de ahorros que deja saldo $4 (< mínimo $5)", "RECHAZADO", () -> intenta(() -> base().retirar("AH-0001", b("96"), F, "")));
        caso("Retiro de ahorros que deja exactamente $5", "ACEPTADO saldo=5.00", () -> {
            Cooperativa c = base(); c.retirar("AH-0001", b("95"), F, ""); return "ACEPTADO saldo=" + saldo(c, "AH-0001"); });
        caso("Retiro mayor al saldo en ahorros ($150 de $100)", "RECHAZADO", () -> intenta(() -> base().retirar("AH-0001", b("150"), F, "")));
        caso("Retiro total en ahorros (queda $0, para poder cerrar)", "ACEPTADO saldo=0.00", () -> {
            Cooperativa c = base(); c.retirar("AH-0001", b("100"), F, ""); return "ACEPTADO saldo=" + saldo(c, "AH-0001"); });
        caso("Corriente: retiro hasta sobregiro de -$200", "ACEPTADO saldo=-200.00", () -> {
            Cooperativa c = base(); c.retirar("CC-0001", b("200"), F, ""); return "ACEPTADO saldo=" + saldo(c, "CC-0001"); });
        caso("Corriente: retiro que lleva a -$200.01", "RECHAZADO", () -> intenta(() -> base().retirar("CC-0001", b("200.01"), F, "")));
        caso("Transferencia a la misma cuenta", "RECHAZADO", () -> intenta(() -> base().transferir("AH-0001", "AH-0001", b("10"), F)));
        caso("Transferencia válida de $40 AH-0001 → AH-0002", "ACEPTADO origen=60.00 destino=340.00", () -> {
            Cooperativa c = base(); c.transferir("AH-0001", "AH-0002", b("40"), F);
            return "ACEPTADO origen=" + saldo(c, "AH-0001") + " destino=" + saldo(c, "AH-0002"); });
        caso("Transferencia que falla en origen (deja $2): ninguna cuenta cambia", "RECHAZADO, saldos sin cambios", () -> {
            Cooperativa c = base();
            try { c.transferir("AH-0001", "AH-0002", b("98"), F); return "FALLO: se aceptó"; }
            catch (OperacionInvalidaException e) {
                boolean igual = saldo(c, "AH-0001").equals("100.00") && saldo(c, "AH-0002").equals("300.00")
                        && c.getCuenta("AH-0001").getMovimientos().size() == 1 && c.getCuenta("AH-0002").getMovimientos().size() == 1;
                return (igual ? "RECHAZADO, saldos sin cambios" : "FALLO: cambiaron los saldos") + " (" + e.getMessage() + ")"; } });
        caso("Transferencia a cuenta destino inactiva: ninguna cuenta cambia", "RECHAZADO, saldos sin cambios", () -> {
            Cooperativa c = base(); c.retirar("AH-0002", b("300"), F, ""); c.cerrarCuenta("AH-0002");
            try { c.transferir("AH-0001", "AH-0002", b("10"), F); return "FALLO: se aceptó"; }
            catch (OperacionInvalidaException e) {
                boolean igual = saldo(c, "AH-0001").equals("100.00") && c.getCuenta("AH-0001").getMovimientos().size() == 1;
                return (igual ? "RECHAZADO, saldos sin cambios" : "FALLO: cambió el origen") + " (" + e.getMessage() + ")"; } });
        caso("Cerrar cuenta con saldo distinto de cero", "RECHAZADO", () -> intenta(() -> base().cerrarCuenta("AH-0001")));
        caso("Cerrar cuenta con saldo cero", "ACEPTADO activa=false", () -> {
            Cooperativa c = base(); c.retirar("AH-0001", b("100"), F, ""); c.cerrarCuenta("AH-0001");
            return "ACEPTADO activa=" + c.getCuenta("AH-0001").isActiva(); });
        caso("Eliminar cuenta CON movimientos: se marca inactiva, no se elimina", "INACTIVADA existe=true activa=false", () -> {
            Cooperativa c = base(); c.depositar("CC-0001", b("10"), F, ""); c.retirar("CC-0001", b("10"), F, "");
            boolean eliminada = c.eliminarCuenta("CC-0001");
            return (eliminada ? "ELIMINADA" : "INACTIVADA") + " existe=" + (c.getCuenta("CC-0001") != null) + " activa=" + c.getCuenta("CC-0001").isActiva(); });
        caso("Eliminar cuenta SIN movimientos", "ELIMINADA existe=false", () -> {
            Cooperativa c = base(); boolean eliminada = c.eliminarCuenta("CC-0001");
            return (eliminada ? "ELIMINADA" : "INACTIVADA") + " existe=" + (c.getCuenta("CC-0001") != null); });
        caso("Depósito en cuenta inactiva", "RECHAZADO", () -> {
            Cooperativa c = base(); c.retirar("AH-0001", b("100"), F, ""); c.cerrarCuenta("AH-0001");
            return intenta(() -> c.depositar("AH-0001", b("10"), F, "")); });
        caso("Apertura de ahorros con $4 (< $5)", "RECHAZADO", () -> intenta(() -> base().abrirCuenta("1000000001", "AHORROS", b("4"), F)));
        caso("Apertura de cuenta para socio inexistente", "RECHAZADO", () -> intenta(() -> base().abrirCuenta("9999999999", "AHORROS", b("10"), F)));
        caso("Registro de socio con cédula duplicada", "RECHAZADO", () -> intenta(() -> base().registrarSocio("1000000001", "Otro")));
        caso("Interés mensual sep-2026 (4 % anual / 12)", "ACEPTADO AH-0001=0.33 AH-0002=1.00", () -> {
            Cooperativa c = base(); c.aplicarIntereses(YearMonth.of(2026, 9));
            return "ACEPTADO AH-0001=" + c.getCuenta("AH-0001").interesDelMes(YearMonth.of(2026, 9)).getMonto()
                    + " AH-0002=" + c.getCuenta("AH-0002").interesDelMes(YearMonth.of(2026, 9)).getMonto(); });
        caso("Los intereses no se acreditan dos veces en el mismo mes", "ACEPTADO segunda aplicación=0 movimientos", () -> {
            Cooperativa c = base(); c.aplicarIntereses(YearMonth.of(2026, 9));
            return "ACEPTADO segunda aplicación=" + c.aplicarIntereses(YearMonth.of(2026, 9)).size() + " movimientos"; });
        caso("Cuenta corriente no genera intereses", "ACEPTADO movimientos INTERES en corriente=0", () -> {
            Cooperativa c = base(); c.depositar("CC-0001", b("500"), LocalDate.of(2026, 9, 2), ""); c.aplicarIntereses(YearMonth.of(2026, 9));
            long n = c.getCuenta("CC-0001").getMovimientos().stream().filter(m -> m.getTipo() == TipoMovimiento.INTERES).count();
            return "ACEPTADO movimientos INTERES en corriente=" + n; });
        caso("Auditoría con datos consistentes", "ACEPTADO 0 inconsistencias", () -> {
            Cooperativa c = base(); c.transferir("AH-0001", "AH-0002", b("20"), F); c.aplicarIntereses(YearMonth.of(2026, 9));
            return "ACEPTADO " + Reportes.inconsistencias(c).size() + " inconsistencias"; });
        caso("Auditoría detecta un saldo alterado manualmente", "ACEPTADO 1 inconsistencias", () -> {
            Cooperativa c = base(); c.getCuenta("AH-0002").forzarSaldo(b("9999.00"));
            return "ACEPTADO " + Reportes.inconsistencias(c).size() + " inconsistencias"; });
        caso("Persistencia: guardar en CSV y volver a cargar", "ACEPTADO saldos iguales, 0 inconsistencias", () -> {
            try {
                Path tmp = Files.createTempDirectory("coop-test");
                Cooperativa c = base(); c.transferir("AH-0001", "AH-0002", b("25.50"), F); c.retirar("CC-0001", b("80"), F, "");
                new Repositorio(tmp.toString()).guardar(c);
                Cooperativa r = new Repositorio(tmp.toString()).cargar();
                boolean iguales = true;
                for (Cuenta x : c.getCuentas()) iguales &= x.getSaldo().compareTo(r.getCuenta(x.getNumero()).getSaldo()) == 0
                        && x.getMovimientos().size() == r.getCuenta(x.getNumero()).getMovimientos().size();
                List<String> inc = Reportes.inconsistencias(r);
                return "ACEPTADO " + (iguales ? "saldos iguales" : "saldos DISTINTOS") + ", " + inc.size() + " inconsistencias";
            } catch (Exception e) { return "ERROR: " + e; } });

        System.out.println("\nResumen: " + aprobados + " de " + total + " casos aprobados.");
        if (aprobados != total) System.exit(1);
    }
}
