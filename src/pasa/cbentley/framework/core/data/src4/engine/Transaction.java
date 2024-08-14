package pasa.cbentley.framework.core.data.src4.engine;

import pasa.cbentley.core.src4.ctx.UCtx;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.core.src4.logging.IStringable;
import pasa.cbentley.core.src4.structs.IntToObjects;
import pasa.cbentley.framework.core.data.src4.ctx.CoreDataCtx;
import pasa.cbentley.framework.core.data.src4.db.IByteCache;

/**
 * 
 * Adding records returns an assumption.
 * <br>
 * Transaction start with parameters. Big transaction means all records affected
 * 
 * @author Charles Bentley
 *
 */
public class Transaction implements IStringable {

   /**
    * At a given time, it will be more efficient to create
    */
   private byte[][]     bytesTransactions;

   private int[]        s;

   private Transaction  chain;

   private IByteCache   cache;

   private CoreDataCtx       rmc;

   private IntToObjects its;

   /**
    * 
    */
   private int          addCount;

   public Transaction(CoreDataCtx dd, IByteCache c) {
      this.rmc = dd;
      this.cache = c;
   }

   public void add(Transaction t) {
      if (chain == null) {
         chain = t;
      } else {
         chain.add(t);
      }
   }

   /**
    * When half of records 
    */
   private void switchMode() {

   }

   public void setBytes(int id, byte[] b) {
      setBytes(id, b, 0, b.length);
   }

   public void setBytes(int id, byte[] b, int offset, int len) {
      byte[] ar = new byte[len];
      System.arraycopy(b, offset, ar, 0, len);
      its.add(ar, id);
      bytesTransactions[id] = ar;
      s[id] = 1;
   }

   public boolean hasRid(int rid) {
      return its.findInt(rid) != -1;
   }

   public byte[] getBytesFromVal(int val) {
      return (byte[]) its.getObjectAtIndex(val);
   }

   public byte[] getBytes(int rid) {
      int val = its.findInt(rid);
      return (byte[]) its.getObjectAtIndex(val);
   }

   /**
    * 
    * @param rs
    * @param data
    * @param offset
    * @param len
    * @return
    */
   public int addBytes(String rs, byte[] data, int offset, int len) {
      return 0;
   }
   
   
   //#mdebug
   public String toString() {
      return Dctx.toString(this);
   }

   public void toString(Dctx dc) {
      dc.root(this, "Transaction");
      toStringPrivate(dc);
   }

   public String toString1Line() {
      return Dctx.toString1Line(this);
   }

   private void toStringPrivate(Dctx dc) {

   }

   public void toString1Line(Dctx dc) {
      dc.root1Line(this, "Transaction");
      toStringPrivate(dc);
   }

   public UCtx toStringGetUCtx() {
      return rmc.getUC();
   }

   //#enddebug
   


}
