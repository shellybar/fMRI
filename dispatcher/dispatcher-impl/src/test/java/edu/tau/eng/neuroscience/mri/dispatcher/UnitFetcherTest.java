package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.datatypes.BaseUnit;
import edu.tau.eng.neuroscience.mri.common.datatypes.FileUnitParameter;
import edu.tau.eng.neuroscience.mri.common.datatypes.StringUnitParameter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.File;

import static org.junit.Assert.*;


@RunWith(PowerMockRunner.class)
@PrepareForTest(UnitFetcher.class)
@PowerMockIgnore("javax.management.*")
public class UnitFetcherTest {

    private static final int TEST_UNIT_ID = 7;
    private static final File TEST_FILE = new File("src/test/resources/test.xml");

    @Before
    public void init() throws Exception {
        PowerMockito.spy(UnitFetcher.class);
        PowerMockito.doReturn(TEST_FILE).when(UnitFetcher.class, "getUnitSettingsFile", TEST_UNIT_ID);
    }

    @Test
    public void testGetUnit() {
        BaseUnit unit = (BaseUnit) UnitFetcher.getUnit(TEST_UNIT_ID);
        assertNotNull("Failed to unmarshal XML to Unit Object", unit);
        assertEquals("Unit created but with wrong ID", TEST_UNIT_ID, unit.getId());
        assertEquals("Unit created but with wrong description", "Unit-test unit", unit.getDescription());
        assertFalse("Unit created but with empty parameter list", unit.getParameters().isEmpty());
        String badParameter = "Unit created but with bad parameter";
        assertEquals(badParameter, FileUnitParameter.class, unit.getParameters().get(0).getClass());
        assertEquals(badParameter, "myFile", unit.getParameters().get(0).getName());
        assertEquals(badParameter, StringUnitParameter.class, unit.getParameters().get(1).getClass());
        assertEquals(badParameter, "myStr", unit.getParameters().get(1).getName());
    }

    @Test
    public void testGetNonexistentUnit() {
        BaseUnit unit = (BaseUnit) UnitFetcher.getUnit(TEST_UNIT_ID + 1);
        assertNull(unit);
    }

}
