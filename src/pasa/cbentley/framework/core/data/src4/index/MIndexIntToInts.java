package pasa.cbentley.framework.core.data.src4.index;

import pasa.cbentley.byteobjects.src4.core.ByteObject;
import pasa.cbentley.byteobjects.src4.core.ByteObjectFactory;
import pasa.cbentley.byteobjects.src4.ctx.IBOTypesBOC;
import pasa.cbentley.core.src4.ctx.IToStringFlagsUC;
import pasa.cbentley.core.src4.ctx.UCtx;
import pasa.cbentley.core.src4.io.BADataIS;
import pasa.cbentley.core.src4.io.BADataOS;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.core.src4.logging.IDLog;
import pasa.cbentley.core.src4.structs.IntBuffer;
import pasa.cbentley.core.src4.thread.IBProgessable;
import pasa.cbentley.core.src4.utils.BitUtils;
import pasa.cbentley.core.src4.utils.IntUtils;
import pasa.cbentley.framework.core.data.src4.ctx.CoreDataCtx;
import pasa.cbentley.framework.core.data.src4.ctx.ObjectDAC;
import pasa.cbentley.framework.core.data.src4.db.IBOCacheRMS;
import pasa.cbentley.framework.core.data.src4.db.IByteCache;
import pasa.cbentley.framework.core.data.src4.db.IByteStore;
import pasa.cbentley.framework.core.data.src4.db.IDataImportExport;
import pasa.cbentley.framework.core.data.src4.interfaces.IBoIndex;

/**
 * An Index using a {@link IByteCache} to store incremental keys linking to value(s).
 * <br>
 * <br>
 * In effect, it is a huge int[][] array on disk, possibly implemented with a random access file.
 * <br>
 * <br>
 * 
 * It may have fixed size records (Chain Index) or Patch Index
 * <br>
 * <br>
 * 
 * Type -> Index -> IndexKey -> Connector -> byte[] -> DataKeys[] -> DataByteArrays from ByteStore 
 * <br>
 * <br>
 * 
 * To create an {@link MIndexIntToInts}, you will need the index handle and a tech param header defined by {@link IBOIndex}.
 * <br>
 * <br>
 * 
 * The {@link MIndexIntToInts} stores its header and administrative data in the first byte[] of the byte cache
 * <br>
 * <br>
 * The index may use the RIDs as it wants.
 * <br>
 * <br>
 * Previously, we had TimeIndex and KeyIndex. They have been merged in this class. For keying date values,
 * the reference date is set as the reference Key by {@link MIndexIntToInts#setReferenceKey(int)}. This is automatically done
 * by {@link MIndexIntToInts#addBidToKey(int, int)}.
 * <br>
 * The date is mapped to a day number
 * <br>
 * <br>
 * TODO: chain where several elements are on the same RID row. a 3 bytes chain pointer (to next row) and a 1 byte number of element on row.
 * <br>
 * <br>
 * 
 * @author Charles-Philip Bentley
 *
 */
public class MIndexIntToInts extends ObjectDAC implements IBoIndex, IBOIndex, IDataImportExport {

   public static final int MINUS_SIGN_16BITS_FLAG = 32768;

   public byte[] copyAreaToArray(IByteInteger byteIntegerArea, byte[] src, int srcoffset, int areaSrc, byte[] dest, int destOffset, int areaDest) {
      int[] datas = byteIntegerArea.toInts(src, srcoffset, areaSrc);

      //#debug
      String msg = "SrcArea=" + areaSrc + " to destArea=" + areaDest + " " + dac.getUC().getIU().toStringIntArray1Line("Copying values", datas, ",");
      //#debug
      toDLog().pFlow(msg, byteIntegerArea, MIndexIntToInts.class, "copyAreaToArray", LVL_05_FINE, true);

      for (int i = 0; i < datas.length; i++) {
         dest = byteIntegerArea.addIntToBytes(dest, destOffset, areaDest, datas[i]);
      }
      return dest;
   }

   /**
    * {@link IBOIndex#INDEX_OFFSET_08_NUM_AREAS2}
    */
   protected int               areaNumber;

   IByteStore                  bs;

   /**
    * Link to the {@link IByteCache} wrapping over the {@link IByteStore}.
    * <br>
    * <br>
    * Data store keeping the index data.
    * <br>
    * Can be null?
    */
   protected IByteCache        byteCache;

   /**
    * When not null, this is the {@link IByteCache} that contains the chaining.
    * <br>
    * When null, patched index instead of chain.
    * <br>
    * <br>
    * no areas in this case. The chainining is done record on record.
    * <br>
    * <br>
    * 
    */
   protected IByteCache        byteCacheChained;

   /**
    * Reader of patched values.
    * <br>
    * <br>
    * {@link IByteInteger} creates new lines to hold the patched integers.
    */
   protected IByteInteger      byteIntegerArea;


   /**
    * Cache holder for {@link IBOIndex#INDEX_OFFSET_04_AUX_BYTESIZE1}, the number of bytes needed to code the chain pointer.
    * <br>
    * <br>
    * When chain byte size has to be increased. it takes a very long time.
    * <br>
    * <br>
    * Reason that for potentially more than 60 000 items, 3 bytes should be used.
    * <br>
    * <br>
    * 
    */
   private int                 chainByteSize;

   /**
    * the value stored at the last maillon in a chain.
    * <br>
    * <br>
    * This is the value where is stored the chain auxiliary header.
    * <br>
    * <br>
    * Initialized by {@link MIndexIntToInts#loadIndexHeader(ByteObject)} to First RID
    */
   protected int               chainEmpty;

   /**
    * Value only stored in the head column to specify that nothing is stored there.
    * <br>
    * <br>
    * Initialized by {@link MIndexIntToInts#loadIndexHeader(ByteObject)} to Second RID
    */
   protected int               chainNull;

   /**
    * Number of elements in the tail.
    * <br>
    * <br>
    * Used as an upper boundary while iterating blindly down a tail. in case data corruption created
    * a tail loop.
    */
   private int                 chainTailSecurityNum;

   protected int               entryheadersize;

   /**
    * The indexed field of a {@link MBOByteObject} 
    * 
    * <br>
    * <br>
    * see {@link MIndexIntToInts#setField(ByteObject)}
    */
   protected ByteObject        field;

   private int                 INDEX_HEADER_POSITION = 0;

   /**
    * The base record store name for this index.
    * <br>
    * <br>
    * Set by the constructor and creating code.
    * <br>
    * <br>
    * It will be shadow suffixed by 0 when the first header read tells us that
    * we have a shadow file.
    */
   protected String            indexByteStoreHandle;

   /**
    * Never take shadow suffixes.
    * <br>
    * <br>
    * It is used to create the shadowed record store name.
    */
   private String              indexByteStoreHandleBackUp;

   /**
    * Defines the {@link MIndexIntToInts} properties. Provided in constructor as a bootstrap. Else it is loaded
    * from the data record stored in the first RID.
    * <br>
    * <br>
    * Stores 
    * <br>
    * static data
    * <li> {@link IBOIndex#INDEX_OFFSET_02_TYPE1} dynamic data. Chain index or pack.
    * <li> {@link IBOIndex#INDEX_OFFSET_03_VALUE_BYTESIZE1}
    * <li> {@link IBOIndex#INDEX_OFFSET_05_REFERENCE_KEY4}
    * <li> {@link IBOIndex#INDEX_OFFSET_08_NUM_AREAS2}
    * <li> ...
    * <br>
    * <br>
    * Can it be null? No.
    * Once the constructor is finished, the index header is never null.
    */
   protected ByteObject        indexHeader;

   /**
    * Starts at 2 and evolves.
    * <br>
    * <br>
    * Same domain as a normalized key {@link MIndexIntToInts#getKeyNormalized(int)}
    * <br>
    * <br>
    * 
    * Unless the boostrap method create X values of empty index entries.
    */
   protected int               keyMaxNorm;

   /**
    * Default, it will always be 2.
    */
   protected int               keyMinNorm;

   /**
    * 
    * Value will be 1000 with a reference key of 1000.
    * <br>
    * <br>
    * 
    * This value is not normalized.
    * <br>
    * <br>
    * 
    */
   protected int               keyMinUser;

   /**
    * Value in user referential i.e. not normalized. Stored at {@link IBOIndex#INDEX_OFFSET_05_REFERENCE_KEY4}
    * <br>
    * <br>
    * When not set by user configuration, the default is {@link IByteStore#getBase()} + 1 to match the default
    * byte store configuration.
    * <br>
    * <br>
    * A value of X means all the keys start at that value.
    * <br>
    * <br>
    */
   protected int               keyReference;

   private int                 minID;

   /**
    * Only diffreent from zero when areas and reference key is changed.
    * e.g areas is 5. there is a shift of 1. the first 4 areas are empty. so when computing
    * the key of, one must shift with this number
    */
   protected int               refAreaShift;

   /**
    * The number of Full RIDs consumed by headers.
    * <br>
    * <br>
    * 
    * Default is 1.
    * <br>
    * <br>
    * 
    * That means the first entry from connector is index header information
    * and cannot be used as a key
    */
   protected int               rsheadersize          = 1;

   /**
    * True when chains use patches
    * {@link IBOIndex#INDEX_FLAG_5_CHAIN_PATCH}
    */
   boolean                     useChainPatch         = false;

   /**
    * Takes into account the patch header.
    * <br>
    * <br>
    * The first byte will be the patch header.
    * <br>
    * <br>
    * 
    */
   private int                 valueByteSize;

   /**
    * Creates a {@link MIndexIntToInts} from serialized data.
    * <br>
    * <br>
    * {@link MIndexIntToInts#serializeReverseImport(BytesCounter)}
    * <br>
    * <br>
    * When un serializing, the boostrap header is written at the beginning, in addition
    * to the 1st record.
    * @param data
    */
   public MIndexIntToInts(CoreDataCtx cdc, byte[] data) {
      super(cdc);
      this.bs = cdc.getByteStore();
      minID = bs.getBase();
      serializeReverseImport(cdc.getUC().createNewBADataIS(data));
   }

   /**
    * Unique Index
    * <br>
    * <br>
    * Takes a copy of the {@link ByteObject} tech.
    * @param byteStore
    * @param tech
    * @param cacheTech {@link IBOCacheRMS}
    */
   public MIndexIntToInts(CoreDataCtx cdc, String byteStore, ByteObject tech) {
      super(cdc);
      this.bs = cdc.getByteStore();
      minID = bs.getBase();
      indexByteStoreHandle = byteStore;
      indexByteStoreHandleBackUp = byteStore;
      loadIndexHeader(tech.cloneCopyHeadCopyParams());
   }

   /**
    * 
    * <br>
    * @param key
    * @param bid
    * @param d
    */
   protected void addBidToChain(int key, int bid, int area, byte[] d, int krid) {
      if (d.length == 0) {
         //null chain
         d = getEmptyChainArray();
      }
      //first check if it is already there
      int[] vales = getIntsChain(key, area, d);
      if (!IntUtils.contains(vales, bid)) {
         //check for null
         int totalByteSize = valueByteSize + chainByteSize;
         int chainDataIndex = (area * totalByteSize) + valueByteSize;
         int valueDataIndex = area * totalByteSize;
         int headChainPointer = ByteArrayStaticUtilz.getValue(d, chainDataIndex, chainByteSize);
         //all heads boostraps will first branch here.
         if (headChainPointer == chainNull) {
            if (useChainPatch) {
               ByteArrayStaticUtilz.setValue(d, valueDataIndex, 1, 1);
               ByteArrayStaticUtilz.setValue(d, valueDataIndex + 1, bid, valueByteSize - 1);
            } else {
               ByteArrayStaticUtilz.setValue(d, valueDataIndex, bid, valueByteSize);
            }
            ByteArrayStaticUtilz.setValue(d, chainDataIndex, chainEmpty, chainByteSize);
            //head is modified so save it
            byteCache.setBytes(krid, d);
         } else {
            //check if patching
            if (useChainPatch) {
               addBidToChainWithPatch(bid, area, d, vales);
            } else {
               if (indexHeader.hasFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_3_TAIL)) {
                  addBidToChainFTail(bid, area, d, krid);
               } else {
                  addBidToChainFHead(bid, area, d, krid);
               }
            }
         }
      }

   }

   /**
    * Adds the value at the head of the chain. i.e that is
    * <br>
    * <br>
    * Checks for kick start Null value.
    * <br>
    * <br>
    * 
    * LIFO: Last in first out
    * <br>
    * <br>
    * @param bid
    * @param area
    * @param headData
    */
   protected void addBidToChainFHead(int bid, int area, byte[] headData, int krid) {
      int totalByteSize = valueByteSize + chainByteSize;
      int chainDataIndex = (area * totalByteSize) + valueByteSize;
      int valueDataIndex = area * totalByteSize;

      //head = add and shift all values down by simply creating a new entry 
      int headChainValue = ByteArrayStaticUtilz.getValue(headData, valueDataIndex, valueByteSize);
      int headChainPointer = ByteArrayStaticUtilz.getValue(headData, chainDataIndex, chainByteSize);

      //copy it new chain maillon.
      byte[] data = new byte[valueByteSize + chainByteSize];
      //so bring old head to new maillon
      ByteArrayStaticUtilz.setValue(data, 0, headChainValue, valueByteSize);
      ByteArrayStaticUtilz.setValue(data, valueByteSize, headChainPointer, chainByteSize);
      //add it to chain byte store
      int rid = byteCacheChained.addBytes(data);
      //the rid is the new chain pointer. no areas in this case
      ByteArrayStaticUtilz.setValue(headData, valueDataIndex, bid, valueByteSize);
      ByteArrayStaticUtilz.setValue(headData, chainDataIndex, rid, chainByteSize);

      byteCache.setBytes(krid, headData);
   }

   /**
    * Adds Bid value to chain array using the tail.
    * <br>
    * <br>
    * FIFO : first (oldest) stays at the head 
    * <br>
    * <br>
    * When called, the chainData in head is NEVER null.
    * @param bid
    * @param area
    * @param head
    */
   protected void addBidToChainFTail(int bid, int area, byte[] head, int krid) {
      int totalByteSize = valueByteSize + chainByteSize;
      int valueDataIndex = area * totalByteSize;
      int chainDataIndex = (area * totalByteSize) + valueByteSize;
      //not null by contract
      int chainData = ByteArrayStaticUtilz.getValue(head, chainDataIndex, chainByteSize);
      //tail = add at the end.. follow the chain and add a new one
      byte[] keyData = head;
      int chainDataPrevious = -1;
      int count = 0;
      //not very beautiful code. but i wanted a securiy that the code won't loop forever.
      while (count < chainTailSecurityNum + 1) {
         if (chainData == chainEmpty) {
            //add here. new key depends on areas.
            //create new
            byte[] data = new byte[valueByteSize + chainByteSize];
            ByteArrayStaticUtilz.setValue(data, 0, bid, valueByteSize);
            ByteArrayStaticUtilz.setValue(data, valueByteSize, chainEmpty, chainByteSize);
            int rid = byteCacheChained.addBytes(data);
            ByteArrayStaticUtilz.setValue(keyData, chainDataIndex, rid, chainByteSize);
            if (keyData == head) {
               byteCache.setBytes(krid, keyData);
            } else {
               //update the last record which now points to the new last record
               setKeyChainedData(chainDataPrevious, keyData);
            }
            //System.out.println("#BoIndexBase addChainTail " + rid);
            break;
         } else {
            //read next entry in the chain
            //after 1st loop, the chain pointer index is always this.

            chainDataIndex = valueByteSize;
            //chainData is the key
            chainDataPrevious = chainData;
            keyData = getKeyChainedData(chainData);
            //read the new chain data
            chainData = ByteArrayStaticUtilz.getValue(keyData, chainDataIndex, chainByteSize);
         }
         count++;
      }

   }

   protected void addBidToChainWithPatch(int bid, int area, byte[] d, int[] vales) {
      vales = getIntUtils().addIntToEndOf(vales, bid);
      int[] patched = getIntUtils().patch(vales, 0, vales.length, 255);
      //update the chain with patches no tailing or heading in patched chains.

      setPatchedValues(area, d, patched);
   }

   /**
    * Adds/Indexes the bid integer to the key.
    * <br>
    * <br>
    * Automatically expand the index key domain to include the given key
    * <br>
    * <br>
    * No check is made whether the bid is already stored at the given key.
    * <br>
    * <br>
    * @param key user key not normalized
    * @param rid
    * @throws IllegalArgumentException when {@link IBOIndex#INDEX_OFFSET_07_KEY_REJECT_2} rejects key
    */
   public void addBidToKey(int key, int bid) {
      if (key < keyReference) {
         setReferenceKey(key);
      }
      int keyNorm = getKeyNormalized(key);
      //the starting case where 0
      if (keyNorm >= keyMaxNorm) {
         int rej = indexHeader.get2(INDEX_OFFSET_07_KEY_REJECT_2);
         if (keyNorm > keyMaxNorm + rej) {
            //#debug
            String msg = "Index Key " + key + " is too big. KeyNorm=" + keyNorm + " keyMax=" + keyMaxNorm + " rejectInterval=" + rej + " RefKey=" + keyReference;
            //#debug
            toDLog().pData(msg, this, MIndexIntToInts.class, "addBidToKey", LVL_05_FINE, true);
         } else {
            //create the room for the key
            addKeysToKey(keyNorm);
         }
      }

      int area = getKeyArea(keyNorm);
      int krid = getKeyRID(keyNorm);
      byte[] bytes = getIndexEntry(krid);
      if (bytes == null) {
         //#ifdef mordan.notprod
         bipWrongKey(keyNorm);
         //#endif
         return;
      }

      if (byteCacheChained == null) {
         addPatch(krid, bid, area, bytes);
      } else {
         addBidToChain(krid, bid, area, bytes, krid);
      }
   }

   /**
    * Checks if the value is a NON null and add it
    * <br>
    * <br>
    * 
    * @param ib
    * @param keyData
    * @return
    */
   private int addChainValueToBuffer(IntBuffer ib, byte[] keyData) {
      int chainData = 0;
      int value = 0;
      int valueIndex = 0;
      int chainDataIndex = valueByteSize;
      addToBufferChain(ib, keyData, valueIndex);
      //the pointer that include key/area
      chainData = ByteArrayStaticUtilz.getValue(keyData, chainDataIndex, chainByteSize);
      return chainData;
   }

   /**
    * 
    * @param ib
    * @param keyData
    * @param area
    * @return
    */
   private int addChainValueToBuffer(IntBuffer ib, byte[] keyData, int area) {
      int totalByteSize = valueByteSize + chainByteSize;
      int valueIndex = area * totalByteSize;
      int chainDataIndex = valueIndex + valueByteSize;
      int chainData = ByteArrayStaticUtilz.getValue(keyData, chainDataIndex, chainByteSize);
      if (chainData == chainNull) {
         return chainEmpty;
      } else {
         addToBufferChain(ib, keyData, valueIndex);
         return chainData;
      }
   }

   /**
    * Method that creates a new entry/entries in the Index so that the given key is a valid index key.
    * <br>
    * <br>
    * 
    * @param keyNorm
    */
   private synchronized void addKeysToKey(int keyNorm) {
      int ridDiff = getRidDiff(keyMaxNorm, keyNorm);
      indexHeader.set4(INDEX_OFFSET_06_KEY_NORM_MAX4, keyNorm);
      saveHeader();
      keyMaxNorm = keyNorm;
      byteCache.connect();
      byte[] data = null;
      if (byteCacheChained != null) {
         data = getEmptyChainHead();
      } else {
         data = byteIntegerArea.createLine();
      }
      for (int i = 0; i <= ridDiff; i++) {
         byteCache.addBytes(data);
      }
      byteCache.disconnect();
   }

   protected void addPatch(int krid, int bid, int area, byte[] bytes) {
      if (bytes.length == 0) {
         bytes = byteIntegerArea.createLine();
      }
      bytes = byteIntegerArea.addIntToBytes(bytes, 0, area, bid);
      byteCache.setBytes(krid, bytes);
   }

   protected void addToBufferChain(IntBuffer ib, byte[] keyData, int valueIndex) {
      int value;
      if (useChainPatch) {
         int patch = ByteArrayStaticUtilz.getValue(keyData, valueIndex, 1);
         value = ByteArrayStaticUtilz.getValue(keyData, valueIndex, valueByteSize - 1);
         for (int i = 0; i < patch; i++) {
            ib.addInt(value);
            value++;
         }
      } else {
         value = ByteArrayStaticUtilz.getValue(keyData, valueIndex, valueByteSize);
         ib.addInt(value);
      }
   }

   protected void bipWrongKey(int key) {
      String msg = " Wrong RID for key=" + key + ", Handle=" + indexByteStoreHandle;
      //#debug
      toDLog().pNull(msg, this, MIndexIntToInts.class, "bipWrongKey", LVL_09_WARNING, true);
   }

   /**
    * What happens when data bytesize is bigger than current byte size?
    * <li> throws an {@link IllegalArgumentException}
    * <li> truncate
    * <li> expand the data byte size. requires a re-write of the whole index
    * <br>
    * <br>
    * @param bid
    */
   protected void checkByteSize(int bid) {
      if (byteCacheChained == null) {
         if (BitUtils.byteSize(bid) > valueByteSize) {
            //#debug
            toDLog().pNull("byteSize of " + bid + ">" + valueByteSize, this, MIndexIntToInts.class, "checkByteSize", LVL_05_FINE, true);
         }
      } else {
         if (useChainPatch) {
            if (BitUtils.byteSize(bid) > valueByteSize - 1) {
               //#debug
               toDLog().pNull("byteSize of " + bid + ">" + (valueByteSize - 1), this, MIndexIntToInts.class, "checkByteSize", LVL_05_FINE, true);
            }
         } else {
            if (BitUtils.byteSize(bid) > valueByteSize) {
               //#debug
               toDLog().pNull("byteSize of " + bid + ">" + valueByteSize, this, MIndexIntToInts.class, "checkByteSize", LVL_05_FINE, true);
            }
         }
      }
   }

   /**
    * Saves the index state.
    */
   public void close() {

   }

   /**
    * Deletes the shadow file pointer if needed.
    * <br>
    * <br>
    * Deletes everything.
    */
   public void deleteIndex() {
      bs.deleteStore(indexByteStoreHandle);
      if (indexHeader.get1(INDEX_OFFSET_02_TYPE1) == IBOIndex.TYPE_1_CHAIN) {
         String chainHandle = indexByteStoreHandle + "_chain";
         bs.deleteStore(chainHandle);
      }
      if (indexByteStoreHandleBackUp != indexByteStoreHandle) {
         bs.deleteStore(indexByteStoreHandleBackUp);
         if (indexHeader.get1(INDEX_OFFSET_02_TYPE1) == IBOIndex.TYPE_1_CHAIN) {
            String chainHandle = indexByteStoreHandleBackUp + "_chain";
            bs.deleteStore(chainHandle);
         }
      }
   }

   /**
    * When calling this method, one must be careful.
    * <br>
    * <br>
    * @param key
    */
   public void ensureKey(int key) {
      if (key < keyReference) {
         setReferenceKey(key);
      }
      int keyNorm = getKeyNormalized(key);
      //the starting case where 0
      if (keyNorm >= keyMaxNorm) {
         //create the room for the key
         addKeysToKey(keyNorm);
      }
   }

   public int getBID(int key) {
      return getInts(key)[0];
   }

   public int[] getBIDs(int key) {
      return getInts(key);
   }

   public int[] getBIDs(int startkey, int endkey) {
      return getBIDs(getIntUtils().getGeneratedInterval(startkey, endkey));
   }

   /**
    * Currrently not optimized
    */
   public int getBIDs(int key, int[] values, int offset, int len, int start) {
      int[] bids = getBIDs(key);
      int count = 0;
      for (int i = 0; i < len; i++) {
         if (start + count < bids.length) {
            values[offset + count] = bids[start + count];
            count++;
         }
      }

      return count;
   }

   public int[] getBIDs(int[] keys) {
      return getInts(keys);
   }

   public int[] getBIDsSorted(int key) {
      int[] vals = getInts(key);
      getIntUtils().sortBasicAscending(vals, 0, vals.length);
      return vals;
   }

   private byte[] getEmptyChainArray() {
      byte[] data;
      data = new byte[(valueByteSize + chainByteSize) * areaNumber];
      //sets the special chain data so that the class is able to differentiate 0,0 
      int index = valueByteSize;
      for (int i = 0; i < areaNumber; i++) {
         ByteArrayStaticUtilz.setValue(data, index, chainNull, chainByteSize);
         index += (chainByteSize + valueByteSize);
      }
      return data;
   }

   /**
    * 
    * @return
    */
   private byte[] getEmptyChainHead() {
      byte[] data = null;
      if (indexHeader.hasFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_1_NULL_CHAIN)) {
         data = new byte[0];
      } else {
         data = getEmptyChainArray();
      }
      return data;
   }

   /**
    * 
    */
   public ByteObject getField() {
      return field;
   }

   /**
    * The number of records used as a header
    * @return
    */
   public int getHeaderSize() {
      return rsheadersize;
   }

   /**
    * This method is dangerous.
    * @param key
    * @return null when invalid key
    */
   protected byte[] getIndexEntry(int key) {
      return byteCache.getBytesCheck(key);
   }

   /**
    * RecordStore string used to store the index data.
    * <br>
    * <br>
    * 
    * @return
    */
   public String getIndexHandle() {
      return indexByteStoreHandle;
   }

   /**
    * Gets the RID/indexed values for that Key
    * <br>
    * <br>
    * Loads the whole byte array from disk. iteration optimization is hard if that array is not broken into pieces.
    * <br>
    * @param key the key
    * @return non null array, empty if none
    */
   private int[] getInts(int key) {
      int normalizedKey = getKeyNormalized(key);
      int rid = getKeyRID(normalizedKey);
      int area = getKeyArea(normalizedKey);
      byte[] indexByteData = getIndexEntry(rid);
      if (indexByteData == null) {
         //#ifdef mordan.notprod
         bipWrongKey(normalizedKey);
         //#endif
         return new int[0];
      }
      if (indexByteData.length == 0)
         return new int[0];
      if (byteCacheChained == null) {
         return byteIntegerArea.toInts(indexByteData, 0, area);
      } else {
         return getIntsChain(rid, area, indexByteData);
      }

   }

   /**
    * Returns all DataKeys for the set of keys contained in the array
    * <br>
    * <br>
    * <br>
    * @param keys
    * @return
    */
   public int[] getInts(int[] keys) {
      int count = 0;
      int[][] akeys = new int[keys.length][];
      for (int i = 0; i < keys.length; i++) {
         akeys[i] = getInts(keys[i]);
         count += akeys[i].length;
      }
      int[] last = new int[count];
      count = 0;
      for (int i = 0; i < akeys.length; i++) {
         for (int j = 0; j < akeys[i].length; j++) {
            last[count] = akeys[i][j];
            count++;
         }
      }
      return last;
   }

   /**
    * Reads the head chain maillon in keyData and go down building the array
    * <br>
    * <br>
    * What is considered an null chain? 
    * <li>An empty keyData? Length of zero? that could be an option.
    * <li>Or a specific configured value?
    * <li>Or a specific chainData?
    * <br>
    * <br>
    * 
    * Chain data cannot be 0 or 1
    * <br>
    * <br>
    * 
    * @param rid RID to load the root chain
    * @param area 0-index used to locate first value of the chain (head by default, tail when flag is on)
    * @param keyData
    * @return
    */
   protected int[] getIntsChain(int rid, int area, byte[] keyData) {
      IntBuffer ib = new IntBuffer(dac.getUC());
      //init by reading the first index value. then read
      int chainDataKey = addChainValueToBuffer(ib, keyData, area);
      while (chainDataKey != chainEmpty) {
         //read the byte data in the chain table
         keyData = getKeyChainedData(chainDataKey);
         if (keyData == null) {
            //chain is broken
            throw new NullPointerException();
         }
         //added for the tail code.. WHY? remove?
         //System.out.println("#BoIndexBase#getIntsChain Buffer " + ib.toString() + "chainData=" + chainData);
         chainDataKey = addChainValueToBuffer(ib, keyData);
      }
      return ib.getIntsClonedTrimmed();
   }

   public IntUtils getIntUtils() {
      return dac.getUC().getIU();
   }

   /**
    * The 0-index key area
    * <br>
    * <br>
    *  
    * {@link IBOIndex#INDEX_OFFSET_04_AUX_BYTESIZE1}
    * @param keyNorm >= 0
    * @return 0 or more
    */
   protected int getKeyArea(int keyNorm) {
      return (keyNorm % areaNumber);
   }

   /**
    * Returns the chain maillon from the chaining {@link IByteCache}.
    * <br>
    * <br>
    * 
    * @param key Domain is [ChainedEmpty + 1, ...[
    * @return
    */
   protected byte[] getKeyChainedData(int key) {
      byte[] bytes = byteCacheChained.getBytes(key);
      return bytes;
   }

   /**
    * 
    */
   public int getKeyMax() {
      return keyMaxNorm + keyReference;
   }

   /**
    * The minimal key
    */
   public int getKeyMin() {
      return keyMinUser;
   }

   /**
    * Normalize key to take into account.
    * <br>
    * <li> header RID size
    * <li> reference key
    * <br>
    * <br>
    * A normalized key domain should be [0,...[
    * @param key
    * @return
    */
   protected int getKeyNormalized(int key) {
      key -= keyReference;
      key = key + refAreaShift;
      return key;
   }

   /**
    * Computes the RID adress for the normalized key.
    * <br>
    * <br>
    * @param normKey Normalized key i.e. [0,...[
    * @return value > 0 , value > headersize
    */
   protected int getKeyRID(int normKey) {
      return (normKey / areaNumber) + rsheadersize;
   }

   /**
    * Index {@link IBOIndex#INDEX_OFFSET_02_REFERENCE_KEY4}
    * <br>
    * <br>
    * 
    * @return
    */
   public int getReferenceKey() {
      return keyReference;
   }

   /**
    * Number of RIDs to create under the current reference key.
    * Taken into account the areas. There is a least a shift of 1 RID.
    * 2 rids occurs when new reference key and old refe key interval is bigger
    * than areaNumber.
    * .
    * When diffKeys modulo areaNumber is 0.
    * <br>
    * <br>
    * 999 and 1000 is 1 difference. With 
    * <br>
    * <br>
    * 
    * @param curRefKey
    * @param newRefKey
    * @return
    */
   private int getRidDiff(int curRefKey, int newRefKey) {
      int diffKeys = Math.abs(curRefKey - newRefKey);

      return IntUtils.divideCeil(diffKeys, areaNumber);
   }

   private int getRIDMin() {
      return minID + rsheadersize;
   }

   public ByteObject getTechIndex() {
      return indexHeader;
   }

   public void initChainCache(IByteStore storeManager) {
      String chainHandle = indexByteStoreHandle + "_chain";
      byteCacheChained = storeManager.getByteCache(chainHandle, null);
      if (byteCacheChained.getNumRecords() == 0) {
         //add header
         byteCacheChained.addBytes(new byte[] { 0, 0 });
         //null chain pointer
         byteCacheChained.addBytes(new byte[] { 0, 0 });
      }
      chainTailSecurityNum = byteCacheChained.getNumRecords();
      chainEmpty = minID;
      chainNull = minID + 1;
      chainByteSize = indexHeader.get1(INDEX_OFFSET_04_AUX_BYTESIZE1);
      useChainPatch = indexHeader.hasFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_5_CHAIN_PATCH);
   }

   /**
    * True if the DataKey is stored at that key 
    * <br>
    * <br>
    * @param key raw key the BID or Int_Date
    * @param bid if the BID is indexed at the key. i.e. BID has date given in key according to index
    * @return
    */
   public boolean isKeyIndexingBid(int key, int bid) {
      int area = getKeyArea(key);
      //the key becomes the rid
      int keyrid = getKeyRID(key);
      //gets the byte data 
      byte[] bytes = getIndexEntry(keyrid);
      if (bytes == null) {
         bipWrongKey(key);
         return false;
      }
      if (bytes.length == 0) {
         return false;
      }
      return byteIntegerArea.isContained(bytes, 0, area, bid);
   }

   /**
    * Are keys ordered when returned using {@link MIndexIntToInts#getBIDs(int)}
    * @return
    */
   public boolean isOrdered() {
      if (indexHeader.get1(INDEX_OFFSET_02_TYPE1) == TYPE_0_PATCH) {
         return true;
      } else {
         if (indexHeader.hasFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_5_CHAIN_PATCH)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Its primary role is to initialize the {@link IByteCache} used by the index.
    * <br>
    * <br>
    * It reads the {@link IBOCacheRMS} byteobject from the index header.
    * <br>
    * <br>
    * 
    * @param bootStrapHeader only used when index byte store is empty.
    */
   public void loadIndexHeader(ByteObject bootStrapHeader) {
      IByteStore storeManager = bs;
      int INDEX_HEADER_POSITION = bs.getBase();
      //loads the connector
      byte[] b = storeManager.getBytes(indexByteStoreHandle, INDEX_HEADER_POSITION);
      ByteObjectFactory byteObjectFactory = dac.getBOC().getByteObjectFactory();
      if (b == null || b.length == 0) {
         //first time loading. uses bootstrap settings?? 
         indexHeader = bootStrapHeader;
         storeManager.setBytesEnsure(indexByteStoreHandle, INDEX_HEADER_POSITION, bootStrapHeader.toByteArray());
         //some bootstrap values are decided here.
         //zero upon loading
         keyMaxNorm = 0;
      } else {
         indexHeader = byteObjectFactory.createByteObjectFromWrap(b, 0);
         keyMaxNorm = indexHeader.get4(INDEX_OFFSET_06_KEY_NORM_MAX4);
      }
      //check for shadowing
      boolean isShadowed = indexHeader.hasFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_6_SHADOW);
      if (isShadowed) {
         indexByteStoreHandle = indexByteStoreHandleBackUp + "0";
         //load the header from it... code if the header is empty due to corruption?
         byte[] headerShadowed = storeManager.getBytes(indexByteStoreHandle, INDEX_HEADER_POSITION);
         if (headerShadowed != null) {
            indexHeader = byteObjectFactory.createByteObjectFromWrap(headerShadowed, 0);
         }
      }
      ByteObject cacheTech = indexHeader.getSubFirst(IBOCacheRMS.OBJECT_TYPE);
      byteCache = storeManager.getByteCache(indexByteStoreHandle, cacheTech);
      keyReference = indexHeader.get4(INDEX_OFFSET_05_REFERENCE_KEY4);
      keyMinUser = keyReference;
      //check any mismatch
      field = indexHeader.getSubFirst(IBOTypesBOC.TYPE_010_POINTER);
      valueByteSize = indexHeader.get1(INDEX_OFFSET_03_VALUE_BYTESIZE1);
      areaNumber = indexHeader.get2(INDEX_OFFSET_08_NUM_AREAS2);
      refAreaShift = indexHeader.get2(INDEX_OFFSET_11_REF_AREA_SHIFT2);
      if (areaNumber == 0) {
         areaNumber = 1;
      }
      //int type = indexHeader.get1(INDEX_OFFSET_02_TYPE1);
      if (indexHeader.get1(INDEX_OFFSET_02_TYPE1) == IBOIndex.TYPE_1_CHAIN) {
         initChainCache(storeManager);
         areaNumber = 1;
      } else {
         ByteObject tech = indexHeader.getSubFirst(IBOAreaInt.AREA_TYPE);
         byteIntegerArea = new IntAreaDecoder(dac, tech);
      }
   }

   /**
    * Looks for bid value in the chain head, then go down the chain.
    * <br>
    * <br>
    *  
    * @param bid
    * @param area
    * @param head
    */
   protected void removeBidFromChain(int bid, int area, byte[] head, int rid) {
      int totalByteSize = valueByteSize + chainByteSize;
      int chainDataIndex = area * totalByteSize + valueByteSize;
      int chainValueIndex = area * totalByteSize;
      int chainData = ByteArrayStaticUtilz.getValue(head, chainDataIndex, chainByteSize);
      int chainValue = ByteArrayStaticUtilz.getValue(head, chainValueIndex, valueByteSize);
      byte[] keyData = null;
      int valueIndex = 0;
      int chainIndex = valueByteSize;
      //first case when head is removed
      if (chainValue == bid) {
         if (chainData != chainEmpty) {
            keyData = getKeyChainedData(chainData);
            chainValue = ByteArrayStaticUtilz.getValue(keyData, valueIndex, valueByteSize);
            chainData = ByteArrayStaticUtilz.getValue(keyData, chainIndex, chainByteSize);
            //move to head
            ByteArrayStaticUtilz.setValue(head, chainValueIndex, chainValue, valueByteSize);
            ByteArrayStaticUtilz.setValue(head, chainDataIndex, chainData, chainByteSize);
            //tag removed chainData so that it can be re-used
         } else {
            //sets the chain as null. that is with no value whatsoever.
            ByteArrayStaticUtilz.setValue(head, chainDataIndex, chainNull, chainByteSize);
         }
         byteCache.setBytes(rid, head);
         return;
      } else {
         //second case when first node in the tail is removed. i.e we are in the tail table
         byte[] firstAfterHead = getKeyChainedData(chainData);
         int prerid = chainData;
         chainData = ByteArrayStaticUtilz.getValue(firstAfterHead, chainIndex, chainByteSize);
         chainValue = ByteArrayStaticUtilz.getValue(firstAfterHead, valueIndex, valueByteSize);
         if (chainValue == bid) {
            //in the head, we have areas so we need the chainDataIndex with it.
            ByteArrayStaticUtilz.setValue(head, chainDataIndex, chainData, chainByteSize);
            byteCache.setBytes(rid, head);
            return;
         }
         //then removes inside the chain
         removeBidInsideChain(bid, prerid, firstAfterHead, chainData);
      }
   }

   protected void removeBidFromChainWithPatch(int bid, int area, byte[] d, int key) {
      int[] vales = getIntsChain(key, area, d);
      vales = getIntUtils().remove(vales, bid);
      int[] patched = getIntUtils().patch(vales, 0, vales.length, 255);
      setPatchedValues(area, d, patched);
   }

   /**
    * No Areas inside the chain. Only for chain heads.
    * <br>
    * <br>
    * 
    * @param bid
    * @param chainData
    * @param prerid RID at which prev was read
    * @param prev data of the first record in tail
    */
   protected void removeBidInsideChain(int bid, int prerid, byte[] prev, int prevChainData) {
      int chainValueIndex = 0;
      int chainDataIndex = valueByteSize;
      int chainValue = -1;
      byte[] keyData = null;
      int chainData = prevChainData;
      while (chainData != chainEmpty) {
         //record data for RID=chainData
         keyData = getKeyChainedData(chainData);
         prevChainData = chainData;
         chainData = ByteArrayStaticUtilz.getValue(keyData, chainDataIndex, chainByteSize);
         chainValue = ByteArrayStaticUtilz.getValue(keyData, chainValueIndex, valueByteSize);
         if (chainValue == bid) {
            //set the deleted chainData link to the previous byte record
            ByteArrayStaticUtilz.setValue(prev, chainDataIndex, chainData, chainByteSize);
            //save that previous byte record at its record id.
            byteCacheChained.setBytes(prerid, prev);
            return;
         }
         prev = keyData;
         prerid = prevChainData;
      }
   }

   /**
    * How do you remove the bid when it is the only value?
    * <br>
    * <br>
    * 
    * @param d
    * @param key
    * @param area
    * @param bid
    */
   private void removeIntChain(byte[] d, int key, int area, int bid) {
      int[] vales = getIntsChain(key, area, d);
      if (IntUtils.contains(vales, bid)) {
         //check if patching
         if (useChainPatch) {
            removeBidFromChainWithPatch(bid, area, d, key);
         } else {
            removeBidFromChain(bid, area, d, key);
         }
      }
   }

   public void removeValue(int key, int bid) {
      int normalizedKey = getKeyNormalized(key);
      int rid = getKeyRID(normalizedKey);
      int area = getKeyArea(normalizedKey);
      byte[] b = getIndexEntry(rid);
      if (b == null) {
         //#ifdef mordan.notprod
         bipWrongKey(normalizedKey);
         //#endif
      }
      if (byteCacheChained == null) {
         byte[] d = byteIntegerArea.removeInt(b, 0, area, bid);
         byteCache.setBytes(rid, d);
      } else {
         removeIntChain(b, rid, area, bid);
      }
   }

   protected void saveHeader() {
      byte[] headerData = indexHeader.toByteArray();
      byteCache.setBytes(minID, headerData);
   }

   /**
    * 
    * @param dos
    */
   public void serializeExport(BADataOS dos) {
      dos.writeChars(indexByteStoreHandle);
      indexHeader.serialize(dos);
      byteCache.serializeExport(dos);
      if (byteCacheChained != null) {
         dos.writeInt(1);
         byteCacheChained.serializeExport(dos);
      } else {
         dos.writeInt(0);
      }
   }

   public byte[] serializeExportPack() {
      BADataOS dos = dac.getUC().createNewBADataOS();
      serializeExport(dos);
      return dos.getByteCopy();
   }

   /**
    * Simply copies
    * @param dis
    */
   private void serializeImport(BADataIS dis) {
      IByteStore storeManager = bs;
      String value = dis.readString();
      ByteObjectFactory boFac = dac.getBOC().getByteObjectFactory();
      //now starts to read the imported data. header of the importing. if data is corrupted, it may bomb right here
      ByteObject indexHeader = boFac.serializeReverse(dis);
      //do a regular empty load on the new handle.

      //#debug
      toDLog().pInit("msg", indexHeader, MIndexIntToInts.class, "serializeImport", LVL_05_FINE, true);

      ByteObject cacheTech = indexHeader.getSubFirst(IBOCacheRMS.OBJECT_TYPE);
      //we have to delete the old store to make sure we are in a blank state
      storeManager.deleteStore(indexByteStoreHandle);
      byteCache = storeManager.getByteCache(indexByteStoreHandle, cacheTech);
      //this writes the header
      //System.out.println("#BoIndexBase#serializeImportWrite " + byteCache.toString());
      byteCache.serializeImport(dis);

      //#debug
      toDLog().pInit("msg", byteCache, MIndexIntToInts.class, "serializeImport", LVL_05_FINE, true);

      int f = dis.readInt(); //header byte telling the byte length
      if (f != 0) {
         //create it anyways because we need the new shadowed store.
         String chainHandle = indexByteStoreHandle + "_chain";
         storeManager.deleteStore(chainHandle);
         byteCacheChained = storeManager.getByteCache(chainHandle, null);
         //import on the shadowed empty. not deleted?  TODO must be changed to new handle!
         System.out.println("#BoIndexBase#serializeImport Chain Cache \n\t" + byteCacheChained.toString());
         byteCacheChained.serializeImport(dis);
      }
      this.indexHeader = indexHeader;
   }

   /**
    * It uses byte store handle given in the constructor. The one read in the {@link BADataIS}
    * is ignored.
    * Delete the current stores without doing any shadowing and validation.
    * <br>
    * <br>
    * 
    */
   public void serializeImportAppend(BADataIS dis) {
      //
      boolean isShadow = indexHeader.hasFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_6_SHADOW);
      serializeImport(dis);
      //set the shadow state
      indexHeader.setFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_6_SHADOW, isShadow);
      saveHeader();
      loadIndexHeader(indexHeader);
   }

   /**
    * To be used when importing a stand alone structure.
    * <br>
    * <br>
    * Keeps the old data until the import procedure is finished in a shadow file.
    * <br>
    * <br>
    * Delete old store and create a shadow pointer to the shadow file.
    * <br>
    * <br>
    * If the old data is in a shadow file, unshadows the structure. The data is now
    * in the file.
    * <br>
    * <br>
    * 
    * @param dis
    */
   public boolean serializeImportWrite(BADataIS dis) {
      //read the shadow pointer header anyways
      ByteObject oldHeader = indexHeader;
      String oldHandle = indexByteStoreHandle;
      try {
         boolean isNewShadow = false; //flag telling where the new imported data should be
         //check if empty
         //reads current shadow state
         boolean isShadowed = indexHeader.hasFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_6_SHADOW);
         if (!isShadowed) {
            isNewShadow = true;
         }
         //update store handle
         if (isNewShadow) {
            indexByteStoreHandle = indexByteStoreHandleBackUp + "0";
         } else {
            //make sure the handle is not shadowed
            indexByteStoreHandle = indexByteStoreHandleBackUp;
         }
         serializeImport(dis);
         //set the header shadow flag
         //delete the old byte stores
         bs.deleteStore(oldHandle);
         if (indexHeader.get1(INDEX_OFFSET_02_TYPE1) == IBOIndex.TYPE_1_CHAIN) {
            String chainHandle = oldHandle + "_chain";
            bs.deleteStore(chainHandle);
         }
         //create the shadow pointer file
         indexHeader.setFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_6_SHADOW, isNewShadow);
         if (isNewShadow) {
            IByteStore storeManager = bs;
            storeManager.addBytes(oldHandle, indexHeader.toByteArray());
         }
         saveHeader();
         loadIndexHeader(indexHeader);
         return true;
      } catch (Exception e) {
         //e.printStackTrace();
         //#debug
         System.out.println("#BoIndexBase#serializeImportWrite ERROR");
         //if any exception, roll back to previous
         indexHeader = oldHeader;
         indexByteStoreHandle = oldHandle;
         if (oldHeader != null) {
            loadIndexHeader(oldHeader);
         }
         return false;
      }
   }

   /**
    * 
    * @param dis
    */
   public void serializeReverseImport(BADataIS dis) {
      ByteObjectFactory boFac = dac.getBOC().getByteObjectFactory();
      ByteObject indexHeader = boFac.serializeReverse(dis);
      //create new byteCache even if the one is not null,because cache settings might be different.
      if (byteCache == null) {
         ByteObject cacheTech = indexHeader.getSubFirst(IBOCacheRMS.OBJECT_TYPE);
         byteCache = bs.getByteCache(indexByteStoreHandle, cacheTech);
      }
      byteCache.serializeImport(dis);
      int f = dis.readByte(); //header byte telling the byte length
      if (f != 0) {
         if (byteCacheChained != null) {
            bs.deleteStore(indexByteStoreHandle);
         } else {
            //create it

         }
         byteCacheChained.serializeImport(dis);
      }
   }

   /**
    * Sets the {@link ByteObject} that describes the key relative to a Business object.
    * <br>
    * <br>
    * When the key is a store id in a buy business object, the field describes the store.
    * <br>
    * <br>
    * @param field
    */
   public void setField(ByteObject field) {
      this.field = field;
   }

   /**
    * 
    * @param key
    * @param data
    */
   protected void setKeyChainedData(int key, byte[] data) {
      byteCacheChained.setBytes(key, data);
   }

   /**
    * 
    * @param area
    * @param d
    * @param patched
    */
   protected void setPatchedValues(int area, byte[] d, int[] patched) {
      //init the loop with first
      int totalByteSize = valueByteSize + chainByteSize;
      int chainDataIndex = area * totalByteSize + valueByteSize;
      int valueDataIndex = area * totalByteSize;
      int chainData = ByteArrayStaticUtilz.getValue(d, chainDataIndex, chainByteSize);
      ByteArrayStaticUtilz.setValue(d, valueDataIndex, patched[0], 1);
      ByteArrayStaticUtilz.setValue(d, valueDataIndex + 1, patched[1], valueByteSize - 1);

      for (int i = 2; i < patched.length; i += 2) {
         int patch = patched[i];
         int val = patched[i + 1];
         d = getKeyChainedData(chainData);
         chainDataIndex = valueByteSize;
         valueDataIndex = 0;
         chainData = ByteArrayStaticUtilz.getValue(d, chainDataIndex + valueByteSize, chainByteSize);
         ByteArrayStaticUtilz.setValue(d, valueDataIndex, patch, 1);
         ByteArrayStaticUtilz.setValue(d, valueDataIndex + 1, val, valueByteSize - 1);
      }
      //set the last chain pointer to 0
      ByteArrayStaticUtilz.setValue(d, chainDataIndex, 0, chainByteSize);

   }

   /**
    * The reference key is often arbitrary? 
    * Compute the number of areas and do a simple shift based on the old areas.
    * 2/08/2014 This code breaks unit tests
    * @param prog
    * @param ridMin
    * @param ridMax
    * @param ridDiff
    * @param numAreaSecond
    * @param numAreaFirst
    */
   protected void setRefAreaShifting(IBProgessable prog, int ridMin, int ridMax, int ridDiff, int numAreaSecond, int numAreaFirst, byte[] data) {
      refAreaShift = numAreaFirst;
      int newRidDiff = numAreaSecond + 1;
      setRefBasicShifting(prog, ridMin, ridMax, ridDiff, data);
   }

   protected void setRefAreaShifting2(IBProgessable prog, int ridMin, int ridMax, int ridDiff, int numAreaSecond, int numAreaFirst, byte[] data) {
      int ridDiffFirst = ridDiff - 1; //rid difference relative to origina array RID
      int ridDiffSecond = ridDiff;
      //do not forget area are 0-indexed
      int areaDiffFirst = numAreaSecond; //the area difference for part one
      int areaDiffSecond = numAreaFirst;
      //System.out.println("numAreaFirst=" + numAreaFirst + " numAreaSecond=" + numAreaSecond);
      //System.out.println("ridDiffFirst=" + ridDiffFirst + " ridDiffSecond=" + ridDiffSecond + " areaDiffFirst=" + areaDiffFirst + " areaDiffSecond=" + areaDiffSecond);
      //go over all records
      for (int i = ridMax; i >= ridMin; i--) {
         //tricky because offset is depending. need a method to get the offset of the ith element
         byte[] main = byteCache.getBytes(i);
         //possible the same reference as main because cache returns a reference
         byte[] destFirst = byteCache.getBytes(i + ridDiffFirst);
         //destination for second part
         byte[] destSecond = byteCache.getBytes(i + ridDiffSecond);
         if (destSecond == null) {
            System.out.println(this.toString());
         }

         //#debug
         String msg = i + " = [" + main.length + "] First = " + (i + ridDiffFirst) + " [" + destFirst.length + "] Second=" + (i + ridDiffSecond) + " [" + destSecond.length + "]";
         //#debug
         toDLog().pFlow(msg, this, MIndexIntToInts.class, "setRefAreaShifting2", LVL_05_FINE, true);

         if (byteCacheChained == null) {
            //move the second part first
            for (int j = numAreaFirst; j < areaNumber; j++) {
               destSecond = byteIntegerArea.clearInt(destSecond, 0, j - areaDiffSecond);
               //System.out.println(((IntAreaDecoder) byteIntegerArea).toString("\n", destSecond, 0));
               destSecond = copyAreaToArray(byteIntegerArea, main, 0, j, destSecond, 0, j - areaDiffSecond);
               //System.out.println(((IntAreaDecoder) byteIntegerArea).toString("\n", destSecond, 0));
               //clear
               //main = byteIntegerArea.clearInt(main, 0, j);
            }

            for (int j = numAreaFirst - 1; j >= 0; j--) {
               destFirst = byteIntegerArea.clearInt(destFirst, 0, j + areaDiffFirst);
               destFirst = copyAreaToArray(byteIntegerArea, main, 0, j, destFirst, 0, j + areaDiffFirst);
               //System.out.println(((IntAreaDecoder) byteIntegerArea).toString("\n", destFirst, 0));
               //main = byteIntegerArea.clearInt(main, 0, j);
            }

            //System.out.println("Current RID = " + i);
            //System.out.println("\tFirst = " + ((IntAreaDecoder) byteIntegerArea).toString("\n\t\t", destFirst, 0));
            //System.out.println("\tSecond= " + ((IntAreaDecoder) byteIntegerArea).toString("\n\t\t", destSecond, 0));

            if (i <= ridMin + ridDiffFirst) {
               //we only need to delete in the last pass. first and main might be identical RIDs
               if (ridDiffFirst == 0) {
                  for (int j = 0; j < numAreaSecond; j++) {
                     destFirst = byteIntegerArea.clearInt(destFirst, 0, j);
                  }
               } else {
                  for (int j = 0; j < numAreaSecond; j++) {
                     main = byteIntegerArea.clearInt(main, 0, j);
                  }
                  byteCache.setBytes(i, main);
               }

            }
         } else {
            int len = (valueByteSize + chainByteSize);

            int offsetFirst = 0;
            int lenFirst = numAreaFirst * len;
            int destOffsetFirst = numAreaSecond * len;

            System.arraycopy(main, offsetFirst, destFirst, destOffsetFirst, lenFirst);

            int offsetSecond = numAreaFirst * len;
            int lenSecond = numAreaSecond * len;
            int destOffsetSecond = 0;

            System.arraycopy(main, offsetSecond, destSecond, destOffsetSecond, lenSecond);

            //erase data ? set the null value.
            if (i <= ridMin + ridDiffFirst) {
               if (ridDiffFirst == 0) {
                  int index = valueByteSize;
                  for (int j = 0; j < numAreaSecond; j++) {
                     ByteArrayStaticUtilz.setValue(destFirst, index, chainNull, chainByteSize);
                     index += (chainByteSize + valueByteSize);
                  }
               } else {
                  //we only need to delete in the last passes where old data is still there
                  int index = valueByteSize;
                  for (int j = 0; j < numAreaSecond; j++) {
                     ByteArrayStaticUtilz.setValue(main, index, chainNull, chainByteSize);
                     index += (chainByteSize + valueByteSize);
                  }
                  byteCache.setBytes(i, main);
               }
            }

         }
         byteCache.setBytes(i + ridDiffFirst, destFirst);
         byteCache.setBytes(i + ridDiffSecond, destSecond);

         //erase the old ?
         //byteCache.setBytes(i, data);

      }
   }

   protected void setRefBasicShifting(IBProgessable prog, int ridMin, int ridMax, int ridDiff, byte[] data) {
      //now starting from last shift diff
      for (int i = ridMax; i >= ridMin; i--) {
         byte[] td = byteCache.getBytes(i);
         byteCache.setBytes(i + ridDiff, td);

         //erase the old
         if ((i - ridMin) <= ridDiff) {
            byteCache.setBytes(i, data);
         }
      }
   }

   /**
    * Potentially timing consuming method. 
    * <br>
    * <br>
    * Heavy Method
    * Shift Up or Down the whole index so ReferenceDate is at record 1
    * Up if the new reference date is after than the old one on a timeline
    * records below new reference date are lost
    * <br>
    * <br>
    * The most used case is:
    * Down if the new reference date is before the old one on a timeline
    * The method 
    * if factored
    * 
    * It has to be threaded externally.
    * <br>
    * <br>
    * When there is enough memory, Changing key reference is much more efficient by creating a new index file.
    * Shadowing
    * <br>
    * <br>
    * The shifting method works without much memory.
    * <br>
    * <br>
    * 
    * @param newRefKey key value not normalized
    */
   protected synchronized void setReferenceKey(int newRefKey) {
      //IBProgessable prog = ui.getProgress();
      IBProgessable prog = null;
      // rid for new root day
      // diff = # days to shift down
      //match so that the reference starts at first value in area. there will be a small mismatch between min key and reference key
      int diffKeys = keyReference - newRefKey;
      int ridMin = getRIDMin(); //first RID where there is index data
      int ridMax = getKeyRID(keyMaxNorm); //last rid where there is index data
      //how do you deal with areas? TODO OOOOOOOOOOOOOOOOOOo
      int ridDiff = getRidDiff(keyReference, newRefKey);
      //System.out.println("ridDiff=" + ridDiff);
      int newRidMax = getKeyRID(keyMaxNorm + diffKeys);
      //last used rid
      // copy newrootrid to _rootDateIndex + 1
      // all subsequent entries until the end
      // -1 because if 10-15 = 5, but 15 is included
      //prog.setMaxValue(newRidMax);
      //first create the byte array above
      //start a transaction. everything will be done in RAM/or shadow byte store until the commit is done.
      byteCache.connect();
      byteCache.transactionStart();
      byte[] data = null;
      if (byteCacheChained != null) {
         data = getEmptyChainHead();
      } else {
         data = byteIntegerArea.createLine();
      }
      //create a number of new entries.
      for (int i = 0; i < ridDiff; i++) {
         byteCache.addBytes(data);
      }
      if (areaNumber == 1) {
         setRefBasicShifting(prog, ridMin, ridMax, ridDiff, data);
      } else {
         //first check if the new key falls into the areashift.
         if (diffKeys < refAreaShift) {
            //no need to shift. just adjust the area shift
            int newRefAreaShift = refAreaShift - diffKeys;
            refAreaShift = newRefAreaShift;
         } else {
            //reduce the diffkey with the areaShift
            int newDiffKeys = diffKeys - refAreaShift;
            int numAreaSecond = newDiffKeys % areaNumber;
            int numAreaFirst = (areaNumber - numAreaSecond);
            if (numAreaFirst == 0) {
               setRefBasicShifting(prog, ridMin, ridMax, ridDiff, data);
            } else {
               setRefAreaShifting(prog, ridMin, ridMax, ridDiff, numAreaSecond, numAreaFirst, data);
            }
         }
      }

      //the transaction closing can take some time as well and will create a sub task in current task.
      byteCache.transactionCommit();
      byteCache.disconnect();
      //close the user feedback
      //prog.close();
      //TODO the reference key can be skewed because
      keyReference = newRefKey;
      keyMinUser = keyReference;
      keyMaxNorm += diffKeys;
      indexHeader.set2(INDEX_OFFSET_11_REF_AREA_SHIFT2, refAreaShift);
      indexHeader.set4(INDEX_OFFSET_05_REFERENCE_KEY4, keyReference);
      indexHeader.set4(INDEX_OFFSET_06_KEY_NORM_MAX4, keyMaxNorm);
      saveHeader();
   }

   //#mdebug
   public void toString(Dctx dc) {
      dc.root(this, MIndexIntToInts.class, 1719);
      toStringPrivate(dc);
      super.toString(dc.sup());
   }

   public void toString1Line(Dctx dc) {
      dc.root1Line(this, MIndexIntToInts.class, 1719);
      toStringPrivate(dc);
      super.toString1Line(dc.sup1Line());
      
      if (dc.hasFlagToStringUC(IToStringFlagsUC.FLAG_UC_11_SERIALIZE_DETAILS)) {
         dc.append(indexByteStoreHandle);
      }
      dc.nlLvl(indexHeader);
      dc.nl();
      dc.append("keyMaxNorm=" + keyMaxNorm);
      dc.append(" keyMinNorm=" + keyMinNorm);
      dc.append(" keyReference=" + keyReference);
      dc.append(" keyMinUser=" + keyMinUser);
      dc.append(" keyDeltaReject " + indexHeader.get2(INDEX_OFFSET_07_KEY_REJECT_2));
      dc.nl();
      dc.append("areaNumber=" + areaNumber);
      dc.append(" refAreaShift=" + refAreaShift);
      dc.append(" entryheadersize=" + entryheadersize);
      dc.append(" rsheadersize=" + rsheadersize);
      dc.append(" valueByteSize=" + valueByteSize + " chainByteSize =" + chainByteSize);

      toStringData(dc.newLevel());

      dc.nlLvlIgnoreNull("Field Indexed", field);
      dc.nlLvl(byteCache);

      dc.nlLvlIgnoreNull("", byteCacheChained);
   }

   private void toStringPrivate(Dctx dc) {
      
   }
   public String toStringData() {
      Dctx dc = new Dctx(toStringGetUCtx());
      toStringData(dc);
      return dc.toString();
   }
      
   public void toStringData(Dctx dc) {
      dc.append("#MIndexIntToInt Data");
      dc.append("[" + getKeyMin() + " to " + getKeyMax() + "]");
      //issue when it is empty keyMin == keyMax why?
      for (int i = getKeyMin(); i <= getKeyMax(); i++) {
         dc.nl();
         try {
            int[] vals = getInts(i);
            dc.append(i + " = ");
            dc.debugAlone(vals, ",");
         } catch (Exception e) {
            dc.append(i + " " + e.getClass().getName() + " " + e.getMessage());
         }
      }
   }
   //#enddebug

}
