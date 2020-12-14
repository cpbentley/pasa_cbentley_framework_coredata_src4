package pasa.cbentley.framework.coredata.src4.engine;

import pasa.cbentley.byteobjects.src4.core.ByteObject;
import pasa.cbentley.core.src4.ctx.UCtx;
import pasa.cbentley.core.src4.io.BAByteOS;
import pasa.cbentley.core.src4.io.BADataIS;
import pasa.cbentley.core.src4.io.BADataOS;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.core.src4.thread.IBProgessable;
import pasa.cbentley.framework.coredata.src4.ctx.CoreDataCtx;
import pasa.cbentley.framework.coredata.src4.db.IByteCache;
import pasa.cbentley.framework.coredata.src4.db.IByteInterpreter;
import pasa.cbentley.framework.coredata.src4.db.IByteStore;

/**
 * No caching. Just forward to {@link IByteStore}
 * <br>
 * <br>
 * @author Mordan
 *
 */
public class ByteCacheDummy implements IByteCache {

   private CoreDataCtx     rmc;

   private String     rs;

   private IByteStore store;

   public ByteCacheDummy(CoreDataCtx rmc, IByteStore bs, String rs) {
      this.rmc = rmc;
      if (rs == null) {
         throw new NullPointerException("Store name cannot be null");
      }
      this.rs = rs;
      store = bs;
   }

   public int addBytes(byte[] data) {
      return store.addBytes(rs, data);
   }

   public int addBytes(byte[] data, int offset, int len) {
      return store.addBytes(rs, data, offset, len);
   }

   public void commitJournal(BADataOS dos) {
      // TODO Auto-generated method stub

   }

   public void commitJournal(BAByteOS dos) {
      // TODO Auto-generated method stub

   }

   public void connect() {
      //no connection in dumy
   }

   public void deleteBytes(int id) {
      store.deleteRecord(rs, id);
   }

   public void deleteStore() {
      // TODO Auto-generated method stub

   }

   public void disconnect() {

   }

   public void ensureCapacity(int rid, int ensureSize, IBProgessable p) {
      store.ensureCapacity(rs, rid, ensureSize, p);
   }

   public int getBase() {
      return store.getBase();
   }

   public byte[] getBytes(int rid) {
      return store.getBytes(rs, rid);
   }

   public int getBytes(int rid, byte[] data, int offset) {
      return store.getBytes(rs, rid, data, offset);
   }

   public byte[] getBytesCheck(int rid) {
      return store.getBytesCheck(rs, rid, false);
   }

   public byte[] getBytesThrough(int rid) {
      return getBytes(rid);
   }

   public IByteCache getCacheMorph(ByteObject type) {
      return this;
   }

   public ByteObject getCacheSpec() {
      return null;
   }

   public long getLastModified() {
      return store.getLastModified(rs);
   }

   public int getNextID() {
      return store.getNextRecordId(rs);
   }

   public int getNumRecords() {
      return store.getNumRecords(rs);
   }

   public int getSize() {
      return store.getSize(rs);
   }

   public int getSizeAvailable() {
      return store.getSizeAvailable(rs);
   }

   public IByteStore getStore() {
      return store;
   }

   public int getVersion() {
      return store.getVersion(rs);
   }

   public void manageCache(int startID, int endID) {

   }

   public void serializeExport(BADataOS dos) {
      store.serializeExport(rs, dos);
   }

   public void serializeImport(BADataIS dis) {
      store.serializeImport(rs, dis);
   }

   public void setBytes(int id, byte[] b) {
      store.setBytes(rs, id, b);
   }

   public void setBytes(int id, byte[] b, int offset, int len) {
      store.setBytes(rs, id, b, offset, len);
   }

   public void setBytesEnsure(int id, byte[] b) {
      store.setBytesEnsure(rs, id, b);
   }

   public void setBytesEnsure(int id, byte[] b, int offset, int len) {
      store.setBytesEnsure(rs, id, b, offset, len);
   }

   public void setMode(int mode) {
      // TODO Auto-generated method stub

   }

   //#mdebug
   public String toString() {
      return Dctx.toString(this);
   }

   public void toString(Dctx sb) {
      sb.root(this, "ByteCacheDummy");
      sb.append(rs);
      store.toString(rs, sb.newLevel());
   }

   public String toString1Line() {
      return Dctx.toString1Line(this);
   }

   public void toString1Line(Dctx dc) {
      dc.root1Line(this, "ByteCacheDummy");
   }

   public UCtx toStringGetUCtx() {
      return rmc.getUCtx();
   }

   public String toStringStoreData(IByteInterpreter ib) {
      return store.toStringData(rs, ib);
   }

   public String toStringStoreHeader() {
      return store.toStringStoreHeader(rs);
   }
   //#enddebug

   public void transactionCancel() {

   }

   public void transactionCommit() {

   }

   public void transactionStart() {

   }
}
