package com.fantasy.cpu;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX Application for Fantasy Machine 16-bit CPU
 * This serves as the main GUI interface for the CPU emulator using FXML.
 */
public class App extends Application {
    
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        
        // Create scene
        Scene scene = new Scene(root, 800, 600);
        
        // Set window properties
        primaryStage.setTitle("Fantasy Machine 16-bit CPU Emulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}