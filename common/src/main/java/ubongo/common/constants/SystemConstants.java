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

    // Configuration
    public static final String DB_CONFIG_FILE_PATH = "db_config";
    public static final String SSH_CONFIG_FILE_PATH = "ssh_config";
    public static final String UNIT_SETTINGS_DIR_PATH = "unit_settings";
    public static final String MACHINES_CONFIG_FILE_PATH = "machines";

}
