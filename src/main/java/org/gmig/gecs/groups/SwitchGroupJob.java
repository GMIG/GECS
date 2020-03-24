package org.gmig.gecs.groups;

import org.gmig.gecs.device.StandardCommands;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.HashSet;
import java.util.Optional;

/**
 * Created by brix on 4/24/2018.
 */
public class SwitchGroupJob implements Job {
    public static final String switcherKey = "swkey";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String switcherID  = context.getJobDetail().getKey().getGroup();
        String switcherCommand  =context.getJobDetail().getKey().getName();
        Object switchersObj = context.getJobDetail().getJobDataMap().get(switcherKey);
        HashSet<SwitchGroup> switchGroups;
        if(switchersObj instanceof  HashSet)
            switchGroups = (HashSet<SwitchGroup>)switchersObj;
        else
            throw new JobExecutionException("SwitchGroup map not set");
        Optional<SwitchGroup> switcher = switchGroups.stream().filter((sw)->sw.getName().equals(switcherID)).findAny();

        if(!switcher.isPresent())
            throw new JobExecutionException("SwitchGroup " + switcherID + " command " + switcherCommand + " not found");
        if(switcherCommand.equals(StandardCommands.switchOn.friendlyName))
            switcher.get().switchOnCmd().exec();
        if(switcherCommand.equals(StandardCommands.switchOff.friendlyName))
            switcher.get().switchOffCmd().exec();
    }
}
