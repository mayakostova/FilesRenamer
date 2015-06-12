package com.odonataworkshop.files.renamer.controller;

import com.odonataworkshop.files.renamer.file.FileManipulation;
import com.odonataworkshop.files.renamer.file.ProcessingHandler;
import com.odonataworkshop.files.renamer.util.MessageType;
import com.odonataworkshop.files.renamer.util.ModalDialog;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.concurrent.TimeUnit;


/**
 * User: maya
 * Date: 13-9-17
 */
public class Controller {

    @FXML
    private TextField txtInputDir;
    @FXML
    private Button btnStart;
    @FXML
    private VBox progressPanel;
    @FXML
    private Label lblProgress;
    @FXML
    private ProgressBar progress;

    private FileManipulation fileManipulator;


    private Stage mStage;

    public Controller() {
        fileManipulator = new FileManipulation();
        fileManipulator.addHandler(new CustomFileProcessingHandler());
    }

    @FXML
    public void onBrowseButtonActionPerformed(ActionEvent event) {
        File startDir = new File(txtInputDir.getText());
        DirectoryChooser openDir = new DirectoryChooser();
        openDir.setTitle("Choose directory");
        if (startDir.exists() && startDir.isDirectory()) {
            openDir.setInitialDirectory(startDir);
        }
        File selectedFile = openDir.showDialog(null);
        if (selectedFile != null) {
            final String fileName = selectedFile.getAbsolutePath();
            txtInputDir.setText(fileName);
        }
    }


    @FXML
    public void onStartButtonActionPerformed(ActionEvent event) {
        if (txtInputDir.getText() != null && !txtInputDir.getText().trim().equals("")) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    fileManipulator.renameFiles(new File(txtInputDir.getText()));
                }
            }).start();

        }

    }

    private static String formatInterval(final long aMiliseconds) {
        final long days = TimeUnit.MILLISECONDS.toDays(aMiliseconds);
        final long hr = TimeUnit.MILLISECONDS.toHours(aMiliseconds - TimeUnit.DAYS.toMillis(days));
        final long min = TimeUnit.MILLISECONDS.toMinutes(aMiliseconds - TimeUnit.DAYS.toMillis(days) - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(aMiliseconds - TimeUnit.DAYS.toMillis(days) - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        return days == 0 ?
                hr == 0 ?
                        min == 0 ?
                                String.format("%02d s", sec) :
                                String.format("%02d:%02d", min, sec)
                        : String.format("%02d:%02d:%02d", hr, min, sec)
                : String.format("%02d %02d:%02d:%02d", days, hr, min, sec);
    }


    public Stage getStage() {
        return mStage;
    }

    public void setStage(Stage aStage) {
        mStage = aStage;
    }


    private class CustomFileProcessingHandler implements ProcessingHandler {
        private long mForTime;
        private Double mTimeLeft;

        public void handleBeforeStartProcess(final Object aObj) {
            mForTime = 0;
            mTimeLeft = 0D;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    if (aObj instanceof File) {
                        File startDir = (File) aObj;
                        if (!startDir.exists()) {
                            ModalDialog.showMessageDialog(getStage(), "Directory does not exist", MessageType.ERROR_MESSAGE);
                        }else if (!startDir.isDirectory()) {
                            ModalDialog.showMessageDialog(getStage(), "The input file is not a directory. Please choose a directory!", MessageType.ERROR_MESSAGE);
                        }else{
                            progressPanel.visibleProperty().set(true);
                            btnStart.setDisable(true);
                            lblProgress.setText(fileManipulator.getProcessedFiles() + "/" + fileManipulator.getTotalFiles());
                        }
                    }

                }
            });

        }

        @Override
        public void handleAfterEndProcess(Object aObj) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    btnStart.setDisable(false);
                    lblProgress.setText("Processing finished for " + formatInterval(mForTime));
                    if (!fileManipulator.getNotRenamedFiles().isEmpty()) {
                        StringBuilder builder = new StringBuilder();
                        builder.append("The following files could not be renamed: \n");
                        for (String file : fileManipulator.getNotRenamedFiles()) {
                            builder.append(file).append("\n");
                        }
                        builder.deleteCharAt(builder.length() - 1);
                        ScrollPane scrollPane = new ScrollPane();
                        TextArea textArea = new TextArea();
                        textArea.setEditable(false);
                        scrollPane.setContent(textArea);
                        textArea.setText(builder.toString());
                        ModalDialog.showMessageDialog(getStage(), textArea, MessageType.WARNING_MESSAGE);
                    }
                }
            });
        }

        @Override
        public void handleProcessStart(Object aObj) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    lblProgress.setText(fileManipulator.getProcessedFiles() + "/" + fileManipulator.getTotalFiles() + "  Time left: " +
                            formatInterval(mTimeLeft.longValue()));
                }
            });
        }

        @Override
        public void handleProcessEnd(Object aObj) {
            long currentTime = System.currentTimeMillis();
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    progress.setProgress(((double) fileManipulator.getProcessedFiles() / fileManipulator.getTotalFiles()));
                }
            });
            long left = fileManipulator.getTotalFiles() - fileManipulator.getProcessedFiles();
            mForTime = (currentTime - fileManipulator.getStartTime()) == 0 ?
                    1 :
                    currentTime - fileManipulator.getStartTime();
            Double midTime = (double) mForTime / fileManipulator.getProcessedFiles();
            mTimeLeft = left * midTime;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    lblProgress.setText(fileManipulator.getProcessedFiles() + "/" + fileManipulator.getTotalFiles() + "  (" +
                            formatInterval(mTimeLeft.longValue()) + "/" +
                            formatInterval(mForTime) + ")");
                }
            });
        }
    }
}
