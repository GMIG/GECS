package org.gmig.gecs.views;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.gmig.gecs.SourcesServer;
import org.gmig.gecs.command.CommandQueue;
import org.gmig.gecs.command.ListenableCommand;
import org.gmig.gecs.device.Device;
import org.gmig.gecs.device.StandardCommands;
import org.gmig.gecs.device.StateRequestResult;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 *
 */
public class DeviceView {
    private Polygon iconPolygon;
    private Label iconLetter;
    private Pane icon;

    private Pane labels = new Pane();

    private Stage stage = new Stage();

    private TextArea logField;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");



    private LinkedBlockingQueue<String> logs = new LinkedBlockingQueue<>();
    private void addToLog(String str){
        if(logs.size() > 100)
            logs.poll();
        logs.add(LocalDateTime.now().format(formatter)+":"+str);
    }



    private Text labelToText(Label l){
        Text theText = new Text(l.getText());
        theText.setFont(l.getFont());
        return theText;
    }

    public DeviceView(Device device, JsonNode viewJson, JsonNode types) throws ClassNotFoundException, IOException {
        Label name = new Label(device.getName());
        if(device.getName().contains("R1") || device.getName().contains("R2")) {
            name.setFont(Font.font("System", 11));
            Label IP = new Label(device.getDescription());
            IP.setFont(Font.font("System", 11));
            IP.setLayoutY(labelToText(name).getLayoutBounds().getHeight() + 1);
            double width = Math.max(labelToText(name).getLayoutBounds().getWidth(), labelToText(IP).getLayoutBounds().getWidth());
            double height = labelToText(IP).getLayoutY() + labelToText(IP).getLayoutBounds().getHeight();
            labels = new Pane(name, IP);
            labels.setMouseTransparent(true);
            labels.setPrefWidth(width);
            labels.setPrefHeight(height);
            labels.setLayoutX(viewJson.get("x-label").doubleValue());
            labels.setLayoutY(viewJson.get("y-label").doubleValue());
        }
        String type = viewJson.get("type").asText();

        for (JsonNode refType : types) {
            if(refType.get("type").asText().equals(type)){
                iconPolygon = new Polygon();
                for (JsonNode val : refType.get("polygon")) {
                    iconPolygon.getPoints().add(val.asDouble());
                }
                iconLetter = new Label(refType.get("letter").asText());
                iconLetter.setLayoutX(iconPolygon.getPoints().stream().max(Double::compareTo).get()/4.);
                //iconLetter.setLayoutY(iconPolygon.getPoints().stream().max(Double::compareTo).get()/4.);
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
        iconPolygon.setRotate(viewJson.get("rot").doubleValue());
        ContextMenu menu = new ContextMenu();
        for (Map.Entry<String, ? extends ListenableCommand<?>> entry : device.getCommandList().entrySet()) {
            MenuItem item = new MenuItem(entry.getKey());
            item.setOnAction(e -> {
                stage.show();
                entry.getValue().exec();
            });
            menu.getItems().add(item);
            String commandID = entry.getKey();
            //if(!entry.getKey().equals(StandardCommands.check.name())){
            entry.getValue().started.add(()->showInfo(commandID+":"+"begin"));
            entry.getValue().success.add((res)->showInfo(commandID+":"+"success"+":"+res));
            entry.getValue().exception.add((ex)->showInfo(commandID+":"+"error"+":"+ex));
        }

        updateTooltipBehavior(0,20000,0,true);
        String IP = "";
        if(device.getDataList().containsKey("IP"))
            IP=(String)device.getData("IP");
        Tooltip tp = new Tooltip(device.getName()+"\n"+device.getDescription()+"\n"+IP);
        Tooltip.install(icon, tp);

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

        if(device.getDataList().containsKey("command queue")){
            ((CommandQueue)device.getData("command queue")).queueEmpty.add(this::setQueueEmpty);
            ((CommandQueue)device.getData("command queue")).queueNotEmpty.add(this::setQueueNotEmpty);

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
        if(device.getCommandList().keySet().contains(StandardCommands.checkedRestart.name())) {
            ListenableCommand<?> restart = device.getCommand(StandardCommands.checkedRestart.name());
            restart.exception.add(this::setError);
            restart.success.add(this::setOn);
        }


        stage.setTitle(device.getName());
        stage.setOnCloseRequest((e)->{e.consume();stage.hide();});
        Parent infoWindow = FXMLLoader.load(getClass().getClassLoader().getResource("deviceInfo.fxml"));
        ((Label) infoWindow.lookup("#name")).setText(device.getName());
        ((TextField) infoWindow.lookup("#ip")).setText((String)device.getData("IP"));
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
           // logField.setScrollTop(0);
            List<String> collect = logs.stream().collect(Collectors.toList());
            Collections.reverse(collect);
            collect.forEach((it) -> logField.appendText(it + "\n"));
        });
    }

    private static void updateTooltipBehavior(double openDelay, double visibleDuration,
                                              double closeDelay, boolean hideOnExit) {
        try {
            // Get the non public field "BEHAVIOR"
            Field fieldBehavior = Tooltip.class.getDeclaredField("BEHAVIOR");
            // Make the field accessible to be able to get and set its value
            fieldBehavior.setAccessible(true);
            // Get the value of the static field
            Object objBehavior = fieldBehavior.get(null);
            // Get the constructor of the private static inner class TooltipBehavior
            Constructor<?> constructor = objBehavior.getClass().getDeclaredConstructor(
                    Duration.class, Duration.class, Duration.class, boolean.class
            );
            // Make the constructor accessible to be able to invoke it
            constructor.setAccessible(true);
            // Create a new instance of the private static inner class TooltipBehavior
            Object tooltipBehavior = constructor.newInstance(
                    new Duration(openDelay), new Duration(visibleDuration),
                    new Duration(closeDelay), hideOnExit
            );
            // Set the new instance of TooltipBehavior
            fieldBehavior.set(null, tooltipBehavior);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void setOn(Object val){
        Platform.runLater(()->iconPolygon.setFill(Color.LIMEGREEN));
    }
    private void setOff(Object val){
        Platform.runLater(()->iconPolygon.setFill(Color.BLACK));
    }
    private void setError(Throwable throwable){
        Platform.runLater(()->iconPolygon.setFill(Color.ORANGERED));
    }

    private void setActive(Object val){
        Platform.runLater(()-> {
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0.5), (e) -> iconPolygon.setFill(Color.BLUE)),
                    new KeyFrame(Duration.seconds(1), (e) -> iconPolygon.setFill(Color.LIMEGREEN)));
            timeline.setCycleCount(1);
            timeline.play();
        });
    }

    private void setQueueNotEmpty(){
        Platform.runLater(()->iconLetter.setBackground(new Background(new BackgroundFill(Color.ANTIQUEWHITE, CornerRadii.EMPTY, Insets.EMPTY))));
    }
    private void setQueueEmpty(){
        Platform.runLater(()->iconLetter.setBackground(Background.EMPTY));
    }
}
