package ubongo.dispatcher;

import ubongo.common.datatypes.Context;
import ubongo.common.datatypes.Unit;

import java.util.List;

public interface Dispatcher {

    void start();

    void dispatch(Context context, List<Unit> units);

    void dispatch(Context context, Unit unit);

}
