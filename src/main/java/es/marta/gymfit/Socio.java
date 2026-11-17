package es.marta.gymfit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Socio {
    public final String id, nombre, apellidos, sexo, provincia, cuota, iban;
    public final LocalDate nacimiento;
    public final List<String> clases;
    public final boolean activo;

    public Socio(String id, String nombre, String apellidos, LocalDate nacimiento,
                 String sexo, String provincia, String cuota, List<String> clases, String iban, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.nacimiento = nacimiento;
        this.sexo = sexo;
        this.provincia = provincia;
        this.cuota = cuota;
        this.clases = new ArrayList<>(clases);
        this.iban = iban;
        this.activo = activo;
    }

    public String nombreCompleto() { return nombre + " " + apellidos; }
    public Socio conEstado(boolean estado) {
        return new Socio(id, nombre, apellidos, nacimiento, sexo, provincia, cuota, clases, iban, estado);
    }
}
