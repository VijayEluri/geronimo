package org.apache.geronimo.system.main;

import java.io.PrintStream;

import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.repository.Artifact;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;

/**
 * A startup monitor that shows the progress of loading and starting
 * modules, outputing a new line for each module started
 * showing the time taken to start the module along with the
 * moduleId.
 * <p/>
 * This startup monitor produces more lines of output than the
 * ProgressBarStartupMonitor but its output is suitable for redirection
 * to a file or for when Geronimo is running under an IDE or other tool.
 * <p/>
 * A summary will also be produced containing a list of ports
 * Geronimo is listening on, the configIds of application modules
 * that were started and the URLs of Web applications that were started.
 *
 * @version $Revision: 1.0$
 */
public class LongStartupMonitor implements StartupMonitor {

    /**
     * PrintStream
     */
    private PrintStream out;

    /**
     * Number of modules to start
     */
    private int numModules;

    /**
     * Number of digits in number of modules to start
     */
    private int numModulesDigits;

    /**
     * Number of modules currently being started
     */
    private int moduleNum;

    /**
     * Length of longest module name
     */
    private int longestModuleNameLength;
    
    /**
     * Time Geronimo was started
     */
    private long started;

    /**
     * Time the current module being processed was started
     */
    private long moduleStarted;

    /**
     * The Kernel of the system being started
     */
    private Kernel kernel;

    public synchronized void systemStarting(long startTime) {
        out = System.out;
        started = startTime;
    }

    public synchronized void systemStarted(Kernel kernel) {
        this.kernel = kernel;
    }

    public synchronized void foundModules(Artifact[] modules) {
        numModules = modules.length;
        numModulesDigits = Integer.toString(numModules).length();
        
        for (int i = 0, len= 0; i < modules.length; i++) {
            len = modules[i].toString().length();
            if (len > longestModuleNameLength)
                longestModuleNameLength = len;
        }
    }

    public synchronized void moduleLoading(Artifact module) {
        StringBuffer buf = new StringBuffer("Module ");
        // pad module index
        int configIndexDigits = Integer.toString(++moduleNum).length();
        for (; configIndexDigits < numModulesDigits; configIndexDigits++) {
            buf.append(' ');
        }
        // append module index / total configs
        buf.append(moduleNum).append('/').append(numModules).append(' ');
        // append module name
        buf.append(module);
        // pad end of module with spaces so trailing startup times will line up
        int len = module.toString().length();
        for (; len < longestModuleNameLength; len++) {
            buf.append(' ');
        }
        out.print(buf);
    }

    public synchronized void moduleLoaded(Artifact module) {
    }

    public synchronized void moduleStarting(Artifact module) {
        moduleStarted = System.currentTimeMillis();
    }

    public synchronized void moduleStarted(Artifact module) {
        long time = System.currentTimeMillis() - moduleStarted;        
        StringBuffer buf = new StringBuffer();
        buf.append(" started in ");
        
        String formattedTime = getFormattedTime(time);
        if (formattedTime.startsWith("0.")) {
            // don't display zero seconds
            formattedTime = " " +formattedTime.substring(1);
        }
        
        // if first number (e.g. seconds or minutes) is one digit,
        // pad it with a leading space to get times to line up nicely
        int index = formattedTime.indexOf(':'); // must look for colon first
        if (index == -1)
            index = formattedTime.indexOf('.');
                
        if (index == 1)
            buf.append(' ');
            
        buf.append(formattedTime);
        
        out.println(buf.toString());
    }

    public synchronized void startupFinished() {
        int time = Math.round((float) (System.currentTimeMillis() - started) / 1000f);

        out.println("Startup completed in " + time + " seconds");
        StartupMonitorUtil.wrapUp(out, kernel);
    }

    public synchronized void serverStartFailed(Exception problem) {
        out.println("Server Startup failed");
        out.println();
        problem.printStackTrace(out);
    }

    // time formatting method - thanks to Maven 
    private static String getFormattedTime( long time )
    {
        String pattern = "s.SSS's'";
        if ( time / 60000L > 0 )
        {
            pattern = "m:s" + pattern;
            if ( time / 3600000L > 0 )
            {
                pattern = "H:m" + pattern;
            }
        }
        DateFormat fmt = new SimpleDateFormat( pattern );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return fmt.format( new Date( time ) );
    }
}
