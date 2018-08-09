package org.gmig.gecs;/**
 * Created by brix on 5/8/2018.
 */

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import org.quartz.SchedulerException;

import java.io.IOException;
import java.text.ParseException;


public class ControlGUI extends Application {


    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) throws IOException, ClassNotFoundException, InterruptedException, ParseException, SchedulerException {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("mainFrame.fxml"));
        Scene scene = new Scene(loader.load());
        TabPane root = (TabPane) scene.lookup("#tabs");
        Loader l = new Loader();
        l.load(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
