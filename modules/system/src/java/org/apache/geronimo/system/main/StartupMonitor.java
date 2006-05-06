package org.apache.geronimo.system.main;

import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.repository.Artifact;

/**
 * An interface used by the Daemon to convey the status of the server
 * startup.
 *
 * @version $Revision: 1.0$
 */
public interface StartupMonitor {
    // Normal calls, will generally occur in this order
    void systemStarting(long startTime);
    void systemStarted(Kernel kernel);
    void foundConfigurations(Artifact[] configurations);
    void configurationLoading(Artifact configuration);
    void configurationLoaded(Artifact configuration);
    void configurationStarting(Artifact configuration);
    void configurationStarted(Artifact configuration);
    void startupFinished();

    // Indicate failures during load
    void serverStartFailed(Exception problem);
}
