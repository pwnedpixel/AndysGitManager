package code;

import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.cell.PropertyValueFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Controller {

    @FXML
    Button openRepoButton, addButton, removeButton;
    @FXML
    AnchorPane topPane;
    @FXML
    TableView<GitFile> filesListView;


    JFileChooser chooser;
    String gitDirectory;
    CommandThread commander;


    public Controller(){

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
        filesListView.getColumns().clear();
        populateTable();

    }

    public void addButtonPress(){
        System.out.println("Removing Selected File");
        GitFile selected = filesListView.getSelectionModel().getSelectedItem();
        filesListView.getColumns().clear();
        new CommandThread().start("git -C "+gitDirectory+" add "+selected.getPath(),args -> {
            populateTable();
        });
    }

    public void removeButtonPress(){
        System.out.println("Removing Selected File");
        GitFile selected = filesListView.getSelectionModel().getSelectedItem();
        filesListView.getColumns().clear();
        new CommandThread().start("git -C "+gitDirectory+" reset "+selected.getPath(),args -> {
            populateTable();
        });
    }

    public void populateTable(){

        new CommandThread().start("git -C "+gitDirectory + " status", args -> {
            //System.out.println("Handler:" +args[0]);
            String[] statusReturn = args[0].split("\\s+");
            boolean nextIsFile = false;
            String currentStatus = "";
            int isStaged = 0;

            //Setup the table
            final ObservableList<GitFile> data =
                    FXCollections.observableArrayList();

            TableColumn selCol = new TableColumn("Staged");
            selCol.setCellValueFactory(new PropertyValueFactory<GitFile, String>("staged"));
            TableColumn statusCol = new TableColumn("Status");
            statusCol.setMinWidth(65);
            statusCol.setCellValueFactory(new PropertyValueFactory<GitFile, String>("status"));
            TableColumn pathCol = new TableColumn("Path");
            pathCol.setMinWidth(400);
            pathCol.setCellValueFactory(new PropertyValueFactory<GitFile, String>("path"));



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

            filesListView.setItems(data);

            // filesListView.getColumns().removeAll(selCol,statusCol,pathCol);
            filesListView.getColumns().addAll(selCol,statusCol, pathCol);

        });
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
