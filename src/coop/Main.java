package coop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/** Menú de consola de la cooperativa. Uso: java -cp out coop.Main [carpetaDatos] */
public class Main {
    private static final Scanner in = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        Repositorio repo = new Repositorio(args.length > 0 ? args[0] : "datos");
        Cooperativa coop = repo.cargar();
        int op = -1;
        do {
            menu();
            try {
                op = Integer.parseInt(texto("Opción: "));
                boolean modifico = true;
                switch (op) {
                    case 1:
                        System.out.println("✔ Socio registrado: " + coop.registrarSocio(texto("Cédula: "), texto("Nombre: ")));
                        break;
                    case 2: {
                        String tipo = texto("Tipo (AHORROS/CORRIENTE): ");
                        Cuenta c = coop.abrirCuenta(texto("Cédula del socio: "), tipo, monto("Depósito inicial: "), fecha("Fecha"));
                        System.out.println("✔ Cuenta abierta: " + c);
                        break;
                    }
                    case 3: {
                        Movimiento m = coop.depositar(texto("Cuenta: "), monto("Monto: "), fecha("Fecha"), "");
                        System.out.println("✔ Depósito registrado. Saldo: " + Formato.m(m.getSaldoPosterior()));
                        break;
                    }
                    case 4: {
                        Movimiento m = coop.retirar(texto("Cuenta: "), monto("Monto: "), fecha("Fecha"), "");
                        System.out.println("✔ Retiro registrado. Saldo: " + Formato.m(m.getSaldoPosterior()));
                        break;
                    }
                    case 5: {
                        Movimiento[] t = coop.transferir(texto("Cuenta origen: "), texto("Cuenta destino: "), monto("Monto: "), fecha("Fecha"));
                        System.out.println("✔ Transferencia realizada. Saldo origen: " + Formato.m(t[0].getSaldoPosterior())
                                + " | Saldo destino: " + Formato.m(t[1].getSaldoPosterior()));
                        break;
                    }
                    case 6:
                        coop.cerrarCuenta(texto("Cuenta a cerrar: "));
                        System.out.println("✔ Cuenta cerrada (inactiva).");
                        break;
                    case 7:
                        System.out.println(coop.eliminarCuenta(texto("Cuenta a eliminar: "))
                                ? "✔ Cuenta eliminada (no tenía movimientos)."
                                : "✔ La cuenta tiene movimientos: no se elimina, se marcó como INACTIVA.");
                        break;
                    case 8: {
                        YearMonth ym = mes("Mes a acreditar");
                        System.out.println("✔ Intereses acreditados en " + coop.aplicarIntereses(ym).size() + " cuenta(s) para " + ym);
                        break;
                    }
                    case 9:
                        modifico = false;
                        reportes(coop);
                        break;
                    case 10:
                        modifico = false;
                        System.out.println(Reportes.listadoCuentas(coop));
                        break;
                    case 0:
                        modifico = false;
                        System.out.println("Hasta luego.");
                        break;
                    default:
                        modifico = false;
                        System.out.println("Opción inválida.");
                }
                if (modifico) repo.guardar(coop);
            } catch (OperacionInvalidaException e) {
                System.out.println("✘ Operación rechazada: " + e.getMessage());
            } catch (NumberFormatException | DateTimeParseException e) {
                System.out.println("✘ Dato con formato inválido (fechas: AAAA-MM-DD, meses: AAAA-MM).");
            }
        } while (op != 0);
    }

    private static void menu() {
        System.out.println("\n===== COOPERATIVA DE AHORRO Y CRÉDITO =====");
        System.out.println(" 1. Registrar socio          6. Cerrar cuenta");
        System.out.println(" 2. Abrir cuenta             7. Eliminar cuenta");
        System.out.println(" 3. Depósito                 8. Acreditar intereses del mes");
        System.out.println(" 4. Retiro                   9. Reportes");
        System.out.println(" 5. Transferencia           10. Listar socios y cuentas");
        System.out.println(" 0. Salir");
    }

    private static void reportes(Cooperativa coop) {
        System.out.println(" 1. Estado de cuenta  2. Intereses del mes  3. Cuentas en sobregiro");
        System.out.println(" 4. Total depositado/retirado en un período  5. Auditoría de saldos");
        switch (Integer.parseInt(texto("Reporte: "))) {
            case 1: System.out.println(Reportes.estadoCuenta(coop, texto("Cuenta: "))); break;
            case 2: System.out.println(Reportes.interesesMes(coop, mes("Mes"))); break;
            case 3: System.out.println(Reportes.cuentasSobregiro(coop)); break;
            case 4: {
                LocalDate d = LocalDate.parse(texto("Desde (AAAA-MM-DD): "));
                LocalDate h = LocalDate.parse(texto("Hasta (AAAA-MM-DD): "));
                if (h.isBefore(d)) throw new OperacionInvalidaException("La fecha final es anterior a la inicial");
                System.out.println(Reportes.totalPeriodo(coop, d, h));
                break;
            }
            case 5: System.out.println(Reportes.auditoriaSaldos(coop)); break;
            default: System.out.println("Reporte inválido.");
        }
    }

    private static String texto(String prompt) {
        System.out.print(prompt);
        return in.hasNextLine() ? in.nextLine().trim() : "0";
    }

    private static BigDecimal monto(String prompt) {
        return new BigDecimal(texto(prompt).replace(',', '.'));
    }

    private static LocalDate fecha(String prompt) {
        String t = texto(prompt + " (AAAA-MM-DD, Enter = hoy): ");
        return t.isEmpty() ? LocalDate.now() : LocalDate.parse(t);
    }

    private static YearMonth mes(String prompt) {
        String t = texto(prompt + " (AAAA-MM, Enter = mes actual): ");
        return t.isEmpty() ? YearMonth.now() : YearMonth.parse(t);
    }
}
