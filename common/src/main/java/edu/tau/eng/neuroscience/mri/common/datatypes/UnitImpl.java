package edu.tau.eng.neuroscience.mri.common.datatypes;


import java.util.List;

public class UnitImpl implements Unit{

    private int id;
    private String description;
    private List<UnitParameter> unitParameters;
    private String inputPath;

    public void setId(int id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setParameters(List<UnitParameter> unitParameters) {
        this.unitParameters = unitParameters;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<UnitParameter> getParameters() {
        return unitParameters;
    }

    @Override
    public String getInputPath() {
        return inputPath;
    }
}
