package es.marta.gymfit;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.*;
import javafx.collections.transformation.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

public class ClubController {
    @FXML TextField nombre, apellidos, iban, busqueda;
    @FXML DatePicker nacimiento;
    @FXML ComboBox<String> provincia;
    @FXML ToggleGroup sexoGrupo, cuotaGrupo;
    @FXML HBox clasesBox;
    @FXML TableView<Socio> tabla;
    @FXML TableColumn<Socio,String> colNombre, colCuota, colEstado;
    @FXML Label aviso, estado, contador, resultados, tituloFormulario, resumen, progreso;
    @FXML Label etiquetanombre, etiquetaapellidos, etiquetanacimiento, etiquetaprovincia, etiquetaiban;
    @FXML MenuBar menuBar;
    @FXML MenuItem nuevoMenu, guardarMenu, deshacerMenu, buscarMenu, ayudaMenu;
    @FXML Button guardarButton, deshacerButton;
    final ObservableList<Socio> socios = FXCollections.observableArrayList();
    Repositorio repositorio;
    private List<Socio> anterior;
    private Socio editando;
    private boolean cargando, bloqueado;
    private String inicial;
    Predicate<Alert> confirmar = a -> a.showAndWait().filter(b -> b.getButtonData() == ButtonBar.ButtonData.OK_DONE).isPresent();

    @FXML public void initialize() {
        etiquetanombre.setLabelFor(nombre);
        etiquetaapellidos.setLabelFor(apellidos);
        etiquetanacimiento.setLabelFor(nacimiento);
        etiquetaprovincia.setLabelFor(provincia);
        etiquetaiban.setLabelFor(iban);
        provincia.getItems().addAll("A Coruña", "Álava", "Albacete", "Alicante", "Almería", "Asturias", "Ávila", "Badajoz", "Barcelona", "Bizkaia", "Burgos", "Cáceres", "Cádiz", "Cantabria", "Castellón", "Ceuta", "Ciudad Real", "Córdoba", "Cuenca", "Gipuzkoa", "Girona", "Granada", "Guadalajara", "Huelva", "Huesca", "Illes Balears", "Jaén", "La Rioja", "Las Palmas", "León", "Lleida", "Lugo", "Madrid", "Málaga", "Melilla", "Murcia", "Navarra", "Ourense", "Palencia", "Pontevedra", "Salamanca", "Santa Cruz de Tenerife", "Segovia", "Sevilla", "Soria", "Tarragona", "Teruel", "Toledo", "Valencia", "Valladolid", "Zamora", "Zaragoza");
        provincia.setVisibleRowCount(8);
        provincia.setOnKeyTyped(e -> {
            String letra = normalizar(e.getCharacter());
            if (letra.length() == 1 && Character.isLetter(letra.charAt(0)))
                provincia.getItems().stream().filter(p -> normalizar(p).startsWith(letra)).findFirst().ifPresent(provincia::setValue);
        });
        nacimiento.setConverter(new StringConverter<LocalDate>() {
            public String toString(LocalDate fecha) { return fecha == null ? "" : Validacion.FECHA.format(fecha); }
            public LocalDate fromString(String texto) {
                if (texto == null || texto.trim().isEmpty()) return null;
                try { return Validacion.fecha(texto); }
                catch (RuntimeException ex) { mensaje("Revisa la fecha: usa dd/mm/aaaa, por ejemplo 14/05/2003", true, nacimiento); return null; }
            }
        });
        nacimiento.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate fecha, boolean vacia) {
                super.updateItem(fecha, vacia);
                setDisable(vacia || fecha.isAfter(LocalDate.now()));
            }
        });
        colNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombreCompleto()));
        colCuota.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().cuota));
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().activo ? "Activo" : "Baja"));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        FilteredList<Socio> filtrados = new FilteredList<>(socios);
        SortedList<Socio> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(ordenados);
        busqueda.textProperty().addListener((o,v,n) -> {
            String consulta = normalizar(n.trim());
            filtrados.setPredicate(s -> normalizar(s.nombreCompleto()).contains(consulta));
            actualizarContadores();
        });
        tabla.setRowFactory(t -> {
            TableRow<Socio> fila = new TableRow<>();
            ContextMenu menu = new ContextMenu();
            MenuItem editar = accion("Editar ficha", this::editar);
            MenuItem copiar = accion("Copiar nombre", this::copiar);
            MenuItem baja = accion("Dar de baja…", this::baja);
            MenuItem reactivar = accion("Reactivar socio", this::reactivar);
            MenuItem eliminar = accion("Eliminar ficha…", this::eliminar);
            menu.getItems().addAll(editar, copiar, new SeparatorMenuItem(), baja, reactivar, new SeparatorMenuItem(), eliminar);
            fila.emptyProperty().addListener((o,a,v) -> fila.setContextMenu(v ? null : menu));
            menu.setOnShowing(e -> {
                tabla.getSelectionModel().select(fila.getItem());
                baja.setDisable(!fila.getItem().activo);
                reactivar.setDisable(fila.getItem().activo);
            });
            fila.setOnMouseClicked(e -> { if (!fila.isEmpty() && e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) editar(); });
            return fila;
        });
        nuevoMenu.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));
        guardarMenu.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
        buscarMenu.setAccelerator(KeyCombination.keyCombination("Ctrl+F"));
        deshacerMenu.setAccelerator(KeyCombination.keyCombination("Ctrl+Z"));
        ayudaMenu.setAccelerator(KeyCombination.keyCombination("F1"));
        tabla.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.DELETE) { eliminar(); e.consume(); }
            else if (e.getCode() == KeyCode.ENTER) { editar(); e.consume(); }
        });
        inicial = firma();
        deshacerButton.setDisable(true); deshacerMenu.setDisable(true);
        for (TextField campo : Arrays.asList(nombre, apellidos, iban, nacimiento.getEditor()))
            campo.textProperty().addListener((o,a,n) -> actualizarResumen());
        provincia.valueProperty().addListener((o,a,n) -> actualizarResumen());
        cuotaGrupo.selectedToggleProperty().addListener((o,a,n) -> actualizarResumen());
        clasesBox.getChildren().forEach(n -> ((CheckBox)n).selectedProperty().addListener((o,a,v) -> actualizarResumen()));
        actualizarResumen();
    }

    public void abrir(Repositorio repo) {
        repositorio = repo;
        try { socios.setAll(repo.cargar()); actualizarContadores(); }
        catch (IOException ex) {
            bloqueado = true;
            guardarButton.setDisable(true);
            mensaje("No se ha podido abrir el archivo de socios. Conserva una copia y revisa su formato o permisos antes de reiniciar", true, null);
            estado.setText("Archivo sin cargar. Guardado bloqueado para proteger los datos");
        }
    }

    private MenuItem accion(String texto, Runnable tarea) { MenuItem item = new MenuItem(texto); item.setOnAction(e -> tarea.run()); return item; }
    static String normalizar(String s) { return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT); }
    private String seleccionado(ToggleGroup grupo) { return grupo.getSelectedToggle() == null ? "" : ((RadioButton)grupo.getSelectedToggle()).getText(); }
    private void seleccionar(ToggleGroup grupo, String texto) { grupo.getToggles().forEach(t -> t.setSelected(((RadioButton)t).getText().equals(texto))); }
    List<String> clases() {
        List<String> elegidas = new ArrayList<>();
        for (Node nodo : clasesBox.getChildren()) if (((CheckBox)nodo).isSelected()) elegidas.add(((CheckBox)nodo).getText());
        return elegidas;
    }
    private String firma() {
        return String.join("|", nombre.getText(), apellidos.getText(), nacimiento.getEditor().getText(), String.valueOf(provincia.getValue()), seleccionado(sexoGrupo), seleccionado(cuotaGrupo), clases().toString(), iban.getText());
    }
    private boolean pendiente() { return !cargando && !firma().equals(inicial); }
    boolean permitirSalir() {
        return !pendiente() || pregunta("Descartar cambios sin guardar", "Los cambios del formulario todavía no se han guardado. Puedes volver al formulario para guardarlos", "Descartar cambios");
    }

    private boolean pregunta(String titulo, String texto, String accion) {
        Alert dialogo = new Alert(Alert.AlertType.CONFIRMATION);
        dialogo.initOwner(tabla.getScene().getWindow()); dialogo.setTitle("GymFit Club");
        dialogo.setHeaderText(titulo); dialogo.setContentText(texto);
        ButtonType aceptar = new ButtonType(accion, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Volver", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getButtonTypes().setAll(aceptar, cancelar);
        dialogo.getDialogPane().getStylesheets().add(getClass().getResource("club.css").toExternalForm());
        ((Button)dialogo.getDialogPane().lookupButton(aceptar)).setDefaultButton(false);
        ((Button)dialogo.getDialogPane().lookupButton(cancelar)).setDefaultButton(true);
        return confirmar.test(dialogo);
    }

    @FXML public void guardar() {
        if (bloqueado) { mensaje("Guardado bloqueado: revisa el archivo de socios y reinicia la aplicación", true, null); return; }
        limpiarErrores();
        if (nombre.getText().trim().isEmpty()) { mensaje("Falta el nombre. Escríbelo, por ejemplo Marta, para guardar el alta", true, nombre); return; }
        if (apellidos.getText().trim().isEmpty()) { mensaje("Faltan los apellidos. Escríbelos, por ejemplo González González", true, apellidos); return; }
        if (nombre.getText().length() > 80) { mensaje("El nombre supera los 80 caracteres. Acórtalo antes de guardar", true, nombre); return; }
        if (apellidos.getText().length() > 120) { mensaje("Los apellidos superan los 120 caracteres. Acórtalos antes de guardar", true, apellidos); return; }
        LocalDate fecha;
        try { fecha = Validacion.fecha(nacimiento.getEditor().getText()); }
        catch (RuntimeException ex) { mensaje("Revisa la fecha: usa dd/mm/aaaa y una fecha real, por ejemplo 14/05/2003", true, nacimiento); return; }
        if (fecha.isAfter(LocalDate.now())) { mensaje("La fecha de nacimiento es futura. Selecciona hoy o una fecha anterior", true, nacimiento); return; }
        if (provincia.getValue() == null) { mensaje("Falta la provincia. Elige su nombre en la lista, por ejemplo Sevilla", true, provincia); return; }
        if (cuotaGrupo.getSelectedToggle() == null) { mensaje("Falta la cuota. Selecciona una de las cinco opciones antes de guardar", true, null); ((Node)cuotaGrupo.getToggles().get(0)).requestFocus(); return; }
        if (!Validacion.ibanValido(iban.getText())) { mensaje("Revisa el IBAN: debe ser español, tener 24 caracteres y dígitos de control válidos. Puedes dejarlo vacío para completarlo después", true, iban); return; }
        for (Socio s : socios) {
            if ((editando == null || !s.id.equals(editando.id)) && normalizar(s.nombreCompleto()).equals(normalizar(nombre.getText().trim()+" "+apellidos.getText().trim())) && s.nacimiento.equals(fecha)) {
                mensaje("Ya hay una ficha con ese nombre y fecha de nacimiento. Búscala y usa Editar ficha para evitar duplicados", true, nombre); return;
            }
        }
        String id = editando == null ? UUID.randomUUID().toString() : editando.id;
        Socio nuevo = new Socio(id, nombre.getText().trim(), apellidos.getText().trim(), fecha, seleccionado(sexoGrupo), provincia.getValue(), seleccionado(cuotaGrupo), clases(), Validacion.ibanNormalizado(iban.getText()), editando == null || editando.activo);
        List<Socio> cambios = new ArrayList<>(socios);
        if (editando != null) cambios.set(cambios.indexOf(editando), nuevo); else cambios.add(nuevo);
        if (aplicar(cambios, "Guardado: " + nuevo.nombreCompleto())) {
            editando = nuevo; inicial = firma();
            tituloFormulario.setText("01 / Editar datos personales"); guardarButton.setText("Guardar cambios");
            busqueda.clear(); tabla.getSelectionModel().select(nuevo); tabla.scrollTo(nuevo);
            mensaje("Ficha guardada: " + nuevo.nombreCompleto() + ". Ya aparece en la tabla de socios", false, null);
        }
    }

    private boolean aplicar(List<Socio> cambios, String texto) {
        if (bloqueado) return false;
        try {
            repositorio.guardar(cambios);
            anterior = new ArrayList<>(socios); socios.setAll(cambios);
            actualizarContadores(); estado.setText(texto);
            deshacerButton.setDisable(false); deshacerMenu.setDisable(false);
            return true;
        } catch (IOException ex) {
            mensaje("No se han guardado los cambios. Revisa el espacio disponible y los permisos de la carpeta de datos y vuelve a Guardar", true, null);
            estado.setText("Error de escritura. El formulario conserva tus datos"); return false;
        }
    }

    @FXML public void nuevo() { if (permitirSalir()) vaciar(); }
    @FXML public void limpiar() {
        if (pregunta("Limpiar formulario", "Se vaciarán los campos de este formulario. Las fichas guardadas se conservarán", "Limpiar formulario")) vaciar();
    }
    private void vaciar() {
        cargando = true; editando = null;
        nombre.clear(); apellidos.clear(); nacimiento.setValue(null); nacimiento.getEditor().clear(); provincia.setValue(null);
        seleccionar(sexoGrupo, "Prefiero no indicarlo"); cuotaGrupo.selectToggle(null); iban.clear();
        clasesBox.getChildren().forEach(n -> ((CheckBox)n).setSelected(false));
        tituloFormulario.setText("01 / Datos personales"); guardarButton.setText("Guardar socio");
        limpiarErrores(); mensaje("Completa los campos con * y pulsa \"Guardar socio\"", false, null);
        cargando = false; inicial = firma(); nombre.requestFocus();
    }
    @FXML public void editar() {
        Socio s = elegido(); if (s == null || !permitirSalir()) return;
        cargarFormulario(s);
    }
    private void cargarFormulario(Socio s) {
        cargando = true; editando = s;
        nombre.setText(s.nombre); apellidos.setText(s.apellidos); nacimiento.setValue(s.nacimiento);
        nacimiento.getEditor().setText(Validacion.FECHA.format(s.nacimiento));
        provincia.setValue(s.provincia); seleccionar(sexoGrupo, s.sexo); seleccionar(cuotaGrupo, s.cuota);
        clasesBox.getChildren().forEach(n -> ((CheckBox)n).setSelected(s.clases.contains(((CheckBox)n).getText())));
        iban.setText(s.iban); tituloFormulario.setText("01 / Editar datos personales"); guardarButton.setText("Guardar cambios");
        limpiarErrores(); mensaje("Editando " + s.nombreCompleto() + ". Pulsa \"Guardar cambios\" al terminar", false, null);
        cargando = false; inicial = firma(); nombre.requestFocus();
    }
    private Socio elegido() {
        Socio s = tabla.getSelectionModel().getSelectedItem();
        if (s == null) mensaje("Selecciona primero una ficha en la tabla de socios", false, null);
        return s;
    }
    @FXML public void copiar() {
        Socio s = elegido(); if (s == null) return;
        ClipboardContent contenido = new ClipboardContent(); contenido.putString(s.nombreCompleto());
        Clipboard.getSystemClipboard().setContent(contenido); estado.setText("Nombre copiado");
    }
    @FXML public void baja() { cambiarEstado(false); }
    @FXML public void reactivar() { cambiarEstado(true); }
    private void cambiarEstado(boolean activo) {
        Socio s = elegido(); if (s == null || s.activo == activo) return;
        if (editando != null && s.id.equals(editando.id) && !permitirSalir()) return;
        if (!activo && !pregunta("Dar de baja a " + s.nombreCompleto(), "La ficha seguirá en la tabla con estado Baja. Podrás reactivarla o deshacer este cambio", "Dar de baja")) return;
        List<Socio> cambios = new ArrayList<>(socios); Socio actualizado = s.conEstado(activo);
        cambios.set(cambios.indexOf(s), actualizado);
        if (aplicar(cambios, (activo ? "Socio reactivado: " : "Socio de baja: ") + s.nombreCompleto())) {
            if (editando != null && s.id.equals(editando.id)) cargarFormulario(actualizado);
            tabla.getSelectionModel().select(actualizado);
        }
    }
    @FXML public void eliminar() {
        Socio s = elegido(); if (s == null) return;
        if (editando != null && s.id.equals(editando.id) && !permitirSalir()) return;
        if (!pregunta("Eliminar la ficha de " + s.nombreCompleto(), "Se borrará la ficha guardada. Puedes recuperarla con Deshacer antes del siguiente cambio o de cerrar la aplicación", "Eliminar ficha")) return;
        List<Socio> cambios = new ArrayList<>(socios); cambios.remove(s);
        if (aplicar(cambios, "Ficha eliminada: " + s.nombreCompleto())) {
            if (editando != null && s.id.equals(editando.id)) vaciar();
            mensaje("Ficha eliminada. Usa Deshacer para recuperarla", false, null);
        }
    }
    @FXML public void deshacer() {
        if (anterior == null || !permitirSalir()) return;
        List<Socio> recuperar = new ArrayList<>(anterior);
        if (aplicar(recuperar, "Último cambio deshecho")) {
            anterior = null; deshacerButton.setDisable(true); deshacerMenu.setDisable(true); vaciar();
            mensaje("Último cambio deshecho y guardado. La tabla muestra los datos recuperados", false, null);
        }
    }
    @FXML public void buscar() { busqueda.requestFocus(); busqueda.selectAll(); }
    @FXML public void salir() { if (permitirSalir()) Platform.exit(); }
    @FXML public void ayuda() {
        informar("Ayuda de GymFit", "1. Escribe el nombre y los apellidos. Elige fecha, provincia y cuota.\n2. Marca las clases que quiera el socio. Sexo e IBAN son opcionales.\n3. Pulsa \"Guardar socio\". La ficha aparecerá en la tabla.\n\nCtrl+N: nueva alta / Ctrl+S: guardar / Ctrl+F: buscar / Ctrl+Z: deshacer último cambio guardado / F1: ayuda\nEn la tabla: Intro edita y Supr elimina con confirmación.\n\nLas provincias se pueden recorrer con el teclado o saltar por inicial. El calendario admite escritura dd/mm/aaaa.\nDeshacer conserva solo el último cambio guardado de esta sesión.\n\nPrototipo académico: usa datos de prueba. Almacenamiento local sin cifrar en:\n" + repositorio.ruta().toAbsolutePath());
    }
    @FXML public void guion() {
        informar("Test de usabilidad: tarea", "Da de alta a Laura Gómez Ruiz, nacida el 14/05/2003, de Sevilla, con cuota Joven y la clase de Spinning.\n\nEl observador cronometra sin ayudar y anota si se completa, el tiempo, errores y dudas. Al terminar pregunta la satisfacción de 1 a 5.\n\nAntes del segundo participante elimina la ficha de Laura y vuelve al formulario vacío. No precargues la solución");
    }
    private void informar(String titulo, String texto) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.initOwner(tabla.getScene().getWindow());
        a.setTitle("GymFit Club"); a.setHeaderText(titulo); a.setContentText(texto);
        a.getDialogPane().setPrefWidth(580); a.showAndWait();
    }
    private void actualizarContadores() {
        long activos = socios.stream().filter(s -> s.activo).count();
        contador.setText(socios.size() + " fichas / " + activos + " socios activos");
        resultados.setText(tabla.getItems().size() + " de " + socios.size() + " fichas");
    }
    private void actualizarResumen() {
        String persona = (nombre.getText().trim() + " " + apellidos.getText().trim()).trim();
        String cuota = seleccionado(cuotaGrupo);
        List<String> elegidas = clases();
        boolean fechaValida;
        try { fechaValida = !Validacion.fecha(nacimiento.getEditor().getText()).isAfter(LocalDate.now()); }
        catch (RuntimeException ex) { fechaValida = false; }
        int completos = (nombre.getText().trim().isEmpty() ? 0 : 1) + (apellidos.getText().trim().isEmpty() ? 0 : 1)
            + (fechaValida ? 1 : 0) + (provincia.getValue() == null ? 0 : 1) + (cuota.isEmpty() ? 0 : 1);
        resumen.setText((persona.isEmpty() ? "Nombre y apellidos pendientes" : persona) + "\n"
            + (fechaValida ? nacimiento.getEditor().getText() : "Fecha de nacimiento pendiente") + "\n"
            + (provincia.getValue() == null ? "Provincia pendiente" : provincia.getValue()) + "\n"
            + (cuota.isEmpty() ? "Cuota pendiente" : "Cuota " + cuota) + " / "
            + (elegidas.isEmpty() ? "Sin clases" : String.join(", ", elegidas)));
        progreso.setText(completos + " de 5 datos obligatorios" + (completos == 5 ? " / Listo para guardar" : ""));
    }
    private void limpiarErrores() {
        for (Node n : Arrays.asList(nombre, apellidos, nacimiento, provincia, iban)) n.getStyleClass().remove("invalid");
    }
    private void mensaje(String texto, boolean error, Node campo) {
        aviso.setText(texto); aviso.getStyleClass().remove("error");
        if (error) aviso.getStyleClass().add("error");
        aviso.setAccessibleText(texto);
        if (campo != null) { campo.getStyleClass().add("invalid"); campo.requestFocus(); }
    }
}
