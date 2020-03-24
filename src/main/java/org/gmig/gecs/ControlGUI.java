package org.gmig.gecs;

/**
 *
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

    Loader l = new Loader();

    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) throws IOException, ClassNotFoundException, InterruptedException, ParseException, SchedulerException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("mainFrame.fxml"));
            Scene scene = new Scene(loader.load());
            TabPane root = (TabPane) scene.lookup("#tabs");
            l.load(root);
            primaryStage.setScene(scene);
            primaryStage.show();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    public void stop(){
        l.dispose();
    }

}
