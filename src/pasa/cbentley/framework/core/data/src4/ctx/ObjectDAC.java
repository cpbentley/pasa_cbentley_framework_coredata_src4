package pasa.cbentley.framework.core.data.src4.ctx;

import pasa.cbentley.core.src4.ctx.UCtx;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.core.src4.logging.IDLog;
import pasa.cbentley.core.src4.logging.IStringable;

public class ObjectDAC implements IStringable {

   protected final CoreDataCtx dac;

   public ObjectDAC(CoreDataCtx dac) {
      this.dac = dac;
   }

   //#mdebug
   public IDLog toDLog() {
      return toStringGetUCtx().toDLog();
   }

   public String toString() {
      return Dctx.toString(this);
   }

   public void toString(Dctx dc) {
      dc.root(this, ObjectDAC.class, 26);
      toStringPrivate(dc);
   }

   public String toString1Line() {
      return Dctx.toString1Line(this);
   }

   private void toStringPrivate(Dctx dc) {

   }

   public void toString1Line(Dctx dc) {
      dc.root1Line(this, ObjectDAC.class);
      toStringPrivate(dc);
   }

   public UCtx toStringGetUCtx() {
      return dac.getUC();
   }

   //#enddebug

}
