package code;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.awt.Color.BLACK;
import static java.awt.SystemColor.text;

public class Controller {

    @FXML
    Button commitButton, amendButton;
    @FXML
    AnchorPane topPane;
    @FXML
    TableView<GitFile> filesListView;
    @FXML
    TextArea commitMessageText;
    @FXML
    TextFlow diffTextArea;
    @FXML
    MenuItem pushButton;


    ArrayList<Text> itemsToAdd = new ArrayList();
    JFileChooser chooser;
    String gitDirectory;
    //Setup the table

    final ObservableList<GitFile> data =
            FXCollections.observableArrayList();

    TableColumn selCol = new TableColumn("Staged");
    TableColumn statusCol = new TableColumn("Status");
    TableColumn pathCol = new TableColumn("Path");



    public Controller(){
        selCol.setCellValueFactory(new PropertyValueFactory<GitFile, String>("staged"));
        statusCol.setMinWidth(65);
        statusCol.setCellValueFactory(new PropertyValueFactory<GitFile, String>("status"));
        pathCol.setMinWidth(400);
        pathCol.setCellValueFactory(new PropertyValueFactory<GitFile, String>("path"));

    }

    public void openButtonPress(Event e){
        System.out.println("Choose a folder");
        chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Select Repository Top-Level Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(chooser) == JFileChooser.APPROVE_OPTION){
            System.out.println("Selected Directory: "+chooser.getSelectedFile());
            gitDirectory = chooser.getSelectedFile().toString();
        } else {
            System.out.println("no directory selected");
            gitDirectory = "";
        }
        filesListView.setItems(data);
        if (filesListView.getColumns().size()==0) {
            filesListView.getColumns().addAll(selCol, statusCol, pathCol);
        }
        populateTable();

    }

    public void addButtonPress(){
        System.out.println("Adding Selected File");
        GitFile selected = filesListView.getSelectionModel().getSelectedItem();
        new CommandThread("git -C "+gitDirectory+" add "+selected.getPath(),args -> {
            populateTable();
        }).start();
    }

    public void removeButtonPress(){
        System.out.println("Removing Selected File");
        GitFile selected = filesListView.getSelectionModel().getSelectedItem();
        new CommandThread("git -C "+gitDirectory+" reset "+selected.getPath(),args -> {
            populateTable();
        }).start();
    }

    public void commitButtonPress(){
        System.out.println("Committing Changes");
        new CommandThread("git -C "+gitDirectory+" commit -m \""+commitMessageText.getText()+"\"",args -> {
            commitMessageText.setText("");
            populateTable();
        }).start();
    }

    public void pushButtonPress(Event e) throws Exception {
        System.out.println("Pushing Changes");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("confirmPopup.fxml"));
        Stage stage = new Stage();
        stage.setScene(new Scene(loader.load()));
        ConfirmPopup controller = loader.<ConfirmPopup>getController();

        stage.showAndWait();
        if (controller.response.equals("yes")){
            new CommandThread("git -C "+gitDirectory+" push", args -> populateTable()).start();
        }
        System.out.println("WE BACK");

    }

    public void amendButtonPress(){
        System.out.println("Committing Changes");
        new CommandThread("git -C "+gitDirectory+" commit --amend -m \""+commitMessageText.getText()+"\"",args -> {
            commitMessageText.setText("");
            populateTable();
        }).start();
    }

    public void viewDiff(){
        System.out.println("Viewing Differences");
        itemsToAdd.clear();
        diffTextArea.getChildren().clear();

        GitFile selected = filesListView.getSelectionModel().getSelectedItem();
        new CommandThread("git -C "+gitDirectory+" diff "+selected.getPath(), args -> {
           // System.out.println("HELLO "+args[0]);
            String s = args[0];
            StringBuilder sb = new StringBuilder();
            Color currentColour = Color.BLACK;
            boolean newLine = false;
            boolean useBackground = false;
            for (int i=0; i<s.length(); i++) {
                switch (s.charAt(i)) {
                    case '+':
                        if (newLine)
                            currentColour = Color.GREEN;
                        useBackground = true;
                        break;
                    case '-':
                        if (newLine)
                            currentColour = Color.RED;
                        useBackground = true;
                        break;
                    case '\n':
                        currentColour = Color.BLACK;
                        useBackground = false;
                        newLine = true;
                        break;
                    case ' ':
                        newLine=false;
                        break;
                    // ... rest of escape characters
                    default:
                        break;

                }
                Text text = new Text(s.charAt(i)+"");
                text.setFill(currentColour);
                itemsToAdd.add(text);
            }
            Platform.runLater(() -> diffTextArea.getChildren().addAll(itemsToAdd));

        }).start();
    }

    public void populateTable(){

        new CommandThread("git -C "+gitDirectory + " status", args -> {
            String[] statusReturn = args[0].split("\\s+");
            boolean nextIsFile = false;
            String currentStatus = "";
            int isStaged = 0;
            data.clear();

            for (String segment : statusReturn){
                if (nextIsFile && isStaged ==0){
                    data.add(new GitFile("true",currentStatus, segment));
                }
                else if (nextIsFile && isStaged >= 2){
                    data.add(new GitFile("false",currentStatus,segment));
                }

                //Check if staged or not
                if (segment.equals("not")){
                    isStaged++;
                } else if (segment.equals("staged") && isStaged == 1){
                    isStaged = 2;
                    data.add(new GitFile("","",""));
                } else if (isStaged != 2){
                    isStaged = 0;
                }

                nextIsFile = (segment.contains("modified") || segment.contains("file:"))?true:false;
                if (segment.contains("file:")){
                    currentStatus = "new file";
                } else if (segment.contains("modified:")){
                    currentStatus = "modified";
                }
            }
        }).start();
    }

    public void refreshButtonPress(){
        System.out.println("Refreshing View");
        populateTable();
        diffTextArea.getChildren().addAll(itemsToAdd);
        itemsToAdd.clear();
    }

    public void commitButtonMouseOver(){
        commitButton.setOpacity(1);
    }
    public void commitButtonMouseOff(){
        commitButton.setOpacity(0.25);
    }

    public void amendButtonMouseOver(){
        amendButton.setOpacity(1);
    }
    public void amendButtonMouseOff(){
        amendButton.setOpacity(0.25);
    }

    public static String unEscapeString(String s){
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<s.length(); i++)
            switch (s.charAt(i)){
                case '\n': sb.append("\\n"); break;
                case '\t': sb.append("\\t"); break;
                // ... rest of escape characters
                default: sb.append(s.charAt(i));
            }
        return sb.toString();
    }


    public static class GitFile {
        private final SimpleStringProperty staged;
        private final SimpleStringProperty path;
        private final SimpleStringProperty status;

        private GitFile(String _staged, String _status, String _path) {
            this.staged = new SimpleStringProperty(_staged);
            this.status = new SimpleStringProperty(_status);
            this.path = new SimpleStringProperty(_path);
        }

        public String getStaged() {
            return staged.get();
        }
        public void setStaged(String _staged) {
            staged.set(_staged);
        }

        public String getStatus(){
            return status.get();
        }

        public void setStatus(String _status){
            status.set(_status);
        }

        public String getPath() {
            return path.get();
        }
        public void setPath(String _path) {
            path.set(_path);
        }

    }

}
