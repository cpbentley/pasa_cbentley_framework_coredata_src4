package pasa.cbentley.framework.coredata.src4.index;

import pasa.cbentley.byteobjects.src4.core.ByteObject;
import pasa.cbentley.core.src4.utils.IntUtils;
import pasa.cbentley.core.src4.utils.ShortUtils;

public class ByteObjectStatic {

   public static void increment(byte[] data, int index, int size, int incr) {
      int val = getValue(data, index, size);
      val += incr;
      setValue(data, index, val, size);
   }

   /**
    * Sign short
    * @param index
    * @param value 0-255 for bytes, -37000 37000 for short
    * @param size
    */
   public static void setValue(byte[] data, int index, int value, int size) {
      if (size == 1) {
         data[index] = (byte) value;
      } else if (size == 2) {
         if (value < 0) {
            value = -value;
            value |= (MIndexIntToInts.MINUS_SIGN_16BITS_FLAG);
         }
         ShortUtils.writeShortBEUnsigned(data, index, value);
      } else if (size == 3) {
         IntUtils.writeInt24BE(data, index, value);
      } else {
         IntUtils.writeIntBE(data, index, value);
      }
   }

   public static int get4Signed(byte[] data, int index) {
      return IntUtils.readIntBE(data, index);
   }

   public static int get1Unsigned(byte[] data, int index) {
      return data[index] & 0xFF;
   }

   /**
    * Read short as a signed value
    * @param index
    * @return
    */
   public static int get2Signed(byte[] data, int index) {
      int v = ShortUtils.readShortBEUnsigned(data, index);
      if ((v & MIndexIntToInts.MINUS_SIGN_16BITS_FLAG) == MIndexIntToInts.MINUS_SIGN_16BITS_FLAG) {
         v &= ~MIndexIntToInts.MINUS_SIGN_16BITS_FLAG;
         v = -v;
      }
      return v;
   }

   /**
    * 32 bits values.
    * Because Java int are signed.. and usually we don't want signed single bytes
    * {@link ByteObject}s follow this convention
    * 
    * <li> 1 - unsigned 0-255 values
    * <li> 2 - signed -
    * <li> 3 - unsigned
    * <li> 4 - signed full integer
    * @param index
    * @param size
    * @return
    */
   public static int getValue(byte[] data, int index, int size) {
      if (size == 1)
         return get1Unsigned(data, index);
      if (size == 2)
         return get2Signed(data, index);
      if (size == 3)
         return get3Unsigned(data, index);
      return get4Signed(data, index);
   }

   public static int get3Unsigned(byte[] data, int index) {
      return IntUtils.readInt24BE(data, index);
   }

}
