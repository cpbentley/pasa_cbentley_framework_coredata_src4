package pasa.cbentley.framework.coredata.src4.engine;

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

public class ConfigManager implements IStringable {

   protected final CoreDataCtx cdc;

   protected final UCtx        uc;

   private String              storeName;

   public ConfigManager(CoreDataCtx cdc, String storeName) {
      this.storeName = storeName;
      this.uc = cdc.getUCtx();
      this.cdc = cdc;

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
      CtxManager ctxManager = uc.getCtxManager();
      if (uc.getConfigU().isEraseSettingsAll()) {
         
         //#debug
         toDLog().pInit("Erasing settings bytestore because of configuration EraseSettingsAll flag", this, ConfigManager.class, "settingsRead", LVL_05_FINE, true);
         
         //delete before reading is equal to erase
         fac.deleteRecordStore(storeName);
      }
      IRecordStore rs = fac.openRecordStore(storeName, true);
      try {
         int base = rs.getBase();
         byte[] data = rs.getRecord(base);
         if (data != null) {
            BAByteIS bis = new BAByteIS(uc, data);
            BADataIS dis = new BADataIS(uc, bis);
            ctxManager.stateRead(dis);

            //#debug
            toDLog().pBridge1("Reading " + data.length + " bytes from " + storeName, ctxManager, CoreDataCtx.class, "loadModuleSettings");
         }
      } catch (StoreInvalidIDException e) {
         //catch invalid record
         if (rs != null)
            try {
               byte[] data = getModuleBytes(ctxManager);
               rs.addRecord(data, 0, data.length);
            } catch (Exception e1) {
               e1.printStackTrace();
            }
      } finally {
         rs.closeRecordStore();
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
         toDLog().pBridge("Writing " + data.length + " bytes to " + storeName, null, ConfigManager.class, "writeModuleSettings");

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
