package pasa.cbentley.framework.coredata.src4.index;

/**
 * Translates a Key to a BID/Factor taking into account the headersize, 
 * <br>
 * <br>
 * @author cbentley
 *
 */
public class KeyAddressor {

   /**
    * 
    */
   public final int FACTOR;

   /**
    * positive Shift
    */
   public final int HEADERSIZE;

   /**
    * KeyAddressor for Index for Keys higher or equal to 1
    * @param factor
    * @param headersize
    */
   public KeyAddressor(int factor, int headersize) {
      FACTOR = factor;
      HEADERSIZE = headersize;
   }

   /**
    * 
    * @param key > 0
    * @return value > 0 , value > headersize
    */
   public int getRID(int key) {
      // +1 because when key = 1, we have to return > headersize 
      return ((key - 1) / FACTOR) + 1 + HEADERSIZE;
   }

   /**
    * 
    * @param key > 0
    * @return
    */
   public int getFactor(int key) {
      if (FACTOR == 1) {
         return 1;
      }
      key = key % FACTOR;
      if (key == 0) {
         return FACTOR;
      }
      return key;
   }
}
