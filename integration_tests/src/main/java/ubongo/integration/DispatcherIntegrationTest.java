package ubongo.integration;

import ubongo.common.datatypes.Unit;
import ubongo.common.exceptions.DispatcherException;
import ubongo.dispatcher.Dispatcher;
import ubongo.dispatcher.DispatcherImpl;
import ubongo.dispatcher.UnitFetcher;

import java.io.File;
import java.util.Properties;

import static ubongo.common.constants.SystemConstants.BASE_DIR;
import static ubongo.common.constants.SystemConstants.MACHINES_CONFIG_FILE_PATH;
import static ubongo.common.constants.SystemConstants.UNIT_SETTINGS_DIR_PATH;
import static ubongo.common.constants.SystemConstants.DB_CONFIG_FILE_PATH;
import static ubongo.common.constants.SystemConstants.SSH_CONFIG_FILE_PATH;



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
