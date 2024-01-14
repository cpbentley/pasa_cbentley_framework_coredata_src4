package pasa.cbentley.framework.coredata.src4.ctx;

import pasa.cbentley.byteobjects.src4.core.BOModuleAbstract;
import pasa.cbentley.byteobjects.src4.core.ByteObject;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.core.src4.logging.IDebugStringable;
import pasa.cbentley.framework.coredata.src4.index.ITechBoIndex;

/**
 * Managed all the Types of this Business Model module.
 * <br>
 * <br>
 * 
 * @author Charles-Philip Bentley
 *
 */
public class BOModuleCoreData extends BOModuleAbstract implements IDebugStringable, IBOTypesCoreData {

   protected final CoreDataCtx cdc;

   public BOModuleCoreData(CoreDataCtx cdc) {
      super(cdc.getBOC());
      this.cdc = cdc;
      //#debug
      toDLog().pInit("starts", this, BOModuleCoreData.class, "BOModuleBusinessObject", LVL_05_FINE, true);
   }

   public ByteObject getFlagOrderedBO(ByteObject bo, int offset, int flag) {
      // TODO Auto-generated method stub
      return null;
   }

   public String toStringGetDIDString(int did, int value) {
      return null;
   }

   public ByteObject merge(ByteObject root, ByteObject merge) {
      // TODO Auto-generated method stub
      return null;
   }

   public void subToString(Dctx sb, ByteObject bo) {
      int type = bo.getType();
      switch (type) {
         case TYPE_202_INDEX:
            cdc.getIndexOperator().toStringHeader(sb, bo);
            break;
         case TYPE_203_AREA_INDEX:
            sb.append("#AreaInt");
            break;
         default:
      }
   }

   public void subToString1Line(Dctx dc, ByteObject bo) {
      int type = bo.getType();
      switch (type) {
         case TYPE_202_INDEX:
            cdc.getIndexOperator().toStringHeader1Line(dc, bo);
            break;
         case TYPE_203_AREA_INDEX:
            dc.append("#AreaInt");
            break;
         default:
      }
   }

   /**
    * String representation of the header given the class id from {@link IObjectManaged#AGENT_OFFSET_05_CLASS_ID2}
    * <br>
    * <br>
    * 
    * @param bo
    * @param nl
    * @param moType
    * @return
    */
   public String subToStringClass(ByteObject bo, int moType) {
      switch (moType) {
         case IBOTypesCoreData.TYPE_202_INDEX:
            return "Index";
         case IBOTypesCoreData.TYPE_203_AREA_INDEX:
            return "Area";
         default:
            break;
      }
      return "Unknown MO Type " + moType;
   }

   /**
    * Displays a name of the offset field. Reflection on the field.
    * <br>
    * @param type
    * @return
    */
   public String subToStringOffset(ByteObject o, int offset) {
      int type = o.getType();
      switch (type) {
         case TYPE_202_INDEX:
            switch (offset) {
               case ITechBoIndex.INDEX_OFFSET_03_VALUE_BYTESIZE1:
                  return "ValueByteSize";
               default:
                  return null;
            }
         default:
            return null;
      }
   }

   /**
    * Class outside the framework implement this method
    * @param type
    * @return null if not found
    */
   public String subToStringType(int type) {
      switch (type) {
         case TYPE_202_INDEX:
            return "Index";
         case TYPE_203_AREA_INDEX:
            return "AreaIndex";
         default:
            return null;
      }
   }

   //#mdebug
   public void toString(Dctx dc) {
      dc.root(this, "BOBusinessObject");
      super.toString(dc.newLevel());
   }

   public boolean toString(Dctx dc, ByteObject bo) {
      // TODO Auto-generated method stub
      return false;
   }

   public void toString1Line(Dctx dc) {
      dc.root(this, "BOBusinessObject");
   }
   //#enddebug

   public boolean toString1Line(Dctx dc, ByteObject bo) {
      // TODO Auto-generated method stub
      return false;
   }

   public String toStringOffset(ByteObject o, int offset) {
      // TODO Auto-generated method stub
      return null;
   }

   public String toStringType(int type) {
      return subToStringType(type);
   }

}
