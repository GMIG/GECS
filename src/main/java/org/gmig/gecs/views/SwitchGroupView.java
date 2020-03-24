package org.gmig.gecs.views;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.controlsfx.control.ToggleSwitch;
import org.gmig.gecs.Loader;
import org.gmig.gecs.command.ComplexCommandBuilder;
import org.gmig.gecs.command.ListenableCommand;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.groups.SwitchGroup;
import org.gmig.gecs.groups.SwitchGroupScheduler;
import org.gmig.gecs.groups.Watchdog;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by brix on 5/23/2018.
 */
public class SwitchGroupView {
    public Pane placePlanPane;
    public Tab tab ;
    public Button set;
    public ListenableCommand<HashMap<String,?>> init;
    public SchedulerView schedulerView;
    private TextArea log;
    private Timer timer = new Timer("calendarView update timer");
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public SwitchGroupView(SwitchGroup switchGroup, JsonNode viewsNode, TabPane tabPane, HashSet<Watchdog> watchdogs) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("switchGroupTab.fxml"));
        loader.setRoot(new Tab());
        tab = loader.load();//(new ByteArrayInputStream(fxml.getBytes()));
        //switchGroupTab.fxml
        Pane calendarPane = (Pane) tab.getContent().lookup("#calendarPane");
        placePlanPane = (Pane) tab.getContent().lookup("#diagPane");
        Button turnOn = (Button)tab.getContent().lookup("#turnOn");
        Button turnOff = (Button)tab.getContent().lookup("#turnOff");
        ToggleSwitch watchdogsSwitch = (ToggleSwitch)tab.getContent().lookup("#watchdog");
        watchdogsSwitch.selectedProperty().addListener((observable, oldValue, newValue) ->
                watchdogs.forEach(watchdog -> watchdog.enabled.set(newValue)));
        watchdogs.forEach((dog)->{
            dog.onRestartedBySwitchOn.add((e)->toLog(dog.module.getName()+" restarted by Watchdog at switch on. Cause:"+e));
            dog.onKilledBySwitchOn.add((e)->toLog(dog.module.getName()+" killed by Watchdog at switch on. Cause:"+e));
            dog.onRestartedByCheck.add((e)->toLog(dog.module.getName()+" restarted by Watchdog at check. Cause:"+e));
            dog.onKilledByCheck.add((e)->toLog(dog.module.getName()+" killed by Watchdog at check. Cause:"+e));
        });
        watchdogsSwitch.selectedProperty().set(true);
        log = (TextArea)tab.getContent().lookup("#log");
        turnOn.setOnMouseClicked((e) -> switchGroup.switchOnCmd().exec());
        turnOff.setOnMouseClicked((e) -> switchGroup.switchOffCmd().exec());
        ImageView view = (ImageView) placePlanPane.lookup("#img");

        for (JsonNode jsonNode : viewsNode) {
            if (jsonNode.get("name").asText().equals(switchGroup.getName())) {
                view.setImage(new Image(Loader.getFile(jsonNode.get("image").asText()).toURI().toString()));
                view.setCache(true);
                for (int i = tabPane.getTabs().size();i <= jsonNode.get("position").asInt();i++)
                    tabPane.getTabs().add(new Tab());
                 tabPane.getTabs().set(jsonNode.get("position").asInt(),tab);
            }
        }

        schedulerView = new SchedulerView();
        TimerTask task = new TimerTask() {
            public void run() {
                 schedulerView.setToday();
            }
        };
        timer.scheduleAtFixedRate(task, 1000*60*60*11, 1000*60*60*11);

        calendarPane.getChildren().add(schedulerView.page);
        set = (Button)tab.getContent().lookup("#setSchedule");

        logCommand(switchGroup.switchOnCmd());
        logCommand(switchGroup.switchOffCmd());

        Button update = (Button)tab.getContent().lookup("#update");
        Set<ManagedDevice> devs = switchGroup.getAllChildren().stream()
                .filter((sw) -> sw instanceof ManagedDevice)
                .map((sw) -> (ManagedDevice) sw).collect(Collectors.toSet());

        ComplexCommandBuilder builder = ComplexCommandBuilder.builder();
        for (ManagedDevice dev : devs) {
            builder.addCommand(0,dev.getName()+":init",dev.stateReq().getCommand());
        }
        //TODO: Move init somewhere away
        init = new ListenableCommand<>(builder.parallel(0),"Init");
        logCommand(init);
        update.setOnMouseClicked((e)-> init.exec());

        tab.setText(switchGroup.getName());
    }

    private void toLog(String string){
        Platform.runLater(()-> { log.setText(LocalDateTime.now().format(formatter) + " " + string + "\n" + log.getText());});
    }
    private void toLogResult(Map<String,?> res) {
        Platform.runLater(()-> res
                .entrySet()
                .stream()
                .filter((msg) -> msg.getValue() instanceof Throwable)
                .forEach((msg) -> log.setText(("\t" + msg.getKey() + ":" + msg.getValue() + "\n"+ log.getText()))));
    }

    private void logCommand(ListenableCommand<HashMap<String,?>> c){
        c.started.add(()->toLog(c.getName()+":started"));
        c.success.add((res)-> {
            toLogResult(res);
            toLog(c.getName()+":finished");
        });

    }

    public String[] checkFields(){
        TextField txtTurnOnHours = (TextField) tab.getContent().lookup("#txtTurnOnHours");
        TextField txtTurnOnMinutes = (TextField) tab.getContent().lookup("#txtTurnOnMinutes");
        TextField txtTurnOffHours = (TextField) tab.getContent().lookup("#txtTurnOffHours");
        TextField txtTurnOffMinutes = (TextField) tab.getContent().lookup("#txtTurnOffMinutes");
        String timeOn = "0 " + txtTurnOnMinutes.getText() + " " + txtTurnOnHours.getText();
        String timeOff = "0 " + txtTurnOffMinutes.getText() + " " + txtTurnOffHours.getText();

        try {
            SwitchGroupScheduler.timedateFormatDecoder.parse("1 1 2017 " + timeOn);
            ((Label) tab.getContent().lookup("#txtTurnOnTimeLabel")).setBackground(
                    new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));

        } catch (ParseException ex) {
            ((Label) tab.getContent().lookup("#txtTurnOnTimeLabel")).setBackground(
                    new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
            timeOn = "";
        }
        try {
            SwitchGroupScheduler.timedateFormatDecoder.parse("1 1 2017 " + timeOff);
            ((Label) tab.getContent().lookup("#txtTurnOffTimeLabel")).setBackground(
                    new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));

        } catch (ParseException ex) {
            ((Label) tab.getContent().lookup("#txtTurnOffTimeLabel")).setBackground(
                    new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
            timeOff = "";
        }
        return new String[]{timeOn,timeOff};
    }
    public void disposeTimer(){
        timer.cancel();
    }

}
