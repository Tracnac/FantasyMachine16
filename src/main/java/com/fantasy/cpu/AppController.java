package com.fantasy.cpu;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * FXML Controller for the Fantasy Machine 16-bit CPU Emulator
 */
public class AppController implements Initializable {
    
    @FXML
    private Button resetButton;
    
    @FXML
    private Button stepButton;
    
    @FXML
    private Button runButton;
    
    @FXML
    private Button stopButton;
    
    @FXML
    private TextArea outputArea;
    
    @FXML
    private Label statusLabel;
    
    private Cpu cpu;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize CPU
        cpu = new Cpu();
        
        // Set initial status
        statusLabel.setText("CPU Status: Ready");
    }
    
    @FXML
    private void onResetCpu() {
        cpu.reset();
        outputArea.appendText("\nCPU Reset\n");
        statusLabel.setText("CPU Status: Reset");
    }
    
    @FXML
    private void onStepCpu() {
        try {
            cpu.step();
            outputArea.appendText("Step executed\n");
            statusLabel.setText("CPU Status: Stepped");
        } catch (Exception e) {
            outputArea.appendText("Error: " + e.getMessage() + "\n");
            statusLabel.setText("CPU Status: Error");
        }
    }
    
    @FXML
    private void onRunCpu() {
        outputArea.appendText("Running CPU...\n");
        statusLabel.setText("CPU Status: Running");
        // TODO: Implement continuous execution in a separate thread
    }
    
    @FXML
    private void onStopCpu() {
        outputArea.appendText("CPU Stopped\n");
        statusLabel.setText("CPU Status: Stopped");
        // TODO: Implement stop functionality
    }
}