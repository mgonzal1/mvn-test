package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DaqSendAcnetTest {

    @Test
    public void test_send()
    {
        Node node = mock(Node.class);
        DataEvent dataEvent = mock(DataEvent.class);
        Completable completable = mock(Completable.class);
        WhatDaq whatDaq = mock(WhatDaq.class);
        DaqSendAcnet daqSendAcnet = new DaqSendAcnet(node,dataEvent,false,completable,50000);
        Assertions.assertTrue(daqSendAcnet.add(whatDaq));
    }
}
