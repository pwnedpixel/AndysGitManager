package code;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/**
 * Created by keecha4 on 6/7/2017.
 */
public class aboutController {

    @FXML
    Button closeButton;

    public void closeButton(){
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
