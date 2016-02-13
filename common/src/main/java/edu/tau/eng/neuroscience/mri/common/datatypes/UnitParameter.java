package edu.tau.eng.neuroscience.mri.common.datatypes;


/**
 * A UnitParameter encapsulates the information about a parameter that a unit may require.
 * For instance, if a unit requires a string, we want to have an identifying name for the parameter,
 * a display name to show in the UI, and a value received from the user.
 * This may also include other information such as whether this parameter is required, etc.
 * This class is extended by several specific parameter classes for the different parameters types.
 */
public class UnitParameter {

    private String name;
    private String display;
    private String value;

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    @Override
    public String toString() {
        //TODO serialize and deserialize JSON
        return "placeholder";
    }

}
