package pasa.cbentley.framework.coredata.src4.index;

import pasa.cbentley.byteobjects.src4.core.BOAbstractOperator;
import pasa.cbentley.byteobjects.src4.core.ByteObject;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.framework.coredata.src4.ctx.CoreDataCtx;

public class IndexOperator extends BOAbstractOperator implements IBOIndex {

   public IndexOperator(CoreDataCtx cdc) {
      super(cdc.getBOC());
   }

   public void toStringHeader(Dctx sb, ByteObject bo) {
      sb.append("#BoIndexBase Tech:");
      sb.append(" ByteSize=" + bo.get1(IBOIndex.INDEX_OFFSET_03_VALUE_BYTESIZE1));
      sb.append(" AuxByteSize=" + bo.get1(IBOIndex.INDEX_OFFSET_04_AUX_BYTESIZE1));
      sb.append(" RefKey=" + bo.get4(IBOIndex.INDEX_OFFSET_05_REFERENCE_KEY4));
      sb.append(" RefAreaShift=" + bo.get2(IBOIndex.INDEX_OFFSET_11_REF_AREA_SHIFT2));
      sb.append(" NormMax=" + bo.get4(IBOIndex.INDEX_OFFSET_06_KEY_NORM_MAX4));
      sb.append(" Reject=" + bo.get2(IBOIndex.INDEX_OFFSET_07_KEY_REJECT_2));
      sb.append(" NumAreas=" + bo.get2(IBOIndex.INDEX_OFFSET_08_NUM_AREAS2));
      sb.append(" OverSize=" + bo.get1(IBOIndex.INDEX_OFFSET_09_OVER_SIZE_TYPE1));
      if (bo.hasFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_1_NULL_CHAIN)) {
         sb.append("NullChain");
      }
      if (bo.hasFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_6_SHADOW)) {
         sb.append("Shadow");
      }
   }

   public void toStringHeader1Line(Dctx sb, ByteObject bo) {
      sb.append("#BoIndexBase Tech:");
      sb.append(" ByteSize=" + bo.get1(IBOIndex.INDEX_OFFSET_03_VALUE_BYTESIZE1));
      sb.append(" AuxByteSize=" + bo.get1(IBOIndex.INDEX_OFFSET_04_AUX_BYTESIZE1));
      sb.append(" RefKey=" + bo.get4(IBOIndex.INDEX_OFFSET_05_REFERENCE_KEY4));
      sb.append(" RefAreaShift=" + bo.get2(IBOIndex.INDEX_OFFSET_11_REF_AREA_SHIFT2));
      sb.append(" NormMax=" + bo.get4(IBOIndex.INDEX_OFFSET_06_KEY_NORM_MAX4));
      sb.append(" Reject=" + bo.get2(IBOIndex.INDEX_OFFSET_07_KEY_REJECT_2));
      sb.append(" NumAreas=" + bo.get2(IBOIndex.INDEX_OFFSET_08_NUM_AREAS2));
      sb.append(" OverSize=" + bo.get1(IBOIndex.INDEX_OFFSET_09_OVER_SIZE_TYPE1));
      if (bo.hasFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_1_NULL_CHAIN)) {
         sb.append("NullChain");
      }
      if (bo.hasFlag(INDEX_OFFSET_01_FLAG, INDEX_FLAG_6_SHADOW)) {
         sb.append("Shadow");
      }
   }

}
