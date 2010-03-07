import sun.misc.Signal;
import sun.misc.SignalHandler;

public class ProgressSignalHandler implements SignalHandler {

    private SignalHandler oldHandler;
    private static Signal sigUSR = new Signal("USR2");
    private KattilaBot callback;
    
    /**
     * Installs signal handler for USR signal. Callback function
     * inside KattilaBot is called when SIGUSR is trapped.
     *
     * @param callback An object to call when SIGUSR is trapped.
     * @returns A new SignalHandler. 
     */
    public static SignalHandler install(KattilaBot callback) {
        ProgressSignalHandler instance = new ProgressSignalHandler();
        instance.oldHandler = Signal.handle(sigUSR, instance);
	instance.callback = callback;
        return instance;
    }
    
    /**
     * Called automatically by JRE.
     */
    public void handle(Signal signal) {
	
        try {
	    if (sigUSR.equals(signal))
		callback.check();

            // Chain back to previous handler, if one exists
            if (oldHandler != SIG_DFL && oldHandler != SIG_IGN) {
                oldHandler.handle(signal);
            }
        } catch (Exception e) {
            System.err.println("Signal handler failed, reason " + e.getMessage());
            e.printStackTrace();
        }
    }
}
