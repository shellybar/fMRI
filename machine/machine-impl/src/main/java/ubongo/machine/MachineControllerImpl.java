package ubongo.machine;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ubongo.common.datatypes.BaseUnit;
import ubongo.common.datatypes.MachineStatistics;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.Unit;
import ubongo.common.exceptions.UnmarshalException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;

// TODO what is this class good for if we have the MachineServer? - Shelly's answer - MachineServer listens to requests via socket. MachineController - run the scripts on the machine.
public class MachineControllerImpl implements MachineController {

    private static Logger logger = LogManager.getLogger(MachineControllerImpl.class);

    @Override
    public MachineStatistics getStatistics() {
        return null;
    }

    @Override
    public void run(Task task) {
        Unit unit = task.getUnit();
        String unitResource = "unit_"+String.format("%03d", unit.getId())+".xml";
        System.out.println(unitResource);
        File unitConf = new File(MachineControllerImpl.class.getClassLoader().getResource(unitResource).getFile());
        try {
            Unit unitProperties = loadBaseUnitConfig(unitConf);
        } catch (UnmarshalException e) {
            e.printStackTrace(); // TODO handle failure
        }


    }

    private Unit loadBaseUnitConfig(File unitConfigFilePath) throws UnmarshalException {
        BaseUnit unit = null;
        logger.debug("Loading unit configuration details from " + unitConfigFilePath.getAbsolutePath() + "...");
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(BaseUnit.class); // TODO - why xml only configured for base unit? Do we need more that base unit?
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unit = (BaseUnit) unmarshaller.unmarshal(unitConfigFilePath);
        } catch (JAXBException e) {
            String originalMsg = e.getMessage();
            String msg = "Failed to parse unit configuration file (file path: "
                    + unitConfigFilePath.getAbsolutePath() + "). " + ((originalMsg == null) ? "" : "Details: " + originalMsg);
            logger.error(msg);
        }
        if (unit == null) {
            throw new UnmarshalException("Failed to retrieve Database Connection configuration. " +
                            "Make sure that " + unitConfigFilePath.getAbsolutePath() + " exists and is configured correctly");
        }
        return unit;
    }
}
