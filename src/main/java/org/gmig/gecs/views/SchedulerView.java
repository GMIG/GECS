package org.gmig.gecs.views;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.Entry;
import com.calendarfx.view.MonthEntryView;
import com.calendarfx.view.MonthView;
import com.calendarfx.view.page.MonthPage;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import org.gmig.gecs.device.StandardCommands;
import org.gmig.gecs.groups.SwitchGroup;
import org.gmig.gecs.groups.SwitchGroupScheduler;
import org.quartz.SchedulerException;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Created by brix on 5/11/2018.
 */
@SuppressWarnings("InfiniteLoopStatement")
public class SchedulerView {
    MonthPage page= new MonthPage();
    private MonthView calendarView = page.getMonthView();//new MonthView();
    private int daysToDisplay = 360*2;

    public void setToday(){
        Platform.runLater(() -> calendarView.setToday(LocalDate.now()));
    }

    public SchedulerView() {
       //page = new MonthPage();
       // MonthView calendarView = page.getMonthView();//new MonthView();
        calendarView.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, Event::consume);
        calendarView.addEventFilter(MouseEvent.ANY, Event::consume);
        Calendar birthdays = new Calendar("Birthdays");
        calendarView.setShowWeekdays(true);
        calendarView.setShowWeekends(true);
        calendarView.setShowWeekNumbers(false);
        calendarView.setEntryViewFactory(entry -> {
            MonthEntryView v = new MonthEntryView(entry);
            v.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, Event::consume);
            v.addEventFilter(MouseEvent.ANY,Event::consume);
            MonthEntrySimpleSkin s = new MonthEntrySimpleSkin(v);
            v.setSkin(s);
            return v;
        });
        CalendarSource myCalendarSource = new CalendarSource("My Calendars");
        myCalendarSource.getCalendars().add(birthdays);
        page.getCalendarSources().addAll(myCalendarSource);
        //calendarView.getCalendarSources().addAll(myCalendarSource);
        calendarView.setShowCurrentWeek(false);

        calendarView.setRequestedTime(LocalTime.now());

        /*Thread updateTimeThread = new Thread("Calendar: Update Time Thread") {
            @Override
            public void run() {
                while (true) {
                    Platform.runLater(() -> calendarView.setToday(LocalDate.now()));
                    try {
                        sleep(1000 * 60 * 60 * 11);
                    } catch (InterruptedException e) {
                    }
                }
            }
        };*/

        AnchorPane.setTopAnchor(calendarView, 0.0);
        calendarView.setPrefHeight(270);
        calendarView.setPrefWidth(400);
        calendarView.setStyle(".month-view > .container > .day-of-week-label,\n" +
                ".month-view > .container > .day-of-weekend-label {\n" +
                "    -fx-text-fill: derive(gray, -40.0%);\n" +
                "    -fx-padding: 0.0 0.0 0.0px 0.0;\n" +
                "    -fx-font-size: 0.75em;\n" +
                "}\n" +
                ".month-view > .container > .day > .header > .day-of-month-label,\n" +
                ".month-view > .container > .day > .header > .day-not-of-month-label {\n" +
                "    -fx-alignment: center-left;\n" +
                "    -fx-font-weight: bold;\n" +
                "}" +
                ".month-view > .container > .day > .header > .day-of-month-label {\n" +
                "    -fx-text-fill: gray;\n" +
                "}\n" +
                "\n" +
                ".month-view > .container > .day > .header > .day-not-of-month-label {\n" +
                "    -fx-text-fill: gray;\n" +
                "}\n" +
                "\n" +
                ".month-view > .container > .day > .header > .weekend-day {\n" +
                "    -fx-text-fill: gray;\n" +
                "}\n"+
                ".month-view > .container > .day {\n" +
                "    -fx-padding: 0.0px;\n" +
                "    -fx-background-insets: 0.0px;\n" +
                "    -fx-border-color: black black black black;\n" +
                "    -fx-border-insets: 0.0;\n" +
                "    -fx-border-width: 0.0 0.0 0.0 0.0;\n" +
                "}\n" +
                ".month-view > .container > .day > .header > .today-label {\n" +
                "    -fx-text-fill: firebrick;\n" +
                "    -fx-background-insets: 0.0 0.0 0.0 0.0;\n" +
                "    -fx-alignment: center;\n" +
                "}\n" +
                ".month-view > .container > .today {\n" +
                "    -fx-background-color: lavenderblush;\n" +
                "    -fx-text-fill: red;\n" +
                "    -fx-border-color: firebrick firebrick firebrick firebrick;\n" +
                "}" );
        //updateTimeThread.setPriority(Thread.MIN_PRIORITY);
        //updateTimeThread.start();
    }

    public void setDates(ArrayList<Date> allDatesOn,ArrayList<Date> allDatesOff){
        CalendarSource myCalendarSource = new CalendarSource("All Dates");
        Calendar dates = new Calendar("Dates");
        page.getCalendarSources().clear();


        for (Date date : allDatesOn) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(date);
            int year = cal.get(java.util.Calendar.YEAR);
            int month = cal.get(java.util.Calendar.MONTH)+1;
            int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            Entry<String> entry = new Entry<>(String.valueOf(hour));
            entry.setInterval(LocalDate.of(year,month,day));
            dates.addEntry(entry);
        }
        for (Date date : allDatesOff) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(date);
            int year = cal.get(java.util.Calendar.YEAR);
            int month = cal.get(java.util.Calendar.MONTH)+1;
            int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            Map<LocalDate,List<Entry<?>>> ONentry = dates.findEntries(LocalDate.of(year,month,day),LocalDate.of(year,month,day), ZoneId.systemDefault());
            if (ONentry.isEmpty()) {
                Entry<String> entry = new Entry<>("-"+String.valueOf(hour));
                entry.setInterval(LocalDate.of(year, month, day));
                dates.addEntry(entry);
            }
            else {
                Entry<String> e =  (Entry<String>)ONentry.values().stream().findFirst().get().stream().findFirst().get();
                e.setTitle(e.getTitle()+ "-" + hour);
            }
        }
        myCalendarSource.getCalendars().add(dates);
        page.getCalendarSources().add(myCalendarSource);
    }

    public void updateDates(SwitchGroupScheduler scheduler, SwitchGroup switchGroup) throws ParseException, SchedulerException {
        ArrayList<Date> on = scheduler.getJobDates(
                StandardCommands.switchOn.friendlyName,
                switchGroup.getName(),
                Date.from(Instant.now()),
                Date.from(Instant.now().plus(daysToDisplay, ChronoUnit.DAYS)));
        ArrayList<Date> off = scheduler.getJobDates(
                StandardCommands.switchOff.friendlyName,
                switchGroup.getName(),
                Date.from(Instant.now()),
                Date.from(Instant.now().plus(daysToDisplay, ChronoUnit.DAYS)));
        setDates(on,off);
    }



}
