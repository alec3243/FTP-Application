package application;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * This class creates a JavaFX GUI that is used to download files off of a
 * TCPServer object. This class connects to the server by creating a TCPClient
 * object and executing it using a Thread.
 * 
 * At no time does this class instantiate a TCPServer object; the server must be
 * executed independently of this class.
 * 
 * @author Alec J Strickland
 *
 */
public class FtpApplication extends Application {
	private TCPClient client;
	private boolean invalidInputLabelExists;
	private boolean timeoutLabelExists;

	public FtpApplication() {
		client = null;
		invalidInputLabelExists = false;
		timeoutLabelExists = false;
	}

	public void startApplication() {
		launch();
	}

	/**
	 * Starts the first window for the application. The user is prompted for an
	 * IP address and a port number that will be used to connect to a server.
	 */
	@Override
	public void start(Stage arg0) throws Exception {
		Stage stage = arg0;
		stage.setTitle("FTP Application");
		stage.setWidth(400);
		stage.setHeight(220);
		final GridPane grid = new GridPane();
		grid.setAlignment(Pos.TOP_LEFT);
		grid.setHgap(10);
		grid.setVgap(20);
		grid.setPadding(new Insets(10, 10, 10, 10));
		final VBox vbox = new VBox();
		// Used to get the IP address and port number that the client wants to
		// connect to.
		final Label ipLabel = new Label("IP Address:");
		final Label portLabel = new Label("Port Number:");
		final TextField ipField = new TextField();
		final Button enter = new Button("Enter");

		final TextField portField = new TextField();
		final VBox vbox2 = new VBox();
		vbox.getChildren().addAll(ipLabel, ipField);
		vbox2.getChildren().addAll(portLabel, portField);
		grid.add(vbox, 0, 0);
		grid.add(vbox2, 0, 1);
		grid.add(enter, 2, 0);
		final Label invalidInputLabel = new Label("Invalid Input.");
		invalidInputLabel.setStyle("-fx-text-fill: red;");
		final Label timeoutLabel = new Label("Connection timed out");

		timeoutLabel.setStyle("-fx-text-fill: red;");
		enter.setOnAction((event) -> {
			final String portFieldText = portField.getText();
			final String ipFieldText = ipField.getText();
			if (confirmInput(portFieldText, ipFieldText)) {
				// Once it is known that both values are valid, enter
				// the main screen.
				client = new TCPClient(ipFieldText, Integer.parseInt(portFieldText));
				// Start TCP connection and thread
				Thread t = new Thread(client);
				t.setDaemon(true);
				t.start();
				long start = System.nanoTime();
				long end = 0;
				long totalTime = 0;
				long timeoutTime = 3000000000L; // 3 seconds
				while (!client.isReady()) {
					Thread.yield();
					end = System.nanoTime();
					totalTime = end - start;
					// time-out occurs if handshake takes at least 3 seconds.
					if (totalTime >= timeoutTime) {
						if (invalidInputLabelExists) {
							grid.getChildren().remove(invalidInputLabel);
							invalidInputLabelExists = false;
						}
						if (!timeoutLabelExists) {
							grid.add(timeoutLabel, 2, 1);
							timeoutLabelExists = true;
						}
						break;
					}
				}
				if (client.isReady()) {
					mainScreen(stage);
				}
			} else {
				if (!invalidInputLabelExists) {
					if (timeoutLabelExists) {
						grid.getChildren().remove(timeoutLabel);
						timeoutLabelExists = false;
					}
					grid.add(invalidInputLabel, 2, 1);
					invalidInputLabelExists = true;
				}
			}

		});

		Scene scene = new Scene(grid);
		scene.setOnKeyPressed((event) -> {
			if (event.getCode() == KeyCode.ENTER) {
				enter.fire();
			}
		});
		stage.setScene(scene);
		stage.show();
	}

	/**
	 * Returns true if the user input is valid. Returns false otherwise.
	 * 
	 * @param port
	 *            the target port number
	 * @param IP
	 *            The raw IP Address as a string
	 * @param IPNumbers
	 *            An array of each separate number in the IP address.
	 * @return true if input is valid. False otherwise.
	 */
	private boolean confirmInput(final String portFieldText, final String IP) {
		if (!isNumeric(portFieldText)) {
			return false;
		}
		// Confirm that the provided IP address and port number are both
		// valid.
		final int port = Integer.parseInt(portFieldText);
		int currentNum = 0;
		// If IP == 'localhost', it is valid.
		if (!IP.equals("localhost")) {
			final String[] IPNumbers = IP.split("\\.");
			for (int i = 0; i < IPNumbers.length; i++) {
				if (!isNumeric(IPNumbers[i])) {
					return false;
				}
			}
			// Confirm that each number in the IP address is an 8-bit unsigned
			// integer and that there are 4 numbers in the address.
			for (int i = 0; i < IPNumbers.length; i++) {
				currentNum = Integer.parseInt(IPNumbers[i]);
				if (currentNum >= 255 || currentNum <= 0) {
					return false;
				}
			}
			if (IPNumbers.length != 4) {
				return false;
			}
		}
		// Confirm that the given port is a 16-bit unsigned integer.
		if (port < 0 || port > 65535) {
			return false;
		}
		return true;
	}

	// Returns true if the string is numeric. False otherwise
	private static boolean isNumeric(String str) {
		return str.matches("-?\\d+(\\.\\d+)?"); // match a number with optional
												// '-' and decimal.
	}

	/**
	 * Puts a new scene onto the stage which contains UI used to download files.
	 * This method also handles all interactions with the TCPClient.
	 * 
	 * @param stage
	 *            The original stage for the application.
	 */
	private void mainScreen(Stage stage) {
		final int WIDTH = 350;
		final int HEIGHT = 600;
		final GridPane grid = new GridPane();
		grid.setAlignment(Pos.TOP_LEFT);
		grid.setHgap(10);
		grid.setVgap(20);
		grid.setPadding(new Insets(25, 25, 25, 25));
		Text sceneTitle = new Text("FTP Client");
		final HBox hbox = new HBox();
		hbox.getChildren().add(sceneTitle);
		hbox.setPadding(new Insets(0, 0, 0, 60));
		grid.add(hbox, 0, 0);

		// Retrieve file names from TCPClient thread
		final HashSet<File> filesSet = client.getFiles();
		LinkedList<File> fileList = new LinkedList<>();
		// Add files to list so that they can be sorted
		for (File file : filesSet) {
			fileList.add(file);
		}
		Collections.sort(fileList);
		ObservableList<File> data;

		/*
		 * ~~~~~~~~~~~~~~~~~~~~~~~ Display files on GUI ~~~~~~~~~~~~~~~~~~~~~~~
		 */

		// Establish TableColumn object
		final TableColumn<File, String> fileCol = new TableColumn<>("Files");
		fileCol.setCellValueFactory(new PropertyValueFactory<File, String>("Name"));
		// Establish TableView object that will contain the TableColumn object
		final TableView<File> tableView = new TableView<>();
		tableView.setEditable(false);
		// Force tableView to have 300 width
		tableView.setMinWidth(300);
		tableView.setMaxWidth(300);
		tableView.getColumns().add(fileCol);
		// Fit width of fileCol to the width of tableView.
		fileCol.prefWidthProperty().bind(tableView.widthProperty());
		fileCol.setResizable(false);
		tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		// Add fileList to tableView
		data = FXCollections.observableList(fileList);
		tableView.setItems(data);

		// Establish download button that will download the currently selected
		// file.
		final Button downloadBtn = new Button("Download");
		// Make the button disabled until a file is selected.
		downloadBtn.setDisable(true);
		downloadBtn.setOnAction((event) -> {
			final File selectedFile = tableView.getSelectionModel().getSelectedItem();
			try {
				client.sendInput(selectedFile);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		// Establish exit button
		final Button exitBtn = new Button("Exit");
		exitBtn.setOnAction((event) -> {
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			System.exit(0);
		});
		final HBox hbox2 = new HBox();
		hbox2.getChildren().addAll(downloadBtn, exitBtn);
		hbox2.setSpacing(30);

		// If a file is selected, enable downloadBtn.
		tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
			if (newSelection != null) {
				downloadBtn.setDisable(false);
			}
		});

		// Add tableView to the VBox
		final VBox vbox = new VBox();
		vbox.setSpacing(10);
		vbox.getChildren().add(tableView);
		// add the VBox to the grid
		grid.add(vbox, 0, 0);
		// add hbox2 containing the buttons to the grid
		grid.add(hbox2, 0, 1);
		final Scene scene = new Scene(new Group(), WIDTH, HEIGHT);
		((Group) scene.getRoot()).getChildren().add(grid);
		stage.setHeight(HEIGHT);
		stage.setWidth(WIDTH * 1.07);
		scene.setOnKeyPressed((event) -> {
			if (event.getCode() == KeyCode.ENTER) {
				downloadBtn.fire();
			}
		});
		stage.setScene(scene);
	}
}
