package pasa.cbentley.framework.core.data.src4.ctx;

import pasa.cbentley.byteobjects.src4.core.ByteObject;
import pasa.cbentley.byteobjects.src4.ctx.ABOCtx;
import pasa.cbentley.byteobjects.src4.ctx.BOCtx;
import pasa.cbentley.byteobjects.src4.ctx.IBOTypesBOC;
import pasa.cbentley.byteobjects.src4.ctx.IStaticIDsBO;
import pasa.cbentley.byteobjects.src4.ctx.IToStringFlagsBO;
import pasa.cbentley.core.src4.ctx.CtxManager;
import pasa.cbentley.core.src4.ctx.UCtx;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.core.src4.utils.DateUtils;
import pasa.cbentley.framework.core.data.src4.db.IBOCacheRMS;
import pasa.cbentley.framework.core.data.src4.db.IByteRecordStoreFactory;
import pasa.cbentley.framework.core.data.src4.db.IByteStore;
import pasa.cbentley.framework.core.data.src4.engine.RMSByteStore;
import pasa.cbentley.framework.core.data.src4.index.IndexFactory;
import pasa.cbentley.framework.core.data.src4.index.IndexOperator;

/**
 * Framework autonmous code context.
 * 
 * {@link UCtx} is a configuration less context
 * 
 * Part of the Bentley Framework where each platform will problem an implementation of the {@link CoreDataCtx}
 * 
 * @author Charles Bentley
 *
 */
public abstract class CoreDataCtx extends ABOCtx {

   private IByteStore    byteStore;

   private IndexFactory  indexFactory;

   private IndexOperator indexOperator;

   public CoreDataCtx(IConfigCoreData config, BOCtx boc) {
      super(config, boc);

      CtxManager cm = uc.getCtxManager();
      cm.registerStaticRange(this, IStaticIDsBO.SID_BYTEOBJECT_TYPES, IBOTypesCoreData.SID_BASETYPE_A, IBOTypesCoreData.SID_BASETYPE_Z);
   }

   protected void applySettings(ByteObject settingsNew, ByteObject settingsOld) {

   }

   public abstract int getBOCtxSettingSize();

   /**
    * Host implementation of {@link CoreDataCtx} returns the {@link IByteRecordStoreFactory}.
    * 
    * @return
    */
   public abstract IByteRecordStoreFactory getByteRecordStoreFactory();

   public IByteStore getByteStore() {
      if (byteStore == null) {
         byteStore = new RMSByteStore(this);
      }
      return byteStore;
   }

   public ByteObject getDefaultTech() {
      ByteObject bo = new ByteObject(boc, IBOTypesBOC.TYPE_020_PARAMATERS, IBOCacheRMS.CACHE_BASIC_SIZE);
      bo.setValue(IBOCacheRMS.CACHE_OFFSET_02_GROW2, 5, 2);
      bo.setValue(IBOCacheRMS.CACHE_OFFSET_03_START_SIZE_4, 5, 2);
      bo.setValue(IBOCacheRMS.CACHE_OFFSET_04_MAX_SIZE_4, 60, 4);

      return bo;
   }

   public IndexFactory getIndexFactory() {
      if (indexFactory == null) {
         indexFactory = new IndexFactory(this);
      }
      return indexFactory;
   }

   public IndexOperator getIndexOperator() {
      if (indexOperator == null) {
         indexOperator = new IndexOperator(this);
      }
      return indexOperator;
   }

   public ByteObject getTech(int maxsize, int startsize, int growadd) {
      ByteObject bo = getDefaultTech();
      bo.set2(IBOCacheRMS.CACHE_OFFSET_02_GROW2, growadd);
      bo.set4(IBOCacheRMS.CACHE_OFFSET_03_START_SIZE_4, startsize);
      bo.set4(IBOCacheRMS.CACHE_OFFSET_04_MAX_SIZE_4, maxsize);

      return bo;
   }

   //#mdebug
   public void toString(Dctx dc) {
      dc.root(this, CoreDataCtx.class, "@line5");
      toStringPrivate(dc);
      super.toString(dc.sup());
      dc.nlLvl(byteStore, "byteStore");
      dc.nlLvl(indexFactory, "indexFactory");
      dc.nlLvl(indexOperator, "indexOperator");
   }

   public void toString1Line(Dctx dc) {
      dc.root1Line(this, CoreDataCtx.class);
      toStringPrivate(dc);
      super.toString1Line(dc.sup1Line());
   }

   private void toStringPrivate(Dctx dc) {

   }

   /**
    * Get Displayed Data about the record store
    * <br>
    * <br>
    * @param recordStore
    * @param nl string between data records
    * @return
    */
   public void toStringStoreHeader(IByteStore store, String rs, Dctx sb) {
      sb.append("#Header");
      if (boc.toStringHasToStringFlag(IToStringFlagsBO.TOSTRING_FLAG_1_SERIALIZE)) {
         sb.appendVarWithSpace("rs", rs);
      }
      sb = sb.newLevel();
      int nums = store.getNumRecords(rs);
      int next = store.getNextRecordId(rs);
      int size = store.getSize(rs);
      sb.append("Numrecords=" + nums);
      sb.append(" Nextid=" + next);
      sb.append(" ");
      if (size > 1000)
         sb.append("Size=" + size / 1000 + "kb");
      else
         sb.append("Size=" + size + "bytes");
      if (boc.toStringHasToStringFlag(IToStringFlagsBO.TOSTRING_FLAG_1_SERIALIZE)) {
         sb.append(" version=" + store.getVersion(rs));
      }
      sb.nl();
      sb.append("last modified=" + DateUtils.getDslashMslashY(store.getLastModified(rs)));
      if (nums + 1 == next) {
         sb.append("[Full]");
      } else {
         sb.append("[Holes]");
      }
      sb.append(" free kb=" + store.getSizeAvailable(rs) / 1000);

   }

   //#enddebug

}
