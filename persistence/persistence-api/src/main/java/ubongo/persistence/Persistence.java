package ubongo.persistence;

import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.Unit;

import java.util.List;

public interface Persistence {

    void start() throws PersistenceException;

    void stop() throws PersistenceException;

    /**
     * Adds units to the Units table with the given analysis name
     * @param analysisName unique identifier
     * @param units list of units to include in the analysis in order
     * @throws PersistenceException
     */
    void createAnalysis(String analysisName, List<Unit> units) throws PersistenceException;

    /**
     * Creates a new flow in Flows table
     * @param studyName is the name of the study
     * @param tasks to execute in this flow
     * @return flowId in DB
     * @throws PersistenceException
     */
    long createFlow(String studyName, List<Task> tasks) throws PersistenceException;

    void startFlow(long flowId);

    void cancelFlow(long flowId);

    List<Task> getNewTasks() throws PersistenceException;

    void updateTaskStatus(Task task) throws PersistenceException;

    List<Task> getTasks(long flowId) throws PersistenceException;

    void cancelTask(Task task) throws PersistenceException;

    Unit getUnit(long unitId) throws PersistenceException;

    List<Unit> getAllUnits() throws PersistenceException;

}
