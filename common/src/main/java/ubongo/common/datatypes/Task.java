package ubongo.common.datatypes;

/**
 * A task encapsulates all the information to run a unit on a machine
 * and return a response once the execution has completed.
 */
public interface Task {

    /**
     * @return a unique task ID as it is saved in the DB.
     */
    int getId();

    void setId(int id);

    /**
     * @return the unit that is required to run in this task.
     */
    Unit getUnit();

    void setUnit(Unit unit);

    /**
     * @return the machine on which to run the unit.
     */
    Machine getMachine();

    void setMachine(Machine machine);

    TaskStatus getStatus();

    void setStatus(TaskStatus status);

}
