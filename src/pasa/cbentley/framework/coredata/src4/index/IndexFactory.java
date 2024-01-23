package pasa.cbentley.framework.coredata.src4.index;

import pasa.cbentley.byteobjects.src4.core.BOAbstractFactory;
import pasa.cbentley.byteobjects.src4.core.ByteObject;
import pasa.cbentley.byteobjects.src4.ctx.BOCtx;
import pasa.cbentley.framework.coredata.src4.ctx.CoreDataCtx;

public class IndexFactory extends BOAbstractFactory implements IBOIndex, IBOAreaInt {

   public IndexFactory(CoreDataCtx cdc) {
      super(cdc.getBOC());
   }

   /**
    * Tech
    * @return
    */
   public ByteObject createEmptyTech() {
      return new ByteObject(boc, AREA_TYPE, AREA_BASIC_SIZE);
   }

   /**
    * With many values, we use a {@link IBOIndex#TYPE_1_CHAIN}
    * @return
    */
   public ByteObject getIndexFewKeysManyValues(int numAreas, int type) {
      ByteObject indexTech = getDefaultTech(IBOIndex.TYPE_1_CHAIN);
      indexTech.set2(IBOIndex.INDEX_OFFSET_08_NUM_AREAS2, numAreas);
      indexTech.set1(IBOIndex.INDEX_OFFSET_03_VALUE_BYTESIZE1, 3);
      indexTech.set4(IBOIndex.INDEX_OFFSET_05_REFERENCE_KEY4, 0);
      return indexTech;
   }

   public ByteObject getTechDef() {
      ByteObject patcherTech = new ByteObject(boc, AREA_TYPE, AREA_BASIC_SIZE);
      patcherTech.set1(IBOAreaInt.MAIN_OFFSET_02_DATA_BYTE_SIZE1, 2); //2 bytes
      patcherTech.set2(IBOAreaInt.MAIN_OFFSET_04_NUM_AREAS2, 1); //5 areas must be the same as avoe
      patcherTech.set1(IBOAreaInt.MAIN_OFFSET_05_BYTE_SIZE_AREA_OFFSET1, 2);
      return patcherTech;
   }

   /**
    * Default byte size of 2.
    * @param type
    * @return
    */
   public ByteObject getDefaultTech(int type) {
      ByteObject bo = new ByteObject(boc, IBOIndex.INDEX_TYPE, IBOIndex.INDEX_BASIC_SIZE);
      bo.setValue(INDEX_OFFSET_02_TYPE1, type, 1);
      bo.setValue(INDEX_OFFSET_03_VALUE_BYTESIZE1, 2, 1);
      bo.setValue(INDEX_OFFSET_04_AUX_BYTESIZE1, 2, 1);
      bo.set2(IBOIndex.INDEX_OFFSET_07_KEY_REJECT_2, 1000);

      return bo;
   }

   public ByteObject getDefaultTechPatch1Area() {
      return getDefaultTechPatch1Area(1, 2);
   }

   public ByteObject getDefaultTechPatch1Area(int numAreas, int dataByteSize) {
      ByteObject bo = new ByteObject(boc, IBOIndex.INDEX_TYPE, IBOIndex.INDEX_BASIC_SIZE);
      bo.set1(INDEX_OFFSET_02_TYPE1, IBOIndex.TYPE_0_PATCH);
      bo.set1(INDEX_OFFSET_03_VALUE_BYTESIZE1, dataByteSize);
      bo.set1(INDEX_OFFSET_04_AUX_BYTESIZE1, 2);
      bo.set2(IBOIndex.INDEX_OFFSET_07_KEY_REJECT_2, 100);

      ByteObject patcherTech = createEmptyTech();
      patcherTech.set1(IBOAreaInt.MAIN_OFFSET_02_DATA_BYTE_SIZE1, dataByteSize); //2 bytes
      patcherTech.set2(IBOAreaInt.MAIN_OFFSET_04_NUM_AREAS2, numAreas); //5 areas must be the same as avoe
      patcherTech.set1(IBOAreaInt.MAIN_OFFSET_05_BYTE_SIZE_AREA_OFFSET1, 2);
      bo.addSub(patcherTech);
      return bo;
   }
}
