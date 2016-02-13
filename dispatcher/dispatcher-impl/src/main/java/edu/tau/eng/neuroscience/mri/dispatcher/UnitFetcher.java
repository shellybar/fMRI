package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.constants.SystemConstants;
import edu.tau.eng.neuroscience.mri.common.datatypes.BaseUnit;
import edu.tau.eng.neuroscience.mri.common.datatypes.Unit;
import edu.tau.eng.neuroscience.mri.common.exceptions.DispatcherException;
import edu.tau.eng.neuroscience.mri.common.exceptions.ErrorCodes;
import edu.tau.eng.neuroscience.mri.common.exceptions.UnitFetcherException;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;
import edu.tau.eng.neuroscience.mri.dispatcher.db.DBProxy;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.List;


/**
 * The UnitFetcher class supplys a method to get a unit Object given its ID
 */
public class UnitFetcher {

    private static Logger logger = LoggerManager.getLogger(UnitFetcher.class);

    /**
     * Get a Unit object with unit settings corresponding to those set
     * in the XML configuration file for the unit with unitId.
     * @param unitId is a number required to locate the relevant XML file
     * @return Unit object with the data corresponding to unitId.
     * @throws UnitFetcherException if unit with unitId does not exist
     */
    public static Unit getUnit(int unitId) throws UnitFetcherException {

        BaseUnit unit = null;
        File file = getUnitSettingsFile(unitId);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(BaseUnit.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unit = (BaseUnit) unmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            String originalMsg = e.getMessage();
            String msg = "Failed to parse unit settings file for unit " + unitId +
                    " (file path: " + file.getAbsolutePath() + "). " +
                    ((originalMsg == null) ? "" : "Details: " + originalMsg);
            logger.error(msg);
        }
        if (unit == null) {
            throw new UnitFetcherException(ErrorCodes.UNIT_UNMARSHAL_EXCEPTION, "Failed to retrieve unit " + unitId +
                    ". Make sure that the unit exists and is configured correctly");
        }
        return unit;
    }

    public static List<Unit> getAllUnits() throws UnitFetcherException {
        // TODO implement
        return null;
    }

    private static File getUnitSettingsFile(int unitId) {
        String pattern = "unit_%03d.xml";
        return new File(SystemConstants.BASE_DIR,
                "unit_settings" + File.separator + String.format(pattern, unitId));
    }

}
