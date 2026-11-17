# GymFit Manager

Marta González González | 2º DAM | Desarrollo de Interfaces

Para esta práctica he rediseñado la pantalla de altas de GymFit Manager, una aplicación de escritorio para la recepción de un gimnasio. La pantalla original tenía menús poco claros, campos que obligaban a recordar códigos y un botón de borrado que destacaba más que el de guardar

He mantenido el formulario y el listado de socios en una misma ventana. La idea es poder completar un alta, revisar sus datos y comprobar que se ha guardado sin tener que ir cambiando de pantalla

## Cambios de la pantalla

- Los datos tienen etiquetas completas y ejemplos. La fecha se puede escribir o elegir en un calendario y la provincia se selecciona por su nombre
- Las cuotas permiten una sola elección. Las clases tienen casillas para poder marcar varias
- El resumen se actualiza mientras se rellena el formulario. Guardar es la acción principal y Vaciar campos pide confirmación
- La tabla permite buscar, editar, dar de baja, reactivar y eliminar una ficha. El menú contextual tiene cinco acciones, sin submenús
- Deshacer recupera el último cambio guardado de la sesión, también en el archivo de datos

Antes de guardar se comprueban los campos obligatorios, las fechas, los duplicados y el IBAN si se ha escrito. Cuando hay un error, el aviso explica qué revisar y conserva el resto de las entradas

Los menús están organizados en Archivo, Editar, Socios y Ayuda. Se pueden usar Ctrl+N para una nueva alta, Ctrl+S para guardar, Ctrl+F para buscar, Ctrl+Z para deshacer y F1 para consultar la ayuda

## Cómo ejecutar el proyecto

Necesitas un JDK 17 o superior. El proyecto utiliza Maven y JavaFX 21.0.2

1. Abre `pom.xml` en IntelliJ IDEA como proyecto Maven
2. Selecciona el JDK y espera a que termine la sincronización de las dependencias
3. Abre la ventana Maven y ejecuta **Plugins > javafx > javafx:run**

Si tienes Maven instalado, también puedes ejecutarlo desde la carpeta del proyecto:

```bash
mvn javafx:run
```

## Archivos principales

| Archivo | Contenido |
|---|---|
| `principal.fxml` | Distribución de la pantalla y controles |
| `club.css` | Colores, tipografía y estados visuales |
| `ClubController.java` | Acciones del formulario, menús y tabla |
| `Validacion.java` | Comprobaciones de fecha e IBAN |
| `Socio.java` | Datos de cada ficha |
| `Repositorio.java` | Lectura y guardado de los socios |
| `Comprobacion.java` | Pruebas técnicas del funcionamiento |

El FXML y el CSS están en `src/main/resources/es/marta/gymfit`. Las clases Java están en `src/main/java/es/marta/gymfit`. Para editar el diseño, abre `principal.fxml` en Scene Builder

Los socios se guardan en `datos/socios.xml`. Las cuatro fichas iniciales y las cinco clases disponibles son datos de demostración

## Pruebas realizadas

Se han superado 39 comprobaciones técnicas con JavaFX 21.0.2 y JDK 21, y con JavaFX 26. Entre ellas están el alta de Laura Gómez Ruiz, las validaciones, la búsqueda, la edición, las confirmaciones y la recuperación de datos con Deshacer. La aplicación se ha abierto también desde IntelliJ

Para repetir las pruebas con datos separados de los socios de la aplicación:

```bash
mvn javafx:run -Djavafx.args="--verificar --data=target/comprobacion-01/socios.xml"
```

La carpeta de prueba debe ser nueva. Si repites la comprobación, cambia `comprobacion-01` por otro nombre para empezar con los datos iniciales

El mini test con dos personas sigue pendiente. El informe se añadirá cuando estén recogidos sus resultados y la conclusión
