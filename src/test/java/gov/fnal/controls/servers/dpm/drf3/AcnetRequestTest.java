//package gov.fnal.controls.servers.dpm.drf3;
//
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class AcnetRequestTest {
//
//    @Test
//    public void testNewAcnetRequest() throws RequestFormatException {
//        // Arrange, Act and Assert
//        assertThrows(DeviceFormatException.class, () -> new AcnetRequest(new DiscreteRequest("Str")));
//    }
//
//    @Test
//    public void testNewAcnetRequest2() throws RequestFormatException {
//        // Arrange and Act
//        AcnetRequest actualAcnetRequest = new AcnetRequest(new DiscreteRequest("U$UUU"));
//
//        // Assert
//        assertEquals("U:UUU", actualAcnetRequest.getDevice());
//        assertNull(actualAcnetRequest.text);
//        assertEquals(Field.ALL, actualAcnetRequest.getField());
//        assertEquals(Property.DIGITAL, actualAcnetRequest.getProperty());
//        assertFalse(actualAcnetRequest.hasEvent);
//        assertFalse(actualAcnetRequest.hasRange);
//        Event expectedEvent = actualAcnetRequest.event;
//        assertSame(expectedEvent, actualAcnetRequest.getEvent());
//        Range expectedRange = actualAcnetRequest.range;
//        assertSame(expectedRange, actualAcnetRequest.getRange());
//    }
//}
