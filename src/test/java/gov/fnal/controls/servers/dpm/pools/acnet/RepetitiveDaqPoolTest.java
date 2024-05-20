package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RepetitiveDaqPoolTest {

    @Test
    public void test_recovery() {
        Node node = Mockito.mock(Node.class);
        DataEvent event = Mockito.mock(DataEvent.class);
        RepetitiveDaqPool pool = new RepetitiveDaqPool(node, event);
        Assertions.assertFalse(pool.process(true));
    }

    @Test
    public void test_insert() {
        Node node = Mockito.mock(Node.class);
        DataEvent event = Mockito.mock(DataEvent.class);

        ReceiveData receiveData = Mockito.mock(ReceiveData.class);
        WhatDaq whatDaq1 = Mockito.mock(WhatDaq.class);
        WhatDaq whatDaq = new WhatDaq(whatDaq1, 487, 10, 457, receiveData);
        RepetitiveDaqPool pool = new RepetitiveDaqPool(node, event);

        pool.insert(whatDaq);
    }
    @Test
    public void test_process() {
        Node node = Mockito.mock(Node.class);
        DataEvent event = Mockito.mock(DataEvent.class);
        RepetitiveDaqPool pool = new RepetitiveDaqPool(node, event);

        Assertions.assertFalse(pool.process(true));
    }
}
