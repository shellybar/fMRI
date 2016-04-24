package ubongo.common.constants;

import java.io.File;


public class SystemConstants {

    // TODO remove - get this info from command line arguments if necessary
    private static String baseStr = System.getenv("MRI_BASE_DIR");
    static {
        if (baseStr == null) {
            baseStr = new File(System.getenv("TEMP"), "mri").getAbsolutePath();
        }
    }
    public static final File BASE_DIR = new File(baseStr);

    public static final int NETWORK_RETRIES =5;
    public static final int SLEEP_BETWEEN_NETWORK_RETRIES = 1000;

    public static final String UBONGO_RABBIT_QUEUE = "ubongo_tasks";

    // Configuration
    public static final String DB_CONFIG_FILE_PATH = "db_config";
    public static final String SSH_CONFIG_FILE_PATH = "ssh_config";
    public static final String UNIT_SETTINGS_DIR_PATH = "unit_settings";
    public static final String MACHINES_CONFIG_FILE_PATH = "machines";

}
