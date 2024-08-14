package pasa.cbentley.framework.core.data.src4.stator;

import pasa.cbentley.byteobjects.src4.stator.ITechStatorBO;
import pasa.cbentley.byteobjects.src4.stator.StatorBO;
import pasa.cbentley.byteobjects.src4.stator.StatorReaderBO;
import pasa.cbentley.core.src4.ctx.CtxManager;
import pasa.cbentley.core.src4.io.BAByteIS;
import pasa.cbentley.core.src4.io.BADataIS;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.framework.core.data.src4.ctx.CoreDataCtx;
import pasa.cbentley.framework.core.data.src4.db.IByteRecordStoreFactory;
import pasa.cbentley.framework.core.data.src4.ex.StoreException;
import pasa.cbentley.framework.core.data.src4.ex.StoreInvalidIDException;
import pasa.cbentley.framework.core.data.src4.ex.StoreNotFoundException;
import pasa.cbentley.framework.core.data.src4.ex.StoreNotOpenException;
import pasa.cbentley.framework.core.data.src4.interfaces.IRecordStore;

/**
 * 
 * @author Charles Bentley
 *
 */
public class StatorReaderCoreData extends StatorReaderBO {

   protected final CoreDataCtx  cdc;

   private String               storeName;


   public StatorReaderCoreData(CoreDataCtx cdc, StatorBO stator, int type, String storeName) {
      this(cdc, stator, type, storeName, null);
   }

   public StatorReaderCoreData(CoreDataCtx cdc, StatorBO stator, int type, String storeName, StatorReaderCoreData parent) {
      super(stator, type);
      this.cdc = cdc;
      this.storeName = storeName;
      this.parent = parent;
      

      //#debug
      toDLog().pInit("Created", this, StatorReaderCoreData.class, "StatorReaderCoreData", LVL_05_FINE, true);
   }

   public void loadDataForType() {
      int type = getType();
      loadDataFromID(type);
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
      BAByteIS in = new BAByteIS(uc, data);
      BADataIS dis = new BADataIS(uc, in );
      this.init(dis);
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
