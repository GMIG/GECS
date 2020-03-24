package org.gmig.gecs.device;

import org.gmig.gecs.command.ListenableCommand;

/**
 *
 */
public interface Commandable {
    String getName();
    ListenableCommand<?> getCommand(String ID);
}
