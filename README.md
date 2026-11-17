# GymFit Manager

Rediseño de la pantalla de recepción para la tarea 6-7 de Desarrollo de Interfaces, realizado por Marta González González

## Abrir el proyecto

Abre pom.xml en IntelliJ IDEA como proyecto Maven, selecciona un JDK 17 o superior y espera la sincronización. En la ventana Maven ejecuta Plugins > javafx > javafx:run

El proyecto utiliza JavaFX 21.0.2. La pantalla editable está en src/main/resources/es/marta/gymfit/principal.fxml. Se puede abrir en Scene Builder y utiliza club.css desde la misma carpeta

En el ordenador de preparación, iniciar-gymfit.cmd permite ejecutar GymFit-Studio.jar con el runtime JavaFX de Scene Builder o con el Java 8 con JavaFX indicado en el lanzador. En otros equipos se recomienda la ejecución Maven

## Funciones

El formulario y la tabla comparten una pantalla de 1280 por 840. Incluye fecha con calendario, provincias por nombre, cuota exclusiva, clases con casillas, resumen del alta, búsqueda, edición, baja, reactivación, eliminación confirmada y recuperación del último cambio guardado de la sesión

Los datos se guardan localmente en datos/socios.xml. Las fichas iniciales y las cinco clases son de demostración. Utiliza datos de prueba

Archivo, Editar, Socios y Ayuda agrupan las acciones. Los atajos principales son Ctrl+N, Ctrl+S, Ctrl+F, Ctrl+Z y F1. El menú contextual tiene cinco acciones y no contiene cascadas

## Comprobaciones

Se han superado 39 comprobaciones técnicas con JavaFX 26 y con JavaFX 21.0.2 y JDK 21, también sobre el JAR. La aplicación se ha abierto también desde IntelliJ. El registro de compilación aportado muestra BUILD SUCCESS en una ejecución incremental

Para repetir las comprobaciones con un archivo nuevo: mvn javafx:run -Djavafx.args="--verificar --data=target/prueba-nueva/socios.xml"

Las pruebas técnicas no sustituyen el mini test con dos participantes. El informe académico se incorporará cuando contenga las mediciones reales y su conclusión
