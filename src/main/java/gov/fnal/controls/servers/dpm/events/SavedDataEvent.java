// $Id: SavedDataEvent.java,v 1.2 2023/06/12 16:37:55 kingc Exp $
package gov.fnal.controls.servers.dpm.events;


public class SavedDataEvent implements DataEvent
{
	int[] fileIndices = null;

	int[] collectionIndices = null;

	String whereClause = null;

	boolean byWhereClause = false;
	
	boolean isRestrictMatches = false;

	boolean repetitive = false;

	/**
	 * Constructor for SavedDataEvent.
	 * 
	 * @param file
	 *            file index.
	 */
	public SavedDataEvent(int file)
	{
		fileIndices = new int[1];
		fileIndices[0] = file;
	}

	/**
	 * Constructor for SavedDataEvent.
	 * 
	 * @param file
	 *            file index.
	 * @param collection
	 *            collection index.
	 */
	public SavedDataEvent(int file, int collection)
	{
		fileIndices = new int[1];
		fileIndices[0] = file;
		collectionIndices = new int[1];
		collectionIndices[0] = collection;
	}

	/**
	 * Constructor for SavedDataEvent.
	 * 
	 * @param files
	 *            file indices.
	 * @param collections
	 *            collection indices.
	 */
	public SavedDataEvent(int[] files, int[] collections)
	{
		int length = files.length;
		fileIndices = new int[length];
		collectionIndices = new int[length];

		repetitive = (length > 1);

		for (int ii = 0; ii < length; ii++) {
			fileIndices[ii] = files[ii];
			collectionIndices[ii] = collections[ii];
		}
	}

	@Override
	public boolean isRepetitive()
	{
		return repetitive;
	}

	/**
	 * Constructor for SavedDataEvent.
	 * 
	 * @param whereClause
	 *            where clause for query.
	 * @param repetitive
	 *            true when true.
	 */
	//public SavedDataEvent(String whereClause, boolean repetitive)
	//{
	//	this.whereClause = whereClause;
	//	byWhereClause = true;
	//	setRepetitive(repetitive);
	//}

	/**
	 * Get the specified file indices.
	 * 
	 * @return the array of file indices.
	 */
	public int[] getFileIndices() {
		return fileIndices;
	}

	/**
	 * Get the specified collection indices.
	 * 
	 * @return the array of collection indices.
	 */
	public int[] getCollectionIndices() {
		return collectionIndices;
	}

	/**
	 * Get the where clause for database query.
	 * 
	 * @return the where clause for database query.
	 */
	public String getWhereClause() {
		return whereClause;
	}

	/**
	 * Inquire if the where clause should be used for database query.
	 * 
	 * @return true if so.
	 */
	public boolean isByWhereClause() {
		return byWhereClause;
	}

	/**
	 * Inquire if matches should be restricted by length/offset.
	 * 
	 * @return true if so.
	 */
	public boolean isRestrictMatches() {
		return isRestrictMatches;
	}

	/**
	 * Declare matches should be restricted.  SDA data logger wants
	 * restricted matches, Store Checker does not.  For now (02/10/2010),
	 * matches are restricted when using where clause.  Future flexibility
	 * can be provided when this is available to all applications and
	 * engines.
	 * 
	 */
	public void doRestrictMatches() {
		isRestrictMatches = true;
	}

	/**
	 * Return a string describing this SavedDataEvent.
	 * 
	 * @return a description of this SavedDataEvent
	 */
	//public String toString() {
	//	if (fileIndices != null)
	//		return ("SavedDataEvent, " + fileIndices.length
	//				+ " file(s) including fileIndex " + fileIndices[0]);
	//	else
	//		return ("SavedDataEvent, whereClause: " + whereClause);
	//}

	//public void addObserver(DataEventObserver observer) {
		// TODO: nothing
	//}

	//public void deleteObserver(DataEventObserver observer) {
		// TODO: nothing
//	}
}

