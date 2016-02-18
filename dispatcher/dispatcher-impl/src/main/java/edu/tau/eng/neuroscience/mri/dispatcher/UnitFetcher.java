package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.constants.SystemConstants;
import edu.tau.eng.neuroscience.mri.common.datatypes.BaseUnit;
import edu.tau.eng.neuroscience.mri.common.datatypes.Unit;
import edu.tau.eng.neuroscience.mri.common.exceptions.DispatcherException;
import edu.tau.eng.neuroscience.mri.common.exceptions.ErrorCodes;
import edu.tau.eng.neuroscience.mri.common.exceptions.UnitFetcherException;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The UnitFetcher class supplys a method to get a unit Object given its ID
 */
public class UnitFetcher {

    private static Logger logger = LoggerManager.getLogger(UnitFetcher.class);
    private static final Pattern UNIT_FILENAME_PATTERN = Pattern.compile("unit_[0-9]{3}.xml");
    private static final Pattern DIGITS_PATTERN = Pattern.compile("-?\\d+");

    /**
     * Get a Unit object with unit settings corresponding to those set
     * in the XML configuration file for the unit with unitId.
     * @param unitId is a number required to locate the relevant XML file
     * @return Unit object with the data corresponding to unitId.
     * @throws UnitFetcherException if unit with unitId does not exist
     */
    public static Unit getUnit(int unitId) throws UnitFetcherException {
        File file = getUnitSettingsFile(unitId);
        return getUnit(file, unitId);
    }

    private static Unit getUnit(File file, int unitId) throws UnitFetcherException {
        BaseUnit unit = null;
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
        File unitsDir = new File(SystemConstants.BASE_DIR, "unit_settings");
        File[] files = unitsDir.listFiles((dir, name) -> UNIT_FILENAME_PATTERN.matcher(name).matches());
        List<Unit> units = new ArrayList<>(files.length);
        for (File file: files) {
            Matcher matcher = DIGITS_PATTERN.matcher(file.getName());
            if (matcher.find()) {
                units.add(getUnit(file, Integer.parseInt(matcher.group())));
            }
        }
        return units;
    }

    // TODO BASE_DIR should come from command line arguments
    private static File getUnitSettingsFile(int unitId) {
        String pattern = "unit_%03d.xml";
        return new File(SystemConstants.BASE_DIR,
                "unit_settings" + File.separator + String.format(pattern, unitId));
    }

}
