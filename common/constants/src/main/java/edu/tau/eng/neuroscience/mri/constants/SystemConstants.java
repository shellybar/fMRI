package edu.tau.eng.neuroscience.mri.constants;

import java.io.File;


public class SystemConstants {

    private static String baseStr = System.getenv("MRI_BASE_DIR");
    static {
        if (baseStr == null) {
            baseStr = new File(System.getenv("TEMP"), "mri").getAbsolutePath();
        }
    }

    public static final File BASE_DIR = new File(baseStr);

}
