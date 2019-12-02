package pasa.cbentley.framework.coredata.src4.interfaces;

public interface IRecordComparator {
   
   public static final int EQUIVALENT = 0;

   public static final int PRECEDES   = -1;

   public static final int FOLLOWS    = 1;

   public int compare(byte[] rec1, byte[] rec2);
}