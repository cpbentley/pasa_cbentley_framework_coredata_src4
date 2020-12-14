package pasa.cbentley.framework.coredata.src4.engine;

import pasa.cbentley.byteobjects.src4.interfaces.ITechStateBO;
import pasa.cbentley.byteobjects.src4.interfaces.StatorWriterBO;
import pasa.cbentley.core.src4.ctx.CtxManager;
import pasa.cbentley.core.src4.ctx.UCtx;
import pasa.cbentley.core.src4.io.BAByteIS;
import pasa.cbentley.core.src4.io.BAByteOS;
import pasa.cbentley.core.src4.io.BADataIS;
import pasa.cbentley.core.src4.io.BADataOS;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.core.src4.logging.IDLog;
import pasa.cbentley.core.src4.logging.IStringable;
import pasa.cbentley.framework.coredata.src4.ctx.CoreDataCtx;
import pasa.cbentley.framework.coredata.src4.db.IByteRecordStoreFactory;
import pasa.cbentley.framework.coredata.src4.ex.StoreException;
import pasa.cbentley.framework.coredata.src4.ex.StoreInvalidIDException;
import pasa.cbentley.framework.coredata.src4.ex.StoreNotFoundException;
import pasa.cbentley.framework.coredata.src4.ex.StoreNotOpenException;
import pasa.cbentley.framework.coredata.src4.interfaces.IRecordStore;

public class StatorWriterCoreData extends StatorWriterBO {

   protected final CoreDataCtx  cdc;

   private String               storeName;

   private StatorWriterCoreData readerContext;

   private StatorWriterCoreData readerView;

   private StatorWriterCoreData writerModel;

   public StatorWriterCoreData(CoreDataCtx cdc, String storeName) {
      super(cdc.getBOC());
      this.storeName = storeName;
      this.cdc = cdc;

   }

   public StatorWriterCoreData(CoreDataCtx cdc, String storeName, StatorWriterCoreData parent) {
      super(cdc.getBOC());
      this.storeName = storeName;
      this.cdc = cdc;
      this.parent = parent;
   }

   public StatorWriterBO getStateWriter(int type) {
      if (type == ITechStateBO.TYPE_3_CTX) {
         if (readerContext == null) {
            readerContext = new StatorWriterCoreData(cdc, storeName, this);
         }
         return readerContext;
      } else if (type == TYPE_1_VIEW) {
         if (readerView == null) {
            readerView = new StatorWriterCoreData(cdc, storeName, this);
         }
         return readerView;
      }
      return this;
   }

   public void serializeToStore() {
      IByteRecordStoreFactory fac = cdc.getByteRecordStoreFactory();
      IRecordStore rs = fac.openRecordStore(storeName, true);
      int base = rs.getBase();
      byte[] dataMain = serialize();
      cdc.getByteStore().setBytesEnsure(storeName, base, dataMain);
      if (readerContext != null) {
         byte[] data = readerContext.getDataWriter().getByteCopy();
         cdc.getByteStore().setBytesEnsure(storeName, base + ITechStateBO.TYPE_3_CTX, data);
      }
      if (readerView != null) {
         byte[] data = readerView.getDataWriter().getByteCopy();
         cdc.getByteStore().setBytesEnsure(storeName, base + ITechStateBO.TYPE_1_VIEW, data);
      }
      if (writerModel != null) {
         byte[] data = writerModel.getDataWriter().getByteCopy();
         cdc.getByteStore().setBytesEnsure(storeName, base + ITechStateBO.TYPE_2_MODEL, data);
      }
   }

   private byte[] getModuleBytes(CtxManager ob) {
      BAByteOS bos = new BAByteOS(uc);
      BADataOS dos = new BADataOS(uc, bos);

      //#debug
      toDLog().pBridge("Writing Modules Data", ob, CoreDataCtx.class, "getModuleBytes");

      ob.stateWrite(dos);
      byte[] data = bos.toByteArray();
      return data;
   }

   /**
    * Save the tech objects of all registered modules
    * @param fac
    * @throws StoreNotFoundException
    * @throws StoreException
    */
   public void settingsWrite() throws StoreNotFoundException, StoreException {
      CtxManager ctxManager = uc.getCtxManager();
      IByteRecordStoreFactory fac = cdc.getByteRecordStoreFactory();
      IRecordStore rs = fac.openRecordStore(storeName, true);
      try {
         byte[] data = getModuleBytes(ctxManager);

         //#debug
         toDLog().pBridge("Writing " + data.length + " bytes to " + storeName, null, StatorWriterCoreData.class, "writeModuleSettings");

         int base = rs.getBase();
         rs.setRecord(base, data, 0, data.length);

      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         rs.closeRecordStore();
      }
   }

   //#mdebug
   public IDLog toDLog() {
      return toStringGetUCtx().toDLog();
   }

   public String toString() {
      return Dctx.toString(this);
   }

   public void toString(Dctx dc) {
      dc.root(this, "ConfigManager");
      toStringPrivate(dc);
   }

   public String toString1Line() {
      return Dctx.toString1Line(this);
   }

   private void toStringPrivate(Dctx dc) {

   }

   public void toString1Line(Dctx dc) {
      dc.root1Line(this, "ConfigManager");
      toStringPrivate(dc);
   }

   public UCtx toStringGetUCtx() {
      return uc;
   }

   //#enddebug

}
