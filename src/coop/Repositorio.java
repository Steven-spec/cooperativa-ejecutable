package coop;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Persistencia en archivos CSV (separador ';'): socios.csv, cuentas.csv y movimientos.csv. */
public class Repositorio {
    private final Path dir;

    public Repositorio(String carpeta) { this.dir = Paths.get(carpeta); }

    public void guardar(Cooperativa coop) throws IOException {
        Files.createDirectories(dir);
        List<String> socios = new ArrayList<>();
        socios.add("cedula;nombre");
        for (Socio s : coop.getSocios()) socios.add(s.getCedula() + ";" + s.getNombre());

        List<String> cuentas = new ArrayList<>();
        cuentas.add("numero;cedula;tipo;saldo;activa");
        List<String> movs = new ArrayList<>();
        movs.add("id;cuenta;fecha;tipo;monto;saldoPosterior;descripcion");
        for (Cuenta c : coop.getCuentas()) {
            cuentas.add(c.getNumero() + ";" + c.getCedulaSocio() + ";" + c.getTipo() + ";"
                    + c.getSaldo().toPlainString() + ";" + c.isActiva());
            for (Movimiento m : c.getMovimientos()) movs.add(m.aCsv());
        }
        escribir("socios.csv", socios);
        escribir("cuentas.csv", cuentas);
        escribir("movimientos.csv", movs);
    }

    public Cooperativa cargar() throws IOException {
        Cooperativa coop = new Cooperativa();
        if (!Files.exists(dir.resolve("socios.csv"))) return coop;
        for (String l : leer("socios.csv")) {
            String[] p = l.split(";", 2);
            coop.agregarSocio(new Socio(p[0], p[1]));
        }
        for (String l : leer("cuentas.csv")) {
            String[] p = l.split(";");
            coop.agregarCuenta(Cooperativa.crearCuenta(p[2], p[0], p[1], new BigDecimal(p[3]), Boolean.parseBoolean(p[4])));
        }
        for (String l : leer("movimientos.csv")) {
            Movimiento m = Movimiento.desdeCsv(l);
            Cuenta c = coop.getCuenta(m.getCuenta());
            if (c == null) throw new IOException("Movimiento " + m.getId() + " apunta a una cuenta inexistente: " + m.getCuenta());
            c.cargarMovimiento(m);
        }
        coop.ajustarContador();
        return coop;
    }

    /** Escribe en un archivo temporal y luego lo reemplaza, para no dejar archivos a medias. */
    private void escribir(String nombre, List<String> lineas) throws IOException {
        Path tmp = dir.resolve(nombre + ".tmp");
        Files.write(tmp, lineas, StandardCharsets.UTF_8);
        Files.move(tmp, dir.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
    }

    private List<String> leer(String nombre) throws IOException {
        List<String> res = new ArrayList<>();
        List<String> todas = Files.readAllLines(dir.resolve(nombre), StandardCharsets.UTF_8);
        for (int i = 1; i < todas.size(); i++) if (!todas.get(i).trim().isEmpty()) res.add(todas.get(i));
        return res;
    }
}
