package coop;

/** Se lanza cuando una operación viola una regla de negocio de la cooperativa. */
public class OperacionInvalidaException extends RuntimeException {
    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
