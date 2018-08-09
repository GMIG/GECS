package org.gmig.gecs.views;

import com.fasterxml.jackson.databind.JsonNode;
import org.gmig.gecs.SourcesServer;
import org.gmig.gecs.command.ListenableCommand;
import org.gmig.gecs.device.Device;
import org.gmig.gecs.device.StandardCommands;
import org.gmig.gecs.device.StateRequestResult;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by brix on 5/10/2018.
 */
public class DeviceView {
    private Polygon iconPolygon;
    private Label iconLetter;
    private Pane icon;

    private Pane labels;
    private Label name;
    private Label IP;

    private Parent infoWindow;
    private Stage stage = new Stage();

    private TextArea logField;
    private Device device;

    private LinkedList<String> logs = new LinkedList<>();
    private void addToLog(String str){
        if(logs.size() > 20)
            logs.removeLast();
        logs.push(LocalDateTime.now()+":"+str);
    }



    private Text labelToText(Label l){
        Text theText = new Text(l.getText());
        theText.setFont(l.getFont());
        return theText;
    }

    public DeviceView(Device device, JsonNode viewJson, JsonNode types) throws ClassNotFoundException, IOException {

        this.device = device;
        name = new Label(device.getName());
        IP = new Label(device.getData("IP"));
        IP.setLayoutY(labelToText(name).getLayoutBounds().getHeight() + 3);
        double width = Math.max(labelToText(name).getLayoutBounds().getWidth(),labelToText(IP).getLayoutBounds().getWidth());
        double height = labelToText(IP).getLayoutY() + labelToText(IP).getLayoutBounds().getHeight();
        labels = new Pane(name,IP);
        labels.setMouseTransparent(true);
        labels.setPrefWidth(width);
        labels.setPrefHeight(height);
        labels.setLayoutX(viewJson.get("x-label").doubleValue());
        labels.setLayoutY(viewJson.get("y-label").doubleValue());
        String type = viewJson.get("type").asText();

        for (JsonNode refType : types) {
            if(refType.get("type").asText().equals(type)){
                iconPolygon = new Polygon();
                for (JsonNode val : refType.get("polygon")) {
                    iconPolygon.getPoints().add(val.asDouble());
                }
                iconLetter = new Label(refType.get("letter").asText());
            }
        }
        if(iconPolygon==null) {
            iconPolygon = new Polygon(0, 0, 30, 0, 30, 30, 0, 30);
            iconLetter = new Label();
        }
        icon = new Pane(iconPolygon,iconLetter);
        iconPolygon.setFill(Color.GREY);
        icon.setPrefWidth(iconPolygon.getLayoutBounds().getWidth());
        icon.setPrefHeight(iconPolygon.getLayoutBounds().getHeight());
        icon.setLayoutX(viewJson.get("x-icon").doubleValue());
        icon.setLayoutY(viewJson.get("y-icon").doubleValue());
        icon.setRotate(viewJson.get("rot").doubleValue());
        ContextMenu menu = new ContextMenu();
        for (Map.Entry<String, ? extends ListenableCommand<?>> entry : device.getCommandList().entrySet()) {
            MenuItem item = new MenuItem(entry.getKey());
            item.setOnAction(e -> {
                stage.show();
                entry.getValue().exec();
            });
            menu.getItems().add(item);
            String commandID = entry.getKey();
            entry.getValue().started.add(()->showInfo(commandID+":"+"begin"));
            entry.getValue().success.add((res)->showInfo(commandID+":"+"success"+":"+res));
            entry.getValue().exception.add((ex)->showInfo(commandID+":"+"error"+":"+ex));
        }
        MenuItem info = new MenuItem("INFO");
        info.setOnAction(e -> stage.show());
        menu.getItems().add(info);

        icon.setOnMouseClicked((e -> menu.show(icon, Side.BOTTOM, 0, 0)));

        if(device.getCommandList().keySet().contains(StandardCommands.check.name())) {
            device.getCommand(StandardCommands.check.name()).exception.add(this::setError);
            device.getCommand(StandardCommands.check.name()).success.add(this::setOn);
        }
        //TODO: refactor this
        if(device.getFactoryType().equals(SourcesServer.class)) {
            device.getArgCommandList().values().forEach((c)->c.success.add(this::setActive));
        }

        if(device.getCommandList().keySet().contains(StandardCommands.init.name())) {
            ListenableCommand<StateRequestResult> init = (ListenableCommand<StateRequestResult>)device.getCommand(StandardCommands.init.name());
            init.exception.add(this::setError);
            init.success.add((res)->{
                if(res.isOn())
                    setOn(res.returned());
                else
                    setOff(res.returned());
            });
        }

        if(device.getCommandList().keySet().contains(StandardCommands.switchOn.name())) {
            ListenableCommand<?> on = device.getCommand(StandardCommands.switchOn.name());
            on.exception.add(this::setError);
            on.success.add(this::setOn);
        }

        if(device.getCommandList().keySet().contains(StandardCommands.switchOff.name())) {
            ListenableCommand<?> off = device.getCommand(StandardCommands.switchOff.name());
            off.exception.add(this::setError);
            off.success.add(this::setOff);
        }

        stage.setTitle(device.getName());
        stage.setOnCloseRequest((e)->{e.consume();stage.hide();});
        infoWindow = FXMLLoader.load(getClass().getClassLoader().getResource("deviceInfo.fxml"));
        ((Label) infoWindow.lookup("#name")).setText(device.getName());
        ((TextField) infoWindow.lookup("#ip")).setText(device.getData("IP"));
        ((TextField) infoWindow.lookup("#description")).setText(device.getDescription());
        ((TextField) infoWindow.lookup("#factory")).setText(device.getFactoryType().getSimpleName());
        stage.setScene(new Scene(infoWindow, 550, 600));
        stage.setAlwaysOnTop(true);
         logField = ((TextArea) infoWindow.lookup("#log"));

    }

    public void addToPane(Pane root){
        root.getChildren().addAll(icon,labels);
    }

    private void showInfo(String str){
        addToLog(str);
        Platform.runLater(()-> {
            logField.clear();
            logs.forEach((it) -> logField.appendText(it + "\n"));
        });
    }

    public void setOn(Object val){
        Platform.runLater(()->iconPolygon.setFill(Color.LIMEGREEN));
    }
    public void setOff(Object val){
        Platform.runLater(()->iconPolygon.setFill(Color.BLACK));
    }
    public void setError(Throwable throwable){
        Platform.runLater(()->iconPolygon.setFill(Color.ORANGERED));
    }

    public void setActive(Object val){
        Platform.runLater(()-> {
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0.5), (e) -> iconPolygon.setFill(Color.BLUE)),
                    new KeyFrame(Duration.seconds(1), (e) -> iconPolygon.setFill(Color.LIMEGREEN)));
            timeline.setCycleCount(1);
            timeline.play();
        });
    }

}
