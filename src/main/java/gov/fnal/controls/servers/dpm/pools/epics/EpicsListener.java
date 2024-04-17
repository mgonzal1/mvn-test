// $Id: EpicsListener.java,v 1.1 2022/06/30 19:47:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.epics;

import org.epics.pva.client.PVAChannel;
import org.epics.pva.data.PVAStructure;

import java.util.BitSet;

public interface EpicsListener
{
	void handleData(final PVAChannel channel, final BitSet changes, 
						final BitSet overruns, final PVAStructure data);
}
