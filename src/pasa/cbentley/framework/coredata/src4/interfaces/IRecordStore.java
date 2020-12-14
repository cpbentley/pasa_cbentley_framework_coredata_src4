package pasa.cbentley.framework.coredata.src4.interfaces;

import pasa.cbentley.core.src4.logging.IStringable;
import pasa.cbentley.framework.coredata.src4.ex.StoreException;
import pasa.cbentley.framework.coredata.src4.ex.StoreFullException;
import pasa.cbentley.framework.coredata.src4.ex.StoreInvalidIDException;
import pasa.cbentley.framework.coredata.src4.ex.StoreNotOpenException;

/**
 * Interface towards a single stack of byte records
 * <br>
 * <br>
 * In the Bentley framework, RIDs start at 0.
 * <br>
 * Implementations adapt to this specification.
 * <br>
 * @author Charles-Philip Bentley
 *
 */
public interface IRecordStore extends IStringable {

   public static final int AUTHMODE_PRIVATE = 0;

   public static final int AUTHMODE_ANY     = 1;

   /**
    * The base integer, first valid value for a record.
    * <br>
    * @return
    */
   public int getBase();

   public void closeRecordStore() throws StoreException;

   public String getName() throws StoreNotOpenException;

   public int getVersion() throws StoreNotOpenException;

   public int getNumRecords() throws StoreNotOpenException;

   public int getSize() throws StoreNotOpenException;

   public int getSizeAvailable() throws StoreNotOpenException;

   public long getLastModified() throws StoreNotOpenException;

   /**
    * Adds the specified RecordListener. If the specified listener is already registered, it will not be added a second time. When a record store is closed, all listeners are removed.
    * @param listener {@link IRecordListener}
    */
   public void addRecordListener(IRecordListener listener);

   /**
    * Removes the specified RecordListener. If the specified listener is not registered, this method does nothing. 
    * @param listener
    */
   public void removeRecordListener(IRecordListener listener);

   public int getNextRecordID() throws StoreNotOpenException, StoreException;

   public int addRecord(byte[] data, int offset, int numBytes) throws StoreNotOpenException, StoreException, StoreFullException;

   /**
    * 
    * @param recordId
    * @throws StoreNotOpenException
    * @throws StoreInvalidIDException if recordid is not valid
    * @throws StoreException
    */
   public void deleteRecord(int recordId) throws StoreNotOpenException, StoreInvalidIDException, StoreException;

   /**
    * 
    * @param recordId
    * @return
    * @throws StoreNotOpenException
    * @throws StoreInvalidIDException
    * @throws StoreException
    */
   public int getRecordSize(int recordId) throws StoreNotOpenException, StoreInvalidIDException, StoreException;

   /**
    * Returns the data stored in the given record. 
    * 
    * @param recordId the ID of the record to use in this operation
    * @param buffer the byte array in which to copy the data
    * @param offset the index into the buffer in which to start copying 
    * @return
    * @throws StoreNotOpenException  if the record store is not open 
    * @throws StoreInvalidIDException if the recordId is invalid 
    * @throws StoreException  if a general store exception occurs 
    * @@throws ArrayIndexOutOfBoundsException - if the record is larger than the buffer supplied
    */
   public int getRecord(int recordId, byte[] buffer, int offset) throws StoreNotOpenException, StoreInvalidIDException, StoreException;

   /**
    * Return null if no data.
    * <br>
    * @param recordId
    * @return the data stored in the given record. Note that if the record has no data, this method will return null. 
    * @throws StoreNotOpenException
    * @throws StoreInvalidIDException if the recordId is invalid 
    * @throws StoreException
    */
   public byte[] getRecord(int recordId) throws StoreNotOpenException, StoreInvalidIDException, StoreException;

   /**
    * Changes the access mode for this {@link IRecordStore}. The authorization mode choices are:
   
    <li> {@link IRecordStore#AUTHMODE_PRIVATE}  - Only allows the MIDlet suite that created the RecordStore to access it. This case behaves identically to openRecordStore(recordStoreName, createIfNecessary).
    <li> {@link IRecordStore#AUTHMODE_ANY}   - Allows any MIDlet to access the RecordStore. Note that this makes your recordStore accessible by any other MIDlet on the device. This could have privacy and security issues depending on the data being shared. Please use carefully.
   
   The owning MIDlet suite may always access the RecordStore and always has access to write and update the store. Only the owning MIDlet suite can change the mode of a RecordStore.
    * @param authmode 
    * @param writable true if the RecordStore is to be writable by other MIDlet suites that are granted access 
    * @throws StoreException
    */
   public void setMode(int authmode, boolean writable) throws StoreException;

   /**
    * 
    * @param recordId
    * @param newData
    * @param offset
    * @param numBytes
    * @throws StoreNotOpenException
    * @throws StoreInvalidIDException when record ID is not part
    * @throws StoreException
    * @throws StoreFullException
    */
   public void setRecord(int recordId, byte[] newData, int offset, int numBytes) throws StoreNotOpenException, StoreInvalidIDException, StoreException, StoreFullException;

   public IRecordEnumeration enumerateRecords(IRecordFilter filter, IRecordComparator comparator, boolean keepUpdated) throws StoreNotOpenException;

}
