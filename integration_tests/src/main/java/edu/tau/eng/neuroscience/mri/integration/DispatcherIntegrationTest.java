package edu.tau.eng.neuroscience.mri.integration;

import edu.tau.eng.neuroscience.mri.common.datatypes.Unit;
import edu.tau.eng.neuroscience.mri.common.exceptions.DispatcherException;
import edu.tau.eng.neuroscience.mri.dispatcher.Dispatcher;
import edu.tau.eng.neuroscience.mri.dispatcher.DispatcherImpl;
import edu.tau.eng.neuroscience.mri.dispatcher.UnitFetcher;

import java.io.File;
import java.util.Properties;

import static edu.tau.eng.neuroscience.mri.common.constants.SystemConstants.*;
import static edu.tau.eng.neuroscience.mri.common.constants.SystemConstants.BASE_DIR;
import static edu.tau.eng.neuroscience.mri.common.constants.SystemConstants.MACHINES_CONFIG_FILE_PATH;

public class DispatcherIntegrationTest {

    public static void main(String[] args) throws DispatcherException {

        Properties props = new Properties();
        props.setProperty(UNIT_SETTINGS_DIR_PATH,
                new File(DispatcherIntegrationTest.class.getClassLoader().getResource("unit_007.xml").getFile()).getParent());
        props.setProperty(DB_CONFIG_FILE_PATH, new File(BASE_DIR.getAbsolutePath(), "configs/db_connection.xml").getAbsolutePath());
        props.setProperty(SSH_CONFIG_FILE_PATH, new File(BASE_DIR.getAbsolutePath(), "configs/ssh_connection.xml").getAbsolutePath());
        props.setProperty(MACHINES_CONFIG_FILE_PATH, new File(BASE_DIR.getAbsolutePath(), "configs/machines.xml").getAbsolutePath());

        Dispatcher dispatcher = new DispatcherImpl(props, true);
        dispatcher.start();

        Unit unit = new UnitFetcher(props.getProperty(UNIT_SETTINGS_DIR_PATH)).getUnit(7);
        unit.setParameterValues("{\"myFile\":\"Test file path\", \"myStr\":\"Test string\"}");
        dispatcher.dispatch(null, unit);
    }

}
