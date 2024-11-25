// $Id: SavedDataSet.java,v 1.2 2024/11/25 03:20:55 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.logging.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.PoolUser;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

import static gov.fnal.controls.db.DbServer.getDbServer;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;


public class SavedDataSet implements DaqPoolUserRequests<WhatDaq>, AcnetErrors
{
	final PoolUser user;
	final int fileIndex;
	final LinkedList<WhatDaq> reqList = new LinkedList<>();
	final ByteBuffer data = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);

	public SavedDataSet(PoolUser user, int fileAlias) throws AcnetStatusException
	{
		this.user = user;
		this.fileIndex = aliasToIndex(fileAlias);
	}

	private int aliasToIndex(int fileAlias) throws AcnetStatusException
	{
		final String query = "SELECT max(file_index) FROM srdb.save_header "
								+ "WHERE file_alias = " + fileAlias + " AND delete_file = 0";
		try {
			final ResultSet rs = getDbServer("srdb").executeQuery(query);

			if (rs.next())
				return rs.getInt(1);

			throw new AcnetStatusException(SAV_RST_NOFILE);
		} catch (Exception e) { }

		throw new AcnetStatusException(SAV_RST_NOFILE);
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

		for (WhatDaq whatDaq : reqList)
			whatDaq.getReceiveData().receiveStatus(error, now, 0);
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
		try {
			final String srQuery = "SELECT di, pi, length, error, timestamp, data " + "FROM srdb.srsave_data WHERE file_index = " + fileIndex + 
										" AND di = " + whatDaq.di() + " AND pi = " + whatDaq.pi() + " ORDER BY segment";

			final ResultSet rs = getDbServer("srdb").executeQuery(srQuery);

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
			else if (whatDaq.offset() + whatDaq.length() > data.remaining())
				whatDaq.getReceiveData().receiveStatus(SAV_RST_NODATA);
			else { 
				data.position(whatDaq.offset());
				whatDaq.getReceiveData().receiveData(data, timestamp, 0);
			}
		} catch (Exception e) {
			whatDaq.getReceiveData().receiveStatus(DPM_INTERNAL_ERROR);
			logger.log(Level.FINE, "internal error", e);
		}
	}
}
