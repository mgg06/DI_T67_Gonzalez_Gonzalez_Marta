package es.marta.gymfit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.nio.file.*;

public class GymFitApp extends Application {
    @Override public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("principal.fxml"));
        Parent root = loader.load();
        ClubController controller = loader.getController();
        Path datos = Paths.get(getParameters().getNamed().getOrDefault("data", "datos/socios.xml"));
        controller.abrir(new Repositorio(datos));
        Scene scene = new Scene(root, 1280, 840);
        stage.setTitle("GymFit Club | Recepción"); stage.setScene(scene);
        stage.setMinWidth(1200); stage.setMinHeight(820);
        stage.setOnCloseRequest(e -> { if (!controller.permitirSalir()) e.consume(); });
        stage.show();
        if (getParameters().getUnnamed().contains("--verificar"))
            Platform.runLater(() -> Comprobacion.ejecutar(stage, root, controller, datos));
    }
}
