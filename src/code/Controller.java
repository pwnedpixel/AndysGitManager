package code;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;

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
    @FXML
    Label currentBranchLabel;

    //Text items to add to the TextFlow window
    ArrayList<Text> itemsToAdd = new ArrayList();
    JFileChooser chooser;
    String gitDirectory;

    //collection of modified and added files to be displayed in the TableView
    final ObservableList<GitFile> data =
            FXCollections.observableArrayList();

    //Creates the columns for the file view
    TableColumn selCol = new TableColumn("Staged");
    TableColumn statusCol = new TableColumn("Status");
    TableColumn pathCol = new TableColumn("Path");


    public Controller(){
        //Associate the correct values with their respective columns
        selCol.setCellValueFactory(new PropertyValueFactory<GitFile, String>("staged"));
        statusCol.setMinWidth(65);
        statusCol.setCellValueFactory(new PropertyValueFactory<GitFile, String>("status"));
        pathCol.setMinWidth(400);
        pathCol.setCellValueFactory(new PropertyValueFactory<GitFile, String>("path"));

    }

    /**
     * Creates a JFileChooser to select the top level directory of a git repository.
     * The TableView is then populated with the modified and new files since last commit.
     */
    public void openButtonPress(){
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
        updateCurrentBranch();

    }

    /**
     * Stages the selected file for commit
     */
    public void addButtonPress(){
        System.out.println("Adding Selected File");
        GitFile selected = filesListView.getSelectionModel().getSelectedItem();
        new CommandThread("git -C "+gitDirectory+" add "+selected.getPath(),args -> {
            populateTable();
        }).start();
    }

    /**
     * de-stages the selected file from being included in the commit.
     */
    public void removeButtonPress(){
        System.out.println("Removing Selected File");
        GitFile selected = filesListView.getSelectionModel().getSelectedItem();
        new CommandThread("git -C "+gitDirectory+" reset "+selected.getPath(),args -> {
            populateTable();
        }).start();
    }

    /**
     * Commits any staged files using the message typed in the commitMessageText field
     */
    public void commitButtonPress(){
        System.out.println("Committing Changes");
        new CommandThread("git -C "+gitDirectory+" commit -m \""+commitMessageText.getText()+"\"",args -> {
            commitMessageText.setText("");
            populateTable();
        }).start();
    }

    /**
     * exits the application
     */
    public void exitButton(){
        Stage stage = (Stage) commitButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Creates a new stage containing a short blurb about the program
     * @throws IOException
     */
    public void aboutButton() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("aboutFXML.fxml"));
        Stage stage = new Stage();
        stage.setScene(new Scene(loader.load()));
        stage.setResizable(false);
        stage.showAndWait();
    }

    /**
     * Pushes any waiting commits after displaying a secondary confirmation window
     * @throws Exception
     */
    public void pushButtonPress() throws Exception {
        System.out.println("Pushing Changes");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("confirmPopup.fxml"));
        Stage stage = new Stage();
        stage.setScene(new Scene(loader.load()));

        //The popups controller
        ConfirmPopup controller = loader.<ConfirmPopup>getController();
        stage.showAndWait();
        if (controller.response.equals("yes")){
            new CommandThread("git -C "+gitDirectory+" push", args -> populateTable()).start();
        }

    }

    /**
     * Amends the last commit, useful for adding more changes or modifying the commit message
     */
    public void amendButtonPress(){
        System.out.println("Committing Changes");
        new CommandThread("git -C "+gitDirectory+" commit --amend -m \""+commitMessageText.getText()+"\"",args -> {
            commitMessageText.setText("");
            populateTable();
        }).start();
    }

    public void updateCurrentBranch(){
        System.out.println("Updating current branch Label");
        new CommandThread("git -C "+gitDirectory+" rev-parse --abbrev-ref HEAD",args -> {
            Platform.runLater(() -> currentBranchLabel.setText("Current Branch: "+args[0]));
        }).start();
    }

    /**
     * Displays the differences between the current version and the last commited version for the currently
     * selected file. additions are marked in green and removals in red.
     */
    public void viewDiff(){
        System.out.println("Viewing Differences");
        itemsToAdd.clear();
        //Clears the currnt view
        diffTextArea.getChildren().clear();

        //Determine the selected file and run "git diff" on it
        GitFile selected = filesListView.getSelectionModel().getSelectedItem();
        new CommandThread("git -C "+gitDirectory+" diff "+selected.getPath(), args -> {
           // System.out.println("HELLO "+args[0]);
            String s = args[0];
            StringBuilder sb = new StringBuilder();
            //By default the text will be black
            Color currentColour = Color.BLACK;
            boolean newLine = false;
            boolean useBackground = false;
            //Loop through each character to find the "+", "-" and "\n"
            //A + or - after a new line means that the line will be coloured either red or green
            //Once the line has ended, the colour is reset to black
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
            //Once the lines have all be added to the itemsToAdd list with the correct colours,
            // they are displayed in the Text area.
            Platform.runLater(() -> diffTextArea.getChildren().addAll(itemsToAdd));

        }).start();
    }

    /**
     * Used to populate the TableView with modified and added files, which can be selected.
     */
    public void populateTable(){

        new CommandThread("git -C "+gitDirectory + " status", args -> {
            //Split up the response into individual words
            String[] statusReturn = args[0].split("\\s+");
            boolean nextIsFile = false;
            String currentStatus = "";
            int isStaged = 0;
            //clear the current date
            data.clear();

            //loop through each segment and determine when the next segment is a file path,
            //  as well as if it is staged/not or modified/added
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

    /**
     * Refreshes the content in the TableView and the difference viewer.
     */
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

    /**
     * Replaces excape characters with their text equivalent
     * @param s the string the parse
     * @return the escapeless string
     */
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

    /**
     * A git file object contains three properties: Staged, path and status.
     * Staged signifies whether or not a file is staged for commit.
     * Path is the path to the file from the top level folder of the repository.
     * Status signifies if a file has been modified or newly added.
     * Each property has a get and set function.
     */
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
