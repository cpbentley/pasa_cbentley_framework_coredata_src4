/*
 * (c) 2018-2020 Charles-Philip Bentley
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */
package pasa.cbentley.framework.coredata.src4.stator;

import pasa.cbentley.byteobjects.src4.stator.StatorBO;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.core.src4.logging.IStringable;
import pasa.cbentley.core.src4.stator.StatorReader;
import pasa.cbentley.core.src4.stator.StatorWriter;
import pasa.cbentley.framework.coredata.src4.ctx.CoreDataCtx;
import pasa.cbentley.framework.coredata.src4.db.IByteRecordStoreFactory;
import pasa.cbentley.framework.coredata.src4.ex.StoreException;
import pasa.cbentley.framework.coredata.src4.ex.StoreInvalidIDException;
import pasa.cbentley.framework.coredata.src4.ex.StoreNotFoundException;
import pasa.cbentley.framework.coredata.src4.ex.StoreNotOpenException;
import pasa.cbentley.framework.coredata.src4.interfaces.IRecordStore;

/**
 * Saves the states of the UI that the app is not aware of because part of the host implementation.
 * 
 * Screen config acts as a key.
 * 
 * Model data depends on the screen configuration.
 * 
 * User may decide to load a Model State from another configuration if the current one is not desirable
 * 
 * @author Charles Bentley
 *
 */
public class StatorCoreData extends StatorBO implements IStringable {

   private String            storeName;

   private final CoreDataCtx cdc;

   public StatorCoreData(CoreDataCtx cdc, String storeName) {
      super(cdc.getBOC());
      this.cdc = cdc;
      this.storeName = storeName;
   }

   protected StatorReader createReader(int type) {
      StatorReaderCoreData stateReader = new StatorReaderCoreData(cdc, this, type, storeName);
      return stateReader;
   }

   public void importFromStore() {
      IByteRecordStoreFactory fac = cdc.getByteRecordStoreFactory();
      IRecordStore rs = fac.openRecordStore(storeName, true);
      byte[] data = null;
      try {
         int base = rs.getBase();
         data = rs.getRecord(base);
      } catch (StoreInvalidIDException e) {
         //no data for it. not a big deal
      } finally {
         rs.closeRecordStore();
      }
      if (data != null) {
         this.importFrom(data);
      }
   }

   public void serializeToStore() {
      IByteRecordStoreFactory fac = cdc.getByteRecordStoreFactory();
      IRecordStore rs = fac.openRecordStore(storeName, true);
      try {
         int base = rs.getBase();
         byte[] dataMain = serializeAll();
         cdc.getByteStore().setBytesEnsure(storeName, base, dataMain);
      } catch (StoreInvalidIDException e) {
         //no data for it. not a big deal
      } finally {
         rs.closeRecordStore();
      }
   }

   /**
    * 
    * @return true if store was deleted
    * @throws StoreException
    * @throws StoreNotFoundException
    * @throws StoreNotOpenException
    * @throws StoreInvalidIDException
    */
   public boolean checkConfigErase() throws StoreException, StoreNotFoundException, StoreNotOpenException, StoreInvalidIDException {
      if (uc.getConfigU().isEraseSettingsAll()) {
         //#debug
         toDLog().pInit("Erasing settings bytestore because of configuration EraseSettingsAll flag", this, StatorCoreData.class, "settingsRead", LVL_05_FINE, true);
         deleteDataAll();
         return true;
      }
      return false;
   }

   public void deleteDataAll() {
      IByteRecordStoreFactory fac = cdc.getByteRecordStoreFactory();
      try {
         fac.deleteRecordStore(storeName);
      } catch (StoreNotFoundException e) {
         //silent on this one..
      }
   }

   public StatorWriter createWriter(int type) {
      StatorWriterCoreData stateWriter = new StatorWriterCoreData(cdc, this, type, storeName);
      return stateWriter;
   }

   //#mdebug
   public void toString(Dctx dc) {
      dc.root(this, StatorCoreData.class, 271);
      toStringPrivate(dc);
      super.toString(dc.sup());
   }

   private void toStringPrivate(Dctx dc) {

   }

   public void toString1Line(Dctx dc) {
      dc.root1Line(this, StatorCoreData.class);
      toStringPrivate(dc);
      super.toString1Line(dc.sup1Line());
   }

   //#enddebug

}
