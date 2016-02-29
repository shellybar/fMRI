package ubongo.common.datatypes;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import ubongo.common.log.Logger;
import ubongo.common.log.LoggerManager;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@XmlRootElement(name = "unit")
@XmlAccessorType(XmlAccessType.FIELD)
public class BaseUnit implements Unit, ExecutableObj, Serializable {

    private static Logger logger = LoggerManager.getLogger(BaseUnit.class);

    @XmlAttribute private int id;
    @XmlElement private String inputPath;
    @XmlElement private String description;
    @XmlElement private String relativePath;
    @XmlElementWrapper(name = "parameters")
    @XmlElements({
            @XmlElement(name = "string-parameter", type = StringUnitParameter.class),
            @XmlElement(name = "file-parameter", type = FileUnitParameter.class)
    })
    private List<UnitParameter> parameters = new ArrayList<>();

    public BaseUnit(int id) {
        this.id = id;
    }

    public BaseUnit() {
    }

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

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
}
