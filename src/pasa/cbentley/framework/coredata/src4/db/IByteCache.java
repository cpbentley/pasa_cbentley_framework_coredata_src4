package pasa.cbentley.framework.coredata.src4.db;

import pasa.cbentley.byteobjects.src4.core.ByteObject;
import pasa.cbentley.core.src4.io.BADataIS;
import pasa.cbentley.core.src4.io.BADataOS;
import pasa.cbentley.core.src4.logging.IStringable;
import pasa.cbentley.core.src4.thread.IBProgessable;

/**
 * Encapsulate the cached access to a single named byte store 
 * <br>
 * <br>
 * It provides the same kind of operations that {@link IByteStore} provides, but on a specific byte store.
 * <br>
 * <br>
 * {@link IByteCache} may provides RAM transaction support. 
 * <br>
 * <br>
 * Writes are not directly propagated to the metal/hardware.
 * <br>
 * <br>
 * While the {@link IByteStore} doesn't cache or pool operations, (the implementation of the RMS might but we cannot assume it does).
 * <br>
 * The ByteCache is pure RAM access to byte[][] array. Disk data access is made for cache misses, writes pushes, transaction finalization.
 * <br>
 * The ByteCache user can decide options:
 * <li> the user of cache.
 * <li> the frame 
 * <li> the size of the cache.
 * <br>
 * <br>
 * <br>
 * @author Charles Bentley
 *
 */
public interface IByteCache extends IStringable {

   /**
    * For those {@link IByteCache} that support it modifies working mode on the fly
    * <li> {@link ICacheTech#CACHE_MODE_0_DEFAULT}
    * <li> {@link ICacheTech#CACHE_MODE_1_TRANSACTION}
    * <br>
    * <br>
    * 
    * @param mode
    */
   public void setMode(int mode);

   /**
    * Write to {@link DataBAOutputStream} the name of the store, the rids of the cached data
    * the data cached, and the actual data. empty if rid is not
    * <br>
    * <br>
    * @param dos
    */
   public void commitJournal(BADataOS dos);

   public int getBase();

   /**
    * Adds a new byte record at the end of the record store
    * <br>
    * <br>
    * The cache may cache it, but after the method returns, the data is permanently saved.
    * <br>
    * <br>
    * @param rs
    * @param data
    * @return the record id of the byte array
    */
   public int addBytes(byte[] data);

   /**
    * Adds a new byte record. data is copied at offset of length len.
    * <br>
    * <br>
    * 
    * @param data
    * @param offset
    * @param len
    * @return
    */
   public int addBytes(byte[] data, int offset, int len);

   /**
    * Gets an {@link IByteCache} that supports the following spec written in a {@link ICacheTech}.
    * <br>
    * <br>
    * 
    * @param tech {@link ICacheTech} 
    * @return
    */
   public IByteCache getCacheMorph(ByteObject tech);

   /**
    * The specification of the Cache.
    * <br>
    * <br>
    * On which u check the kind of cache and its capabilities.
    * <br>
    * <br>
    * 
    * @return {@link ICacheTech} 
    */
   public ByteObject getCacheSpec();

   /**
    * Open the database access.
    * <br>
    * <br>
    * Call {@link IByteCache#disconnect()}
    * <br>
    * <br>
    *  Reads will load in cache. Writes will be kept in RAM until the transaction finishes.
    * <br>
    * <br>
    * Then everything is written to a record_store_name_transaction
    * <br>
    * <br>
    * Then once this is completed the other record store is deleted.
    * So if the system crashes during the final copying into the transaction store, our data in original store is not compromised.
    * 
    * and the system can get back on its feet.
    */
   public void connect();

   /**
    * Starts a transaction (TRMODE). If a transaction is alreayd going, auto commit the previous one.
    * <br>
    * <br>
    * All writes with 
    * <li>{@link IByteCache#setBytes(int, byte[])}
    * <li> {@link IByteCache#addBytes(byte[])}
    * <br>
    * <br>
    * will be done in RAM.
    * <br>
    * <br>
    * We use the <b>Closed Nested Transaction Model</b>. (vs Open Nested)<br>
    * <br>
    * When a transaction starts while another transaction is already running,
    * the new transaction occurs inside.
    * 
    * changes made by the nested transaction are not seen by the 'host' transaction until the nested
    * transaction is committed. This follows from the isolation property of transactions.
    * <br>
    * <br>
    * Transaction start with parameters. Big transaction means all records affected
    * <br>
    * <br>
    * <br>
    * Access to shadowing at this level? No.
    */
   public void transactionStart();

   public void transactionCancel();

   /**
    * Commit the current transaction by writing all transaction memory segment to disk
    * <br>
    * <br>
    * How do you prevent a crash from compromising the data integrity during the commit process? Durability.
    * <br>
    * <br>
    * You write to a different file, that means copying the whole database?
    * <br>
    * <br>
    * 
    * If no transaction active. nothing happens.
    */
   public void transactionCommit();

   /**
    * Ask the {@link IByteCache} to include the RID interval in its cache.
    * <br>
    * <br>
    * Kind of a pre-load method.
    * <br>
    * <br>
    * User starts a thread to learn if it has re-arrange the cache. you can force
    * re-arrangement with the method update()
    * <br>
    * <br>
    * @param startID the first RID to be included in the cache
    * @param endID the last RID to be included in the cache. 
    */
   public void manageCache(int startID, int endID);

   /**
    * Delete the record. In Fixed Size stores, it is often useless to delete a single record.
    * <br>
    * <br>
    * @param id
    */
   public void deleteBytes(int id);

   /**
    * If a transaction is
    */
   public void disconnect();

   /**
    * Make the index valid.
    * <br>
    * <br>
    * Initialize with records empty arrays of given size
    * <br>
    * <br>
    * @param rid
    * @param ensureSize the byte size of each records
    * @param p the {@link IMProgessable} to track
    */
   public void ensureCapacity(int rid, int ensureSize, IBProgessable p);

   /**
    * Gets the byte array at RID. Create an empty array if RID was not created.
    * <br>
    * Null if RID value overflowing the maximum records of this {@link IByteCache}.
    * <br>
    * This method returns the cache reference or create a new array for a cache miss depending
    * on the 
    * <br>
    * <br>
    * The RID domain starts at 1. But why?
    * <br>
    * <br>
    * 
    * @param rid >= 0
    * @return null key is not pointing to a byte array
    * 
    * @see IByteStore#getBytes(String, int)
    */
   public byte[] getBytes(int rid);

   /**
    * Returns null when rid is invalid
    * @param rid
    * @return
    */
   public byte[] getBytesCheck(int rid);

   /**
    * Returns the byte at rid ignoring any caching business.
    * <br>
    * <br>
    * Mainly used when {@link ICacheTech#CACHE_FLAG_2_REFERENCE} is set and byte array must be compared with the original.
    * <br>
    * <br>
    * Attention: there most is a discrepency between the cache
    * @param rid
    * @return
    */
   public byte[] getBytesThrough(int rid);

   /**
    * Write data directly to a buffer at the given offset.
    * <br>
    * <br>
    * 
    * @param rs name of table
    * @param rid index of entry
    * @param data buffer to which data is written
    * @param offset offset in buffer at which record data is written
    * @return the number of bytes written
    * @throws ArrayIndexOutOfBoundsException - if the record is larger than the buffer supplied
    */
   public int getBytes(int rid, byte[] data, int offset);

   /**
    * Timestamp of when the store was last modified
    * <br>
    * <br>
    * @param rs
    * @return -1 if an error occured
    */
   public long getLastModified();

   /**
    * The next RID to be returned by {@link IByteCache#addBytes(byte[])}
    * <br>
    * <br>
    * @return
    */
   public int getNextID();

   /**
    * The number of records in the record table.
    * <br>
    * <br>
    * @param rs
    * @return
    */
   public int getNumRecords();

   /**
    * Number of bytes consumed by this store
    * <br>
    * <br>
    * @param rs
    * @return
    */
   public int getSize();

   /**
    * Size left in bytes to the encapsulated byte store.
    * <br>
    * <br>
    * @return
    */
   public int getSizeAvailable();

   /**
    * Returns the counter of write/delete modifications since the creation of the store.
    * <br>
    * <br>
    * Allow for synchronization between 2 stores
    * <br>
    * <br>
    * @return 
    */
   public int getVersion();

   /**
    * Sets the byte data at index id.
    * <br>
    * <br>
    * The change is made permanent and the cache array is updated if the reference does not match.
    * <br>
    * <br>
    * @param id
    * @param b
    * @throws 
    */
   public void setBytes(int id, byte[] b);

   public void setBytes(int id, byte[] b, int offset, int len);

   public void setBytesEnsure(int id, byte[] b, int offset, int len);

   /**
    * See description in {@link IByteStore#setBytesEnsure(String, int, byte[])}
    * for Ensure details.
    * <br>
    * When Cache has the id, Ensure is irrevelant.
    * <br>
    * The Cache byte data is updated
    * <br>
    * Unless inside a transaction,the ByteStore is updated too.
    * <br>
    * @param id
    * @param b
    */
   public void setBytesEnsure(int id, byte[] b);

   public void serializeExport(BADataOS dos);

   public void serializeImport(BADataIS dis);


}
