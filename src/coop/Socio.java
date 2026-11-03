package coop;

public class Socio {
    private final String cedula;
    private final String nombre;

    public Socio(String cedula, String nombre) {
        this.cedula = cedula;
        this.nombre = nombre;
    }

    public String getCedula() { return cedula; }
    public String getNombre() { return nombre; }

    @Override
    public String toString() { return cedula + " - " + nombre; }
}
