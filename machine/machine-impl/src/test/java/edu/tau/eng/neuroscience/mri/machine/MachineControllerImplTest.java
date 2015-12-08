package edu.tau.eng.neuroscience.mri.machine;

import edu.tau.eng.neuroscience.mri.common.MachineStatistics;
import org.junit.Test;
import static org.junit.Assert.*;


public class MachineControllerImplTest {

    @Test
    public void testGetStatistics() {
        // TODO this is only a placeholder
        MachineController mc = new MachineControllerImpl();
        MachineStatistics ms = mc.getStatistics();
        assertTrue(ms == null);
    }

}
