package edu.tau.eng.neuroscience.mri.dispatcher;

public class DispatcherImpl implements Dispatcher {

    /**
     * A call to this function notifies the dispatcher that a fatal error has occurred
     */
    public static void notifyFatal(Throwable e) {
        // TODO notify UI, try to solve based on error code...
    }

}
