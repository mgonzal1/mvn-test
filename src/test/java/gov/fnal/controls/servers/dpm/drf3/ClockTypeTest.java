package gov.fnal.controls.servers.dpm.drf3;

import org.junit.jupiter.api.Test;

import static gov.fnal.controls.servers.dpm.drf3.ClockType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ClockTypeTest {

    @Test
    public void testEnumConstants() {
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

    @Test
    public void testParseInvalidInput() throws RequestFormatException {
        assertThrows(RequestFormatException.class, () -> {
            ClockType.parse("InvalidValue");
        });
    }

    @Test
    public void testParseValidInputIsNull() throws NullPointerException {
        assertThrows(NullPointerException.class, () -> {
            ClockType.parse(null);
        });

    }
}
