package ubongo.common.datatypes;

import java.util.List;

/**
 * Unit is the basic fMRI service that runs on a machine.
 * It contains all the information required to run an fMRI script/program.
 */
public interface Unit {

    /**
     * a unique identifier of the unit as saved in the configuration files or DB.
     * @return the unit's id.
     */
    int getId();

    void setId(int id);

    String getDescription();

    void setDescription(String description);

    List<UnitParameter> getParameters();

    void setParameters(List<UnitParameter> unitParameters);

    void setParameterValues(String json);

    /**
     * @return the path which holds the task input files
     */
    String getInputPath();

    void setInputPath(String inputPath);

    static String getUnitFileName(long unitId, String suffix) {
        String pattern = "unit_%03d" + suffix;
        return String.format(pattern, unitId);
    }
}
