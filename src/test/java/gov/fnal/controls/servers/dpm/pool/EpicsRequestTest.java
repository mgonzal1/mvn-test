package gov.fnal.controls.servers.dpm.pool;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.epics.EpicsListener;
import gov.fnal.controls.servers.dpm.pools.epics.EpicsRequest;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class EpicsRequestTest {

    @Test
    public void test_isAllUpperCase() throws AcnetStatusException {
        EpicsListener listener = mock(EpicsListener.class);
        EpicsRequest epicsRequest = new EpicsRequest(":ZOdU~j:@3vH 4s>!{szj|LN+vv1zYO>0WjW(.&;,tr6t;doU~j$.Fi_Q`74dz5mz(zb@hvumm5j!3$ufC;oWjCoR)ZYkybgCWG0<-string <-literal <-text to the<- clipboard",listener);
    }
}
