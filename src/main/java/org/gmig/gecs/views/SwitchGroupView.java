package org.gmig.gecs.views;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import org.gmig.gecs.Loader;
import org.gmig.gecs.command.ComplexCommandBuilder;
import org.gmig.gecs.command.ListenableCommand;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.groups.SwitchGroup;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    TextArea log;

    public SwitchGroupView(SwitchGroup switchGroup, JsonNode viewsNode, TabPane tabPane) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("switchGroupTab.fxml"));
        loader.setRoot(new Tab());
        tab = loader.load();//(new ByteArrayInputStream(fxml.getBytes()));
        //switchGroupTab.fxml
        AnchorPane calendarPane = (AnchorPane) tab.getContent().lookup("#calendarPane");
        placePlanPane = (Pane) tab.getContent().lookup("#diagPane");
        Button turnOn = (Button)tab.getContent().lookup("#turnOn");
        Button turnOff = (Button)tab.getContent().lookup("#turnOff");
        log = (TextArea)tab.getContent().lookup("#log");
        turnOn.setOnMouseClicked((e) -> switchGroup.switchOnCmd().exec());
        turnOff.setOnMouseClicked((e) -> switchGroup.switchOffCmd().exec());
        ImageView view = (ImageView) placePlanPane.lookup("#img");

        for (JsonNode jsonNode : viewsNode) {
            if (jsonNode.get("name").asText().equals(switchGroup.getName())) {
                view.setImage(new Image(Loader.getFile(jsonNode.get("image").asText()).toURI().toString()));
                view.setFitWidth(1700);
                view.setPreserveRatio(true);
                view.setSmooth(true);
                view.setCache(true);
                for (int i = tabPane.getTabs().size();i <= jsonNode.get("position").asInt();i++)
                    tabPane.getTabs().add(new Tab());
                 tabPane.getTabs().set(jsonNode.get("position").asInt(),tab);
            }
        }

        schedulerView = new SchedulerView();
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
        init = new ListenableCommand<>(builder.parallel(0),"Init");
        logCommand(init);
        update.setOnMouseClicked((e)-> init.exec());

        tab.setText(switchGroup.getName());
    }

    private void toLog(String string){
        Platform.runLater(()->{
            log.appendText(LocalDateTime.now() + " " + string + "\n");
        });
    }
    private void toLogResult(Map<String,?> res) {
        Platform.runLater(()-> {
            res.entrySet().stream().filter((msg) -> msg.getValue() instanceof Throwable).forEach((msg) -> log.appendText("\t" + msg.getKey() + ":" + msg.getValue() + "\n"));
        });
    }

    private void logCommand(ListenableCommand<HashMap<String,?>> c){
        c.started.add(()->toLog(c.getName()+":started"));
        c.success.add((res)-> {
            toLog(c.getName()+":finished");
            toLogResult(res);
        });

    }
}
