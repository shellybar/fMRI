package ubongo.persistence;

import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.Unit;

import java.nio.file.Path;
import java.util.List;

public interface Persistence {

    void start() throws PersistenceException;

    void stop() throws PersistenceException;

    /**
     * Creates new flow in Flows table
     * @param studyName is the name of the study
     * @param studyRootDir is the path to the directory
     *                     where all study files are stored
     * @return flow id
     */
    long createFlow(String studyName, Path studyRootDir) throws PersistenceException;

    void cancelFlow(long flowId);

    void addTasks(List<Task> tasks) throws PersistenceException;

    List<Task> getNewTasks() throws PersistenceException;

    void updateTaskStatus(Task task) throws PersistenceException;

    List<Task> getTasks(long flowId) throws PersistenceException;

    void cancelTask(Task task) throws PersistenceException;

    Unit getUnit(long unitId) throws PersistenceException;

    List<Unit> getAllUnits() throws PersistenceException;

}
