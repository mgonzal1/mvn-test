package gov.fnal.controls.servers.dpm.drf3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class AcnetRequestTest {

//    @Test
//    public void testGetDefaultProperty() {
//        // Arrange, Act and Assert
//        assertEquals(Property.READING, AcnetRequest.getDefaultProperty("Device"));
//        assertEquals(Property.READING, AcnetRequest.getDefaultProperty("\""));
//        assertEquals(Property.READING, AcnetRequest.getDefaultProperty("(?i)[A-Z0#][:?_|&@$~][A-Z0-9_:-]{1,62}"));
//    }

    @Test
    public void testNewAcnetRequest() throws RequestFormatException {
        // Arrange, Act and Assert
        assertThrows(DeviceFormatException.class, () -> new AcnetRequest(new DiscreteRequest("Str")));
    }

    @Test
    public void testNewAcnetRequest2() throws RequestFormatException {
        // Arrange and Act
        AcnetRequest actualAcnetRequest = new AcnetRequest(new DiscreteRequest("U$UUU"));

        // Assert
        assertEquals("U:UUU", actualAcnetRequest.getDevice());
        assertNull(actualAcnetRequest.text);
        assertEquals(Field.ALL, actualAcnetRequest.getField());
        assertEquals(Property.DIGITAL, actualAcnetRequest.getProperty());
        assertFalse(actualAcnetRequest.hasEvent);
        assertFalse(actualAcnetRequest.hasRange);
        Event expectedEvent = actualAcnetRequest.event;
        assertSame(expectedEvent, actualAcnetRequest.getEvent());
        Range expectedRange = actualAcnetRequest.range;
        assertSame(expectedRange, actualAcnetRequest.getRange());
    }
}
