package dynamictable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.TextFieldTableCell ;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

public class DynamicTable extends Application {

  @Override
  public void start(final Stage primaryStage) {
    final BorderPane root = new BorderPane();
    final TableView<ObservableList<StringProperty>> table = new TableView<>();
    table.setEditable(true);
    final TextField urlTextEntry = new TextField();
    urlTextEntry.setPromptText("Enter URL of tab delimited file");
    final CheckBox headerCheckBox = new CheckBox("Data has header line");
    urlTextEntry.setOnAction(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        populateTable(table, urlTextEntry.getText(),
            headerCheckBox.isSelected());
      }
    });

    final FileChooser fileChooser = new FileChooser();
    
    Button saveButton = new Button("Save...");
    saveButton.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            save(primaryStage, table, headerCheckBox, fileChooser);
        }
    });
    
    HBox controls = new HBox();
    controls.getChildren().addAll(urlTextEntry, headerCheckBox, saveButton);
    HBox.setHgrow(urlTextEntry, Priority.ALWAYS);
    HBox.setHgrow(headerCheckBox, Priority.NEVER);  
    root.setTop(controls);
    root.setCenter(table);

    
    Button dumpFirstRowButton = new Button("Show first row");
    dumpFirstRowButton.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            for (StringProperty sp : table.getItems().get(0)) {
                System.out.print(sp.get()+"\t");
            }
            System.out.println();
        }
    });
    root.setBottom(dumpFirstRowButton);
    Scene scene = new Scene(root, 600, 400);
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  private void populateTable(
      final TableView<ObservableList<StringProperty>> table,
      final String urlSpec, final boolean hasHeader) {
    table.getItems().clear();
    table.getColumns().clear();
    table.setPlaceholder(new Label("Loading..."));
    Task<Void> task = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        try (BufferedReader in = getReaderFromUrl(urlSpec)) {
            // Header line
            if (hasHeader) {
              final String headerLine = in.readLine();
              final String[] headerValues = headerLine.split("\t");
              Platform.runLater(new Runnable() {
                @Override
                public void run() {
                  for (int column = 0; column < headerValues.length; column++) {
                    table.getColumns().add(
                        createColumn(column, headerValues[column]));
                  }
                }
              }); 
            }
    
            // Data:
    
            String dataLine;
            while ((dataLine = in.readLine()) != null) {
              final String[] dataValues = dataLine.split("\t");
              Platform.runLater(new Runnable() {
                @Override
                public void run() {
                  // Add additional columns if necessary:
                  for (int columnIndex = table.getColumns().size(); columnIndex < dataValues.length; columnIndex++) {
                    table.getColumns().add(createColumn(columnIndex, ""));
                  }
                  // Add data to table:
                  ObservableList<StringProperty> data = FXCollections
                      .observableArrayList();
                  for (String value : dataValues) {
                    data.add(new SimpleStringProperty(value));
                  }
                  table.getItems().add(data);
                }
              });
            }
        }
        return null;
      }
    };
    Thread thread = new Thread(task);
    thread.setDaemon(true);
    thread.start();
  }

  private TableColumn<ObservableList<StringProperty>, String> createColumn(
      final int columnIndex, String columnTitle) {
    TableColumn<ObservableList<StringProperty>, String> column = new TableColumn<>();
    String title;
    if (columnTitle == null || columnTitle.trim().length() == 0) {
      title = "Column " + (columnIndex + 1);
    } else {
      title = columnTitle;
    }
    column.setText(title);
    column.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<ObservableList<StringProperty>, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(
              CellDataFeatures<ObservableList<StringProperty>, String> cellDataFeatures) {
            ObservableList<StringProperty> values = cellDataFeatures.getValue();
            // Pad to current value if necessary:
            for (int index = values.size(); index <= columnIndex; index++) {
                values.add(index, new SimpleStringProperty(""));
            }
            return cellDataFeatures.getValue().get(columnIndex);
          }
        });
    column.setCellFactory(TextFieldTableCell.<ObservableList<StringProperty>>forTableColumn());
    return column;
  }

  private BufferedReader getReaderFromUrl(String urlSpec) throws Exception {
    URL url = new URL(urlSpec);
    URLConnection connection = url.openConnection();
    InputStream in = connection.getInputStream();
    return new BufferedReader(new InputStreamReader(in));
  }

  private void save(final Stage primaryStage,
		final TableView<ObservableList<StringProperty>> table,
		final CheckBox headerCheckBox, final FileChooser fileChooser) {
	File file = fileChooser.showSaveDialog(primaryStage);
	if (file != null) {
	    try (PrintWriter out = new PrintWriter(file)) {
		   if (headerCheckBox.isSelected()) {
		       for (Iterator<TableColumn<ObservableList<StringProperty>, ?>> iterator = table.getColumns().iterator(); iterator.hasNext(); ) {
		           TableColumn<ObservableList<StringProperty>,?> col = iterator.next();
		           out.print(col);
		           if (iterator.hasNext()) {
		               out.print("\t");
		           }
		       }
		       out.println();
		   }
		   for (ObservableList<StringProperty> row : table.getItems() ) {
		       
		       for (Iterator<StringProperty> iterator = row.iterator(); iterator.hasNext();) {
		           StringProperty sp = iterator.next();
		           out.print(sp.get());
		           if (iterator.hasNext()) {
		               out.print("\t");
		           }
		       }
		       out.print("\n");
		   }
	    } catch (IOException exc) {
	        exc.printStackTrace();
	    }
	}
}

public static void main(String[] args) {
    launch(args);
  }
}
