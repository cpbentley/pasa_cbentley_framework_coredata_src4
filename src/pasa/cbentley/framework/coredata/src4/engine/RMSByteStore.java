package pasa.cbentley.framework.coredata.src4.engine;

import pasa.cbentley.byteobjects.src4.core.ByteObject;
import pasa.cbentley.byteobjects.src4.ctx.IToStringFlagsBO;
import pasa.cbentley.core.src4.ctx.IToStringFlags;
import pasa.cbentley.core.src4.ctx.UCtx;
import pasa.cbentley.core.src4.helpers.StringBBuilder;
import pasa.cbentley.core.src4.io.BADataIS;
import pasa.cbentley.core.src4.io.BADataOS;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.core.src4.logging.IDLog;
import pasa.cbentley.core.src4.logging.IStringable;
import pasa.cbentley.core.src4.thread.IBProgessable;
import pasa.cbentley.framework.coredata.src4.ctx.CoreDataCtx;
import pasa.cbentley.framework.coredata.src4.db.IByteCache;
import pasa.cbentley.framework.coredata.src4.db.IByteInterpreter;
import pasa.cbentley.framework.coredata.src4.db.IByteRecordStoreFactory;
import pasa.cbentley.framework.coredata.src4.db.IByteStore;
import pasa.cbentley.framework.coredata.src4.db.IBOCacheRMS;
import pasa.cbentley.framework.coredata.src4.ex.StoreException;
import pasa.cbentley.framework.coredata.src4.ex.StoreInvalidIDException;
import pasa.cbentley.framework.coredata.src4.ex.StoreNotFoundException;
import pasa.cbentley.framework.coredata.src4.interfaces.IRecordStore;

/**
 * Facade that implements {@link IByteStore}.
 * <br>
 * <br>
 * Used for single actions. Each action requires opening and closing the store, unless done on cache
 * <br>
 * Implements {@link IByteStore} using the {@link RMSUtils} and {@link IRecordStore} of MIDP 2.0
 * <br>
 * <br>
 * @author Mordan
 *
 */
public class RMSByteStore implements IByteStore, IStringable {

   private IByteRecordStoreFactory rc;

   private CoreDataCtx      rmc;

   public RMSByteStore(CoreDataCtx rmc) {
      this.rmc = rmc;
      this.rc = rmc.getByteRecordStoreFactory();
   }

   public int addBytes(String recordStore, byte[] data) {
      if (data == null) {
         return addBytes(recordStore, data, 0, 0);
      } else {
         return addBytes(recordStore, data, 0, data.length);
      }
   }

   public int addBytes(String recordStore, byte[] data, int offset, int len) {
      IRecordStore rs = null;
      try {
         rs = rc.openRecordStore(recordStore, true);
         //see API. byte array can be null.
         return rs.addRecord(data, offset, len);
      } catch (Exception e) {
         //#debug
         toDLog().pData("recordStore=" + recordStore, this, RMSByteStore.class, "addBytes", LVL_10_SEVERE, false, e);
      } finally {
         finalClose(rs);
      }
      return -1;
   }

   public void deleteAll() {
      String[] arrayRecordStore = rc.listRecordStores();
      if (arrayRecordStore != null) {
         for (int i = 0; i < arrayRecordStore.length; i++) {
            deleteStore(arrayRecordStore[i]);
         }
      }
   }

   public void deleteRecord(String recordStore, int rid) {
      IRecordStore rs = null;
      try {
         rs = rc.openRecordStore(recordStore, false);
         rs.deleteRecord(rid);
      } catch (StoreInvalidIDException e) {
         //#debug
         toDLog().pData("recordStore=" + recordStore + " Invalid rid=" + rid, this, RMSByteStore.class, "deleteRecord", LVL_10_SEVERE, true, e);
      } catch (Exception e) {
         //#debug
         toDLog().pData("recordStore=" + recordStore + " rid=" + rid, this, RMSByteStore.class, "deleteRecord", LVL_10_SEVERE, true, e);
      } finally {
         finalClose(rs);
      }
   }

   /**
    * if null, silently returns
    * 
    * @param storeName
    */
   public boolean deleteStore(String storeName) {
      if (storeName == null) {
         return true;
      }
      try {
         IRecordStore rs = rc.openRecordStore(storeName, false);
         rs.closeRecordStore();
         boolean cont = true;
         int count = 0;
         while (cont && count < 100) {
            try {
               count++;
               rc.deleteRecordStore(storeName);
               //#debug
               toDLog().pData(storeName + " deleted after " + count + " tries", this, RMSByteStore.class, "deleteStore", LVL_03_FINEST, true);
               return true;
            } catch (StoreException e) {
               rs.closeRecordStore();
               //#debug
               toDLog().pData("recordStore=" + storeName, this, RMSByteStore.class, "deleteStore", LVL_10_SEVERE, true, e);
            }
         }
         //#debug
         toDLog().pData("Could not close store. recordStore=" + storeName, this, RMSByteStore.class, "deleteStore", LVL_10_SEVERE, true);
         return false;
      } catch (StoreNotFoundException e) {
         //#debug
         toDLog().pData("recordStore=" + storeName + " does not exist. Could not be deleted", this, RMSByteStore.class, "deleteStore", LVL_05_FINE, true, e);
      } catch (StoreException e) {
         //#debug
         toDLog().pData("recordStore=" + storeName, this, RMSByteStore.class, "deleteStore", LVL_10_SEVERE, true, e);
      }
      return false;
   }

   /**
    * Ensures the capacity filling the blanks with a non null byte array of size
    * <br>
    * 
    * <br>
    * @param rs
    * @param rid
    * @param arraySize Size of filling array
    * @param p {@link IMProgessable} to 
    */
   private void ensureCapacity(IRecordStore rs, int rid, int arraySize, IBProgessable p, IByteRecordStoreFactory c) {
      try {
         int next = rs.getNextRecordID();
         if (next > rid) {
            return;
         } else {
            int nums = rid - next + 1;
            //#mdebug
            if (nums > 10) {
               // #debug
               toDLog().pData("Ensuring Capacity for more than 10 records : #\" + nums", this, RMSByteStore.class, "ensureCapacity", LVL_09_WARNING, true);
            }
            //#enddebug
            if (p != null) {
               p.setMaxValue(nums);
            }
            byte[] ar = new byte[arraySize];
            for (int i = 0; i < nums; i++) {
               rs.addRecord(ar, 0, ar.length);
               if (p != null) {
                  p.increment(1);
               }
            }
            if (p != null) {
               p.close(null);
            }
         }
      } catch (Exception e) {
         // #debug
         e.printStackTrace();
      }
   }

   /**
    * This is a Salt Factory, method can last a long time on a slow platform and that's bad for the user
    * interaction with the user interface opened RecordStore
    * <br>
    * <br>
    * 
    * @param rs
    * @param rid
    */
   private void ensureCapacity(IRecordStore rs, int rid, int arraySize, IByteRecordStoreFactory c) {
      ensureCapacity(rs, rid, arraySize, null, c);
   }

   private void ensureCapacity(IRecordStore rs, int size, IByteRecordStoreFactory c) {
      ensureCapacity(rs, size, 0, c);
   }

   public void ensureCapacity(IByteRecordStoreFactory rc, String rs, int rid, int arraySize) {
      ensureCapacity(rc, rs, rid, arraySize, null);
   }

   public void ensureCapacity(IByteRecordStoreFactory rc, String rsname, int rid, int arraySize, IBProgessable p) {
      IRecordStore rs = null;
      try {
         rs = rc.openRecordStore(rsname, true);
         ensureCapacity(rs, rid, arraySize, p, rc);
      } catch (Exception e) {
         // #debug
         e.printStackTrace();
      } finally {
         finalClose(rs);
      }
   }

   public void ensureCapacity(String rs, int rid) {
      ensureCapacity(rc, rs, rid, 0);
   }

   public void ensureCapacity(String rs, int rid, int size) {
      ensureCapacity(rc, rs, rid, size);
   }

   public void ensureCapacity(String rs, int rid, int size, IBProgessable p) {
      ensureCapacity(rc, rs, rid, size, p);
   }

   public void finalClose(IRecordStore rs) {
      try {
         if (rs != null) {
            rs.closeRecordStore();
         }
      } catch (StoreException e) {
         e.printStackTrace();
      }
   }

   /**
    * Not null even if no record stores
    */
   public String[] getAllStores() {
      String[] d = rc.listRecordStores();
      if (d == null) {
         d = new String[0];
      }
      return d;
   }

   //#mdebug
   public String[] getAllStoreSignatures() {
      String[] arrayRecordStore = getAllStores();
      for (int i = 0; i < arrayRecordStore.length; i++) {
         arrayRecordStore[i] = toStringStoreHeader(arrayRecordStore[i]);
      }
      return arrayRecordStore;
   }
   //#enddebug

   public int getBase() {
      return rc.getBase();
   }

   /**
    * 
    */
   public IByteCache getByteCache(String rs, ByteObject cacheParam) {
      if (cacheParam == null) {
         return new ByteCacheDummy(rmc, this, rs);
      } else {
         int type = cacheParam.get1(IBOCacheRMS.CACHE_OFFSET_06_TYPE1);
         return new ByteCacheFrame(rmc, rs, cacheParam);
      }
   }

   /**
    * 
    */
   public byte[] getBytes(String recordStore, int rid) {
      return getBytesCheck(recordStore, rid, true);
   }

   public int getBytes(String recordStore, int rid, byte[] data, int offset) {
      return getBytesPrivate(recordStore, rid, data, offset, false);
   }

   public byte[] getBytesCheck(String recordStore, int rid, boolean ensure) {
      return getBytesPrivate(recordStore, rid, ensure);
   }

   public int getBytesEnsure(String recordStore, int rid, byte[] data, int offset) {
      return getBytesPrivate(recordStore, rid, data, offset, true);
   }

   public int getByteSize(String recordStore, int rid) {
      IRecordStore rs = null;
      int size = -1;
      try {
         rs = rc.openRecordStore(recordStore, true);
         size = rs.getRecordSize(rid);
      } catch (Exception e) {
         //#debug
         toDLog().pData("recordStore=" + recordStore + " rid=" + rid, this, RMSByteStore.class, "getByteSize", LVL_10_SEVERE, true, e);
      } finally {
         finalClose(rs);
      }
      return size;
   }

   /**
    * 
    * See {@link IByteStore#getBytesCheck(String, int, boolean)}
    * <br>
    * <br>
    * Empty array is returned when {@link IRecordStore#getRecord(int)} returns null but key is valid
    * <br>
    * <br>
    * This class does not generate {@link Exception}. It returns null.
    * @param recordStore
    * @param rid
    * @return null if error or invalid key or null is stored
    */
   private byte[] getBytesPrivate(String recordStore, int rid, boolean ensure) {
      if (rid < rc.getBase()) {
         //#debug
         toDLog().pData("Bad rid=" + rid, this, RMSByteStore.class, "insideGetBytes", LVL_05_FINE, true);
         return null;
      }
      IRecordStore rs = null;
      try {
         rs = rc.openRecordStore(recordStore, true);
         if (ensure) {
            ensureCapacity(rs, rid, rc);
         } else {
            if (rid >= rs.getNextRecordID()) {
               return null;
            }
         }
         byte[] b = rs.getRecord(rid);
         return b;
      } catch (Exception e) {
         //#debug
         toDLog().pData("recordStore=" + recordStore + " rid=" + rid, this, RMSByteStore.class, "insideGetBytes", LVL_10_SEVERE, false, e);
      } finally {
         finalClose(rs);
      }
      return null;
   }

   private int getBytesPrivate(String recordStore, int rid, byte[] data, int offset, boolean ensure) {
      if (rid < rc.getBase()) {
         //#debug
         toDLog().pData("Bad rid=" + rid, this, RMSByteStore.class, "getBytesEnsure", LVL_05_FINE, true);
         return 0;
      }
      IRecordStore rs = null;
      try {
         rs = rc.openRecordStore(recordStore, true);
         if (ensure) {
            ensureCapacity(rs, rid, rc);
         }
         return rs.getRecord(rid, data, offset);
      } catch (Exception e) {
         //#debug
         toDLog().pData("recordStore=" + recordStore + " rid=" + rid, this, RMSByteStore.class, "getBytesEnsure", LVL_10_SEVERE, false, e);
      } finally {
         finalClose(rs);
      }
      return 0;
   }

   public long getLastModified(String recordStore) {
      IRecordStore rs = null;
      try {
         rs = rc.openRecordStore(recordStore, true);
         return rs.getLastModified();
      } catch (Exception e) {
         //#debug
         toDLog().pData("recordStore=" + recordStore, this, RMSByteStore.class, "getNextRecordId", LVL_10_SEVERE, true, e);
      } finally {
         finalClose(rs);
      }
      return -1;
   }

   /**
    * -1 if store is not 
    */
   public int getNextRecordId(String recordStore) {
      IRecordStore rs = null;
      try {
         rs = rc.openRecordStore(recordStore, false);
         int i = rs.getNextRecordID();
         return i;
      } catch (Exception e) {
         //#debug
         toDLog().pData("recordStore=" + recordStore, this, RMSByteStore.class, "getNextRecordId", LVL_10_SEVERE, true, e);
         return Integer.MIN_VALUE;
      } finally {
         finalClose(rs);
      }
   }

   public int getNumRecords(String recordStore) {
      IRecordStore rs = null;
      try {
         rs = rc.openRecordStore(recordStore, true);
         int i = rs.getNumRecords();
         return i;
      } catch (Exception e) {
         //#debug
         toDLog().pData("recordStore=" + recordStore, this, RMSByteStore.class, "getNumRecords", LVL_10_SEVERE, true, e);
         return -1;
      } finally {
         finalClose(rs);
      }
   }

   public IByteRecordStoreFactory getRMS() {
      return rc;
   }

   public int getSize(String recordStore) {
      IRecordStore rs = null;
      try {
         rs = rc.openRecordStore(recordStore, true);
         int i = rs.getSize();
         return i;
      } catch (Exception e) {
         //#debug
         toDLog().pData("recordStore=" + recordStore, this, RMSByteStore.class, "getSize", LVL_10_SEVERE, true, e);
         return -1;
      } finally {
         finalClose(rs);
      }
   }

   public int getSizeAvailable(String recordStore) {
      IRecordStore rs = null;
      try {
         rs = rc.openRecordStore(recordStore, true);
         int i = rs.getSizeAvailable();
         return i;
      } catch (Exception e) {
         //#debug
         toDLog().pData("recordStore=" + recordStore, this, RMSByteStore.class, "getSizeAvailable", LVL_10_SEVERE, true, e);
         return -1;
      } finally {
         finalClose(rs);
      }
   }

   public int getVersion(String recordStore) {
      IRecordStore rs = null;
      try {
         rs = rc.openRecordStore(recordStore, true);
         return rs.getVersion();
      } catch (Exception e) {
         // #debug
         e.printStackTrace();
      } finally {
         finalClose(rs);
      }
      return -1;
   }

   public boolean isCached(String rs) {
      return false;
   }

   public boolean isCachedStore(String rs, int type) {
      return false;
   }

   public boolean isUsed(String rs) {
      String[] s = getAllStores();
      for (int i = 0; i < s.length; i++) {
         if (s[i].equals(rs)) {
            return true;
         }
      }
      return false;
   }

   public void serializeExport(String rs, BADataOS dos) {
      int start = getNumRecords(rs);
      int end = getNextRecordId(rs);
      dos.writeInt(start);
      dos.writeInt(end);

      //write all records, flag deleted
      for (int i = 0; i < end; i++) {
         byte[] data = getBytes(rs, i);
         dos.writeByteArray(data); //writes 0 when data is null
      }
   }

   /**
    * What happens if the ID is invalid because it was deleted?
    * <br>
    * <br>
    * Import will generate an exception.
    * <br>
    * <br>
    * Should we delete the store and create it again? or should it be an additive import?
    * <br>
    * <br>
    *  
    * @param rs
    * @param dis
    */
   public void serializeImport(String rs, BADataIS dis) {
      int numRecords = dis.readInt();
      int nextRecords = dis.readInt();
      //#debug
      toDLog().pData("numRecords=" + numRecords + " nextRecords=" + nextRecords, this, RMSByteStore.class, "serializeImport", LVL_05_FINE, true);
      //create from scratch
      //check if there is data already in the store?
      //deleteStore(rs);
      for (int i = 0; i < nextRecords; i++) {
         byte[] data = dis.readByteArray();
         addBytes(rs, data);
      }
   }

   public void setBytes(String recordStore, int id, byte[] b) {
      setBytesEnsure(recordStore, id, b, 0, b.length, false);
   }

   public void setBytes(String recordStore, int id, byte[] b, int offset, int len) {
      setBytesEnsure(recordStore, id, b, 0, b.length, false);
   }

   public void setBytesEnsure(String rs, int id, byte[] b) {
      setBytesEnsure(rs, id, b, 0, b.length, true);
   }

   public void setBytesEnsure(String rs, int id, byte[] b, int offset, int len) {
      setBytesEnsure(rs, id, b, offset, len, true);
   }

   private void setBytesEnsure(String recordStore, int id, byte[] b, int offset, int len, boolean ensure) {
      IRecordStore rs = null;
      try {
         rs = rc.openRecordStore(recordStore, true);
         if (ensure) {
            ensureCapacity(rs, id, rc);
         }
         rs.setRecord(id, b, offset, len);
      } catch (Exception e) {
         //#debug
         toDLog().pData("recordStore=" + recordStore, this, RMSByteStore.class, "setBytesEnsure", LVL_10_SEVERE, true, e);
      } finally {
         finalClose(rs);
      }
   }

   //#mdebug
   public IDLog toDLog() {
      return rmc.toDLog();
   }

   public String toString() {
      return Dctx.toString(this);
   }

   public void toString(Dctx dc) {
      dc.root(this, "RMSByteStore");
      toStringPrivate(dc);
   }

   public String toString(String rs) {
      return Dctx.toString(this);
   }

   /**
    * 
    */
   public void toString(String rs, Dctx sb) {
      sb.append("#RMSByteStore");
      sb.tab();
      toStringStoreHeader(rs, sb.newLevelTab());
      toStringData(rs, sb.newLevelTab(), null);
   }

   public String toString1Line() {
      return Dctx.toString1Line(this);
   }

   public void toString1Line(Dctx dc) {
      dc.root1Line(this, "RMSByteStore");
      toStringPrivate(dc);
   }

   public void toStringData(String recordStore, Dctx nl, IByteInterpreter ib) {
      toStringStoreDataDebugString(this, recordStore, ib, nl);
   }

   public String toStringData(String recordStore, IByteInterpreter ib) {
      Dctx dc = new Dctx(rmc.getUC(), "\n\t");
      toStringData(recordStore, dc, ib);
      return dc.toString();
   }

   public UCtx toStringGetUCtx() {
      return rmc.getUC();
   }

   public String toStringOneLine(String rs) {
      StringBBuilder sb = new StringBBuilder(rmc.getUC());
      sb.append("#RMSByteStore " + rs);
      sb.append(toStringStoreOneLineDebugString(this, rs));
      return sb.toString();
   }

   private void toStringPrivate(Dctx dc) {

   }

   /**
    * All our record stores start with a header data in RID=1
    * <br>
    * <br>
    * 
    * @param store
    * @param rs
    * @param ib
    * @return
    */
   public void toStringStoreDataDebugString(IByteStore store, String rs, IByteInterpreter ib, Dctx sb) {
      sb.append("#Data ");
      if (rmc.getBOC().toStringHasToStringFlag(IToStringFlagsBO.TOSTRING_FLAG_1_SERIALIZE)) {
         sb.append(rs);
      }
      int num = store.getNextRecordId(rs);
      int numChars = String.valueOf(num).length();
      for (int j = 0; j < num; j++) {
         byte[] b = store.getBytes(rs, j);
         sb.nl();
         sb.append("RID " + rmc.getUC().getStrU().prettyInt0Padd(j, numChars));
         if (b != null) {
            if (rmc.getUC().toStringHasToStringFlag(IToStringFlags.FLAG_DATA_01_SUCCINT)) {
               sb.append(" = [" + b.length + "bytes]\t");
               String str = rmc.getUC().getIU().debugString(b, 0, Math.min(10, b.length), ",");
               sb.append(str);
            } else {
               if (ib != null) {
                  sb.append("\t" + ib.getDisplayString(b, 0, IByteInterpreter.OPTION_ALL));
               } else {
                  String str = rmc.getUC().getBU().debugString(b, ",");
                  sb.append(" = [" + b.length + "bytes]\t");
                  sb.append(str);
               }
            }
         } else {
            sb.append("Null Byte Array");
         }
      }
   }

   public String toStringStoreHeader(String recordStore) {
      Dctx dc = new Dctx(rmc.getUC(), "\n\t");
      toStringStoreHeader(recordStore, dc);
      return dc.toString();
   }

   public void toStringStoreHeader(String recordStore, Dctx nl) {
      rmc.toStringStoreHeader(this, recordStore, nl);
   }

   public String toStringStoreOneLineDebugString(IByteStore store, String rs) {
      StringBBuilder sb = new StringBBuilder(rmc.getUC());
      int nums = store.getNumRecords(rs);
      int next = store.getNextRecordId(rs);
      int size = store.getSize(rs);
      sb.append(" NumRecords=" + nums);
      sb.append(" NextID=" + next);
      sb.append(" Size=" + size);
      return sb.toString();
   }
   //#enddebug

}
