package edu.tau.eng.neuroscience.mri.dispatcher;


import edu.tau.eng.neuroscience.mri.common.datatypes.Context;
import edu.tau.eng.neuroscience.mri.common.datatypes.Unit;
import edu.tau.eng.neuroscience.mri.common.exceptions.DispatcherException;

import java.util.List;

/**
 * FlowVerificator receives a desired flow (List of Units) from the user,
 * verifies that all of the restrictions and prerequisites are met in this flow
 * (i.e. the order of the units in the list is valid) and if so, passes the
 * analysis flow on to the underlying layers for execution.
 */
public interface FlowVerificator {

    /**
     * Receives a request for an analysis flow, verifies its validity and
     * passes it on to the QueueManager. If the flow is not valid, a new flow is suggested
     * (may be viewed as a negotiation between the user and the system).
     * @param units is the ordered list of units to run in this analysis as requested by the user.
     * @return a List of Units; if the flow units is valid, the returned value would be the same
     * List (by reference). Otherwise, the returned value is a suggested reordering of the flow.
     * @throws DispatcherException if no valid order can be computed for the given list of units.
     */
    List<Unit> startAnalysis(Context context, List<Unit> units) throws DispatcherException;

}
