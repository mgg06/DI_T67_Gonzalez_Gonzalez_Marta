package es.marta.gymfit;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Comprobaciones técnicas con datos aislados, independientes del test de usuarios. */
public final class Comprobacion {
    private static final List<String> log = new ArrayList<>();
    private static void comprobar(boolean correcto, String prueba) {
        if (!correcto) throw new AssertionError(prueba);
        log.add("OK - " + prueba);
    }
    private static void captura(Parent root, Path carpeta, String nombre) throws Exception {
        root.applyCss(); root.layout();
        WritableImage imagen = root.snapshot(null, null);
        ImageIO.write(SwingFXUtils.fromFXImage(imagen, null), "png", carpeta.resolve(nombre + ".png").toFile());
    }
    public static void ejecutar(Stage stage, Parent root, ClubController c, Path datos) {
        Path carpeta = datos.toAbsolutePath().getParent();
        int codigo = 0;
        try {
            Files.createDirectories(carpeta);
            comprobar(c.socios.size() == 4, "Carga de las cuatro fichas de demostración");
            comprobar(c.progreso.getText().startsWith("0 de 5"), "Guía inicial de campos obligatorios");
            captura(root, carpeta, "01-pantalla-inicial");
            comprobar(Math.round(root.getBoundsInLocal().getWidth()) == 1280 && Math.round(root.getBoundsInLocal().getHeight()) == 840,
                "Dimensiones reales de la pantalla: 1280 por 840");
            comprobar(c.menuBar.getMenus().size() == 4 && c.menuBar.getMenus().get(0).getText().replace("_", "").equals("Archivo")
                && c.menuBar.getMenus().get(1).getText().replace("_", "").equals("Editar")
                && c.menuBar.getMenus().get(3).getText().replace("_", "").equals("Ayuda"), "Orden estándar de la barra de menús");
            ToolBar herramientas = (ToolBar)root.lookup(".tool-bar");
            comprobar(herramientas.getItems().stream().filter(n -> n instanceof Button)
                .allMatch(n -> ((Button)n).getGraphic() != null && ((Button)n).getTooltip() != null), "Herramientas con icono, texto y tooltip");
            comprobar(c.etiquetanombre.getLabelFor() == c.nombre && c.etiquetaapellidos.getLabelFor() == c.apellidos
                && c.etiquetanacimiento.getLabelFor() == c.nacimiento && c.etiquetaprovincia.getLabelFor() == c.provincia
                && c.etiquetaiban.getLabelFor() == c.iban, "Cinco etiquetas asociadas a sus campos accesibles");
            ContextMenu contextual = null;
            for (javafx.scene.Node n : root.lookupAll(".table-row-cell")) {
                TableRow<?> fila = (TableRow<?>)n;
                if (!fila.isEmpty() && fila.getContextMenu() != null) { contextual = fila.getContextMenu(); break; }
            }
            comprobar(contextual != null && contextual.getItems().stream().filter(i -> !(i instanceof SeparatorMenuItem)).count() <= 7
                && contextual.getItems().stream().noneMatch(i -> i instanceof Menu), "Contextual real con un máximo de siete acciones y sin cascadas");
            c.guardarButton.fire();
            comprobar(c.socios.size() == 4 && c.aviso.getText().contains("Falta el nombre"), "Alta vacía bloqueada con mensaje útil");
            captura(root, carpeta, "02-error-validacion");
            c.nombre.setText(String.join("", Collections.nCopies(81, "A"))); c.apellidos.setText("Gómez Ruiz");
            c.guardar();
            comprobar(c.socios.size() == 4 && c.aviso.getText().contains("80")
                && root.getScene().getFocusOwner() == c.nombre && c.nombre.getStyleClass().contains("invalid"),
                "Nombre demasiado largo bloqueado con aviso y foco en Nombre");
            c.nombre.setText("Laura"); c.apellidos.setText(String.join("", Collections.nCopies(121, "A")));
            c.guardar();
            comprobar(c.socios.size() == 4 && c.aviso.getText().contains("120")
                && root.getScene().getFocusOwner() == c.apellidos && c.apellidos.getStyleClass().contains("invalid"),
                "Apellidos demasiado largos bloqueados con aviso y foco en Apellidos");
            c.nombre.setText("Laura"); c.apellidos.setText("Gómez Ruiz");
            c.nacimiento.getEditor().setText("31/02/2003");
            c.guardar(); comprobar(c.aviso.getText().contains("fecha real"), "Rechazo de fecha imposible");
            c.nacimiento.getEditor().setText(Validacion.FECHA.format(LocalDate.now().plusDays(1)));
            c.guardar(); comprobar(c.aviso.getText().contains("futura"), "Rechazo de nacimiento futuro");
            c.nacimiento.setValue(LocalDate.of(2003,5,14)); c.nacimiento.getEditor().setText("14/05/2003");
            c.guardar(); comprobar(c.aviso.getText().contains("provincia"), "Provincia obligatoria");
            c.provincia.setValue("Sevilla"); c.guardar(); comprobar(c.aviso.getText().contains("cuota"), "Cuota obligatoria");
            for (Toggle t : c.cuotaGrupo.getToggles()) if (((RadioButton)t).getText().equals("Joven")) t.setSelected(true);
            ((CheckBox)c.clasesBox.getChildren().get(0)).setSelected(true);
            c.iban.setText("ES0000000000000000000000"); c.guardar();
            comprobar(c.aviso.getText().contains("IBAN"), "IBAN incorrecto bloqueado");
            comprobar(Validacion.ibanValido("ES91 2100 0418 4502 0005 1332"), "IBAN de prueba con checksum válido aceptado");
            comprobar(!Validacion.ibanValido("ES91 2100 0418 4502 0005 1333"), "Checksum alterado rechazado");
            c.iban.clear(); c.guardarButton.fire();
            comprobar(c.resumen.getText().contains("Laura Gómez Ruiz") && c.resumen.getText().contains("Spinning") && c.progreso.getText().startsWith("5 de 5"), "Resumen del alta y progreso sincronizados con los campos");
            comprobar(c.socios.size() == 5, "Alta de Laura guardada desde el botón real");
            Socio laura = c.socios.get(4);
            comprobar(laura.nombreCompleto().equals("Laura Gómez Ruiz") && laura.provincia.equals("Sevilla") && laura.cuota.equals("Joven") && laura.clases.equals(Arrays.asList("Spinning")) && laura.nacimiento.equals(LocalDate.of(2003,5,14)), "Datos completos de la tarea de Laura");
            comprobar(new Repositorio(datos).cargar().size() == 5, "Persistencia en disco y recarga");
            captura(root, carpeta, "03-alta-guardada");
            c.busqueda.setText("gomez"); comprobar(c.tabla.getItems().size() == 1, "Búsqueda ignora mayúsculas y tildes");
            c.busqueda.setText("zzzz"); comprobar(c.tabla.getItems().isEmpty(), "Búsqueda sin coincidencias");
            c.busqueda.clear(); c.nuevo();
            c.nombre.setText("Laura"); c.apellidos.setText("Gómez Ruiz"); c.nacimiento.getEditor().setText("14/05/2003"); c.provincia.setValue("Sevilla");
            c.cuotaGrupo.selectToggle(c.cuotaGrupo.getToggles().get(3)); c.guardar();
            comprobar(c.socios.size() == 5 && c.aviso.getText().contains("Ya hay una ficha"), "Duplicado de nombre y nacimiento bloqueado");
            c.confirmar = a -> false; c.limpiar(); comprobar(c.nombre.getText().equals("Laura"), "Cancelar limpieza conserva el formulario");
            c.confirmar = a -> true; c.limpiar(); comprobar(c.nombre.getText().isEmpty() && c.socios.size() == 5, "Confirmar limpieza vacía solo el formulario");
            c.tabla.getSelectionModel().select(laura); c.editar(); c.apellidos.setText("Gómez Ruiz Prueba"); c.guardar();
            comprobar(c.socios.size() == 5 && c.socios.get(4).apellidos.endsWith("Prueba"), "Edición modifica la ficha sin duplicarla");
            c.deshacer(); comprobar(c.socios.get(4).apellidos.equals("Gómez Ruiz"), "Deshacer restaura la edición en memoria y disco");
            laura = c.socios.get(4); c.tabla.getSelectionModel().select(laura);
            c.confirmar = a -> false; c.baja(); comprobar(c.socios.get(4).activo, "Cancelar baja mantiene estado activo");
            c.confirmar = a -> true; c.baja(); comprobar(!c.socios.get(4).activo, "Baja conserva la ficha con estado Baja");
            c.reactivar(); comprobar(c.socios.get(4).activo, "Reactivación recupera estado activo");
            c.confirmar = a -> false; c.eliminar(); comprobar(c.socios.size() == 5, "Cancelar eliminación conserva la ficha");
            c.confirmar = a -> true; c.eliminar(); comprobar(c.socios.size() == 4, "Confirmar eliminación borra solo la ficha elegida");
            c.deshacer(); comprobar(c.socios.size() == 5 && new Repositorio(datos).cargar().size() == 5, "Deshacer eliminación recupera la ficha en disco");
            c.nombre.setText("Pendiente"); c.confirmar = a -> false;
            comprobar(!c.permitirSalir(), "Salida con cambios pendientes puede cancelarse");
            c.confirmar = a -> true; c.nuevo();
            comprobar(c.nuevoMenu.getAccelerator() != null && c.guardarMenu.getAccelerator() != null && c.buscarMenu.getAccelerator() != null && c.deshacerMenu.getAccelerator() != null, "Atajos registrados en MenuItem");
            Path corrupto = carpeta.resolve("corrupto.xml"); Files.write(corrupto, "archivo incompleto".getBytes(StandardCharsets.UTF_8));
            try { new Repositorio(corrupto).cargar(); throw new AssertionError("Se aceptó archivo corrupto"); }
            catch (java.io.IOException esperado) { log.add("OK - Archivo corrupto rechazado sin sobrescribir"); }
            Path carpetaNoArchivo = carpeta.resolve("no-es-archivo"); Files.createDirectories(carpetaNoArchivo);
            Repositorio bueno = c.repositorio; c.repositorio = new Repositorio(carpetaNoArchivo);
            int antes = c.socios.size(); c.tabla.getSelectionModel().select(c.socios.get(0)); c.eliminar();
            comprobar(c.socios.size() == antes && c.aviso.getText().contains("No se han guardado"), "Fallo de escritura mantiene los datos en memoria");
            c.repositorio = bueno;
            c.confirmar = a -> a.showAndWait().filter(b -> b.getButtonData() == ButtonBar.ButtonData.OK_DONE).isPresent();
            c.nuevo();
            comprobar(root.getBoundsInLocal().getWidth() >= 1200, "Pantalla de escritorio renderizada a tamaño completo");
            log.add("TOTAL: " + (log.size()) + " comprobaciones superadas");
        } catch (Throwable ex) { codigo = 1; log.add("ERROR - " + ex); ex.printStackTrace(); }
        try { Files.write(carpeta.resolve("verificacion.txt"), log, StandardCharsets.UTF_8); } catch (Exception ex) { ex.printStackTrace(); codigo = 1; }
        for (String linea : log) System.out.println(linea);
        Platform.exit();
        System.exit(codigo);
    }
}
