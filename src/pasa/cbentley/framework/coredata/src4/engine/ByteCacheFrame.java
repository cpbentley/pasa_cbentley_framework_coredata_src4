package pasa.cbentley.framework.coredata.src4.engine;


import pasa.cbentley.byteobjects.src4.core.ByteObject;
import pasa.cbentley.core.src4.ctx.UCtx;
import pasa.cbentley.core.src4.io.BADataIS;
import pasa.cbentley.core.src4.io.BADataOS;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.core.src4.logging.ITechLvl;
import pasa.cbentley.core.src4.thread.IBProgessable;
import pasa.cbentley.core.src4.utils.BitUtils;
import pasa.cbentley.framework.coredata.src4.ctx.CoreDataCtx;
import pasa.cbentley.framework.coredata.src4.db.IByteCache;
import pasa.cbentley.framework.coredata.src4.db.IByteStore;
import pasa.cbentley.framework.coredata.src4.db.IBOCacheRMS;
import pasa.cbentley.framework.coredata.src4.interfaces.IRecordStore;

/**
 * Tracks an interval of BIDs. Keep those records in memory for fast read access.
 * <br>
 * Keeps a hot open reference to an {@link IRecordStore}
 * <br>
 * Write operations always go through to the metal.
 * <br>
 * This cache is good when sequencetially reading records. It will automatically preloads next records, and keep older
 * records for a time in case the reading goes backwards.
 * <br>
 * 
 * This cache does not provides a <b>Transaction</b> mechanism. Because a frame cannot efficiently represent
 * random multiple records.
 * <br>
 * <br>
 * The cache setting may ask the frame to automatically extends to the whole record store. In effect, duplicating the
 * record store on disk into RAM memory.
 * <br>
 * <br>
 * The {@link IByteCache#manageCache(int, int)} allows the user to preload records from disk to memory.
 * <br>
 * <br>
 * The {@link IByteCache#getBytes(int)} may return reference or a copy {@link IBOCacheRMS#CACHE_FLAG_2_REFERENCE}
 * <br>
 * Raw cache for byte arrays
 * <br>
 * Works at the level of the hard_ID
 * <br>
 * <br>
 * This class is especially good at forward caching.
 * <br>
 * <br>
 * It caches data by chunks
 * <br>
 * <br>
 * @author Mordan
 *
 */
public class ByteCacheFrame implements IByteCache, IBOCacheRMS {

   /**
    * the actual caching of arrays.
    * <br>
    * <br>
    * When deleted, the byte array is null
    */
   private byte[][]     bytes;

   private int          countMiss;

   private int          countTargets;

   private int          flags;

   /**
    * defined the frame start data id
    */
   private int          frameRIDFirst;

   // keep the rid that were modified since the last cache update
   // public int[] modifiedids = new int[0];

   /**
    * the cache frame end as a RID
    */
   private int          frameRIDLast;

   /**
    * the number of padding data at the end of the cache
    */
   private int          growAdditioner  = 5;

   /**
    * Maps the RID
    */
   private int[]        ints;

   private boolean      isInitialized   = false;

   /**
    * max size of a frame
    */
   private int          maxFrameSize;

   /**
    * Cache the next rid to be used in the recordstore
    */
   private int          nextRID         = 0;

   /**
    * index of the {@link ByteCacheFrame#frameRIDLast} when the cache is full it is equal maxSize-1
    */
   private int          recordidLastIndex;

   private String       recordStoreName;

   /**
    * Cache of the Number of Records, valid only after initialization
    */
   private int          recordStoreSize = 0;

   private CoreDataCtx       rmc;

   private Transaction  root;

   private int          startFrameSize;

   /**
    * {@link IBOCacheRMS} tech parameters
    */
   private ByteObject   tech;

   private IRecordStore rs;

   /**
    * @param print
    *            the NULL MObject
    * @param rsname
    * @param size
    *            the max number of cached items. size should be 50% more than
    *            the actual number of records in the store or should just the
    *            size if it is a rarely growing
    */
   public ByteCacheFrame(CoreDataCtx dd, String rsname) {
      this(dd, rsname, dd.getTech(60, 5, 5));
   }

   public ByteCacheFrame(CoreDataCtx bs, String rsname, ByteObject tech) {
      if (rsname == null)
         throw new NullPointerException("record store name cannot be null");
      this.rmc = bs;
      this.tech = tech;
      recordStoreName = rsname;
      startFrameSize = tech.get4(IBOCacheRMS.CACHE_OFFSET_03_START_SIZE_4);
      maxFrameSize = tech.get4(IBOCacheRMS.CACHE_OFFSET_04_MAX_SIZE_4);
      growAdditioner = tech.get2(IBOCacheRMS.CACHE_OFFSET_02_GROW2);
      rs = rmc.getByteRecordStoreFactory().openRecordStore(rsname, false);
      reset();
      initCache(0);
   }

   /**
    * 
    */
   public int addBytes(byte[] data) {
      return getStore().addBytes(recordStoreName, data);
   }

   public int addBytes(byte[] data, int offset, int len) {
      return getStore().addBytes(recordStoreName, data, offset, len);
   }

   public void commitJournal(BADataOS dos) {
      // TODO Auto-generated method stub

   }

   /**
    * 
    */
   public void connect() {
   }

   /**
    * 
    */
   public void deleteBytes(int rid) {
      if (rid >= 0) {
         rs.deleteRecord(rid);
         if (!isOutOfFrame(rid)) {
            bytes[map(rid)] = null;
         }
      }
   }

   /**
    * 
    */
   public void disconnect() {
      rs.closeRecordStore();
   }

   /**
    * Grows the cache frame size so it at least supports the recid
    * <br>
    * <br>
    * 
    * @param recid nothing happens if recid bigger than maxSize
    * @return true if recid can be used, false if cannot grow
    */
   private boolean ensureCacheCapacity(int recid) {
      if (ints != null && recid < ints.length) {
         return true;
      } else {
         int newsize = Math.min(recid + growAdditioner, maxFrameSize);
         if (recordidLastIndex == maxFrameSize - 1) {
            if (newsize >= recordidLastIndex) {
               // cannot grow anymore, frame has to move
               return false;
            }
         } else {
            //#debug
            System.out.println("BIP:2123 Changing Cache Size to " + newsize + " for " + recordStoreName);
            if (ints == null) {
               ints = new int[newsize];
               bytes = new byte[newsize][];
            } else {
               int[] oints = ints;
               byte[][] obytes = bytes;
               ints = new int[newsize];
               bytes = new byte[newsize][];
               System.arraycopy(oints, 0, ints, 0, oints.length);
               System.arraycopy(obytes, 0, bytes, 0, obytes.length);
            }
         }
         return true;
      }
   }

   public void ensureCapacity(int rid) {
      ensureCapacity(rid, 0, null);
   }

   public void ensureCapacity(int rid, int ensureSize, IBProgessable p) {
      rmc.getByteStore().ensureCapacity(recordStoreName, rid, ensureSize, p);
      nextRID = getNextID();
   }

   public int getBase() {
      return getStore().getBase();
   }

   /**
    * When cache misses, see 
    * Method returns a reference to the cache array. Thus if modified, the method {@link IByteCache#setBytes(byte[], int)} must be
    * called with that array in order to make sure the modifications are saved.
    * <br>
    * <br>
    * 
    * @param rid > 0
    * @return null if record id is not valid or record is empty
    */
   public byte[] getBytes(int rid) {
      CacheRequest cr = new CacheRequest();
      if (root != null) {

      }
      if (isOutOfFrame(rid)) {

         //#debug
         rmc.getUC().toDLog().pMemory("Miss for RID=" + rid, this, IByteCache.class, "", ITechLvl.LVL_05_FINE, true);

         byte[] b = rs.getRecord(rid);
         this.manageCache(rid, rid, b);
         return b;
      } else {
         // record is is contained in the cache
         byte[] myD = bytes[map(rid)];
         if (!tech.hasFlag(CACHE_OFFSET_01_FLAG, CACHE_FLAG_2_REFERENCE)) {
            if (myD != null) {
               byte[] data = new byte[myD.length];
               System.arraycopy(myD, 0, data, 0, myD.length);
            } else {
               return null;
            }
         }
         return myD;
      }
   }

   /**
    * 
    */
   public int getBytes(int rid, byte[] data, int offset) {
      byte[] bytes = getBytes(rid);
      System.arraycopy(bytes, 0, data, offset, bytes.length);
      return bytes.length;
   }

   public byte[] getBytesCheck(int rid) {
      throw new RuntimeException();
   }

   /**
    * 
    */
   public byte[] getBytesThrough(int rid) {
      return rs.getRecord(rid);
   }

   public byte[][] getCacheData() {
      return bytes;
   }

   public IByteCache getCacheMorph(ByteObject type) {
      return this;
   }

   public ByteObject getCacheSpec() {
      return tech;
   }

   public int getFrameEnd() {
      return frameRIDLast;
   }

   public int getFrameStart() {
      return frameRIDFirst;
   }

   public int[] getInts() {
      return ints;
   }

   public long getLastModified() {
      return rs.getLastModified();
   }

   public int getNextID() {
      return nextRID;
   }

   public int getNumRecords() {
      return recordStoreSize;
   }

   public int getSize() {
      return rs.getSize();
   }

   public int getSizeAvailable() {
      return getStore().getSizeAvailable(recordStoreName);
   }

   public IByteStore getStore() {
      return rmc.getByteStore();
   }

   public int getVersion() {
      return getStore().getVersion(recordStoreName);
   }

   public boolean hasFlag(int flag) {
      return BitUtils.hasFlag(flags, flag);
   }

   /**
    * 
    * @param options
    *            ignored
    */
   public void initCache(int options) {
      if (!isInitialized) {
         // most basic
         updateFrame(1);
         isInitialized = true;
      }
   }

   /**
    * True if all spots are taken
    * 
    * @return
    */
   public boolean isFull() {
      return (bytes.length == recordidLastIndex + 1);
   }

   /**
    * True if the RID is inside the Cache Frame
    * <br>
    * <br>
    * 
    * @param recordid
    * @return
    */
   private boolean isOutOfFrame(int recordid) {
      return recordid > frameRIDLast || recordid < frameRIDFirst;
   }

   /**
    * True if the cache full contains the RecordStore
    * 
    * @return
    */
   public boolean isRecordStoreContained() {
      return (frameRIDLast == nextRID - 1) && (recordidLastIndex >= recordStoreSize);
   }

   public void manageCache(int startID, int endID) {
      // TODO Auto-generated method stub

   }

   /**
    * Maybe keeps the byte array 
    */
   public void manageCache(int idStart, int idEnd, byte[] data) {
      // TODO Auto-generated method stub

   }

   /**
    * maps the record id to the index in the cache
    * 
    * @param rid
    * @return
    */
   private int map(int rid) {
      return rid - frameRIDFirst;
   }

   private void reset() {
      int base = getStore().getBase();
      frameRIDFirst = base - 1;
      frameRIDLast = frameRIDFirst;
      recordidLastIndex = -1;
      bytes = null;
   }

   /**
    * 
    */
   public void setBytes(int id, byte[] b) {
      if (root != null) {
         root.setBytes(id, b);
      } else {
         if (ints == null) {
            initCache(0);
         }
         //borderline case that is equivalent to addBytes
         if (id == nextRID) {
            update(id, b);
         }
         getStore().setBytes(recordStoreName, id, b);
         //update with the mapping function the byte data in the cache
         if (!isOutOfFrame(id)) {
            bytes[map(id)] = b;
         }
         recordStoreSize = rs.getNumRecords();
         nextRID = rs.getNextRecordID();

      }
   }

   public void setBytes(int id, byte[] b, int offset, int len) {
      byte[] d = new byte[len];
      System.arraycopy(b, offset, d, 0, len);
      setBytes(id, d);
   }

   public void setBytesEnsure(int id, byte[] b) {
      getStore().ensureCapacity(recordStoreName, id);
      setBytes(id, b);
   }

   public void setBytesEnsure(int id, byte[] b, int offset, int len) {
      getStore().ensureCapacity(recordStoreName, id);
      setBytes(id, b, offset, len);
   }

   public void setFlag(int flag, boolean v) {
      flags = BitUtils.setFlag(flags, flag, v);
   }

   public void setLastCacheid(int recordid, int indexval) {
      frameRIDLast = recordid;
      recordidLastIndex = indexval;
   }

   public void setMode(int mode) {
      // TODO Auto-generated method stub

   }

   //#mdebug
   public String toString() {
      return Dctx.toString(this);
   }

   public void toString(Dctx sb) {
      sb.root(this, "ByteCacheFrame");
   }

   public String toString1Line() {
      return Dctx.toString1Line(this);
   }

   public void toString1Line(Dctx dc) {
      dc.root1Line(this, "ByteCacheFrame");
   }
   //#enddebug

   public UCtx toStringGetUCtx() {
      return rmc.getUC();
   }

   public void transactionCancel() {
      // TODO Auto-generated method stub

   }

   public void transactionCommit() {
      // TODO Auto-generated method stub

   }

   public void transactionStart() {
      Transaction t = new Transaction(rmc, this);
      if (root != null) {
         root.add(t);
      } else {
         root = t;
      }
   }

   /**
   * 
   * if not present
   * increase the content of the cache with the object
   * if rid already present in cache, update the byte array
   * cache already full do nothing
   * use the moveFrame method to make it visible
   * @param mo
   */
   private void update(int id, byte[] ar) {
      if (ints == null)
         updateFrame(1);
      if (id > 0) {
         // case the next falls outside the cache array length
         // the method grows it, if inside, does nothing and return true
         if (ensureCacheCapacity(recordidLastIndex + 1)) {
            recordidLastIndex++;
            ints[recordidLastIndex] = id;
            bytes[recordidLastIndex] = ar;
            setLastCacheid(id, recordidLastIndex);
            frameRIDFirst = ints[0];
         }
      }
   }

   /**
    * Full Refresh from the database table from start frame. Reads frame length records from disk to RAM. 
    * <br>
    * <br>
    * Update the cache configuration so that start does not generate a cache miss.
    * <br>
    * <br>
    * 
    * exception case arises when there are holes in the record store and size < num of records and start > size size is smaller
    * <br>
    * <br>
    * 
    * @param start
    * 
    */
   public void updateFrame(int start) {
      // #debug
      System.out.println("#ByteCache#updateFrame " + recordStoreName + ": frame start=" + start);
      if (start < maxFrameSize) {
         start = 1;
      }
      try {
         recordStoreSize = getSize();
         nextRID = getNextID();
         if (nextRID == 1) {
            // #debug
            // System.out.println("Creating Cache Structure "+ _rsname);
            ints = new int[growAdditioner];
            bytes = new byte[growAdditioner][];
            updateFrameTuple();
            return;
         }
         if (start >= nextRID) {
            // #debug
            System.out.println("BIP:805 Update on cache failed. start >= nextid; start=" + start + " nextid=" + nextRID);
            return;
         }
         int max = Math.min(recordStoreSize + growAdditioner, maxFrameSize);
         if (ints != null && max == ints.length) {
            // do nothing
         } else {
            ints = new int[max];
            bytes = new byte[max][];
         }

         // Brute Force to ensure Order of ascending record id
         int count = 0;
         int lastID = Math.min(start + maxFrameSize, nextRID);

         for (int id = start; id < lastID; id++) {
            bytes[count] = rs.getRecord(id);
            ints[count] = id;
            count++;
            if (count == max) {
               break;
            }
         }
      } catch (Exception e) {
         // #debug
         e.printStackTrace();
      } finally {
         if (ints == null) {
            ints = new int[0];
            bytes = new byte[0][];
         }
      }
      updateFrameTuple();
   }

   /**
    * Update the Frame (tuple _recordidfirst and _recordidlast)
    * 
    */
   public void updateFrameTuple() {
      // if startrecordid does not exists, position 0 will have the first existing id after start
      if (ints != null && ints.length != 0) {
         frameRIDFirst = ints[0];
         for (int i = ints.length - 1; i > 1; i--) {
            if (ints[i] != 0) {
               setLastCacheid(ints[i], i);
               break;
            }
         }
      } else {
         frameRIDFirst = 0;
         frameRIDLast = 0;
         recordidLastIndex = -1;
      }

   }

   public boolean validateRecordID(int id) {
      if (ints == null) {
         initCache(0);
      }
      // record size is correct
      if (id <= 0 || id > recordStoreSize) {
         return false;
      }
      return true;
   }

   public void serializeExport(BADataOS dos) {
      // TODO Auto-generated method stub
      
   }

   public void serializeImport(BADataIS dis) {
      // TODO Auto-generated method stub
      
   }

}
