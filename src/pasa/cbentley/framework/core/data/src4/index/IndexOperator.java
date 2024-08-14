package pasa.cbentley.framework.core.data.src4.index;

import pasa.cbentley.byteobjects.src4.core.BOAbstractOperator;
import pasa.cbentley.byteobjects.src4.core.ByteObject;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.framework.core.data.src4.ctx.CoreDataCtx;

public class IndexOperator extends BOAbstractOperator implements IBOIndex {

   public IndexOperator(CoreDataCtx cdc) {
      super(cdc.getBOC());
   }

   //#mdebug
   public void toStringHeader(Dctx dc, ByteObject bo) {
      dc.append("#BoIndexBase Tech:");
      dc.append(" ByteSize=" + bo.get1(IBOIndex.INDEX_OFFSET_03_VALUE_BYTESIZE1));
      dc.append(" AuxByteSize=" + bo.get1(IBOIndex.INDEX_OFFSET_04_AUX_BYTESIZE1));
      dc.append(" RefKey=" + bo.get4(IBOIndex.INDEX_OFFSET_05_REFERENCE_KEY4));
      dc.append(" RefAreaShift=" + bo.get2(IBOIndex.INDEX_OFFSET_11_REF_AREA_SHIFT2));
      dc.append(" NormMax=" + bo.get4(IBOIndex.INDEX_OFFSET_06_KEY_NORM_MAX4));
      dc.append(" Reject=" + bo.get2(IBOIndex.INDEX_OFFSET_07_KEY_REJECT_2));
      dc.append(" NumAreas=" + bo.get2(IBOIndex.INDEX_OFFSET_08_NUM_AREAS2));
      dc.append(" OverSize=" + bo.get1(IBOIndex.INDEX_OFFSET_09_OVER_SIZE_TYPE1));
      if (bo.hasFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_1_NULL_CHAIN)) {
         dc.append("NullChain");
      }
      if (bo.hasFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_6_SHADOW)) {
         dc.append("Shadow");
      }
   }

   public void toStringHeader1Line(Dctx dc, ByteObject bo) {
      dc.append("#BoIndexBase Tech:");
      dc.append(" ByteSize=" + bo.get1(IBOIndex.INDEX_OFFSET_03_VALUE_BYTESIZE1));
      dc.append(" AuxByteSize=" + bo.get1(IBOIndex.INDEX_OFFSET_04_AUX_BYTESIZE1));
      dc.append(" RefKey=" + bo.get4(IBOIndex.INDEX_OFFSET_05_REFERENCE_KEY4));
      dc.append(" RefAreaShift=" + bo.get2(IBOIndex.INDEX_OFFSET_11_REF_AREA_SHIFT2));
      dc.append(" NormMax=" + bo.get4(IBOIndex.INDEX_OFFSET_06_KEY_NORM_MAX4));
      dc.append(" Reject=" + bo.get2(IBOIndex.INDEX_OFFSET_07_KEY_REJECT_2));
      dc.append(" NumAreas=" + bo.get2(IBOIndex.INDEX_OFFSET_08_NUM_AREAS2));
      dc.append(" OverSize=" + bo.get1(IBOIndex.INDEX_OFFSET_09_OVER_SIZE_TYPE1));
      if (bo.hasFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_1_NULL_CHAIN)) {
         dc.append("NullChain");
      }
      if (bo.hasFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_6_SHADOW)) {
         dc.append("Shadow");
      }
   }
   //#enddebug

}
