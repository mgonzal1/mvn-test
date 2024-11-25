// $Id: SequencedDataSet.java,v 1.3 2024/11/19 22:34:44 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.PoolUser;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

import static gov.fnal.controls.db.DbServer.getDbServer;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class SequencedDataSet implements DaqPoolUserRequests<WhatDaq>, AcnetErrors
{
	final static String[] sequencedOwners = { "ColliderShot", "E835Store","PbarTransferShot", "RecyclerShot" };
	final static HashSet<String> sequencedOwnerSet = new HashSet<>(Arrays.asList(sequencedOwners));
	final ByteBuffer data = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);

	final PoolUser user;
	final LinkedList<WhatDaq> reqList = new LinkedList<>();
	final int fileIndex;
	final int collectionIndex;

	public SequencedDataSet(PoolUser user, int usageNo, int fileAlias, int collectionAlias, int setAlias) throws AcnetStatusException
	{
		this.user = user;

		try {
            final String query = "SELECT DISTINCT B.file_alias, A.file_index, A.collection_index"
                                    + " FROM srdb.sda_data A, srdb.save_header B WHERE B.owner LIKE '" + owner(usageNo)
                                    + "' AND B.file_alias = " + fileAlias + " AND A.file_index = B.file_index "
                                    + " AND A.collection_alias = " + collectionAlias + " AND A.set_alias = " + setAlias;
            final ResultSet rs = getDbServer("srdb").executeQuery(query);

            if (rs.next()) {
				fileIndex = rs.getInt(2);
				collectionIndex = rs.getInt(3);
            } else {
				fileIndex = -1;
				collectionIndex = -1;
			}

            rs.close();
		} catch (Exception e) {
		    logger.log(Level.WARNING, "exception creating saveDataSource", e);
			throw new AcnetStatusException(DPM_INTERNAL_ERROR);
		}
	}

	private String owner(int usageNo)
	{
		if (usageNo > 0 && usageNo <= sequencedOwners.length)
			return sequencedOwners[usageNo - 1];

		return "";
	}

	@Override
	public void insert(WhatDaq whatDaq)
	{
		reqList.add(whatDaq);
	}

	@Override
	public synchronized void cancel(PoolUser user, int error)
	{
		final long now = System.currentTimeMillis();

		for (WhatDaq whatDaq : reqList) {
			if (error != 0 && !whatDaq.isMarkedForDelete())
				whatDaq.getReceiveData().receiveStatus(error, now, 0);
			whatDaq.setMarkedForDelete();
		}
	}

	@Override
	public synchronized boolean process(final boolean __)
	{
		for (WhatDaq whatDaq : reqList) {
			if (!whatDaq.isMarkedForDelete())
				getData(whatDaq);
		}

		reqList.clear();
		user.complete();

		return false;
	}

	private void getData(WhatDaq whatDaq)
	{
		final long now = System.currentTimeMillis();

		try {
			final String query = "SELECT D.file_index, file_alias, collection_index, collection_alias, set_alias, segment, error, timestamp, data, length, offsett "
										+ " FROM srdb.sda_data D, srdb.save_header H"
										+ " WHERE D.file_index = H.file_index AND di = " + whatDaq.di()
										+ " AND pi = " + whatDaq.pi()
										+ " AND ((D.file_index = " + fileIndex
										+ " AND collection_index = " + collectionIndex + "))"
										+ " ORDER BY segment";

			final ResultSet rs = getDbServer("srdb").executeQuery(query);

			data.clear();

			int error = SAV_RST_NODI;
			long timestamp = System.currentTimeMillis();

			while (rs.next()) {
				error = rs.getInt("error");
				timestamp = rs.getTimestamp("timestamp").getTime();
				
				final byte[] dataSegment = rs.getBytes("data");
				if (dataSegment != null)
					data.put(dataSegment);
			}

			data.flip();

			if (error != 0)
				whatDaq.getReceiveData().receiveStatus(error);
			else if (data.remaining() == 0)
				whatDaq.getReceiveData().receiveStatus(SAV_RST_NODATA);
			else
				whatDaq.getReceiveData().receiveData(data, timestamp, 0);
		} catch (Exception e) {
			whatDaq.getReceiveData().receiveStatus(DPM_INTERNAL_ERROR, now, 0);
		}
	}

	@Override
	public String toString()
	{
		return "SequencedDataSet";
	}

	public static void main(String[] args) throws Exception
	{
		final String query = "SELECT D.file_index, H.file_alias, collection_index, collection_alias, set_alias, di, pi, segment, error, timestamp, data, length, offsett "
									+ " FROM srdb.sda_data D, srdb.save_header H"
									+ " WHERE H.file_alias = " + "9157"  + " AND D.file_index=H.file_index"
									+ " ORDER BY di,pi,segment";

		final ResultSet rs = getDbServer("srdb").executeQuery(query);

		while (rs.next()) {
			logger.log(Level.INFO, String.format("%8d %8d %8d %8d %8d%n", 
								rs.getInt("file_alias"), rs.getInt("collection_alias"), rs.getInt("set_alias"),
								rs.getInt("di"), rs.getInt("pi"))
			);
		}

		System.exit(0);
	}
}
