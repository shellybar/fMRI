package ubongo.common.datatypes;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@XmlRootElement(name = "unit")
@XmlAccessorType(XmlAccessType.FIELD)
public class BaseUnit implements Unit, Serializable {

    private static Logger logger = LogManager.getLogger(BaseUnit.class);

    @XmlElement
    private String name;

    @XmlElement
    private String description;

    @XmlAttribute
    private int id;

    @XmlElement (name = "input-files")
    private String inputPaths;

    @XmlElement (name = "output-dir")
    private String outputDir;

    @XmlElementWrapper (name = "parameters")
    @XmlElements({
            @XmlElement (name = "string-parameter", type = StringUnitParameter.class),
            @XmlElement (name = "file-parameter", type = FileUnitParameter.class)
    })
    private List<UnitParameter> parameters = new ArrayList<>();

    public BaseUnit(int id) {
        this.id = id;
    }

    public BaseUnit() {}

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getOutputDir() {
        return outputDir;
    }

    @Override
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    @Override
    public List<UnitParameter> getParameters() {
        return parameters;
    }

    @Override
    public String getInputPaths() { /* TODO do! */
        return inputPaths;
    }

    @Override
    public void setInputPaths(String inputPaths) {
        this.inputPaths = inputPaths;
    }

    @Override
    public void setParameters(List<UnitParameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public void setParameterValues(String json) throws JsonParseException {
        try {
            Map<String, String> jsonMap =
                    new Gson().fromJson(json, new TypeToken<HashMap<String, String>>(){}.getType());
            for (UnitParameter param : parameters) {
                param.setValue(jsonMap.get(param.getName()));
            }
        } catch (RuntimeException e) {
            logger.fatal("Failed to set parameter values for unit from JSON: " + json);
            throw new JsonParseException(e.getMessage());
        }
    }
}
