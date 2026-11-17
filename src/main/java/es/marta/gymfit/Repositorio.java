package es.marta.gymfit;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public class Repositorio {
    private final Path archivo;
    public Repositorio(Path archivo) { this.archivo = archivo; }
    public Path ruta() { return archivo; }

    public List<Socio> cargar() throws IOException {
        if (!Files.exists(archivo)) return ejemplos();
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(archivo)) { p.loadFromXML(in); }
        List<Socio> socios = new ArrayList<>();
        try {
            int cantidad = Integer.parseInt(Objects.requireNonNull(p.getProperty("cantidad")));
            if (cantidad < 0 || cantidad > 100000) throw new IllegalArgumentException("Cantidad fuera de rango");
            for (int i = 0; i < cantidad; i++) {
                String k = i + ".";
                List<String> clases = new ArrayList<>();
                String lista = Objects.requireNonNull(p.getProperty(k + "clases"));
                if (!lista.isEmpty()) clases.addAll(Arrays.asList(lista.split(",")));
                String estado = Objects.requireNonNull(p.getProperty(k + "activo"));
                if (!estado.equals("true") && !estado.equals("false")) throw new IllegalArgumentException("Estado inválido");
                socios.add(new Socio(req(p,k,"id"), req(p,k,"nombre"), req(p,k,"apellidos"), LocalDate.parse(req(p,k,"nacimiento")),
                    req(p,k,"sexo"), req(p,k,"provincia"), req(p,k,"cuota"), clases, req(p,k,"iban"), Boolean.parseBoolean(estado)));
            }
        } catch (RuntimeException ex) { throw new IOException("El archivo de socios no tiene el formato esperado", ex); }
        return socios;
    }
    private String req(Properties p, String k, String campo) { return Objects.requireNonNull(p.getProperty(k + campo)); }

    public void guardar(List<Socio> socios) throws IOException {
        Properties p = new Properties();
        p.setProperty("cantidad", String.valueOf(socios.size()));
        for (int i = 0; i < socios.size(); i++) {
            Socio s = socios.get(i);
            String k = i + ".";
            p.setProperty(k+"id", s.id); p.setProperty(k+"nombre", s.nombre); p.setProperty(k+"apellidos", s.apellidos);
            p.setProperty(k+"nacimiento", s.nacimiento.toString()); p.setProperty(k+"sexo", s.sexo);
            p.setProperty(k+"provincia", s.provincia); p.setProperty(k+"cuota", s.cuota);
            p.setProperty(k+"clases", String.join(",", s.clases)); p.setProperty(k+"iban", s.iban);
            p.setProperty(k+"activo", String.valueOf(s.activo));
        }
        Path ruta = archivo.toAbsolutePath();
        Files.createDirectories(ruta.getParent());
        Path temporal = Files.createTempFile(ruta.getParent(), "gymfit-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temporal)) { p.storeToXML(out, "GymFit Club - socios", "UTF-8"); }
            try { Files.move(temporal, ruta, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporal, ruta, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporal); }
    }

    public static List<Socio> ejemplos() {
        return new ArrayList<>(Arrays.asList(
            new Socio("GF-001", "Alberto", "Larios", LocalDate.of(1999,3,12), "Mujer", "Sevilla", "Plus", Arrays.asList("Pilates"), "", true),
            new Socio("GF-002", "David", "Alonso Aguilar", LocalDate.of(2001,7,21), "Hombre", "Cádiz", "Joven", Arrays.asList("Spinning"), "", true),
            new Socio("GF-003", "Laura", "Rubio Gallardo", LocalDate.of(1988,11,30), "Mujer", "Sevilla", "Premium", Arrays.asList("Yoga", "Pilates"), "", true),
            new Socio("GF-004", "Paula", "Rivas González", LocalDate.of(2005,2,2), "Hombre", "Huelva", "Básica", Collections.emptyList(), "", false)));
    }
}
