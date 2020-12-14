package pasa.cbentley.framework.coredata.src4.db;

import pasa.cbentley.byteobjects.src4.core.ByteObject;
import pasa.cbentley.core.src4.io.BADataIS;
import pasa.cbentley.core.src4.io.BADataOS;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.core.src4.logging.IStringable;
import pasa.cbentley.core.src4.memory.IMemFreeable;
import pasa.cbentley.core.src4.thread.IBProgessable;
import pasa.cbentley.framework.coredata.src4.ex.StoreException;

/**
 * Interface to the database system storing byte arrays. Manages and provides basic and atomic operations.
 * <br>
 * <br>
 * This interface provides quick atomic thread safe operations, and minimize exception throwing by returning error values.
 * <li>Store is opened 
 * <li>Method action is executed
 * <li>Store is closed.
 * <br>
 * For transactions, get a reference to an {@link IByteCache} with {@link IByteStore#getByteCache(String, ByteObject)}
 * <br>
 * Each store has a name. Record IDs start at {@link IByteStore#getBase()} 
 * <br>
 * <br>
 * Get all reachable names with  {@link IByteStore#getAllStores()}
 * <br>
 * <br>
 * For all methods, if a named table does not exist, it will be created 
 * <br>
 * <br>
 * 
 * <b>{@link IByteCache}</b>:
 * <br>
 * Use {@link IByteCache} for repetitive actions on a given named store. {@link IByteCache} does not open and close the store at each 
 * operations.
 * The implementation of {@link IByteStore} might be very slow at writing and reading. Because it does it from disk. 
 * <br>
 * All write and read operations be done on the cache. 
 * <br>
 * Calling commit on the cache object will commit all changes to the ByteStore. 
 * <br>
 * {@link IByteStore#getByteCache(String, int[])} is the method to create an {@link IByteCache} on a given store.
 * <br>
 * See for Parameters.
 * <br>
 * <br>
 * @author Charles Bentley
 *
 */
public interface IByteStore extends IStringable {

   public static final int CACHE_FRAME = 1;

   /**
    * only cache last accessed records
    */
   public static final int CACHE_LAST  = 1;

   /**
    * no cache currently use
    */
   public static final int CACHE_NONE  = 1;

   /**
    * Adds a new byte record at the end of the record store
    * <br>
    * the data to be stored in this record. If the record is to have zero-length data (no data), this parameter may be null.
    * <br>
    * @param rs the record store name.
    * @param data this parameter may be null.
    * @return the record id of the byte array
    */
   public int addBytes(String rs, byte[] data);

   public int addBytes(String rs, byte[] data, int offset, int len);

   public int getBase();

   /**
    * Deletes all the byte tables
    */
   public void deleteAll();

   /**
    * Deletes a record in the table, making the RID invalid.
    * <br>
    * <br>
    * Afterwards, all set and get method will generate an {@link InvalidRecordIDException}
    * <br>
    * <br>
    * 
    * @param rs
    * @param rid
    */
   public void deleteRecord(String rs, int rid);

   /**
    * Deletes the store
    * @param storeName
    * @return true if store was deleted
    */
   public boolean deleteStore(String rs);

   /**
    * Make the index valid.
    * Initialize with empty arrays of size 0
    * <br>
    * <br>
    * 
    * @param rs
    * @param rid
    */
   public void ensureCapacity(String rs, int rid);

   /**
    * What happens when an error occurs and big value rid is called?
    * What is the desirable behaviour?
    * <br>
    * When {@link IMProgessable} is not null, the user has a feedback,
    * however without one, 
    * @param rs
    * @param rid
    * @param ensureSize when size is negative, add nulls
    * @param p
    */
   public void ensureCapacity(String rs, int rid, int ensureSize, IBProgessable p);

   /**
    * Returns all the names of the stores created
    * <br>
    * <br>
    * Empty array if no record stores.
    * @return Note that if the MIDlet suite does not have any record stores, this function will return <b>null</b>.
    */
   public String[] getAllStores();

   //#mdebug
   /**
    * Array of Store Signature (= light Debug Data with size, number of records, next id etc.)
    * <br>
    * <br>
    * @return
    */
   public String[] getAllStoreSignatures();
   //#enddebug

   /**
    * Return the default byte cache. Cache operations should only be used for intensive database operations.
    * <br>
    * The normal {@link IByteStore} methods are already cached naturally for normal calls.
    * <br>
    * Depending on the the implementation of {@link IByteCache}, memory consumption will increase.
    * <br>
    * <br>
    * On memory constrained devices, each cache has a ration rating of its current memory consumption.
    * <br>
    * A cache implements {@link IMemFreeable} so that memory controller may take actioins.
    * <br>
    * The Mem controls, or is it a Memory Agent? It is more like a RgbCache. It is luxury memory.
    * MemAgent framework provides means to manage datastructure byte arrays from/to disk into chunks.
    * <br>
    * <br>
    * 
    * @param rs store name. unique id.
    * @param tech config parameters specifying the kind of cache implementation {@link ICacheTech}
    * @return
    */
   public IByteCache getByteCache(String rs, ByteObject tech);

   /**
    * Gets the byte[] at index rid and create one of size 0 if none.
    * <br>
    * the data stored in the given record. Note that if the record has no data, this method will return null. 
    * <br>
    * this method does not throw any {@link StoreException}.
    * <br>
    * Returns null if rid is < {@link IByteStore#getBase()}
    * <br>
    * @param rs name
    * @param rid
    * @return null when rid is out of range or null is stored at rid
    */
   public byte[] getBytes(String rs, int rid);

   /**
    * Retursn the number of bytes stored at that rid.
    * <br>
    * 
    * @param rs
    * @param rid
    * @return -1
    */
   public int getByteSize(String rs, int rid);

   /**
    * Get the bytes for the RID.
    * <br>
    * <br>
    * When the rid is outside the current RID domain and ensure is true, create empty byte records inside until the RID is valid.
    * <br>
    * @param rs
    * @param rid
    * @param ensure
    * @return null when rid is invalid and ensure is false
    */
   public byte[] getBytesCheck(String rs, int rid, boolean ensure);

   /**
    * Write data directly to a buffer
    * <br>
    * <br>
    * @param rs name of table
    * @param rid index of entry
    * @param data buffer to which data is written
    * @param offset offset in buffer at which record data is written
    * @return the number of bytes written
    * @throws ArrayIndexOutOfBoundsException - if the record is larger than the buffer supplied
    */
   public int getBytes(String rs, int rid, byte[] data, int offset);

   /**
    * Timestamp of when the store was last modified
    * @param rs
    * @return -1 if an error occured
    */
   public long getLastModified(String rs);

   /**
    * The next free index in the table. Starts at zero
    * @param rs
    * @return
    */
   public int getNextRecordId(String rs);

   /**
    * The number of records
    * @param rs
    * @return
    */
   public int getNumRecords(String rs);

   /**
    * Returns the amount of space, in bytes, that the record store occupies. 
    * <br>
    * <br>
    * The size returned includes any overhead associated with the implementation, such as the data structures used to hold the state of the record store, etc. 
    * <br>
    * <br>
    * @param rs store name
    * @return
    */
   public int getSize(String rs);

   /**
    * Size left in bytes
    * @param rs
    * @return
    */
   public int getSizeAvailable(String rs);

   /**
    * Allow for synchronization between 2 stores
    * @param rs
    * @return the counter of write/delete modifications 
    * since the creation of the store
    */
   public int getVersion(String rs);

   /**
    * 
    * @param rs
    * @return true if table is in RAM memory
    */
   public boolean isCached(String rs);

   /**
    * Implements caching for the store
    * @param rs store name
    * @param type of caching
    * @return false if caching failed
    */
   public boolean isCachedStore(String rs, int type);

   public boolean isUsed(String rs);

   /**
    * Serialize the record store to the {@link DataBAOutputStream}. This always succeeds.
    * <br>
    * <br>
    * 
    * @param rs
    * @param dos
    */
   public void serializeExport(String rs, BADataOS dos);

   /**
    * Serialize back into a state defined by the {@link DataBAInputStream}.
    * <br>
    * <br>
    * Only understands data written by {@link IByteStore#serializeExport(String, DataBAOutputStream)}.
    * <br>
    * <br>
    * When is the old data deleted?
    * <br>
    * <br>
    * 
    * @param rs
    * @param dis
    */
   public void serializeImport(String rs, BADataIS dis);

   /**
    * Sets the byte data at index id
    * <br>
    * <br>
    * If id is invalid, nothing happens.
    * <br>
    * Does not throw any {@link StoreException}
    * <br>
    * @param rs
    * @param id
    * @param b
    */
   public void setBytes(String rs, int id, byte[] b);

   public void setBytes(String rs, int id, byte[] b, int offset, int len);

   /**
    * Save the bytes at position id.
    * <br>
    * Ensures might not work. If it fails, a log will posted
    * and the method will fail.
    * <br>
    * @param rs
    * @param id
    * @param b
    * @param ensure true if force the create of position id
    */
   public void setBytesEnsure(String rs, int id, byte[] b);

   public void setBytesEnsure(String rs, int id, byte[] b, int offset, int len);

   //#mdebug
   /**
    * 
    * @param rs
    * @return
    */
   public String toString(String rs);

   /**
    * Returns the Debug Header and Debug Data together.
    * <br>
    * <br>
    * @param rs
    * @return
    */
   public void toString(String rs, Dctx sb);

   public String toStringData(String rs, IByteInterpreter ib);

   /**
    * Method to dump the content of a store as human readable info for debugging purposes.
    * <br>
    * <br>
    * Reads each record and append it to the returned string
    * <br>
    * <br>
    * @param rs
    * @param ib can be null. returned string will reflect that
    * @return
    */
   public void toStringData(String rs, Dctx nl, IByteInterpreter ib);

   public String toStringOneLine(String rs);

   public String toStringStoreHeader(String rs);

   /**
    * Get debug data for the table.
    * Does not return data
    * @param rs
    * @return
    */
   public void toStringStoreHeader(String rs, Dctx nl);
   //#enddebug

}
