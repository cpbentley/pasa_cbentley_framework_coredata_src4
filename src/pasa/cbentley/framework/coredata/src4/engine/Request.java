package pasa.cbentley.framework.coredata.src4.engine;

public class Request {
   public byte[]  data;

   public boolean contains;

   public Integer key;

   public int     recordid;

   public byte[] getBytes() {
      if (data == null) {
         return null;
      } else {
         int recordSize = data.length;
         byte[] buffer = new byte[recordSize];
         System.arraycopy(data, 0, buffer, 0, recordSize);
         return buffer;
      }
   }

   public int getBytes(byte[] buffer, int offset) {
      int recordSize = data.length;
      System.arraycopy(data, 0, buffer, offset, recordSize);
      return recordSize;
   }
}