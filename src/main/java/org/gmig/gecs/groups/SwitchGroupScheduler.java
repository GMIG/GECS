package org.gmig.gecs.groups;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.gmig.gecs.command.Command;
import org.apache.log4j.Logger;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.calendar.BaseCalendar;
import org.quartz.impl.calendar.CronCalendar;
import org.quartz.impl.calendar.HolidayCalendar;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.OperableTrigger;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class SwitchGroupScheduler {
    private static HashMap<String, Command<?>> commandMap = new HashMap<>();

    private static final Logger logger = Logger.getLogger(SwitchGroupScheduler.class);
    private final SchedulerFactory schedFact;
    private final Scheduler scheduler;
    private final HashSet<SwitchGroup> switchGroups;
    public static SimpleDateFormat dateFormatDecoder = new SimpleDateFormat("d M y");
    public static SimpleDateFormat timedateFormatDecoder = new SimpleDateFormat("d M y s m H");
    public static SimpleDateFormat timeFormatDecoder = new SimpleDateFormat("s m H");


    public SwitchGroupScheduler(HashSet<SwitchGroup> switchGroups) throws SchedulerException {
        Properties props = new Properties();
        props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
        props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        props.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        props.setProperty("org.quartz.threadPool.threadCount", "10");
        schedFact = new StdSchedulerFactory(props);
        scheduler = schedFact.getScheduler();
        this.switchGroups = switchGroups;
    }

    private HolidayCalendar getExcludeDatesCalendar(JsonNode specialSchedule, String switcherName) throws ParseException {
        HolidayCalendar excludeDates = new HolidayCalendar();
        for (JsonNode jsonNode : specialSchedule) {
            if (jsonNode.get("switchGroup").asText().equals(switcherName)) {
                Date exclude = dateFormatDecoder.parse(jsonNode.get("date").asText());
                excludeDates.addExcludedDate(exclude);
            }
        }
        return excludeDates;
    }

    private Set<SimpleTrigger> initSpecialTriggers(JsonNode specialSchedule) throws ParseException, SchedulerException {
        HashSet<SimpleTrigger> triggers = new HashSet<>();
        for (JsonNode record : specialSchedule) {
            String date = record.get("date").asText();
            String name = record.get("switchGroup").asText();
            Iterator<Map.Entry<String, JsonNode>> fields = record.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (!entry.getKey().equals("switchGroup") && !entry.getKey().equals("date")) {
                    Date when = timedateFormatDecoder.parse(date + " " + entry.getValue().asText());
                    if (when.after(Date.from(Instant.now()))) {
                        SimpleTrigger tg = newTrigger()
                                .forJob(entry.getKey(), name)
                                .startAt(when)
                                .withSchedule(simpleSchedule())
                                .build();
                        scheduler.scheduleJob(tg);
                        triggers.add(tg);
                    }
                }
            }
        }
        return triggers;
    }

    private JobDetail constructJobDetail(String commandName, String switcherName) {
        JobKey jk = new JobKey(commandName, switcherName);
        JobDetail job = newJob(SwitchGroupJob.class)
                .withIdentity(jk)
                .build();
        job.getJobDataMap().put(SwitchGroupJob.switcherKey, switchGroups);
        return job;
    }

    private Set<Trigger> initCronTriggers(JsonNode cronSchedule, JsonNode specialSchedule) throws ParseException, SchedulerException {
        for (JsonNode switcherSchedule : cronSchedule) {
            String switcherName = switcherSchedule.get("switchGroup").asText();
            Iterator<Map.Entry<String, JsonNode>> fields = switcherSchedule.fields();
            HolidayCalendar excludesFromSpecialSchedule = getExcludeDatesCalendar(specialSchedule, switcherName);
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (!entry.getKey().equals("switchGroup")) {
                    String commandName = entry.getKey();
                    JobDetail jd = constructJobDetail(commandName, switcherName);
                    JsonNode switcherSchedules = entry.getValue();
                    JsonNode excludes = switcherSchedules.get("exclude");
                    BaseCalendar excludeCal = excludesFromSpecialSchedule;
                    for (JsonNode cronExclude : excludes)
                        excludeCal = new CronCalendar(excludeCal, cronExclude.asText());
                    scheduler.addCalendar(commandName + switcherName, excludeCal, true, true);
                    JsonNode includes = switcherSchedules.get("include");
                    Set<Trigger> cronTriggers = new HashSet<>();
                    for (JsonNode cronInclude : includes) {
                        Trigger t = newTrigger()
                                .forJob(jd)
                                .withSchedule(cronSchedule(cronInclude.asText()))
                                .modifiedByCalendar(commandName + switcherName)
                                .startNow()
                                .build();
                        cronTriggers.add(t);
                    }
                    scheduler.scheduleJob(jd, cronTriggers, true);
                }
            }
        }
        return new HashSet<>();
    }

    public void loadSchedule(String cronScheduleString, String specialScheduleStirng) throws ParseException, SchedulerException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        scheduler.clear();
        JsonNode cronSchedule = mapper.readTree(cronScheduleString);
        JsonNode specialSchedule = mapper.readTree(specialScheduleStirng);
        initCronTriggers(cronSchedule, specialSchedule);
        initSpecialTriggers(specialSchedule);
        scheduler.start();
    }

    public HashMap<JobKey,List<Date>> getDates(Date from, Date to) throws ParseException, SchedulerException {
        HashMap<JobKey,List<Date>> times = new HashMap<>();
        for (String groupName : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                String jobName = jobKey.getName();
                String jobGroup = jobKey.getGroup();
                times.put(jobKey,getJobDates(jobName, jobGroup,  from,  to));
            }
        }
        return times;
    }

    public ArrayList<Date> getJobDates(String command,String job, Date from, Date to) throws ParseException, SchedulerException {
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(new JobKey(command, job));
                //logger.info("\nCommand:" + command + " SwitchGroup: " + job);
                ArrayList<Date> tr = new ArrayList<>();
                for (Trigger trigger : triggers) {
                    org.quartz.Calendar calendar = scheduler.getCalendar(trigger.getCalendarName());
                    tr.addAll(TriggerUtils.computeFireTimesBetween((OperableTrigger) trigger, calendar, from, to));
                }
                Collections.sort(tr);
                //logger.info(tr.stream().map(Date::toString).collect(Collectors.joining("\n")));
        return tr;
    }

             // "switchGroup": "cafe",
              //        "date": "10 5 2018",
               //       "switch on": "0 0 10",
                //      "switch off": "0 0 18"

    public void updateSpecialSchedule(Date day, String switchGroup, String timeOn, String timeOff, File file) throws  IOException {
        JsonFactory f = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(f);
        JsonNode root = mapper.readTree(file);
        ArrayNode node = (ArrayNode)root;
        for (Iterator<JsonNode> iter = node.iterator(); iter.hasNext(); ) {
            JsonNode element = iter.next();
            if((element.get("switchGroup").asText().equals(switchGroup) && (element.get("date").asText().equals(dateFormatDecoder.format(day)))))
                iter.remove();
        }
        ObjectNode data = mapper.createObjectNode();
        data.put("switchGroup",switchGroup);
        data.put("date",dateFormatDecoder.format(day));
        if(!timeOn.isEmpty()) data.put("switch on",(timeOn));
        if(!timeOff.isEmpty()) data.put("switch off",(timeOff));
        node.add(data);

        JsonGenerator gen = f.createGenerator(file,
                JsonEncoding.UTF8);
        gen.useDefaultPrettyPrinter();
        gen.writeTree(node);
        gen.close();
    }

}