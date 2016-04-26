package ubongo.common.datatypes;

import java.util.List;

/**
 * Unit is the basic fMRI service that runs on a machine.
 * It contains all the information required to run an fMRI script/program.
 */
public interface Unit extends Cloneable {

    int getId();

    void setId(int id);

    String getDescription();

    void setDescription(String description);

    List<UnitParameter> getParameters();

    void setParameters(List<UnitParameter> unitParameters);

    void setParameterValues(String json);

    String getInputPaths();

    void setInputPaths(String inputPath);

    String getName();

    void setName(String name);

    String getOutputDir();

    void setOutputDir(String outputDir);

    Object clone() throws CloneNotSupportedException;

    static String getUnitFileName(long unitId, String suffix) {
        String pattern = "unit_%03d" + suffix;
        return String.format(pattern, unitId);
    }
}
