package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.Unit;
import java.util.List;

/**
 * Created by regevr on 11/1/2015.
 */
public interface FlowVerificator {

    List<Unit> startAnalysis(List<Unit> units);

}
