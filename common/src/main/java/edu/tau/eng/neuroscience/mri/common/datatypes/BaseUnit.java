package edu.tau.eng.neuroscience.mri.common.datatypes;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


@XmlRootElement(name = "unit")
@XmlAccessorType(XmlAccessType.FIELD)
public class BaseUnit implements Unit, ExecutableObj{

    @XmlAttribute
    private int id;

    @XmlElement
    private String inputPath;

    @XmlElement
    private String description;

    @XmlElementWrapper(name = "parameters")
    @XmlElements({
            @XmlElement(name = "string-parameter", type = StringUnitParameter.class),
            @XmlElement(name = "file-parameter", type = FileUnitParameter.class)
    })
    private List<UnitParameter> parameters = new ArrayList<>();

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<UnitParameter> getParameters() {
        return parameters;
    }

    @Override
    public String getInputPath() { /* TODO do! */
        return inputPath;
    }

    @Override
    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public void setParameters(List<UnitParameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String getExecutionInputs() {
        StringBuilder sb = new StringBuilder();
        List<UnitParameter> inputParams = this.getParameters();
        for (UnitParameter param :inputParams ){
            sb.append(" \"");
            sb.append(param.getValue());
            sb.append("\"");
        }
        return sb.toString();
    }
}
