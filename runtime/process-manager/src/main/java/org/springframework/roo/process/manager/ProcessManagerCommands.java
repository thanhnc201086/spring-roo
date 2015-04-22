package org.springframework.roo.process.manager;

import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Commands related to file system monitoring and process management.
 * 
 * @author Ben Alex
 * @author Juan Carlos García
 * @since 1.1
 */
@Component
@Service
public class ProcessManagerCommands implements CommandMarker {
	
	private static final String DEVELOPMENT_MODE_COMMAND = "development mode";
	private static final String PROJECT_SCAN_SPEED_COMMAND = "project scan speed";
	private static final String PROJECT_SCAN_STATUS_COMMAND = "project scan status";
	private static final String PROJECT_SCAN_NOW_COMMAND = "project scan now";

	protected final static Logger LOGGER = HandlerUtils.getLogger(ProcessManagerCommands.class);
	
    // ------------ OSGi component attributes ----------------
   	private BundleContext context;

    private ProcessManager processManager;
    private Shell shell;

    protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
    }

    @CliCommand(value = DEVELOPMENT_MODE_COMMAND, help = "Switches the system into development mode (greater diagnostic information)")
    public String developmentMode(
            @CliOption(key = { "", "enabled" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "true", help = "Activates development mode") final boolean enabled) {
        
    	if(processManager == null){
    		processManager = getProcessManager();
    	}
    	
    	Validate.notNull(processManager, "ProcessManager is required");
    	
    	if(shell == null){
    		shell = getShell();
    	}
    	
    	Validate.notNull(shell, "Shell is required");
    	
    	processManager.setDevelopmentMode(enabled);
        shell.setDevelopmentMode(enabled);
        return "Development mode set to " + enabled;
    }

    @CliCommand(value = PROJECT_SCAN_NOW_COMMAND, help = "Perform a manual file system scan")
    public String scan() {
    	if(processManager == null){
    		processManager = getProcessManager();
    	}
    	
    	Validate.notNull(processManager, "ProcessManager is required");
    	
        final long originalSetting = processManager
                .getMinimumDelayBetweenScan();
        try {
            processManager.setMinimumDelayBetweenScan(1);
            processManager.timerBasedScan();
        }
        finally {
            // Switch on manual scan again
            processManager.setMinimumDelayBetweenScan(originalSetting);
        }
        return "Manual scan completed";
    }

    @CliCommand(value = PROJECT_SCAN_STATUS_COMMAND, help = "Display file system scanning information")
    public String scanningInfo() {
    	if(processManager == null){
    		processManager = getProcessManager();
    	}
    	
    	Validate.notNull(processManager, "ProcessManager is required");
    	
        final StringBuilder sb = new StringBuilder("File system scanning ");
        final long duration = processManager.getLastScanDuration();
        if (duration == 0) {
            sb.append("never executed; ");
        }
        else {
            sb.append("last took ").append(duration).append(" ms; ");
        }
        final long minimum = processManager.getMinimumDelayBetweenScan();
        if (minimum == 0) {
            sb.append("automatic scanning is disabled");
        }
        else if (minimum < 0) {
            sb.append("auto-scaled scanning is enabled");
        }
        else {
            sb.append("scanning frequency has a minimum interval of ")
                    .append(minimum).append(" ms");
        }
        return sb.toString();
    }

    @CliCommand(value = PROJECT_SCAN_SPEED_COMMAND, help = "Changes the file system scanning speed")
    public String scanningSpeed(
            @CliOption(key = { "", "ms" }, mandatory = true, help = "The number of milliseconds between each scan") final long minimumDelayBetweenScan) {
    	if(processManager == null){
    		processManager = getProcessManager();
    	}
    	
    	Validate.notNull(processManager, "ProcessManager is required");
    	
    	processManager.setMinimumDelayBetweenScan(minimumDelayBetweenScan);
        return scanningInfo();
    }
    
    public ProcessManager getProcessManager(){
    	// Get all components implement ProcessManager interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(ProcessManager.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (ProcessManager) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load ProcessManager on ProcessManagerCommands.");
			return null;
		}
    }
    
    public Shell getShell(){
    	// Get all Shell implement Shell interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(Shell.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (Shell) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load Shell on ProcessManagerCommands.");
			return null;
		}
    }
}