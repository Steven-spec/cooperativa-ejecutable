package coop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Genera los archivos de datos de prueba usando las mismas operaciones del sistema,
 * por lo que son consistentes entre sí por construcción. Uso: java -cp out coop.GeneradorDatos [carpeta]
 */
public class GeneradorDatos {
    static BigDecimal b(String v) { return new BigDecimal(v); }
    static LocalDate d(int mes, int dia) { return LocalDate.of(2026, mes, dia); }

    public static void main(String[] args) throws Exception {
        Cooperativa c = new Cooperativa();
        c.registrarSocio("1000000001", "María Fernanda Cevallos");
        c.registrarSocio("1000000002", "Luis Andrade Pozo");
        c.registrarSocio("1000000003", "Rosa Maldonado Terán");
        c.registrarSocio("1000000004", "Carlos Quilumbaquí");
        c.registrarSocio("1000000005", "Ana Lucía Torres");
        c.registrarSocio("1000000006", "Pedro Játiva (sin cuentas)");

        // Septiembre 2026
        c.abrirCuenta("1000000001", "AHORROS", b("100"), d(9, 1));      // AH-0001 (María, 2 cuentas)
        c.abrirCuenta("1000000001", "CORRIENTE", b("50"), d(9, 1));     // CC-0001
        c.abrirCuenta("1000000002", "AHORROS", b("500"), d(9, 1));      // AH-0002
        c.abrirCuenta("1000000003", "AHORROS", b("20"), d(9, 2));       // AH-0003
        c.abrirCuenta("1000000003", "CORRIENTE", b("0"), d(9, 2));      // CC-0002 (sin movimientos)
        c.abrirCuenta("1000000004", "AHORROS", b("30"), d(9, 2));       // AH-0004
        c.abrirCuenta("1000000004", "CORRIENTE", b("200"), d(9, 2));    // CC-0003
        c.abrirCuenta("1000000005", "AHORROS", b("250"), d(9, 3));      // AH-0005
        c.depositar("AH-0002", b("150"), d(9, 8), "Depósito en ventanilla");
        c.retirar("CC-0002", b("120"), d(9, 10), "Retiro con sobregiro");          // CC-0002 = -120
        c.retirar("AH-0004", b("30"), d(9, 12), "Retiro total para cierre");
        c.cerrarCuenta("AH-0004");                                                  // AH-0004 inactiva
        c.transferir("AH-0005", "AH-0001", b("50"), d(9, 15));
        c.transferir("CC-0001", "AH-0003", b("100"), d(9, 18));                    // CC-0001 = -50 (sobregiro)
        c.aplicarIntereses(YearMonth.of(2026, 9));

        // Octubre 2026
        c.depositar("AH-0001", b("75.50"), d(10, 5), "Depósito de sueldo");
        c.retirar("AH-0002", b("200"), d(10, 7), "Retiro en ventanilla");
        c.depositar("CC-0003", b("35.25"), d(10, 9), "Depósito en cheque");
        c.retirar("CC-0003", b("100"), d(10, 14), "Pago de servicios");
        c.depositar("AH-0005", b("40"), d(10, 20), "Depósito");
        c.aplicarIntereses(YearMonth.of(2026, 10));

        String carpeta = args.length > 0 ? args[0] : "datos";
        new Repositorio(carpeta).guardar(c);
        System.out.println("Datos de prueba generados en '" + carpeta + "'.");
        System.out.println(Reportes.auditoriaSaldos(c));
    }
}
