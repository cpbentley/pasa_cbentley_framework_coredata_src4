package pasa.cbentley.framework.core.data.src4.index;
//package pasa.cbentley.framework.coredata.src4.index;
//
//import pasa.cbentley.framework.coredata.src4.db.IByteInterpreter;
//
///**
// * Compress several groups of integers on the same byte array
// * <br>
// * <br>
// * Stores x series of integer values. If x = 4 we might have
// * [13] [1 2 3 4] [24 25 27] [45 47 48 49]
// * <br>
// * <br>
// * This class will effectively stores 
// * [13] [1-3] [24-1 27] [45 47-2] 
// * <br>
// * <br>
// * 
// * There are several options to increase storage efficiency. 
// * That will depends on the size of integers stored
// * <br>
// * <br>
// * 
// *  [ [HEADER HEADER HEADER] [
// * Data contains ordered ids or blocks of ids, blocks are identified by byte header
// * The very first byte defines the structure.
// * MASK_SIMPLE is simple. one byte header defines length of the Continuous Vector of Id
// * Max length is 256. if 257 a next block is needed with a length of 1
// * <br>
// * <br>
// * 
// * the factor is the number of entries in a single byte[] array
// * the factor length is defined in the control byte, 
// * a factor of 1 is the simpliest
// * factor of 2 means we have to define the offset of the 2nd virtal byte array
// * <br>
// * <br>
// * 
// * How do you know the total number of units?
// * <br>
// * <br>
// * 
// * Add all the headers
// * the header 2-3 bytes tell the # of units inside
// * <br>
// * <br>
// * @author cbentley
// *
// */
//public class Compressor implements IByteInterpreter {
//
//   public static final int MASK_EMPTY_DOWN = 64;
//
//   public static final int MASK_EMPTY_UP   = 128;
//
//   public static final int MAXL            = 255;
//
//   /**
//    * 
//    * @param b data
//    * @param of position at which to set rid
//    * @param rid int to set at position of
//    */
//   public static void setShortInt(byte[] b, int of, int rid) {
//      b[of] = (byte) ((rid >>> 8) & 0xFF);
//      b[of + 1] = (byte) ((rid >>> 0) & 0xFF);
//   }
//
//   /**
//    * the header size of the data, without the compression header
//    */
//   private int firstHeaderSize;
//
//   /**
//    * The header byte size at each RID byte array.
//    * <br>
//    * <br>
//    * 
//    */
//   private int fullheadersize;
//
//   /**
//    * Byte size of a chunk
//    * chunk = 1st byte for length, 2-3 for rid
//    * <br>
//    * <br>
//    * 
//    */
//   private int mchunk  = 3;
//
//   /**
//    * The number of integer groups that divide a RID byte array.
//    */
//   private int mfactor = 1;
//
//   /**
//    * 
//    * @param headersize the number of bytes at the beginning for headers
//    * @param chunk the size of a chunk 1+ the number of date bytes
//    * @param factor the number of compartiments
//    */
//   public Compressor(int headersize, int chunk, int factor) {
//      this.mchunk = chunk;
//      this.mfactor = factor;
//      firstHeaderSize = headersize;
//      // the header contains
//      // will contain the offset value for factors > 1
//      fullheadersize = headersize + (mfactor - 1) * 2;
//      // offset value for factor = 1 is implicit ( it is equal to _fullheadersize)
//
//   }
//
//   /**
//    * expands bytes at offset, width _chunk
//    * copies rid header and shortint value at offset
//    * @param bytes
//    * @param offset
//    * @param rid
//    * @return
//    */
//   private byte[] addChunkFull(byte[] bytes, int offset, int rid) {
//      bytes = MUtils.increaseCapacity(bytes, mchunk, offset);
//      //header
//      bytes[offset] = 1;
//      bytes[offset + 1] = (byte) ((rid >>> 8) & 0xFF);
//      bytes[offset + 2] = (byte) ((rid >>> 0) & 0xFF);
//      return bytes;
//   }
//
//   /**
//    * method for adding in the last
//    * @param bytes
//    * @param rid
//    * @return
//    */
//   public byte[] addIntSize(byte[] bytes, int rid) {
//      return this.addIntSize(bytes, rid, fullheadersize, getFactorEnd(bytes, 1));
//   }
//
//   /**
//    * The byte array is divided into several area/columns.
//    * <br>
//    * <br>
//    * 
//    * @param bytes
//    * @param rid
//    * @param factor
//    * @return
//    */
//   public byte[] addIntSize(byte[] bytes, int rid, int factor) {
//      int len = bytes.length;
//      int start = getFactorEnd(bytes, factor);
//      int offset = getFactorOffset(bytes, factor);
//      byte[] b = addIntSize(bytes, rid, offset, start);
//      if (b.length != len) {
//         //one was added, shift the diff in all starts above the current factor
//         for (int i = factor; i < mfactor; i++) {
//            int val = MUtils.shortFromByteArray(b, wierdMethod(i));
//            val = val + (b.length - len);
//            setShortInt(b, wierdMethod(i), val);
//         }
//      }
//      return b;
//   }
//
//   /**
//    * 
//    * @param bytes the data, non null, not empty
//    * @param end when to stop 
//    * @param start where to start reading ints backwards
//    * ie the length of bytes or the start of the next factor
//    * @param rid the int value to add in the array
//    * 
//    * @return same array if value already contained
//    * A chunk is of a length of five
//    */
//   private byte[] addIntSize(byte[] bytes, int rid, int end, int start) {
//      // add conditions are
//      // CASE 2b = p + 1 >= rid >= p - 1
//      // CASE 2c rid = p + 1 => extends
//      // CASE 2a rid = p - 1 => extends next
//      // CASE 3 rid > p + 1
//      // CASE 1 rid < p -1 
//      if (start - end == 0) {
//         bytes = addChunkFull(bytes, start, rid);
//         return bytes;
//      }
//      //i is the position of the current header
//      int i = start - mchunk;
//      //i is decremented by chunk each time, it does not fit in the frame
//      while (i >= end) {
//         //
//         int header = bytes[i];
//         int intFrameEnd = MUtils.shortFromByteArray(bytes, i + 1) + header - 1;
//         int intFrameStart = MUtils.shortFromByteArray(bytes, i + 1);
//         if (rid < intFrameStart - 1) {
//            //go to the next chunk
//            if (i == end) {
//               //we reached the bottom, create a new lone entry
//               bytes = addChunkFull(bytes, i, rid);
//               break;
//            } else {
//               //check for other chunks below
//               i = i - mchunk;
//            }
//         } else if (rid > intFrameEnd + 1) {
//            bytes = addChunkFull(bytes, i + mchunk, rid);
//            break;
//         } else {
//            //inside the frame
//            if (rid == intFrameEnd + 1) {
//               //CASE 2 check if the chunk has reach maximum capacity of information
//               if (header == MAXL) {
//                  bytes = addChunkFull(bytes, i + mchunk, rid);
//               } else {
//                  //BEST CASE
//                  bytes[i]++;
//               }
//               break;
//            } else if (rid == intFrameStart - 1) {
//               if (header == MAXL) {
//                  if (i == end) {
//                     //we reached the bottom, create a new lone entry
//                     bytes = addChunkFull(bytes, i, rid);
//                     break;
//                  } else {
//                     //check for other chunks below
//                     i = i - mchunk;
//                  }
//               } else {
//                  //case where we can add start - 1
//                  bytes[i]++;
//                  setShortInt(bytes, i + 1, rid);
//                  break;
//               }
//            } else {
//               //fits inside nothing to do, exit
//               break;
//            }
//         }
//      }
//      return bytes;
//   }
//
//   /**
//    * Removes all data for factor
//    * <br>
//    * <br>
//    * @param b
//    * @param factor
//    * @return
//    */
//   public byte[] clear(byte[] b, int factor) {
//      int len = getFactorLen(b, factor);
//      int foffset = getFactorOffset(b, factor);
//      byte[] nb = new byte[b.length - len];
//      int count = 0;
//      //from 0 to data start, do a simply copy
//      for (int i = 0; i < foffset; i++) {
//         nb[i] = b[i];
//         count++;
//      }
//      //for first factor, 
//      if (count == nb.length)
//         return nb;
//      //we are at factor offset
//      setShortInt(nb, wierdMethod(factor), count);
//      //don't copy anything
//      for (int i = factor + 1; i <= mfactor; i++) {
//         //copy
//         int flen = getFactorLen(b, i);
//         int oldOffset = getFactorOffset(b, i);
//         //System.out.println("New offset fof Factor "+ i +" = "+ count + " at "+ wierdMethod(i-1));
//         setFactorOffset(nb, i, count);
//         setShortInt(nb, wierdMethod(i - 1), count);
//         for (int j = oldOffset; j < oldOffset + flen; j++) {
//            nb[count] = b[j];
//            count++;
//         }
//      }
//      return nb;
//   }
//
//   public boolean contain(byte[] ar, int rid) {
//      return contain(ar, rid, 1);
//   }
//
//   /**
//    * 
//    * @param ar != null
//    * @param rid valid id
//    * @param factor
//    * @return
//    */
//   public boolean contain(byte[] ar, int rid, int factor) {
//      int len = getFactorEnd(ar, factor);
//      for (int i = getFactorOffset(ar, factor); i < len; i = i + mchunk) {
//         int head = ar[i];
//         int val = MUtils.shortFromByteArray(ar, i + 1);
//         if (rid >= val && rid <= val + head)
//            return true;
//      }
//      return false;
//   }
//
//   public byte[] createLine() {
//      byte[] bytes = new byte[fullheadersize];
//      if (mfactor != 1) {
//         for (int i = 0; i < mfactor - 1; i++) {
//            setShortInt(bytes, firstHeaderSize + 2 * i, fullheadersize);
//         }
//      }
//      return bytes;
//   }
//
//   public int[] decompress(byte[] b) {
//      return decompress(b, 1);
//   }
//
//   /**
//    * Decompress and return the ints from b for that factor
//    * @param b
//    * @param factor
//    * @return
//    */
//   public int[] decompress(byte[] b, int factor) {
//      int len = getFactorEnd(b, factor);
//      int start = getFactorOffset(b, factor);
//      if (len != 0) {
//         int size = 0;
//         for (int i = start; i < len; i = i + mchunk) {
//            //at b[i] we have the number of elements in the current chunk
//            size = size + b[i];
//         }
//         int[] ar = new int[size];
//         int count = 0;
//         for (int i = start; i < len; i = i + mchunk) {
//            int hea = b[i];
//            int val = MUtils.shortFromByteArray(b, i + 1);
//            for (int j = 0; j < hea; j++) {
//               ar[count] = val + j;
//               count++;
//            }
//         }
//         return ar;
//      }
//      return new int[0];
//   }
//
//   public int getChunkSize() {
//      return mchunk;
//   }
//
//   /**
//    * 
//    */
//   public String getDisplayString(byte[] b, int offset, int options) {
//      int start = getHeaderSize();
//      int chunk = getChunkSize();
//      if (b == null || b.length == 0) {
//         return "emtpy";
//      } else {
//         StringBuffer sb = new StringBuffer(b.length / 2);
//         //for debug
//         if (options == 1) {
//            sb.append("Length:" + b.length);
//            sb.append("header:");
//            for (int i = 0; i < start; i++) {
//               sb.append(b[i] + "-");
//            }
//            for (int i = 1; i <= getFactorNum(); i++) {
//               sb.append("f" + i + ":" + getFactorLen(b, i) + ":" + getFactorOffset(b, i) + "-" + getFactorEnd(b, i));
//               sb.append(' ');
//            }
//            sb.append("Data=");
//         }
//         for (int j = 1; j <= getFactorNum(); j++) {
//            sb.append("D" + j + ":");
//            int end = getFactorEnd(b, j);
//            for (int i = getFactorOffset(b, j); i < end; i = i + chunk) {
//               int val = MUtils.shortFromByteArray(b, i + 1);
//               int incr = b[i];
//               if (incr == 1) {
//                  sb.append(val);
//                  if (i != end - chunk)
//                     sb.append(',');
//                  else
//                     sb.append(' ');
//               } else {
//                  int vale = val + incr - 1;
//                  sb.append(val);
//                  sb.append('-');
//                  sb.append(vale);
//                  if (i != end - chunk)
//                     sb.append(',');
//                  else
//                     sb.append(' ');
//               }
//            }
//            sb.append('\t');
//         }
//         return sb.toString();
//      }
//   }
//
//   /**
//    * Where in the array b, does the data for factor f starts?
//    * @param b
//    * @param f
//    * @return the index for the first data of factor f
//    */
//   public int getFactorEnd(byte[] b, int f) {
//      if (f == mfactor)
//         return b.length;
//      else
//         return getFactorOffset(b, f + 1);
//   }
//
//   /**
//    * the number of bytes for a factor
//    * @param i
//    * @return
//    */
//   public int getFactorLen(byte[] b, int i) {
//      return getFactorEnd(b, i) - getFactorOffset(b, i);
//   }
//
//   public int getFactorNum() {
//      return mfactor;
//   }
//
//   /**
//    * 
//    * @param b
//    * @param f
//    * @return
//    */
//   public int getFactorOffset(byte[] b, int f) {
//      if (f == 1) {
//         //we start reading right after the header
//         return fullheadersize;
//      } else {
//         // -2 because f==1 is ignored and we start reading at _firstHeaderSize for f=2
//         return MUtils.shortFromByteArray(b, firstHeaderSize + (f - 2) * 2);
//      }
//   }
//
//   public int getFirstHeaderSize() {
//      return firstHeaderSize;
//   }
//
//   public int getHeaderSize() {
//      return fullheadersize;
//   }
//
//   public byte[] remove(byte[] b, int rid, int factor) {
//      int[] id = decompress(b, factor);
//      int[] ne = MUtils.remove(id, rid);
//      b = clear(b, factor);
//      for (int i = 0; i < ne.length; i++) {
//         b = addIntSize(b, ne[i], factor);
//      }
//      return b;
//   }
//
//   public void setFactorOffset(byte[] b, int factor, int offset) {
//      if (factor == 1)
//         return;
//      setShortInt(b, firstHeaderSize + (factor - 2) * 2, offset);
//   }
//
//   /**
//    * The position for reading the offset of the factor
//    * Method should only be used for factor > 1
//    * @param factor
//    * @return
//    * @throws RunTimeException
//    */
//   public int wierdMethod(int factor) {
//      return firstHeaderSize + (factor - 1) * 2;
//   }
//
//   public int[] toInts(byte[] data, int offset, int factor) {
//      // TODO Auto-generated method stub
//      return null;
//   }
//
//   public byte[] toBytes(int[] values) {
//      // TODO Auto-generated method stub
//      return null;
//   }
//
//   public boolean isContained(byte[] data, int offset, int area, int value) {
//      // TODO Auto-generated method stub
//      return false;
//   }
//
//   public byte[] toBytes(int[] values, int area) {
//      // TODO Auto-generated method stub
//      return null;
//   }
//
//   public byte[] addIntToBytes(byte[] data, int offset, int area, int value) {
//      // TODO Auto-generated method stub
//      return null;
//   }
//}
