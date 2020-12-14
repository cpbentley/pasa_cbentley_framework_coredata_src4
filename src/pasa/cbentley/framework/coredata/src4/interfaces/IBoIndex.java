package pasa.cbentley.framework.coredata.src4.interfaces;

import pasa.cbentley.byteobjects.src4.core.ByteObject;
import pasa.cbentley.core.src4.logging.IStringable;

/**
 * Indexing any integer values to a key
 * <br>
 * <br>
 * Strings are indexed using Pointed String collectors.
 * <br>
 * <br>
 * All key parameters are normalized
 * <li> a BID
 * <li> a Date
 * <br>
 * <br>
 * The index further normalize based on the root reference defined in the index byte header {@link ITechBoIndex#INDEX_OFFSET_05_REFERENCE_KEY4}
 * 
 * <br>
 * <br>
 * Key enumeration
 * 
 * Example:
 * business have keys starting from 1000 to 1100,...
 * <br>
 * <br>
 * Reference key is 1000.<br>
 * 
 * keyMin is 1000 unless changed
 * <br>
 * keyMax is 1100 and will change when a new key is added
 * <br>
 * <br>
 * For the time index, we have a class to maps
 * We want to map Dates to integers.
 * <br>
 * <br>
 * @author Charles-Philip Bentley
 *
 */
public interface IBoIndex extends IStringable {

   /**
    * Index bid to the key
    * <br>
    * <br>
    * If the key is below the minKey, the minKey is changed if possible and the reference key adjusted
    * <br>
    * <br>
    * This method could possibly crash the application if a very big key is paramed.
    * <br>
    * <br>
    * 
    * @param key
    * @param bid
    * @throws IllegalArgumentException when key is rejected because too big.
    */
   public void addBidToKey(int key, int bid);

   /**
    * Returns the first BID value
    * <br>
    * <br>
    * @param key domain is [0,...[
    * @return
    */
   public int getBID(int key);

   /**
    * Gets the BID values for that Key
    * <br>
    * <br>
    * 
    * @param key the key
    * @return non null array, empty if none
    */
   public int[] getBIDs(int key);

   /**
    * Fill index bid values inside the array at offset and for len.
    * Stops filling when reaching end.
    * <br>
    * @param key
    * @param values
    * @param offset
    * @param len
    * @param iteration value in the bids 
    * @return the number of values read into the array at offset
    */
   public int getBIDs(int key, int[] values, int offset, int len, int start);

   /**
    * 
    * @param startkey
    * @param endkey
    * @return
    */
   public int[] getBIDs(int startkey, int endkey);

   /**
    * Returns all BIDs for the set of keys contained in the array
    * <br>
    * <br>
    * @param keys
    * @return
    */
   public int[] getBIDs(int[] keys);

   /**
    * The indexed field of the {@link MBOByteObject}.
    * <br>
    * <br>
    * 
    * @return may return null
    */
   public ByteObject getField();

   /**
    * The highest key accepted by the index in {@link IBoIndex#getBID(int)} methods
    * <br>
    * <br>
    * Used in index enumeration.
    * <br>
    * <br>
    * Who decides it?
    * @return
    */
   public int getKeyMax();

   /**
    * The lowest possible key accepted without exceptions by {@link IBoIndex#getBID(int)} methods
    * <br>
    * <br>
    * An enumeration will start at keyMin and increment by one to enumeration all values stored at each key.
    * <br>
    * <br>
    * When an index key is a BID, the minimum value is usually 1. {@link MBOByteObject#getBID()}
    * <br>
    * <br>
    * @return
    */
   public int getKeyMin();

   /**
    * Returns the {@link ByteObject} describing the technical options of the Index.
    * <br>
    * <br>
    * {@link ITechBoIndex} specifies.
    * <br>
    * <br>
    * 
    * @return
    */
   public ByteObject getTechIndex();

   /**
    * True if the BID is stored at that key 
    * <br>
    * <br>
    * @param key
    * @param rid
    * @return
    */
   public boolean isKeyIndexingBid(int key, int bid);

}
