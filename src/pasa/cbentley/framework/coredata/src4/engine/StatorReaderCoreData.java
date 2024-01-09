package pasa.cbentley.framework.coredata.src4.engine;

import pasa.cbentley.byteobjects.src4.interfaces.ITechStateBO;
import pasa.cbentley.byteobjects.src4.interfaces.StatorReaderBO;
import pasa.cbentley.core.src4.ctx.CtxManager;
import pasa.cbentley.core.src4.ctx.UCtx;
import pasa.cbentley.core.src4.io.BAByteIS;
import pasa.cbentley.core.src4.io.BADataIS;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.core.src4.stator.StatorReader;
import pasa.cbentley.framework.coredata.src4.ctx.CoreDataCtx;
import pasa.cbentley.framework.coredata.src4.db.IByteRecordStoreFactory;
import pasa.cbentley.framework.coredata.src4.ex.StoreException;
import pasa.cbentley.framework.coredata.src4.ex.StoreInvalidIDException;
import pasa.cbentley.framework.coredata.src4.ex.StoreNotFoundException;
import pasa.cbentley.framework.coredata.src4.ex.StoreNotOpenException;
import pasa.cbentley.framework.coredata.src4.interfaces.IRecordStore;

/**
 * 
 * @author Charles Bentley
 *
 */
public class StatorReaderCoreData extends StatorReaderBO {
   
   protected final CoreDataCtx  cdc;

   private String               storeName;

   private StatorReaderCoreData readerContext;

   private StatorReaderCoreData readerView;

   public StatorReaderCoreData(CoreDataCtx cdc, String storeName) {
      this(cdc, storeName, null);
   }

   public StatorReaderCoreData(CoreDataCtx cdc, String storeName, StatorReaderCoreData parent) {
      super(cdc.getBOC());
      this.cdc = cdc;
      this.storeName = storeName;
      this.parent = parent;
   }

   public StatorReaderBO getStateReader(int type) {
      if (type == ITechStateBO.TYPE_3_CTX) {
         if (readerContext == null) {
            readerContext = new StatorReaderCoreData(cdc, storeName, this);
            readerContext.loadDataFromID(TYPE_3_CTX);
         }
         return readerContext;
      } else if (type == TYPE_1_VIEW) {
         if (readerView == null) {
            readerView = new StatorReaderCoreData(cdc, storeName, this);
            readerView.loadDataFromID(TYPE_1_VIEW);
         }
         return readerView;
      }
      return this;
   }

   public void checkConfigErase() throws StoreException, StoreNotFoundException, StoreNotOpenException, StoreInvalidIDException {
      IByteRecordStoreFactory fac = cdc.getByteRecordStoreFactory();
      if (uc.getConfigU().isEraseSettingsAll()) {
         //#debug
         toDLog().pInit("Erasing settings bytestore because of configuration EraseSettingsAll flag", this, ConfigManager.class, "settingsRead", LVL_05_FINE, true);
         //delete before reading is equal to erase
         fac.deleteRecordStore(storeName);
      }
   }

   protected void loadDataFromID(int id) {
      IByteRecordStoreFactory fac = cdc.getByteRecordStoreFactory();
      IRecordStore rs = fac.openRecordStore(storeName, true);
      byte[] data = null;
      try {
         int base = rs.getBase();
         data = rs.getRecord(base + id);
      } catch (StoreInvalidIDException e) {
         //no data for it. not a big deal
      } finally {
         rs.closeRecordStore();
      }
      this.data = data; //set null if none
   }

   /**
    * Loads the code context settings into the {@link CtxManager} .
    * 
    * @throws StoreException
    * @throws StoreNotFoundException
    * @throws StoreNotOpenException
    * @throws StoreInvalidIDException
    */
   public void settingsRead() throws StoreException, StoreNotFoundException, StoreNotOpenException, StoreInvalidIDException {
      IByteRecordStoreFactory fac = cdc.getByteRecordStoreFactory();
      IRecordStore rs = fac.openRecordStore(storeName, true);
      byte[] data = null;
      try {
         int base = rs.getBase();
         data = rs.getRecord(base);
      } catch (StoreInvalidIDException e) {
      } finally {
         rs.closeRecordStore();
      }
      this.data = data; //set null if none
   }

   //#mdebug
   public void toString(Dctx dc) {
      dc.root(this, StatorReaderCoreData.class, "@line105");
      toStringPrivate(dc);
      super.toString(dc.sup());
   }

   private void toStringPrivate(Dctx dc) {
      dc.appendVarWithSpace("storeName", storeName);
   }

   public void toString1Line(Dctx dc) {
      dc.root1Line(this, StatorReaderCoreData.class);
      toStringPrivate(dc);
      super.toString1Line(dc.sup1Line());
   }

   //#enddebug

}
