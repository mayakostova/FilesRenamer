package com.odonataworkshop.files.renamer;

import com.odonataworkshop.files.renamer.controller.Controller;
import com.odonataworkshop.files.renamer.util.SkinBuilder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.ConfigurationFactory;


import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * User: maya
 * Date: 13-9-17
 */
public class StartApplication extends Application {
    protected static final Logger log = LogManager.getLogger(StartApplication.class);
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Files Renamer");
        primaryStage.initStyle(StageStyle.UNDECORATED);
        FXMLLoader fxmlLoader = new FXMLLoader(Thread.currentThread().getContextClassLoader().getResource("main.fxml"));
        Parent root = (Parent) fxmlLoader.load();
        Group rootGroup = new Group(root);
        String image = StartApplication.class.getResource("/images/background.jpg").toExternalForm();
        root.setStyle("-fx-background-image: url('"+image+"');display:block");
        rootGroup.setStyle("-fx-effect: dropshadow( gaussian , rgba(255,255,255,0.5) , 0,0,0,1 );");
        primaryStage.setScene(new Scene(rootGroup, 400, 200));
        primaryStage.getIcons().add(new Image(StartApplication.class.getResource("/images/icon.png").toExternalForm()));
        SkinBuilder builder = SkinBuilder.create(primaryStage).addCloseButton(rootGroup, true).addShadow(rootGroup).enableDragging(root);
        Object controller = fxmlLoader.getController();
        if(controller instanceof Controller){
            ((Controller) controller).setStage(primaryStage);
        }
        primaryStage.show();
        builder.setLocation();
    }


    public static void main(String[] args) {


        launch(args);

    }
}
