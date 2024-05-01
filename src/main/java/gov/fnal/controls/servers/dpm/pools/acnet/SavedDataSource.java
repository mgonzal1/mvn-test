// $Id: SavedDataSource.java,v 1.10 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.Arrays;
import java.util.HashSet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.SavedDataEvent;
import gov.fnal.controls.servers.dpm.pools.PoolUser;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

import gov.fnal.controls.db.DbServer;
import static gov.fnal.controls.db.DbServer.getPostgreSQLServer;

/**
 * SavedDataSource is a data source for reading SavedData files. SavedData files
 * are SQL tables. Each file has a file index number that is unique to the
 * owner and increments with each save. Each file has a file alias number that
 * has meaning to the owner. For the SaveRestore owner, the file alias number is
 * the Save File number and one Save File number should only be mapped to one
 * file index number that is NOT marked for delete. For SDA files, the file
 * alias number is the shot number, and multiple alias to file index numbers are
 * expected to exist. The highest file index number is provided by default when
 * mapping by store number. Within a file device/properties are unique within a
 * collection index number. The collection index number is incremented by a
 * DataScheduler and provided to DataDispositions. The collection alias number
 * has meaning to the owner; for example the collection alias for an SDA file is
 * the case number. Multiple collection indices may map a given collection
 * alias. The duplicates represent previous store overs in SDA's sense.
 */
public class SavedDataSource implements DaqPoolUserRequests<WhatDaq>, DbServer.Constants, AcnetErrors
{
	static final HashSet<String> owners = new HashSet<>(Arrays.asList("ColliderShot", "E835Store", 
																		"PbarTransferShot", "RecyclerShot"));

	private Vector<WhatDaq> userRequestList = null;
	private List<SnapRequest> snapShotRequestList = null;

	private PoolUser user = null;

	/**
	 * if snapshots are saved as blobs
	 */
	private boolean snapBlobs = true;

	/**
	 * @serial has sets within collections
	 */
	protected boolean sequencedDataSet = false;

	/**
	 * @serial data source is open when true
	 */
	private boolean openedSource = false;

	/**
	 * @serial header is needed on open when true
	 */
	private boolean needHeaderOnOpen = false;

	/**
	 * @serial delete older alias when true
	 */
	private boolean deleteOlderAlias = false;

	// ******* file header *******
	/**
	 * @serial file owner
	 */
	protected String owner;

	/**
	 * @serial file alias number (owner determined)
	 */
	protected int fileAlias = -1;

	/**
	 * @serial file index number (database incrementing)
	 */
	protected int fileIndex = -1;

	/**
	 * @serial (on retrieval) owner's collection alias
	 */
	private int fetchCollectionAlias = -1;

	/**
	 * @serial (on retrieval) owner's set within the collection alias
	 */
	private int fetchSetAlias = -1;

	/**
	 * @serial map from alias to index
	 */
	private boolean mapFileIndexOnOpen = false;

	/**
	 * @serial file title
	 */
	private String title = " ";

	/**
	 * @serial owner specified
	 */
	protected String comment1 = " ";

	/**
	 * @serial owner specified
	 */
	protected String comment2 = " ";

	/**
	 * @serial owner specified
	 */
	@SuppressWarnings("unused")
	private String comment3 = " ";

	/**
	 * @serial operator protection (must be cleared to delete)
	 */
	protected boolean protectOper;

	/**
	 * @serial archive protection (must be cleared to delete)
	 */
	protected boolean protectArchive;

	/**
	 * @serial modify protection (must be set to allow modify)
	 */
	protected boolean protectModify;

	/**
	 * @serial destroy protection (when set, high availability for reuse)
	 */
	protected boolean protectDestroy;

	/**
	 * @serial file is marked for delete
	 */
	protected boolean delete;

	protected DataEvent event;

	/**
	 * @serial file is incomplete
	 */
	protected boolean incomplete;

	/**
	 * @serial version of SavedData architecture
	 */
	protected short versionSavedData;

	/**
	 * @serial version of owner architecture
	 */
	protected short versionOwner;

	/**
	 * @serial save start time
	 */
	protected long startTime;

	/**
	 * @serial save end time
	 */
	protected long endTime;

	/**
	 *  The maximum data size
	 */
	public final static int MAXDATASIZE = 16384;

	/**
	 * The maximum number of bytes in the row
	 */
	public final static int MAXBYTESINROW = 192;

	/**
	 * The SAVE_USER string
	 */
	public final static String SAVE_USER = "SRSAVE";

	/**
	 * The SAVE_PASS string
	 */
	public final static String SAVE_PASS = "PW162888099";

	//private transient static int numReportStatistics = 0;
	

	//private transient static int numSleepWriteQueue = 0;

	//private transient static int numGetData = 0;

	//private transient static int numGetDataWhereClause = 0;

	//private transient static int numGetDataWhereResult = 0;

	//private transient static int numGetDataWhereIncomplete = 0;

	//private transient static int numGetDataWhereBadLength = 0;

	//private transient static String lastGetDataWhereBadLength = null;

	//private transient static int numGetDataWhereSleep = 0;

	//private transient static int numGetDataWhereOk = 0;

	//private transient static int numGetDataWhereException = 0;

	//private transient static String lastPadDataException = null;

	//private transient static String lastGetDataWhereArrayCopyException = null;

	//private transient static int numPadDataException = 0;

	//private transient static int numGetDataWhereArrayCopyException = 0;
	//private transient static int numAliasToIndexException = 0;
	//private transient static int numCancelUserRequestException = 0;
	//private transient static int numDeleteFileException = 0;
	//private transient static int numGetDataException = 0;
	//private transient static int numGetFTPDataBlobsException = 0;
	//private transient static int numGetFTPHeaderException = 0;
	//private transient static int numGetHeaderException = 0;
	//private transient static int numGetSnapDataBlobsException = 0;
	//private transient static int numGetSnapDataException = 0;
	//private transient static int numGetSnapHeaderException = 0;
	//private transient static int numSkipWhereNotEncompassing = 0;
	//private transient static int numNoSkipWhereNotEncompassing = 0;


	/**
	 * Open a SavedData file by index for reading and read its header. Used only
	 * by readers.
	 * 
	 * @param owner
	 *            the SavedData owner.
	 * @param fileIndex
	 *            the SavedData file index.
     * @exception AcnetStatusException
	 */
	 /*
	public SavedDataSource(String owner, int fileIndex) throws AcnetStatusException
	{
		this.owner = new String(owner);
		this.fileIndex = fileIndex;
		needHeaderOnOpen = true;

		if (owner.equalsIgnoreCase("ColliderShot"))
			sequencedDataSet = true;
		else if (owner.equalsIgnoreCase("E835Store"))
			sequencedDataSet = true;
		else if (owner.equalsIgnoreCase("PbarTransferShot"))
			sequencedDataSet = true;
		else if (owner.equalsIgnoreCase("RecyclerShot"))
			sequencedDataSet = true;
	}
	*/

	/**
	 * Open a SavedData file by index for reading and read its header. Used only
	 * by readers.
	 * 
	 * @param fileIndex
	 *            the SavedData file index.
     * @exception AcnetStatusException
	 */
	 /*
	public SavedDataSource(int fileIndex) throws AcnetStatusException {
		//super(DataSourceDefinitions.DATASOURCE_SAVED_DATA);
		this.fileIndex = fileIndex;
		deleteOlderAlias = false;
		mapFileIndexOnOpen = false;
		needHeaderOnOpen = true;
		openSavedDataSource();
		if (owner.equalsIgnoreCase("ColliderShot"))
			sequencedDataSet = true;
		else if (owner.equalsIgnoreCase("E835Store"))
			sequencedDataSet = true;
		else if (owner.equalsIgnoreCase("PbarTransferShot"))
			sequencedDataSet = true;
		else if (owner.equalsIgnoreCase("RecyclerShot"))
			sequencedDataSet = true;
	}
	*/

	/**
	 * Open a SavedData file by alias for writing.
	 * 
	 * @param owner
	 *            the SavedData owner.
	 * @param fileAlias
	 *            the SavedData file alias.
	 * @param append
	 *            appending to existing file when true.
	 * @param readHeader
	 *            read the header when true.
     * @exception AcnetStatusException
	 */
	 /*
	public SavedDataSource(String owner, int fileAlias, boolean append, boolean readHeader) throws AcnetStatusException
	{
		//super(DataSourceDefinitions.DATASOURCE_SAVED_DATA);
		this.owner = new String(owner);
		this.fileAlias = fileAlias;
		if (readHeader)
			needHeaderOnOpen = true;
		if (append || readHeader)
			mapFileIndexOnOpen = true; // appenders and readers
		if (owner.equalsIgnoreCase("ColliderShot"))
			sequencedDataSet = true;
		else if (owner.equalsIgnoreCase("E835Store"))
			sequencedDataSet = true;
		else if (owner.equalsIgnoreCase("PbarTransferShot"))
			sequencedDataSet = true;
		else if (owner.equalsIgnoreCase("RecyclerShot"))
			sequencedDataSet = true;
		//if (!sequencedDataSet && !append
		//		&& this instanceof SavedDataDisposition)
		//	deleteOlderAlias = true;
	}
	*/

	public SavedDataSource(int fileAlias)
	{
		this.owner = "SaveFile";
		this.fileAlias = fileAlias;
		this.mapFileIndexOnOpen = true;
		
	}

	/**
	 * Open a SavedData file by alias for writing.
	 * 
	 * @param owner
	 *            the SavedData owner.
	 * @param fileAlias
	 *            the SavedData file alias.
	 * @param collectionAlias
	 *            the collection alias, i.e. 4 for InjectProtons in
	 *            ColliderShot.
	 * @param setAlias
	 *            the set alias, i.e. 1 for bunch one when injecting protons.
     * @exception AcnetStatusException
	 */
	public SavedDataSource(String owner, int fileAlias, int collectionAlias, int setAlias) throws AcnetStatusException
	{
		//super(DataSourceDefinitions.DATASOURCE_SAVED_DATA);
		//this.owner = new String(owner);
		this.owner = owner;
		this.fileAlias = fileAlias;
		this.fetchCollectionAlias = collectionAlias;
		this.fetchSetAlias = setAlias;
		this.needHeaderOnOpen = true;
		this.sequencedDataSet = owners.contains(owner);

/*
		if (owner.equalsIgnoreCase("ColliderShot"))
			sequencedDataSet = true;
		else if (owner.equalsIgnoreCase("E835Store"))
			sequencedDataSet = true;
		else if (owner.equalsIgnoreCase("PbarTransferShot"))
			sequencedDataSet = true;
		else if (owner.equalsIgnoreCase("RecyclerShot"))
			sequencedDataSet = true;
		*/
		//if (!sequencedDataSet && this instanceof SavedDataDisposition)
		//	deleteOlderAlias = true;
	}

	/**
	 * Specify a collection of files for reading or open a SavedData file for
	 * writing with defaults specified by the owner.
	 * 
	 * @param owner
	 *            the SavedData owner.
     * @exception AcnetStatusException
	 */
	 /*
	public SavedDataSource(String owner) throws AcnetStatusException
	{
		//super(DataSourceDefinitions.DATASOURCE_SAVED_DATA);
		this.owner = new String(owner);
		if (owner.equalsIgnoreCase("ColliderShot"))
			sequencedDataSet = true;
		else if (owner.equalsIgnoreCase("E835Store"))
			sequencedDataSet = true;
		else if (owner.equalsIgnoreCase("PbarTransferShot"))
			sequencedDataSet = true;
		else if (owner.equalsIgnoreCase("RecyclerShot"))
			sequencedDataSet = true;
	}
	*/

	private int[] toIntArray(byte[] data)
	{
		try {
			final DataInputStream is = new DataInputStream(new ByteArrayInputStream(data));
			final int[] val = new int[data.length / 2];

			for (int ii = 0; ii < val.length; ii++)
				val[ii] = is.readShort() & 0xffff;

			is.close();

			return val;
		} catch (Exception e) { }

		return null;
	}

	private long[] toLongArray(byte[] data)
	{
		try {
			final DataInputStream is = new DataInputStream(new ByteArrayInputStream(data));
			final long[] val = new long[data.length / 8];

			for (int ii = 0; ii < val.length; ii++)
				val[ii] = is.readLong();

			is.close();

			return val;
		} catch (IOException e) {
			return null;
		}
	}

	private double[] toDoubleArray(byte[] data)
	{
		try {
			final DataInputStream is = new DataInputStream(new ByteArrayInputStream(data));
			final double[] val = new double[data.length / 8];

			for (int ii = 0; ii < val.length; ii++)
				val[ii] = is.readDouble();

			is.close();
			
			return val;
		} catch (IOException e) {
			return null;
		}
	}

	public void open() throws AcnetStatusException
	{
		open(null);
	}

	/**
	 * Open a SavedData on the engine.
     * @exception AcnetStatusException
	 */
	public void open(DataEvent event) throws AcnetStatusException
	{
		this.event = event;

		if (openedSource)
			throw new AcnetStatusException(DAE_SAVE_GET_EXCEPTION, "Source already open");

		userRequestList = new Vector<>();
		snapShotRequestList = new LinkedList<>();
		openedSource = true;
		// delete previous alias
		//if (deleteOlderAlias)
			//SavedDataDisposition.deleteSavedDataAlias(owner, fileAlias);
		// map fileAlias to fileIndex
		if (mapFileIndexOnOpen) {
			aliasToIndex();
			// System.out.println("sql for highest not marked for delete
			// index");
		}
		if (needHeaderOnOpen)
			getHeader();
	}

	/**
	 * Read the SavedData file header.
     * @exception AcnetStatusException
	 */
	private void getHeader() throws AcnetStatusException {
		//String whereAlias;
		//if ( DO_SYBASE )
		 //   whereAlias = "(select max(file_index) from srdb.finstrom.save_header "
		//		+ "where file_alias = " + fileAlias + " and delete_file = 0)";
		//if ( DO_POSTGRES )
		final String whereAlias = "(SELECT max(file_index) FROM srdb.save_header "
				+ "WHERE file_alias = " + fileAlias + " AND delete_file = 0)";
		String whereIndex = Integer.toString(fileIndex);
		String whereQualifier = whereAlias;
		if (fileAlias == -1 && fileIndex != -1)
			whereQualifier = whereIndex;
		// Denise, need two versions: owner and save, need delete boolean and
		// incomplete boolean
		//String query;
		//if ( DO_SYBASE )
		 //   query = "select file_alias, file_index, owner, title, comment1, "
		//		+ "comment2, comment3, operator_protected, archive_protected, modify_protected, "
		//		+ "destroy_protected, delete_file, incomplete, start_time, owner_version, "
		//		+ "save_version, stop_time from srdb.finstrom.save_header where "
		//		+ "delete_file = 0 and file_index = " + whereQualifier;
		//if ( DO_POSTGRES )
		final String query = "SELECT file_alias, file_index, owner, title, comment1, "
				+ "comment2, comment3, operator_protected, archive_protected, modify_protected, "
				+ "destroy_protected, delete_file, incomplete, start_time, owner_version, "
				+ "save_version, stop_time FROM srdb.save_header WHERE "
				+ "delete_file = 0 AND file_index = " + whereQualifier;

		//if (classBugs)
		//	System.out
		//			.println("GETHEADER: fileAlias=" + fileAlias
		//					+ ", fileIndex=" + fileIndex
		//					+ ", fetchCollectionAlias=" + fetchCollectionAlias
		//					+ ", fetchSetAlias=" + fetchSetAlias);
		try {
			//if (classBugs)
			//	System.out.println(query);
			//ResultSet rs;
			//if ( DO_SYBASE )
			 //   rs = BDBS.executeQuery(query);
			//if ( DO_POSTGRES )
			final ResultSet rs = getPostgreSQLServer("srdb").executeQuery(query);

			if (rs == null) {
				//System.out.println( "SavedDataSource.getHeader()'s query below returned null ResultSet:" );
				//System.out.println( query );
			} else {
				if (rs.next() == false) {
					//System.out.println( "SavedDataSource.getHeader()'s query below returned empty ResultSet:" );
					//System.out.println( query );
				} else {
					fileAlias = rs.getInt(1);
					fileIndex = rs.getInt(2);
					owner = rs.getString(3);
					title = rs.getString(4);
					comment1 = rs.getString(5);
					comment2 = rs.getString(6);
					//comment3 = rs.getString(7);
					protectOper = rs.getBoolean(8);
					protectArchive = rs.getBoolean(9);
					protectModify = rs.getBoolean(10);
					protectDestroy = rs.getBoolean(11);
					delete = rs.getBoolean(12);
					incomplete = rs.getBoolean(13);
					startTime = rs.getTimestamp(14).getTime();
					versionOwner = rs.getShort(15);
					versionSavedData = rs.getShort(16);
					if (rs.getTimestamp(17) != null)
						endTime = rs.getTimestamp(17).getTime();
				}
				rs.close();
			}
		} catch (SQLException sqe) {
			////++numGetHeaderException;
			//System.out.println("SavedData, getHeader, SQL exception" + sqe);
			throw new AcnetStatusException(DAE_SAVE_GET_EXCEPTION, "SavedData, getHeader, SQL exception", sqe);
		} catch (Exception ex) {
			//++numGetHeaderException;
			//System.out.println("SavedData, getHeader, exception" + ex);
			throw new AcnetStatusException(DAE_SAVE_GET_EXCEPTION, "SavedData, getHeader, exception", ex);
		}
	}

	/**
	 * Read the SnapShot file header.
     * @param collectionIndex
     * @param fileIndex
     * @param device
     * @return a read of SnapShot file header
     * @exception AcnetStatusException
	 */
	private ExtendedFTPRequest getFTPHeader(int fileIndex, int collectionIndex,
			WhatDaq device) throws AcnetStatusException {
		//if ( DO_SYBASE )
		 //   query = "select file_index, file_alias, collection_index, collection_alias "
		//		+ ", set_alias, owner, error, date, di, specification_event from srdb.finstrom.snap_header_blob "
		//		+ " where file_index = "
		//		+ fileIndex
		//		+ " and "
		//		+ "collection_index = "
		//		+ collectionIndex
		//		+ " and di = "
		//		+ device.getDeviceIndex() + " and is_ftp=1";
		//if ( DO_POSTGRES )
		final String query = "SELECT file_index, file_alias, collection_index, collection_alias "
				+ ", set_alias, owner, error, date, di, specification_event FROM srdb.snap_header_blob "
				+ " WHERE file_index = "
				+ fileIndex
				+ " AND "
				+ "collection_index = "
				+ collectionIndex
				+ " AND di = "
				+ device.getDeviceIndex() + " AND is_ftp=1";

		Trigger trigger = null;
		FTPScope scope = null;
		// ReArm reArm = null;
		FTPRequest ftpReq = null;
		long snapDate = System.currentTimeMillis();
		int snapError = DAE_SAVE_HEADER_NOTFOUND;

		//if (classBugs)
		//	System.out
		//			.println("GETSNAPHEADER: fileAlias=" + fileAlias
		//					+ ", fileIndex=" + fileIndex
		//					+ ", fetchCollectionAlias=" + fetchCollectionAlias
		//					+ ", fetchSetAlias=" + fetchSetAlias);
		try {
		//	if (classBugs)
		//		System.out.println(query);
			ResultSet rs;
			//if ( DO_SYBASE )
			 //   rs = BDBS.executeQuery(query);
			//if ( DO_POSTGRES )
			    rs = getPostgreSQLServer("srdb").executeQuery(query);
			if (rs.next()) {
				fileIndex = rs.getInt(1);
				fileAlias = rs.getInt(2);
				collectionIndex = rs.getInt(3);
				fetchCollectionAlias = rs.getInt(4);
				fetchSetAlias = rs.getInt(5);
				owner = rs.getString(6);
				snapError = rs.getShort(7);
				snapDate = rs.getTimestamp(8).getTime();
				String snapSpecification = rs.getString(10);
				try {
					scope = FTPScope.getFTPScope(snapSpecification);
					trigger = SnapScope.getTrigger(snapSpecification);
					ftpReq = new FTPRequest(device, null, trigger, scope, null, new ReArm(false));
				} catch (Exception e) {
					//++numGetFTPHeaderException;
					//System.out.println("SavedDataSource snapSpecification: "
					//		+ snapSpecification + ", e: " + e);
					//e.printStackTrace();
				}
			}
			rs.close();
			return (new ExtendedFTPRequest(ftpReq, snapDate, snapError));
		} catch (SQLException sqe) {
			//++numGetFTPHeaderException;
			//System.out.println("SavedData, getSnapHeader, SQL exception" + sqe);
			throw new AcnetStatusException(DAE_SAVE_GET_EXCEPTION, "SavedData, getSnapHeader, SQL exception", sqe);
		} catch (Exception ex) {
			//++numGetFTPHeaderException;
			//System.out.println("SavedData, getSnapHeader, exception" + ex);
			//ex.printStackTrace();
			throw new AcnetStatusException(DAE_SAVE_GET_EXCEPTION, "SavedData, getSnapHeader, exception", ex);
		}
	}

	private ExtendedSnapRequest getSnapHeader(int fileIndex, int collectionIndex, WhatDaq device) throws AcnetStatusException
	{
		final String query = "SELECT file_index, file_alias, collection_index, collection_alias "
				+ ", set_alias, owner, error, date, di, specification_event FROM srdb.snap_header_blob "
				+ " WHERE file_index = "
				+ fileIndex
				+ " AND "
				+ "collection_index = "
				+ collectionIndex
				+ " AND di = "
				+ device.getDeviceIndex() + " AND is_ftp=0";

		Trigger trigger = null;
		SnapScope scope = null;
		ReArm reArm = null;
		SnapRequest snapReq = null;
		long snapDate = System.currentTimeMillis(); 
		int snapError = DAE_SAVE_HEADER_NOTFOUND;

		try {
			ResultSet rs;
			    rs = getPostgreSQLServer("srdb").executeQuery(query);

			if (rs.next()) {
				fileIndex = rs.getInt(1);
				fileAlias = rs.getInt(2);
				collectionIndex = rs.getInt(3);
				fetchCollectionAlias = rs.getInt(4);
				fetchSetAlias = rs.getInt(5);
				owner = rs.getString(6);
				snapError = rs.getShort(7);
				snapDate = rs.getTimestamp(8).getTime();
				String snapSpecification = rs.getString(10);
				try {
					scope = SnapScope.getSnapScope(snapSpecification);
					trigger = SnapScope.getTrigger(snapSpecification);
					reArm = SnapScope.getReArm(snapSpecification);
					snapReq = new SnapRequest(device, null, trigger, scope,
							null, reArm);
				} catch (Exception e) {
				}
			}
			rs.close();
			return (new ExtendedSnapRequest(snapReq, snapDate, snapError));
		} catch (SQLException sqe) {
			throw new AcnetStatusException(DAE_SAVE_GET_EXCEPTION, "SavedData, getSnapHeader, SQL exception", sqe);
		} catch (Exception ex) {
			throw new AcnetStatusException(DAE_SAVE_GET_EXCEPTION, "SavedData, getSnapHeader, exception", ex);
		}
	}

	public void aliasToIndex() throws AcnetStatusException
	{
		final String query = "SELECT max(file_index) FROM srdb.save_header "
				+ "WHERE file_alias = " + fileAlias + " AND delete_file = 0 ";
		try {
			final ResultSet rs = getPostgreSQLServer("srdb").executeQuery(query);
			rs.next();
			fileIndex = rs.getInt(1);
			rs.close();
		} catch (SQLException sqe) {
			throw new AcnetStatusException(DAE_SAVE_GET_EXCEPTION, "SavedData, aliasToIndex, SQL exception", sqe);
		} catch (Exception ex) {
			throw new AcnetStatusException(DAE_SAVE_GET_EXCEPTION, "SavedData, aliasToIndex, exception", ex);
		}
	}

	public void setUser(PoolUser user)
	{
		this.user = user;
	}

	public void insertUserRequest(SnapRequest snapRequest)
	{
		snapShotRequestList.add(snapRequest);
	}

	@Override
	public void insert(WhatDaq userWhatDaq)
	{
		userRequestList.addElement(userWhatDaq);
	}

	@Override
	public void cancel(PoolUser user, int error)
	{
		final long now = System.currentTimeMillis();

		for (WhatDaq whatDaq : userRequestList) {
			if (error != 0 && !whatDaq.isMarkedForDelete())
				whatDaq.getReceiveData().receiveStatus(error, now, 0);
			whatDaq.setMarkedForDelete();
		}
		for (SnapRequest req : snapShotRequestList) {
			req.callback.plotData(now, error, 0, null, null, null);
			req.setDelete();
		}
	}

	@Override
	public synchronized boolean process(boolean forceRetransmission)
	{
		int[] fileIndices = null;
		int[] collectionIndices = null;
		String whereClause = null;
		boolean useWhereClause = false;

		if (event instanceof SavedDataEvent) {
			SavedDataEvent saveEvent = (SavedDataEvent) event;
			fileIndices = saveEvent.getFileIndices();
			collectionIndices = saveEvent.getCollectionIndices();
			whereClause = saveEvent.getWhereClause();
			useWhereClause = saveEvent.isByWhereClause();
		}

		while (!snapShotRequestList.isEmpty()) {
			Object o = snapShotRequestList.remove(0);
			if (o instanceof SnapRequest) {
				SnapRequest nxtReq = (SnapRequest) o;
				if (nxtReq.getDelete())
					continue;
				getSnapData(nxtReq, fileIndices, collectionIndices);
			} else if (o instanceof FTPRequest) {
				FTPRequest nxtReq = (FTPRequest) o;
				if (nxtReq.getDelete())
					continue;
				getFTPDataBlobs(nxtReq, fileIndices, collectionIndices);
			}
		}
		
		WhatDaq nxtWhat;
		
		while (!userRequestList.isEmpty()) {
			nxtWhat = userRequestList.firstElement();
			userRequestList.removeElementAt(0);
			if (nxtWhat.isMarkedForDelete())
				continue;
			if (useWhereClause)
				getDataWhereClause(nxtWhat, whereClause);
			else
				getData(nxtWhat, fileIndices, collectionIndices);
		}
		user.complete();
		return false;
	}

	public void getSnapShotReqList(List<SnapRequest> snapReqs)
	{
		snapShotRequestList = snapReqs;
	}

	private void getData(WhatDaq whatDaq, int[] fileIndices, int[] collectionIndices)
	{
		int segmentNumber = 0;
		int error = 0;
		long holdTime = 0;

		long time = System.currentTimeMillis();
		byte[] tmpData = null;
		byte[] holdData = null;
		int lengthDiff = 0;
		int dbLength = 0, dbOffset = 0;
		int desiredLength = whatDaq.getLength();
		int desiredOffset = whatDaq.getOffset();
		boolean restoreDesiredLengthOffset = false;
		int dbFileIndex = 0, dbFileAlias = 0;
		int dbCollectionIndex = 0, dbCollectionAlias = 0, dbSetAlias = 0;
		int saveFileIndex = 0, saveCollectionIndex = 0, saveSetAlias = 0;
		boolean firstTime = true, incompleteData = false;
		byte[] finalData = null;
		byte[] data = null;

		try {
			finalData = new byte[MAXDATASIZE];
			if (sequencedDataSet == false) { // SaveRestore data
				aliasToIndex();

				final String srQuery = "SELECT di, pi, length, error, timestamp, data "
						+ "FROM srdb.srsave_data where "
						+ "file_index = " + fileIndex + " AND di = "
						+ whatDaq.getDeviceIndex() + " AND pi = "
						+ whatDaq.getPropertyIndex() + " ORDER BY segment";

				final ResultSet rs = getPostgreSQLServer("srdb").executeQuery(srQuery);

				rs.next();

				try {
					error = rs.getInt(4);
				} catch (Exception e) {
					whatDaq.getReceiveData().receiveStatus(SAV_RST_NODI, time, 0);

					return;
				}

				if (rs == null || error == SAV_RST_NODATA) {
					dbLength = 0;
					error = SAV_RST_NODATA;
					holdData = null;
				} else {
					dbLength = rs.getInt(3);
					error = rs.getShort(4);
					time = startTime = rs.getTimestamp(5).getTime();
					holdData = rs.getBytes(6);
				}
				firstTime = false;
				if (holdData == null)
					finalData = null;
				else {
					lengthDiff = padAmount(dbLength, holdData.length,
							segmentNumber);
					if (lengthDiff > 0) {
						tmpData = padData(holdData, holdData.length
								+ lengthDiff);
						holdData = tmpData;
					}
					System.arraycopy(holdData, 0, finalData, 0, holdData.length);
				}

				/*
				 * if there's more than 1 record (length > 192), all we need is
				 * the data and length
				 */
				while (rs.next()) {
					segmentNumber++;
					dbLength = rs.getInt(3);

					holdData = rs.getBytes(6);
					if (holdData == null) {
						finalData = null;
					} else {
						lengthDiff = padAmount(dbLength, holdData.length, segmentNumber);
						if (lengthDiff > 0) {
							tmpData = padData(holdData, holdData.length + lengthDiff);
							holdData = tmpData;
						}
						System.arraycopy(holdData, 0, finalData,
								(segmentNumber * MAXBYTESINROW),
								holdData.length);
					}
				}
				rs.close();
			} else {
				final String sdaQuery = "SELECT sd.file_index, file_alias, collection_index, "
						+ "collection_alias, set_alias, segment, error, timestamp, data, "
						+ "length, offsett FROM srdb.sda_data sd, srdb.save_header sh "
						+ "WHERE sd.file_index = sh.file_index AND di = "
						+ whatDaq.getDeviceIndex()
						+ " AND pi = "
						+ whatDaq.getPropertyIndex()
						+ " AND ((sd.file_index = " + fileIndices[0];
				String sdaQueryEnd = ") order by sd.file_index, collection_index, set_alias, segment ";
				StringBuffer sdaBuffer = new StringBuffer(sdaQuery.length() + sdaQueryEnd.length() + 5000);
				sdaBuffer.append(sdaQuery);

				if (collectionIndices == null) {
					sdaBuffer.append(")");
					for (int ii = 1; ii < fileIndices.length; ii++) {
						sdaBuffer.append(" or (sd.file_index = " + fileIndices[ii] + ")");
					}
				} else {
					sdaBuffer.append(" and collection_index = " + collectionIndices[0] + ")");
					for (int ii = 1; ii < fileIndices.length; ii++) {
						sdaBuffer.append(" or (sd.file_index = " + fileIndices[ii] + " and collection_index = " + collectionIndices[ii] + ")");
					}
				}
				sdaBuffer.append(sdaQueryEnd);

				final ResultSet rs = getPostgreSQLServer("srdb").executeQuery(sdaBuffer.toString());

				while (rs != null && rs.next()) {
					/* get all information for 1st record */
					/* additional di/pi check */
					if (restoreDesiredLengthOffset) {
						restoreDesiredLengthOffset = false;
						whatDaq.setLengthOffset(desiredLength, desiredOffset);
					}
					dbFileIndex = rs.getInt(1);
					dbFileAlias = rs.getInt(2);
					dbCollectionIndex = rs.getInt(3);
					dbCollectionAlias = rs.getInt(4);
					dbSetAlias = rs.getInt(5);

					dbLength = rs.getInt(10);
					dbOffset = rs.getInt(11);

					short dbSegment = rs.getShort(6);
					// might be other matchers, Store Checker does not want this					
					if (firstTime == true) {
						if (dbSegment != 0)
							incompleteData = true;
					}
					if (((dbFileIndex != saveFileIndex) || (dbCollectionIndex != saveCollectionIndex) || (dbSetAlias != saveSetAlias)) && (firstTime != true)) {
						if (incompleteData)
							whatDaq.getReceiveData().receiveStatus(DAE_SAVE_INCOMPLETE_DATA, time, 0);
						else {
							if (whatDaq.getLength() > dbLength && whatDaq.getLength() == whatDaq .getMaxLength() && whatDaq.getOffset() == 0) {
								restoreDesiredLengthOffset = true;
								whatDaq.setLengthOffset(dbLength, dbOffset);
							}
							if ((whatDaq.getOffset() < dbOffset) || (whatDaq.getOffset() > (dbOffset + dbLength)) || ((whatDaq.getOffset() + whatDaq .getLength()) > (dbOffset + dbLength))) {
								finalData = new byte[MAXDATASIZE];
								whatDaq.getReceiveData().receiveData(SAV_RST_ILLEN, time, 0);
							} else if (finalData == null) { // no data
								finalData = new byte[MAXDATASIZE];
								whatDaq.getReceiveData().receiveData(SAV_RST_NODATA, time, 0);
							} else {
								data = new byte[whatDaq.getLength()];
								System.arraycopy(finalData, whatDaq.getOffset() - dbOffset, data, 0, whatDaq.getLength());
								whatDaq.getReceiveData().receiveData(data, 0, time, 0);
							}
						}
						segmentNumber = 0;
						incompleteData = (dbSegment != 0);
					}
					saveFileIndex = dbFileIndex;
					saveCollectionIndex = dbCollectionIndex;
					saveSetAlias = dbSetAlias;
					error = rs.getShort(7);

					holdTime = rs.getLong(8);

					holdData = rs.getBytes(9);
					dbLength = rs.getInt(10);
					dbOffset = rs.getInt(11);

					if ((holdData == null) || (incompleteData == true))
						finalData = null;
					else {
						lengthDiff = padAmount(dbLength, holdData.length, segmentNumber);
						if (lengthDiff > 0) {
							tmpData = padData(holdData, holdData.length
									+ lengthDiff);
							holdData = tmpData;
						}
						if (finalData == null)
							finalData = new byte[MAXDATASIZE];
						System.arraycopy(holdData, 0, finalData,
								(segmentNumber * MAXBYTESINROW),
								holdData.length);
					}
					segmentNumber++;
					firstTime = false;
				}
				rs.close();
			}

			if (firstTime == true) {
				whatDaq.getReceiveData().receiveStatus(SAV_RST_NODATA, time, 0);
			} else {// query returned a record
				if (incompleteData == true) {// missing data segments
					whatDaq.getReceiveData().receiveStatus(DAE_SAVE_INCOMPLETE_DATA, time, 0);
				} else {
					if (whatDaq.getLength() > dbLength && whatDaq.getLength() == whatDaq.getMaxLength() && whatDaq.getOffset() == 0) {
						restoreDesiredLengthOffset = true;
						whatDaq.setLengthOffset(dbLength, dbOffset);
					}
					if ((whatDaq.getOffset() < dbOffset) || (whatDaq.getOffset() > (dbOffset + dbLength)) || ((whatDaq.getOffset() + whatDaq.getLength()) > (dbOffset + dbLength))) { 
						whatDaq.getReceiveData().receiveStatus(SAV_RST_ILLEN, time, 0);

					} else if (finalData == null) // no data
						whatDaq.getReceiveData().receiveStatus(SAV_RST_NODATA, time, 0);
					else {
						data = new byte[whatDaq.getLength()];
						// System.out.println("off "+whatDaq.getOffset()+" len
						// "+whatDaq.getLength()+" dboff "+dbOffset+" dblen
						// "+dbLength+" data.length "+data.length);
						// We have been here before - tbolsh
						System.arraycopy(finalData, whatDaq.getOffset() - dbOffset, data, 0, whatDaq.getLength());
						// System.arraycopy(finalData, 0, data, 0,
						// whatDaq.getLength());
						//if (classBugs)
						//	System.out
						//			.println("SavedDataSource.getData(): ending receiveData()\n"
						////					+ dbFileAlias + ", " + dbCollectionAlias 
						//					+ ", " + dbSetAlias);

						whatDaq.getReceiveData().receiveData(data, 0, time, 0);
					}
					//whatDaq.getReceiveData().receiveData(whatDaq, error, 0, data, time, holdContext);
					//whatDaq.getReceiveData().receiveData(error, 0, data, time);
				}
			}
		} catch (Exception e) {
			//++numGetDataException;
			//error = DAE_SAVE_GET_EXCEPTION;
			//System.out.println("SavedDataSource.getData(): " + ex
			//		+ "\n\tquery: " + srQuery);
			//ex.printStackTrace();
			//whatDaq.getReceiveData().receiveData(whatDaq, error, 0, data, time, holdContext);
			//whatDaq.getReceiveData().receiveData(error, 0, data, time);
			whatDaq.getReceiveData().receiveStatus(DAE_SAVE_GET_EXCEPTION, time, 0);
		}
	}

	/**
	 * Get the data, sending it to receiveData of whatDaq.
	 * 
	 * @param whatDaq
	 *            what to get
     * @param whereClause
	 */
	private void getDataWhereClause(WhatDaq whatDaq, String whereClause) {
		//++numGetDataWhereClause;
		//CallbackDisposition callbackDisposition = null;
		int countReceiveData = 0;
		int dbDi = whatDaq.getDeviceIndex();
		int dbPi = whatDaq.getPropertyIndex();
		int segmentNumber = 0;
		int error = 0;
		int holdError = 0;
		//CollectionContext holdContext = null;

		long holdTime = 0;

		byte[] tmpData = null;
		byte[] holdData = null;
		int lengthDiff = 0;
		int dbLength = 0, dbOffset = 0;
		int dbFileIndex = 0, dbFileAlias = 0;
		int desiredLength = whatDaq.getLength();
		int desiredOffset = whatDaq.getOffset();
		boolean restoreDesiredLengthOffset = false;
		int dbCollectionIndex = 0, dbCollectionAlias = 0, dbSetAlias = 0;
		int saveFileIndex = 0, saveCollectionIndex = 0, saveSetAlias = 0;
		boolean firstTime = true, incompleteData = false;
		byte[] finalData = null;
		byte[] data = null;

		//boolean debug = false;
		//if (false && whatDaq.getDeviceName().startsWith("I:SBD04X")) {
		//	System.out.println("getData: " + whatDaq);
		//	debug = true;
		//}

		try {
			finalData = new byte[MAXDATASIZE]; // Binary data from database
			//if (classBugs)
			//	System.out.println("getDataWhereClause: " + whereClause
			//			+ " for " + whatDaq);
			//if ( DO_SYBASE )
			 //   sdaQuery = "select sd.file_index, file_alias, collection_index, " // 1-4
			//		+ "collection_alias, set_alias, segment, error, timestamp, data, " // 4-9
			//		+ "length, offset " // 10-11
			//		// + "from srdb.finstrom.sda_data sd,
			//		// srdb.finstrom.save_header sh "
			//		+ "from srdb.finstrom.sda_data sd, srdb.finstrom.save_header sh "
			//		+ "where sh.owner = '"
			//		+ owner
			//		+ "' and sd.file_index = sh.file_index and di = "
			//		+ dbDi
			//		+ " and pi = " + dbPi;
			//if ( DO_POSTGRES )
			final String sdaQuery = "SELECT sd.file_index, file_alias, collection_index, " // 1-4
					+ "collection_alias, set_alias, segment, error, timestamp, data, " // 4-9
					+ "length, offsett " // 10-11
					// + "FROM srdb.sda_data sd,
					// srdb.save_header sh "
					+ "FROM srdb.sda_data sd, srdb.save_header sh "
					+ "WHERE sh.owner = '"
					+ owner
					+ "' AND sd.file_index = sh.file_index AND di = "
					+ dbDi
					+ " AND pi = " + dbPi;
			String sdaQueryEnd = " order by sd.file_index, collection_index, segment ";
			StringBuffer sdaBuffer = new StringBuffer(sdaQuery.length()
					+ sdaQueryEnd.length() + 5000);
			sdaBuffer.append(sdaQuery);
			// JD:  The where clause comes from the SavedDataEvent,
			// and seems to be used by KevinQuickTest and
			// SDADataItem.  Advise removing this functioality.
			if (whereClause != null)
				sdaBuffer.append(" and " + whereClause);
			sdaBuffer.append(sdaQueryEnd);
			//if (debug || classBugs)
			//	System.out
			//			.println("SavedDataSource.getDataWhereClause(): executing query = "
			//					+ sdaBuffer.toString());
			ResultSet rs;
			//if ( DO_SYBASE )
			 //   rs = BDBS.executeQuery(sdaBuffer.toString());
			//if ( DO_POSTGRES )
			    rs = getPostgreSQLServer("srdb").executeQuery(sdaBuffer.toString());

			while (rs.next()) {
				//++numGetDataWhereResult;
				// Thread.currentThread().yield();
				if (restoreDesiredLengthOffset) {
					//if (debug) System.out.println("restoreDesired");
					restoreDesiredLengthOffset = false;
					whatDaq.setLengthOffset(desiredLength, desiredOffset);
				}
				//if (callbackDisposition != null
				//		&& (++countReceiveData % 50) == 0) {
				//	while (callbackDisposition.sendQueueSize() > (CallbackDisposition.RMI_SEND_SIZE << 1)) {
				//		++numGetDataWhereSleep;
						// System.out.println("SavedDataSource, sleep while send
						// queue empties: " +
						// callbackDisposition.sendQueueSize());
				//		Thread.sleep(100L);
				//	}
				//}
				/* get all information for 1st record */
				/* additional di/pi check */
				dbFileIndex = rs.getInt(1);
				dbFileAlias = rs.getInt(2);
				dbCollectionIndex = rs.getInt(3);
				dbCollectionAlias = rs.getInt(4);
				dbSetAlias = rs.getInt(5);
				short dbSegment = rs.getShort(6);
				error = rs.getShort(7);
				holdData = rs.getBytes(9);
				dbLength = rs.getInt(10);
				dbOffset = rs.getInt(11);
				// might be other matchers, SDA data logging needs this				
				if ((whatDaq.getOffset() < dbOffset)			
						|| (whatDaq.getOffset() > (dbOffset + dbLength))
						|| ((whatDaq.getOffset() + whatDaq
								.getLength()) > (dbOffset + dbLength))) { 
					//++numSkipWhereNotEncompassing;
					//if (debug) System.out.println("skip, dbLength: " + dbLength + ", dbOffset: " + dbOffset);
					continue;
				}

				if (firstTime == true) {
					//holdContext = new SDAContext(owner, dbFileAlias,
					//		dbFileIndex, dbCollectionAlias, dbCollectionIndex,
					//		dbSetAlias);
					holdTime = rs.getLong(8);
					holdError = error;
					if (dbSegment != 0)
						incompleteData = true;
				}
				if (((dbFileIndex != saveFileIndex)
						|| (dbCollectionIndex != saveCollectionIndex) || (dbSetAlias != saveSetAlias))
						&& (firstTime != true)) {
					if (incompleteData == true) {
						//++numGetDataWhereIncomplete;
						//whatDaq.getReceiveData().receiveData(whatDaq, DAE_SAVE_INCOMPLETE_DATA, 0, null, 
						//										holdTime == null ? 0 : holdTime.getTime(), holdContext);
						whatDaq.getReceiveData().receiveData(DAE_SAVE_INCOMPLETE_DATA, holdTime, 0);
																//holdTime == null ? 0 : holdTime.getTime());
					} else {
						if (whatDaq.getLength() > dbLength && whatDaq.getLength() == whatDaq .getMaxLength() && whatDaq.getOffset() == 0) {
							restoreDesiredLengthOffset = true;
							whatDaq.setLengthOffset(dbLength, dbOffset);
						}
						if ((whatDaq.getOffset() < dbOffset) || (whatDaq.getOffset() > (dbOffset + dbLength)) || ((whatDaq.getOffset() + whatDaq.getLength()) > (dbOffset + dbLength))) { 
							// users request cant be satisfied with saved data

							//lastGetDataWhereBadLength = "SavedDataSource.getDataWhereClause, refusal # 1, dbo: "
							//		+ dbOffset
							//		+ ", dbl: "
							//		+ dbLength
							//		+ ", w: "
							//		+ whatDaq;
							//++numGetDataWhereBadLength;
							/*
							error = SAV_RST_ILLEN;
							data = null;
							*/
							finalData = new byte[MAXDATASIZE];
							whatDaq.getReceiveData().receiveStatus(holdError, holdTime, 0);
						} else if (finalData == null) { // no data
							//data = null;
							finalData = new byte[MAXDATASIZE];
							whatDaq.getReceiveData().receiveStatus(holdError, holdTime, 0);
						} else {// copy data
							data = new byte[whatDaq.getLength()];
							System.arraycopy(finalData, 0, data, 0, whatDaq .getLength());
							whatDaq.getReceiveData().receiveData(data, 0, holdTime, 0);
						}
						//++numGetDataWhereOk;
						//if (debug || classBugs)
						//	System.out
						//			.println("SavedDataSource, call receiveData ok "
						//					+ whatDaq
						//					+ ", dataLen: "
						//					+ (data != null ? Integer
						//							.toString(data.length)
						//							: "null")
						//					+ ", err: " + Integer.toHexString(holdError & 0xffff)
						//					+ ", ");// + holdContext);
						//whatDaq.getReceiveData().receiveData(whatDaq, holdError, 0, data, 
						//										holdTime == null ? 0 : holdTime.getTime(), holdContext);
						//whatDaq.getReceiveData().receiveData(holdError, 0, data, holdTime); //holdTime == null ? 0 : holdTime.getTime());
					}
					segmentNumber = 0;
					incompleteData = (dbSegment != 0);
				}
				//holdContext = new SDAContext(owner, dbFileAlias, dbFileIndex,
				//		dbCollectionAlias, dbCollectionIndex, dbSetAlias);
				//holdTime = rs.getTimestamp(8);
				holdTime = rs.getLong(8);
				holdError = error;
				saveFileIndex = dbFileIndex;
				saveCollectionIndex = dbCollectionIndex;
				saveSetAlias = dbSetAlias;

				if ((holdData == null) || (incompleteData == true))
					finalData = null; // fill data buffer with null
				else { // not in error, so get and copy data
					lengthDiff = padAmount(dbLength, holdData.length,
							segmentNumber);
					//if (classBugs) {
					//	if (lengthDiff < 0)
					//		System.out.println("dsf:  di = "
					//				+ whatDaq.getDeviceIndex() + ", pi = "
					//				+ whatDaq.getPropertyIndex()
					//				+ ", dbLength = " + dbLength
					//				+ ", holdData.length = " + holdData.length
					//				+ ", segmentNumber = " + segmentNumber);
					//}
					if (lengthDiff > 0) {
						try {
							tmpData = padData(holdData, holdData.length
									+ lengthDiff);
							holdData = tmpData;
						} catch (Exception e) {
							//++numPadDataException;
							//lastPadDataException = "holdData.length: "
							//		+ holdData.length + ", lengthDiff: "
							//		+ lengthDiff + ", e: " + e;
							//System.out.println(lastPadDataException);
							//e.printStackTrace();
						}
					}
					try {
						System.arraycopy(holdData, 0, finalData,
								(segmentNumber * MAXBYTESINROW),
								holdData.length);
					} catch (Exception e) {
						//++numGetDataWhereArrayCopyException;
						//lastGetDataWhereArrayCopyException = "holdData: "
						//		+ holdData
						//		+ (holdData != null ? "length: "
						//				+ holdData.length
						//				: " hold data is null")
						//		+ ", finalData: "
						//		+ finalData
						//		+ (finalData != null ? "length: "
						//				+ finalData.length
						//				: " final data is null")
						//		+ ", segmentNumber: " + segmentNumber + ", "
						//		+ e + whatDaq; // + ", " + holdContext;
						// System.out.println(lastGetDataWhereArrayCopyException);
					}
				}
				segmentNumber++;
				firstTime = false;
			}
			rs.close();
			if (firstTime == true) { // We only get here if the query returns
										// no data at all
				//holdContext = null;
				holdTime = System.currentTimeMillis();
			} else {// query returned a record
				if (incompleteData == true) // missing data segments
				{
					//if (classBugs)
					//	System.out.println("in incompleteData");
					//++numGetDataWhereIncomplete;
					//whatDaq.getReceiveData().receiveData(whatDaq, DAE_SAVE_INCOMPLETE_DATA, 0, null, holdTime.getTime(), holdContext);
					//whatDaq.getReceiveData().receiveData(DAE_SAVE_INCOMPLETE_DATA, 0, null, holdTime); //holdTime.getTime());
					whatDaq.getReceiveData().receiveStatus(DAE_SAVE_INCOMPLETE_DATA, holdTime, 0);
				} else {
					if (whatDaq.getLength() > dbLength && whatDaq.getLength() == whatDaq.getMaxLength() && whatDaq.getOffset() == 0) {
						restoreDesiredLengthOffset = true;
						whatDaq.setLengthOffset(dbLength, dbOffset);
					}
					if ((whatDaq.getOffset() < dbOffset) || (whatDaq.getOffset() > (dbOffset + dbLength)) || ((whatDaq.getOffset() + whatDaq.getLength()) > (dbOffset + dbLength))) { 
						// users request can't be satisfied with saved data
						//System.out.println("SavedDataSource.getDataWhereClause, refusal # 2, o: " + whatDaq.getOffset() + ", l: "
						//		+ whatDaq.getLength() + ", dbo: " + dbOffset
						//		+ ", dbl: " + dbLength + ", w: " + whatDaq);
						/*
						error = SAV_RST_ILLEN;
						data = null;
						*/
						whatDaq.getReceiveData().receiveStatus(SAV_RST_ILLEN, holdTime, 0); //holdTime.getTime());
					} else if (finalData == null) // no data
						//data = null;
						whatDaq.getReceiveData().receiveStatus(SAV_RST_NODATA, holdTime, 0); //holdTime.getTime());
					else // copy data
					{
						data = new byte[whatDaq.getLength()];
						// System.out.println("off "+whatDaq.getOffset()+" len
						// "+whatDaq.getLength()+" dboff "+dbOffset+" dblen
						// "+dbLength+" data.length "+data.length);
						// System.arraycopy(finalData, whatDaq.getOffset(),
						// data, 0, whatDaq.getLength());
						System.arraycopy(finalData, 0, data, 0, whatDaq .getLength());
						//if (debug || classBugs)
						//	System.out
						//			.println("SavedDataSource.getDataWhereClause(): calling ending receiveData()\n"
						//							+ dbFileAlias + ", " + dbCollectionAlias 
						//							+ ", " + dbSetAlias);
						whatDaq.getReceiveData().receiveData(data, 0, holdTime, 0); //holdTime.getTime());
					}
					//++numGetDataWhereOk;
					//if (classBugs)
					//	System.out
					//			.println("SavedDataSource, call receiveData ok "
					//					+ whatDaq + ", dataLen: " + data.length);
					//whatDaq.getReceiveData().receiveData(whatDaq, error, 0, data, holdTime.getTime(), holdContext);
					//whatDaq.getReceiveData().receiveData(error, 0, data, holdTime); //holdTime.getTime());
				}
			}
		} catch (Exception e) {
			//error = DAE_SAVE_GET_EXCEPTION;
			//whatDaq.getReceiveData().receiveData(whatDaq, error, 0, data, holdTime.getTime(), holdContext);
			//whatDaq.getReceiveData().receiveData(error, 0, data, holdTime); //holdTime.getTime());
			whatDaq.getReceiveData().receiveStatus(DAE_SAVE_GET_EXCEPTION, holdTime, 0); //holdTime.getTime());
		}
	}

	/**
	 * Get the data, sending it to receiveData of whatDaq.
	 * 
     * @param snapReq
     * @param fileIndices
     * @param collectionIndices
	 */
	private void getSnapData(SnapRequest snapReq, int[] fileIndices,
			int[] collectionIndices) {
		//CollectionContext context = null;
		int dbFileIndex = 0, dbFileAlias = 0;
		int dbCollectionIndex = 0, dbCollectionAlias = 0, dbSetAlias = 0;
		int saveFileIndex = 0, saveCollectionIndex = 0, saveSetAlias = 0;
		ExtendedSnapRequest newSnapReq = null;
		long[] holdMicroSecs = new long[16384];
		int[] holdNanoSecs = new int[16384];
		double[] holdValues = new double[16384];
		long[] microSecs = null;
		int[] nanoSecs = null;
		double[] values = null;
		boolean firstTime = true;
		int index = 0;

		if (snapBlobs) {
			getSnapDataBlobs(snapReq, fileIndices, collectionIndices);
			return;
		}
		try {
			//if (classBugs)
			//	System.out.println("GETSNAPDATA: fileIndex=" + fileIndex
			//			+ ", fileAlias=" + fileAlias
			//			+ ", fetchCollectionAlias=" + fetchCollectionAlias
			//			+ ", fetchSetAlias=" + fetchSetAlias);

			/* dsf - new snap query */
			//if ( DO_SYBASE )
			 //   snapQuery = "select file_index, file_alias, collection_index, "
			//		+ "collection_alias, set_alias, microsecs, nanosecs, data "
			//		+ "from srdb.finstrom.snap_data sd, srdb.finstrom.snap_header sh "
			//		+ "where sd.snap_index = sh.snap_data_index and di = "
			//		+ snapReq.getDevice().getDeviceIndex()
			//		+ " and ((file_index = ";
			//if ( DO_POSTGRES )
			final String snapQuery = "SELECT file_index, file_alias, collection_index, "
					+ "collection_alias, set_alias, microsecs, nanosecs, data "
					+ "FROM srdb.snap_data sd, srdb.snap_header sh "
					+ "WHERE sd.snap_index = sh.snap_data_index AND di = "
					+ snapReq.getDevice().getDeviceIndex()
					+ " AND ((file_index = ";
			String snapQueryEnd = ") order by file_index, collection_index, set_alias";
			StringBuffer snapBuffer = new StringBuffer(snapQuery.length()
					+ snapQueryEnd.length() + 5000);
			snapBuffer.append(snapQuery);
			snapBuffer.append(fileIndices[0] + " and collection_index = "
					+ collectionIndices[0] + " )");
			// fileIndices and collectionIndices are the same length
			for (int ii = 1; ii < fileIndices.length; ii++) {
				snapBuffer.append("or (file_index = " + fileIndices[ii]
						+ " and collection_index = " + collectionIndices[ii]
						+ ")");
			}
			snapBuffer.append(snapQueryEnd);
			//if (classBugs)
			//	System.out
			//			.println("SavedDataSource.getSnapData(): executing query = "
			//					+ snapBuffer.toString());
			//if ( DO_SYBASE )
			 //   rs = BDBS.executeQuery(snapBuffer.toString());
			//if ( DO_POSTGRES )
			final ResultSet rs = getPostgreSQLServer("srdb").executeQuery(snapBuffer.toString());

			while (rs.next()) {
				/* get all information for 1st record */
				/* additional di/pi check */
				dbFileIndex = rs.getInt(1);
				dbFileAlias = rs.getInt(2);
				dbCollectionIndex = rs.getInt(3);
				dbCollectionAlias = rs.getInt(4);
				dbSetAlias = rs.getInt(5);

				if (firstTime == true) {
					//context = new SDAContext(owner, dbFileAlias, dbFileIndex,
					//		dbCollectionAlias, dbCollectionIndex, dbSetAlias);
					newSnapReq = getSnapHeader(dbFileIndex, dbCollectionIndex,
							snapReq.getDevice());
					snapReq.pushCallbackInfo(newSnapReq.snapRequest);
				}

				if (((dbFileIndex != saveFileIndex)
						|| (dbCollectionIndex != saveCollectionIndex) || (dbSetAlias != saveSetAlias))
						&& (firstTime != true)) {
					microSecs = new long[holdMicroSecs.length];
					System.arraycopy(holdMicroSecs, 0, microSecs, 0,
							holdMicroSecs.length);
					nanoSecs = new int[holdNanoSecs.length];
					System.arraycopy(holdNanoSecs, 0, nanoSecs, 0,
							holdNanoSecs.length);
					values = new double[holdValues.length];
					System.arraycopy(holdValues, 0, values, 0,
							holdValues.length);
					//if (classBugs)
					//	System.out
					//			.println("SavedDataSource.getSnapData(): calling plotData()...");
					snapReq.callback.plotData(newSnapReq.snapTime, newSnapReq.snapError, index, 
												microSecs, nanoSecs, values);
					newSnapReq = getSnapHeader(dbFileIndex, dbCollectionIndex,
							snapReq.getDevice());
					snapReq.pushCallbackInfo(newSnapReq.snapRequest);
					//context = new SDAContext(owner, dbFileAlias, dbFileIndex,
					//		dbCollectionAlias, dbCollectionIndex, dbSetAlias);
					index = 0;
				}

				holdMicroSecs[index] = rs.getLong(6);
				holdNanoSecs[index] = rs.getShort(7);
				holdValues[index] = rs.getDouble(8);

				saveFileIndex = dbFileIndex;
				saveCollectionIndex = dbCollectionIndex;
				saveSetAlias = dbSetAlias;
				index++;
				firstTime = false;
			}
			rs.close();
			if (firstTime == true) {
				for (int ii = 0; ii < fileIndices.length; ii++) {
					//context = new SDAContext(owner, 0, fileIndices[ii], 0,
					//		collectionIndices[ii], 0);
					newSnapReq = getSnapHeader(dbFileIndex, dbCollectionIndex,
							snapReq.getDevice());
					snapReq.pushCallbackInfo(newSnapReq.snapRequest);
					//snapReq.callback.plotData(snapReq, newSnapReq.snapTime,
					//		context, SAV_RST_NODATA, index,
					//		microSecs, nanoSecs, values);
					snapReq.callback.plotData(newSnapReq.snapTime, SAV_RST_NODATA, 
												index, microSecs, nanoSecs, values);
				}
			} else {
				microSecs = new long[holdMicroSecs.length];
				System.arraycopy(holdMicroSecs, 0, microSecs, 0,
						holdMicroSecs.length);
				nanoSecs = new int[holdNanoSecs.length];
				System.arraycopy(holdNanoSecs, 0, nanoSecs, 0,
						holdNanoSecs.length);
				values = new double[holdValues.length];
				System.arraycopy(holdValues, 0, values, 0, holdValues.length);
				//if (classBugs)
				//	System.out
				//			.println("SavedDataSource.getSnapData(): calling plotData()...");
				//snapReq.callback.plotData(snapReq, newSnapReq.snapTime, context,
				//		newSnapReq.snapError, index, microSecs, nanoSecs, values);
				snapReq.callback.plotData(newSnapReq.snapTime, newSnapReq.snapError, 
											index, microSecs, nanoSecs, values);
			}
		} catch (Exception ex) {
			//++numGetSnapDataException;
			//System.out.println("SavedDataSource.getSnapData(): " + ex);
		}
	}

	/**
	 * Get the data blob, send it to receiveData of whatDaq.
     * @param snapReq
     * @param fileIndices
     * @param collectionIndices
	 * 
	 */
	private void getSnapDataBlobs(SnapRequest snapReq, int[] fileIndices, int[] collectionIndices)
	{
		int dbFileIndex = 0; //, dbFileAlias = 0;
		//int dbCollectionIndex = 0; //, dbCollectionAlias = 0; //, dbSetAlias = 0;
		//ExtendedSnapRequest newSnapReq = null;
		//byte[] byteMicros = null;
		//byte[] byteNanos = null;
		//byte[] byteValues = null;
		//long[] microSecs = null;
		//int[] nanoSecs = null;
		//double[] values = null;

		try {
			//if (classBugs)
			//	System.out.println("GETSNAPDATA: fileIndex=" + fileIndex
			//			+ ", fileAlias=" + fileAlias
			//			+ ", fetchCollectionAlias=" + fetchCollectionAlias
			//			+ ", fetchSetAlias=" + fetchSetAlias);

			/* dsf - new snap query */
			//if ( DO_SYBASE )
			 //   snapQuery = "select file_index, file_alias, collection_index, "
			//		+ "collection_alias, set_alias, microsecs, nanosecs, data "
			//		+ "from srdb.finstrom.snap_data_blob where di = "
			//		+ snapReq.getDevice().getDeviceIndex()
			//		+ " and ((file_index = ";
			//if ( DO_POSTGRES )
			final String snapQuery = "SELECT file_index, file_alias, collection_index, "
					+ "collection_alias, set_alias, microsecs, nanosecs, data "
					+ "FROM srdb.snap_data_blob WHERE di = "
					+ snapReq.getDevice().getDeviceIndex()
					+ " AND ((file_index = ";
			String snapQueryEnd = ") and is_ftp=0 order by file_index, collection_index, set_alias";
			// String snapQueryEnd = ") order by file_index, collection_index,
			// set_alias";
			StringBuffer snapBuffer = new StringBuffer(snapQuery.length()
					+ snapQueryEnd.length() + 5000);
			snapBuffer.append(snapQuery);
			snapBuffer.append(fileIndices[0] + " and collection_index = "
					+ collectionIndices[0] + " )");
			// fileIndices and collectionIndices are the same length
			for (int ii = 1; ii < fileIndices.length; ii++) {
				snapBuffer.append("or (file_index = " + fileIndices[ii]
						+ " and collection_index = " + collectionIndices[ii]
						+ ")");
			}
			snapBuffer.append(snapQueryEnd);
			//if (classBugs)
			//	System.out
			//			.println("SavedDataSource.getSnapData(): executing query = "
			//					+ snapBuffer.toString());
			//if ( DO_SYBASE )
			 //   rs = BDBS.executeQuery(snapBuffer.toString());
			//if ( DO_POSTGRES )
			final ResultSet rs = getPostgreSQLServer("srdb").executeQuery(snapBuffer.toString());
			int dbCollectionIndex = 0;

			while (rs.next()) {
				dbFileIndex = rs.getInt(1);
				//dbFileAlias = rs.getInt(2);
				dbCollectionIndex = rs.getInt(3);
				//dbCollectionAlias = rs.getInt(4);
				//dbSetAlias = rs.getInt(5);

				//byteMicros = rs.getBytes(6);
				//byteNanos = rs.getBytes(7);
				//byteValues = rs.getBytes(8);

				//context = new SDAContext(owner, dbFileAlias, dbFileIndex,
				//		dbCollectionAlias, dbCollectionIndex, dbSetAlias);
				final ExtendedSnapRequest req = getSnapHeader(dbFileIndex, dbCollectionIndex, snapReq.getDevice());

				snapReq.pushCallbackInfo(req.snapRequest);
				//microSecs = ByteArrayConverter.longArrayFromByteArray(byteMicros);

				//final long[] micros = toLongArray(byteMicros);
				final long[] micros = toLongArray(rs.getBytes(6));

				//if (byteNanos != null)
					//nanoSecs = ByteArrayConverter.intArrayFromShortByteArray(byteNanos);
				final int[] nanos = toIntArray(rs.getBytes(7));

				//values = ByteArrayConverter.doubleArrayFromByteArray(byteValues);
				final double[] values = toDoubleArray(rs.getBytes(8));

				//if (classBugs)
				//	System.out
				//			.println("SavedDataSource.getSnapData(): calling plotData()...");
				if (micros != null) {
					//snapReq.callback.plotData(snapReq, newSnapReq.snapTime,
					//		context, newSnapReq.snapError, microSecs.length,
					//		microSecs, nanoSecs, values);
					snapReq.callback.plotData(req.snapTime, req.snapError, micros.length, micros, nanos, values);
				} else {
					//snapReq.callback.plotData(snapReq, newSnapReq.snapTime,
					//		context, SAV_RST_NODATA, 0, microSecs, nanoSecs, values);
					snapReq.callback.plotData(req.snapTime, SAV_RST_NODATA, 0, micros, nanos, values);
				}
			}

			rs.close();

			if (dbFileIndex == 0) { /* no records returned from query */
				for (int ii = 0; ii < fileIndices.length; ii++) {
					//context = new SDAContext(owner, 0, fileIndices[ii], 0,
					//		collectionIndices[ii], 0);
					final ExtendedSnapRequest req = getSnapHeader(dbFileIndex, dbCollectionIndex, snapReq.getDevice());

					snapReq.pushCallbackInfo(req.snapRequest);
					//snapReq.callback.plotData(snapReq, newSnapReq.snapTime,
							//context, SAV_RST_NODATA, 0, microSecs, nanoSecs, values);
					snapReq.callback.plotData(req.snapTime, SAV_RST_NODATA, 0, null, null, null);
				}
			}
		} catch (Exception e) {
			//++numGetSnapDataBlobsException;
			//System.out.println("SavedDataSource.getSnapDataBlobs(): " + ex);
			//System.out.println("" + System.currentTimeMillis());
			//ex.printStackTrace();
		}
	}

	/**
	 * Get the data blob, send it to receiveData of whatDaq.
	 * 
     * @param snapReq
     * @param fileIndices
     * @param collectionIndices
	 */
	private void getFTPDataBlobs(FTPRequest snapReq, int[] fileIndices, int[] collectionIndices)
	{
		//CollectionContext context = null;
		int dbFileIndex = 0; //, dbFileAlias = 0;
		//int dbCollectionIndex = 0, dbCollectionAlias = 0, dbSetAlias = 0;
		// ExtendedSnapRequest newSnapReq = null;
		//ExtendedFTPRequest newFTPReq = null;
		//byte[] byteMicros = null;
		//byte[] byteNanos = null;
		//byte[] byteValues = null;
		//long[] microSecs = null;
		//int[] nanoSecs = null;
		//double[] values = null;

		try {
			//if (classBugs)
			//	System.out.println("GETSNAPDATA: fileIndex=" + fileIndex
			//			+ ", fileAlias=" + fileAlias
			//			+ ", fetchCollectionAlias=" + fetchCollectionAlias
			//			+ ", fetchSetAlias=" + fetchSetAlias);

			/* dsf - new snap query */
			//if ( DO_SYBASE )
			 //   snapQuery = "select file_index, file_alias, collection_index, "
			//		+ "collection_alias, set_alias, microsecs, nanosecs, data "
			//		+ "from srdb.finstrom.snap_data_blob where di = "
			//		+ snapReq.getDevice().getDeviceIndex()
			//		+ " and ((file_index = ";
			//if ( DO_POSTGRES )
			final String snapQuery = "SELECT file_index, file_alias, collection_index, "
					+ "collection_alias, set_alias, microsecs, nanosecs, data "
					+ "FROM srdb.snap_data_blob WHERE di = "
					+ snapReq.getDevice().getDeviceIndex()
					+ " AND ((file_index = ";
			String snapQueryEnd = ") and is_ftp=1 order by file_index, collection_index, set_alias";
			// String snapQueryEnd = ") order by file_index,
			// collection_index, set_alias";
			StringBuffer snapBuffer = new StringBuffer(snapQuery.length()
					+ snapQueryEnd.length() + 5000);
			snapBuffer.append(snapQuery);
			snapBuffer.append(fileIndices[0] + " and collection_index = "
					+ collectionIndices[0] + " )");
			// fileIndices and collectionIndices are the same length
			for (int ii = 1; ii < fileIndices.length; ii++) {
				snapBuffer.append("or (file_index = " + fileIndices[ii]
						+ " and collection_index = "
						+ collectionIndices[ii] + ")");
			}
			snapBuffer.append(snapQueryEnd);
			//if (classBugs)
			//	System.out
			//			.println("SavedDataSource.getSnapData(): executing query = "
			//					+ snapBuffer.toString());
			//if ( DO_SYBASE )
			 //   rs = BDBS
			//		.executeQuery(snapBuffer.toString());
			//if ( DO_POSTGRES )
			final ResultSet rs = getPostgreSQLServer("srdb").executeQuery(snapBuffer.toString());
			int dbCollectionIndex = 0;

			while (rs.next()) {
				dbFileIndex = rs.getInt(1);
				//dbFileAlias = rs.getInt(2);
				dbCollectionIndex = rs.getInt(3);
				//dbCollectionAlias = rs.getInt(4);
				//dbSetAlias = rs.getInt(5);
				//byteMicros = rs.getBytes(6);
				//byteNanos = rs.getBytes(7);
				//byteValues = rs.getBytes(8);

				//context = new SDAContext(owner, dbFileAlias, dbFileIndex,
				//		dbCollectionAlias, dbCollectionIndex, dbSetAlias);
				final ExtendedFTPRequest req = getFTPHeader(dbFileIndex, dbCollectionIndex, snapReq.getDevice());
				// snapReq.pushCallbackInfo(newFTPReq.snapRequest);
				//microSecs = ByteArrayConverter.longArrayFromByteArray(byteMicros);
				final long[] micros = toLongArray(rs.getBytes(6));

				//if (byteNanos != null)
					//nanoSecs = ByteArrayConverter.intArrayFromShortByteArray(byteNanos);
					final int[] nanos = toIntArray(rs.getBytes(7));

				//values = ByteArrayConverter.doubleArrayFromByteArray(byteValues);
				final double[] values = toDoubleArray(rs.getBytes(8));

				//if (classBugs)
				//	System.out
				//			.println("SavedDataSource.getSnapData(): calling plotData()...");
				if (micros != null) {
					//snapReq.callback.plotData(snapReq, newFTPReq.snapTime, context, newFTPReq.snapError, microSecs.length,
					//		microSecs, nanoSecs, values);
					snapReq.callback.plotData(req.snapTime, req.snapError, micros.length, micros, nanos, values);
				} else {
					//snapReq.callback.plotData(snapReq, newFTPReq.snapTime,
					//		context, SAV_RST_NODATA, 0, microSecs, nanoSecs, values);
					snapReq.callback.plotData(req.snapTime, SAV_RST_NODATA, 0, micros, nanos, values);
				}
			}

			rs.close();

			if (dbFileIndex == 0) { /* no records returned from query */
				for (int ii = 0; ii < fileIndices.length; ii++) {
					//context = new SDAContext(owner, 0, fileIndices[ii], 0,
					//		collectionIndices[ii], 0);
					final ExtendedFTPRequest req = getFTPHeader(dbFileIndex, dbCollectionIndex, snapReq.getDevice());
					// snapReq.pushCallbackInfo(newFTPReq.snapRequest);
					//snapReq.callback.plotData(snapReq, newFTPReq.snapTime,
					//		context, SAV_RST_NODATA, 0, microSecs, nanoSecs, values);
					snapReq.callback.plotData(req.snapTime, SAV_RST_NODATA, 0, null, null, null);
				}
			}
		} catch (Exception ex) {
			//++numGetFTPDataBlobsException;
			//System.out.println("SavedDataSource.getFTPDataBlobs(): " + ex);
			//System.out.println("" + System.currentTimeMillis());
			//ex.printStackTrace();
		}
	}

	/**
	 * Pad data with zeros if necessary.
	 * 
	 * @param data
	 *            the byte array of data that needs to be 0 padded.
	 * @param length
	 *            the length that this data segment should be.
     * @return padded data
	 */
	private static byte[] padData(byte[] data, int length) {
		// Expand array and Pad out data if needed
		byte tmp[] = new byte[length];
		System.arraycopy(data, 0, tmp, 0, data.length); // Copy data to tmp
		for (int ii = data.length; ii < length; ii++)
			// Zero pad remaining portions
			tmp[ii] = 0;
		return tmp;
	}

	/**
	 * Returns amount of zero padding necessary (if any).
	 * 
	 * @param deviceLen
	 *            the length for this di/pi.
	 * @param rowLen
	 *            the length of the database row.
	 * @param segmentNum
	 *            the segment number.
	 * @return amount of zero padding necessary
	 */
	private static int padAmount(int deviceLen, int rowLen, int segmentNum) {
		int dataRemaining = 0;

		dataRemaining = deviceLen - (segmentNum * MAXBYTESINROW);
		if (dataRemaining > MAXBYTESINROW)
			return (MAXBYTESINROW - rowLen);
		else
			return (dataRemaining - rowLen);
	}

	/**
	 * Deletes SavedData files for specified owner and alias.
	 * 
	 * @param owner
	 *            the SavedData owner.
	 * @param fileAlias
	 *            the SavedData file alias.
	 * @return true if file is deleted
	 */
	 /*
	public static boolean deleteFile(String owner, int fileAlias) {
		try {
			SavedDataSource delFile = new SavedDataSource(owner, fileAlias,
					false, true);
			System.out
					.println("SavedDataSource marking file for delete, not checking for privilege");
			delFile.delete = true;
		} catch (Exception e) {
			//++numDeleteFileException;
			System.out.println("SavedDataSource failed to delete a file: " + e);
			return false;
		}
		return true;
	}
	*/

	/**
	 * Return a string describing this SavedDataSource.
	 * 
	 * @return a string describing this SavedDataSource
	 */
	public String toString() {
		return ("SavedDataSource, owner: " + owner + " file: " + fileIndex
				+ "/" + fileAlias + ", title " + title);
	}

	/**
	 * Return the SavedData file index.
	 * 
	 * @return file index
	 */
	public int getFileIndex() {
		return fileIndex;
	}

	/**
	 * Return the SavedData file alias.
	 * 
	 * @return file alias
	 */
	public int getFileAlias() {
		return fileAlias;
	}

	/**
	 * Return the SavedData title.
	 * 
	 * @return title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Return the SavedData start time.
	 * 
	 * @return start time
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * Return the SavedData end time.
	 * 
	 * @return end time
	 */
	public long getEndTime()
	{
		return endTime;
	}

	/**
	 * Inquire if operator protection is set.
	 * 
	 * @return true if operator protection is set, false otherwise
	 */
	public boolean getProtectOper() {
		return (protectOper);
	}

	/**
	 * Inquire if archive protection is set.
	 * 
	 * @return true if archive protection is set, false otherwise
	 */
	public boolean getProtectArchive() {
		return (protectArchive);
	}

	/**
	 * Inquire if modify protection is set.
	 * 
	 * @return true if modify protection is set, false otherwise
	 */
	public boolean getProtectModify() {
		return (protectModify);
	}

	/**
	 * Inquire if destroy protection is set.
	 * 
	 * @return true if destroy protection is set, false otherwise
	 */
	public boolean getProtectDestroy() {
		return (protectDestroy);
	}

	/**
	 * Inquire if this file contains collections of data.
     * @return whether file contains data collections
	 */
	public boolean isSequencedDataSet() {
		return sequencedDataSet;
	}

	/**
	 * Mark this file to contain collections of data.
	 */
	public void setSequencedDataSet() {
		sequencedDataSet = true;
	}

	/**
	 * Return SavedData owner version.
	 * 
	 * @return owner version
	 */
	public short getOwnerVersion() {
		return (versionOwner);
	}

	/**
	 * Return SavedData owner.
	 * 
	 * @return owner
	 */
	public String getOwner() {
		return (owner);
	}

	/**
	 * Set the state of the debug message switch.
	 * 
	 * @param onOff
	 *            boolean state for the switch.
	 */
	//public static void setClassBugs(boolean onOff) {
	//	classBugs = onOff;
	//}

	/**
	 * Inquire of the state of the debug message switch.
	 * 
	 * @return true if debug messaging is on, otherwise false
	 */
	//public static boolean isClassBugs() {
	//	return classBugs;
	//}

	private class ExtendedSnapRequest {
		final SnapRequest snapRequest;

		final long snapTime;

		final int snapError;

		ExtendedSnapRequest(SnapRequest snapRequest, long snapTime, int snapError)
		{
			this.snapRequest = snapRequest;
			this.snapTime = snapTime;
			this.snapError = snapError;
		}
	}

	private class ExtendedFTPRequest {
		@SuppressWarnings("unused")
		final FTPRequest snapRequest;

		final long snapTime;

		final int snapError;

		ExtendedFTPRequest(FTPRequest snapRequest, long snapTime, int snapError)
		{
			this.snapRequest = snapRequest;
			this.snapTime = snapTime;
			this.snapError = snapError;
		}
	}
}
