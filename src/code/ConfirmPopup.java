package code;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/**
 * Created by keecha4 on 6/7/2017.
 */
public class ConfirmPopup {


    @FXML
    Button yesButton, noButton;

    String response = "no";

    void initilize(){}

    public void yesButton(){
        System.out.println("YES");
        response="yes";
        closeWindow();
       // handler.onComplete("yes");
    }
    public void noButton() {
        System.out.println("NO");
        response="no";
        closeWindow();
        //handler.onComplete("no");
    }

    public void closeWindow(){
        Stage stage = (Stage) yesButton.getScene().getWindow();
        stage.close();
    }
}
