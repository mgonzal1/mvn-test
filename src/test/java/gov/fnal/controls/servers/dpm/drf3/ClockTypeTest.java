package gov.fnal.controls.servers.dpm.drf3;

import org.junit.Test;

import static gov.fnal.controls.servers.dpm.drf3.ClockType.*;
import static org.junit.Assert.assertEquals;

public class ClockTypeTest {

    @Test
    public void testEnumConstants(){
        assertEquals("H", HARDWARE.toString());
        assertEquals("S", SOFTWARE.toString());
        assertEquals("E", EITHER.toString());
    }

    @Test
    public void testParseValidInputHardware() throws RequestFormatException {
        ClockType clockType = ClockType.parse("Hard");
        assertEquals(HARDWARE, clockType);
    }

    @Test
    public void testParseValidInputSoftware() throws RequestFormatException {
        ClockType clockType = ClockType.parse("Soft");
        assertEquals(SOFTWARE, clockType);
    }

    @Test
    public void testParseValidInputEither() throws RequestFormatException {
        ClockType clockTypeE = ClockType.parse("Epics");
        assertEquals(EITHER, clockTypeE);
    }

    @Test (expected = RequestFormatException.class)
    public void testParseInvalidInput() throws RequestFormatException {
        ClockType.parse("InvalidValue");
    }

    @Test (expected = NullPointerException.class)
    public void testParseValidInputIsNull() throws NullPointerException {
        ClockType.parse(null);
    }
}
