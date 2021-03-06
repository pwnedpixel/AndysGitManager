package code;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("layout.fxml"));
        primaryStage.setTitle("Andy's Git Manager");
        primaryStage.setScene(new Scene(root, 1000 , 800));
        setUserAgentStylesheet(STYLESHEET_CASPIAN);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
