package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.datatypes.Context;
import edu.tau.eng.neuroscience.mri.common.datatypes.Unit;

import java.util.List;

public interface Dispatcher {

    void start();

    void dispatch(Context context, List<Unit> units);

    void dispatch(Context context, Unit unit);

}
