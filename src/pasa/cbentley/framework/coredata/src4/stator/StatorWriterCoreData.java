package pasa.cbentley.framework.coredata.src4.stator;

import pasa.cbentley.byteobjects.src4.core.ByteObject;
import pasa.cbentley.byteobjects.src4.stator.ITechStatorBO;
import pasa.cbentley.byteobjects.src4.stator.StatorBO;
import pasa.cbentley.byteobjects.src4.stator.StatorWriterBO;
import pasa.cbentley.core.src4.ctx.CtxManager;
import pasa.cbentley.core.src4.io.BAByteOS;
import pasa.cbentley.core.src4.io.BADataOS;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.framework.coredata.src4.ctx.CoreDataCtx;
import pasa.cbentley.framework.coredata.src4.db.IByteRecordStoreFactory;
import pasa.cbentley.framework.coredata.src4.ex.StoreException;
import pasa.cbentley.framework.coredata.src4.ex.StoreNotFoundException;
import pasa.cbentley.framework.coredata.src4.interfaces.IRecordStore;

/**
 * Implements the {@link ByteObject} key mechanism for the Framework.
 * <p>
 * We want to disociate Ctx state from View and from Model so we can combine them as we want.
 * 
 * We do not want a monolith state file
 * </p>
 * 
 * Compared to {@link StatorWriterBO}, we have access to {@link CoreDataCtx} for actually writing to disk.
 * 
 * 
 * @author Charles Bentley
 *
 */
public class StatorWriterCoreData extends StatorWriterBO {

   protected final CoreDataCtx  cdc;

   private String               storeName;


   public StatorWriterCoreData(CoreDataCtx cdc, StatorBO stator, int type, String storeName) {
      this(cdc, stator, type, storeName, null);
   }

   public StatorWriterCoreData(CoreDataCtx cdc, StatorBO stator, int type, String storeName, StatorWriterCoreData parent) {
      super(stator, type);
      this.storeName = storeName;
      this.cdc = cdc;
      this.parent = parent;

      //#debug
      toDLog().pInit("Created", this, StatorWriterCoreData.class, "StatorWriterCoreData", LVL_03_FINEST, true);
   }


   public void serializeToStore() {
      IByteRecordStoreFactory fac = cdc.getByteRecordStoreFactory();
      IRecordStore rs = fac.openRecordStore(storeName, true);
      int base = rs.getBase();
      byte[] dataMain = serialize();
      cdc.getByteStore().setBytesEnsure(storeName, base, dataMain);
   }


   //#mdebug
   public void toString(Dctx dc) {
      dc.root(this, StatorWriterCoreData.class, 140);
      toStringPrivate(dc);
      super.toString(dc.sup());

   }

   private void toStringPrivate(Dctx dc) {

   }

   public void toString1Line(Dctx dc) {
      dc.root1Line(this, StatorWriterCoreData.class);
      toStringPrivate(dc);
      super.toString1Line(dc.sup1Line());
   }

   //#enddebug

}
