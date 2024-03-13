package pasa.cbentley.framework.coredata.src4.index;

import pasa.cbentley.byteobjects.src4.core.ByteObject;
import pasa.cbentley.core.src4.ctx.UCtx;
import pasa.cbentley.core.src4.helpers.StringBBuilder;
import pasa.cbentley.core.src4.logging.Dctx;
import pasa.cbentley.core.src4.logging.IDLog;
import pasa.cbentley.core.src4.utils.BitUtils;
import pasa.cbentley.core.src4.utils.IntUtils;
import pasa.cbentley.framework.coredata.src4.ctx.CoreDataCtx;
import pasa.cbentley.framework.coredata.src4.db.IByteInterpreter;

/**
 * Compress several groups of integers on the same byte array. Kind of a Matrix of Integers.
 * <br>
 * <br>
 * The structure always reads a byte array at an offset looking for a header at that offset.
 * <li> {@link IByteInteger#addIntToBytes(byte[], int, int, int)}
 * <li> {@link IByteInteger#isContained(byte[], int, int, int)}
 * <li> {@link IByteInteger#toInts(byte[], int, int)}
 * <br>
 * <br>
 * The area specified is the "column" to look for.
 * <br>
 * <br>
 * 
 * Stores x series of integer values. If x = 4 we might have
 * [13] [1 2 3 4] [24 25 27] [45 47 48 49]
 * <br>
 * <br>
 * This class will effectively stores 
 * [13] [1-3] [24-1 27] [45 47-2] 
 * <br>
 * <br>
 * The value stored are 1 2-3 or 4 bytes : {@link IBOAreaInt#MAIN_OFFSET_02_DATA_BYTE_SIZE1}
 * <br>
 * <br>
 * 
 * There are several options to increase storage efficiency. 
 * That will depends on the size of integers stored
 * <br>
 * <br>
 * 
 *  [ [HEADER HEADER HEADER] [
 * Data contains ordered ids or blocks of ids, blocks are identified by byte header
 * The very first byte defines the structure.
 * MASK_SIMPLE is simple. one byte header defines length of the Continuous Vector of Id
 * Max length is 256. if 257 a next block is needed with a length of 1
 * <br>
 * <br>
 * 
 * the factor is the number of entries in a single byte[] array
 * the factor length is defined in the control byte, 
 * a factor of 1 is the simpliest
 * factor of 2 means we have to define the offset of the 2nd virtal byte array
 * <br>
 * <br>
 * 
 * How do you know the total number of units?
 * <br>
 * <br>
 * 
 * Add all the headers
 * the header 2-3 bytes tell the # of units inside
 * <br>
 * <br>
 * @author cbentley
 *
 */
public class IntAreaDecoder implements IByteInteger, IBOAreaInt {

   /**
    * 
    * @param b data
    * @param of position at which to set rid
    * @param rid int to set at position of
    */
   public static void setShortInt(byte[] b, int of, int rid) {
      b[of] = (byte) ((rid >>> 8) & 0xFF);
      b[of + 1] = (byte) ((rid >>> 0) & 0xFF);
   }

   /**
    * Byte size of a chunk.
    * 
    * chunk = 1st byte for number of values, 2-3 for start value
    * <br>
    * <br>
    * 
    */
   private int                 areaByteSize  = 3;

   private int                 byteSizeAreaOffsets;

   /**
    * The value is updated when a value byte size is bigger?
    * <br>
    * <br>
    * But you can't update all lines since you don't have access to them.
    * <br>
    * <br>
    * This requires all lines to have at least 1 byte of header to tell if a line is deviant from the main spec.
    * <br>
    * <br>
    * 
    */
   private int                 dataByteSize;

   /**
    * The base header byte size at each Line byte array.
    * <br>
    * <br>
    * Does not contain the byte size of the table
    * <br>
    * <br>
    * 
    */
   private int                 lineHeaderByteSize;

   private int                 lineTableOffsetByteSize;

   /**
    * The number of integer groups that divide a RID byte array.
    */
   private int                 numAreas      = 1;

   /**
    * {@link IBOAreaInt#MAIN_OFFSET_06_NUM_DATA_PER_AREAS2}
    */
   private int                 numDatas      = 0;

   private int                 patchByteSize = 3;

   private ByteObject          tech;

   protected final CoreDataCtx cdc;

   /**
    * Often the index using the decoder will not set a tech.
    * @param tech
    */
   public IntAreaDecoder(CoreDataCtx cdc, ByteObject tech) {
      this.cdc = cdc;
      this.tech = tech;
      numAreas = tech.get2(MAIN_OFFSET_04_NUM_AREAS2);
      numDatas = tech.get2(MAIN_OFFSET_06_NUM_DATA_PER_AREAS2);
      byteSizeAreaOffsets = tech.get1(MAIN_OFFSET_05_BYTE_SIZE_AREA_OFFSET1);
      dataByteSize = tech.get1(MAIN_OFFSET_02_DATA_BYTE_SIZE1);
      if (isOffsetTable()) {
         //use of patches
         patchByteSize = dataByteSize + 1;
         lineTableOffsetByteSize = numAreas * byteSizeAreaOffsets;
         lineHeaderByteSize = 1;
         if (tech.hasFlag(MAIN_OFFSET_01_FLAG, MAIN_FLAG_1_DATA_BYTE_SIZE_DIFF)) {

         }
      } else {
         areaByteSize = tech.get1(MAIN_OFFSET_02_DATA_BYTE_SIZE1) * numDatas;
         lineHeaderByteSize = 1;
      }
   }
   
   public int getNumAreasPerLine() {
      return numAreas;
   }
   
   private boolean isOffsetTable() {
      return numDatas == 0;
   }

   /**
    * The byte array is divided into several area/columns.
    * <br>
    * <br>
    * In a non patched structure, shift old values. oldest one is erased. un sorted.
    * <br>
    * <br>
    * 
    * @param bytes
    * @param area
    * @param rid
    * @return
    */
   public byte[] addIntSize(byte[] bytes, int offset, int area, int value) {
      checkArea(area);
      if (isOffsetTable()) {
         int offsetEnd = getAreaEndOffsetPatch(bytes, offset, area);
         int offsetStart = getAreaStartOffsetPatch(bytes, offset, area);
         int oldLength = bytes.length;
         bytes = addIntSizePatch(bytes, value, offsetStart, offsetEnd);

         //modifies the patched areas line header
         int diff = bytes.length - oldLength;
         if (diff != 0) {
            //one was added, shift the diff in all area end offset above the current factor
            for (int i = area; i < numAreas; i++) {
               int areaOffset = getLineAreaEndOffset(offset, i);
               ByteArrayStaticUtilz.increment(bytes, areaOffset, byteSizeAreaOffsets, diff);
            }
         }
         return bytes;
      } else {
         int offsetStart = getAreaStartOffsetNum(bytes, offset, area);
         if (numDatas == 1) {
            ByteArrayStaticUtilz.setValue(bytes, offsetStart, value, dataByteSize);
         } else {
            //shift and then writes
            int endOffset = getAreaEndOffsetNum(bytes, offset, area) - dataByteSize;
            for (int i = numDatas - 1; i > 0; i--) {
               endOffset -= dataByteSize;
               int val = ByteArrayStaticUtilz.getValue(bytes, endOffset, dataByteSize);
               ByteArrayStaticUtilz.setValue(bytes, endOffset + dataByteSize, val, dataByteSize);
            }
            ByteArrayStaticUtilz.setValue(bytes, offsetStart, value, dataByteSize);
         }
         return bytes;
      }

   }

   /**
    * Called when adding using patches.
    * <br>
    * <br>
    * 
    * @param bytes the data, non null, not empty
    * @param value the int value to add in the array
    * @param areaStartOffset when to stop 
    * @param areaEndOffset where to start reading ints backwards
    * ie the length of bytes or the start of the next factor
    * 
    * @return same array if value already contained
    * A chunk is of a length of five
    */
   private byte[] addIntSizePatch(byte[] bytes, int value, int areaStartOffset, int areaEndOffset) {
      // add conditions are
      // CASE 2b = p + 1 >= rid >= p - 1
      // CASE 2c rid = p + 1 => extends
      // CASE 2a rid = p - 1 => extends next
      // CASE 3 rid > p + 1
      // CASE 1 rid < p -1 
      if (areaEndOffset - areaStartOffset == 0) {
         bytes = addPatchFull(bytes, areaEndOffset, value);
         return bytes;
      }
      int patchChunkByteSize = dataByteSize + 1;
      //i is the position of the current header
      int i = areaEndOffset - patchChunkByteSize;
      //i is decremented by chunk each time, it does not fit in the frame
      while (i >= areaStartOffset) {
         //
         int header = bytes[i];
         int patchValue = ByteArrayStaticUtilz.getValue(bytes, i + 1, dataByteSize);
         int intFrameStart = patchValue;
         int intFrameEnd = patchValue + header - 1;
         if (value < intFrameStart - 1) {
            //go to the next patch
            if (i == areaStartOffset) {
               //we reached the bottom, create a new lone entry
               bytes = addPatchFull(bytes, i, value);
               break;
            } else {
               //check for other chunks below
               i = i - patchChunkByteSize;
            }
         } else if (value > intFrameEnd + 1) {
            bytes = addPatchFull(bytes, i + patchChunkByteSize, value);
            break;
         } else {
            //inside the frame
            if (value == intFrameEnd + 1) {
               //CASE 2 check if the chunk has reached maximum capacity of information
               if (header == PATCH_MAX_HEADER) {
                  bytes = addPatchFull(bytes, i + patchChunkByteSize, value);
               } else {
                  //BEST CASE
                  bytes[i]++;
               }
               break;
            } else if (value == intFrameStart - 1) {
               if (header == PATCH_MAX_HEADER) {
                  if (i == areaStartOffset) {
                     //we reached the bottom, create a new lone entry
                     bytes = addPatchFull(bytes, i, value);
                     break;
                  } else {
                     //check for other chunks below
                     i = i - patchChunkByteSize;
                  }
               } else {
                  //case where we can add start - 1
                  bytes[i]++;
                  setShortInt(bytes, i + 1, value);
                  break;
               }
            } else {
               //fits inside nothing to do, exit
               break;
            }
         }
      }
      return bytes;
   }

   /**
    * 
    */
   public byte[] addIntToBytes(byte[] data, int offset, int area, int value) {
      return addIntSize(data, offset, area, value);
   }

   /**
    * expands bytes at offset, width _chunk
    * copies rid header and shortint value at offset
    * <br>
    * <br>
    * @param bytes
    * @param offset
    * @param rid
    * @return
    */
   private byte[] addPatchFull(byte[] bytes, int offset, int rid) {
      bytes = cdc.getUC().getMem().increaseCapacity(bytes, patchByteSize, offset);
      //header
      bytes[offset] = 1;
      ByteArrayStaticUtilz.setValue(bytes, offset + 1, rid, dataByteSize);
      return bytes;
   }

   protected void checkArea(int area) {
      if (area < 0 || area > numAreas) {
         throw new IllegalArgumentException(area + " for " + numAreas);
      }
   }

   /**
    * Removes all data for area
    * <br>
    * <br>
    * For fixed sized areas, set all values to 0.
    * <br>
    * <br>
    * Modifies byte position for all bytes.
    * <br>
    * 
    * @param data
    * @param area
    * @return
    */
   public byte[] clearInt(byte[] data, int offset, int area) {
      checkArea(area);
      if (isOffsetTable()) {
         int areaByteLen = getAreaByteLength(data, offset, area);
         int areaStartOffset = getAreaStartOffsetPatch(data, offset, area);
         int areaEndOffset = getAreaEndOffsetPatch(data, offset, area);
         byte[] newData = new byte[data.length - areaByteLen];
         int count = 0;
         //copy the start from 0 to area start.
         System.arraycopy(data, 0, newData, 0, areaStartOffset);
         //copy the end
         System.arraycopy(data, areaEndOffset, newData, areaEndOffset - areaByteLen, data.length - areaEndOffset);

         //update offset table
         for (int upAreas = area; upAreas < numAreas; upAreas++) {
            //copy
            int areaOffsets = getLineAreaEndOffset(offset, upAreas);
            //decrement 
            ByteArrayStaticUtilz.increment(newData, areaOffsets, byteSizeAreaOffsets, -areaByteLen);
         }

         return newData;
      } else {
         int readOffset = getAreaStartOffsetNum(data, offset, area);
         for (int i = 0; i < numDatas; i++) {
            ByteArrayStaticUtilz.setValue(data, readOffset, 0, dataByteSize);
            readOffset += dataByteSize;
         }
         return data;
      }
   }

   /**
    * 
    */
   public byte[] createLine() {
      if (isOffsetTable()) {
         int value = lineHeaderByteSize + lineTableOffsetByteSize;
         byte[] bytes = new byte[value];
         //empty areas. all start at the same offset
         int offset = lineHeaderByteSize;
         for (int i = 0; i < numAreas; i++) {
            ByteArrayStaticUtilz.setValue(bytes, offset, value, byteSizeAreaOffsets);
            offset += byteSizeAreaOffsets;
         }
         return bytes;
      } else {
         byte[] bytes = new byte[lineHeaderByteSize + (numAreas * numDatas * dataByteSize)];
         return bytes;
      }
   }

   /**
    * the number of bytes for a factor
    * @param area
    * @return 0 when empty
    */
   public int getAreaByteLength(byte[] b, int offset, int area) {
      return getAreaEndOffset(b, offset, area) - getAreaStartOffset(b, offset, area);
   }

   public int getAreaEndOffset(byte[] b, int offset, int area) {
      if (isOffsetTable()) {
         return getAreaEndOffsetPatch(b, offset, area);
      } else {
         return getAreaEndOffsetNum(b, offset, area);
      }
   }

   public int getAreaEndOffsetNum(byte[] b, int offset, int area) {
      return offset + lineHeaderByteSize + ((area + 1) * numDatas * dataByteSize);
   }

   /**
    * Reads the Offset table and return the end offset for that area. 
    * Incidently this is the start offset of area + 1.
    * <br>
    * <br>
    * @param b
    * @param offset
    * @param area 0 <= area < numAreas
    * @return the index for the first data of factor f
    */
   public int getAreaEndOffsetPatch(byte[] b, int offset, int area) {
      int tableOffset = offset;
      tableOffset += lineHeaderByteSize;
      tableOffset += (area * byteSizeAreaOffsets);
      // -2 because f==1 is ignored and we start reading at _firstHeaderSize for f=2
      return offset + ByteArrayStaticUtilz.getValue(b, tableOffset, byteSizeAreaOffsets);
   }

   public int getAreaStartOffset(byte[] b, int offset, int area) {
      if (isOffsetTable()) {
         return getAreaStartOffsetPatch(b, offset, area);
      } else {
         return getAreaStartOffsetNum(b, offset, area);
      }
   }

   public int getAreaStartOffsetNum(byte[] b, int offset, int area) {
      int areaOffset = (area * numDatas * dataByteSize);
      return offset + lineHeaderByteSize + areaOffset;
   }

   /**
    * The offset of first area is implicitely at line header. Offset is added.
    * <br>
    * <br>
    * @param b
    * @param offset
    * @param area
    * @return
    */
   public int getAreaStartOffsetPatch(byte[] b, int offset, int area) {
      if (area == 0) {
         //we start reading right after the header
         return offset + lineHeaderByteSize + lineTableOffsetByteSize;
      } else {
         return getAreaEndOffsetPatch(b, offset, area - 1);
      }
   }

   /**
    * Provides online debugging of a IntArea line.
    * When flag FLAG_DISPLAY_1_HEADER
    */
   public String getDisplayString(byte[] b, int offset, int options) {
      StringBBuilder sb = new StringBBuilder(cdc.getUC());
      if (b == null || b.length == 0) {
         return "emtpy";
      } else {
         //for debug
         if (BitUtils.hasFlag(options, IByteInterpreter.FLAG_OPTION_1_HEADER)) {
            sb.append("Header offset=" + offset + " size=" + lineHeaderByteSize);
            for (int i = 0; i < numAreas; i++) {
               sb.append("[Area" + i);
               sb.append(":");
               sb.append(getAreaStartOffset(b, offset, i));
               sb.append(",");
               sb.append(getAreaEndOffset(b, offset, i));
               //sb.append(getAreaByteLength(b, offset, i));
               sb.append(']');
               sb.append(' ');
            }
         }
         if (BitUtils.hasFlag(options, IByteInterpreter.FLAG_OPTION_2_DATA)) {
            sb.append("Data=");
            for (int areaID = 0; areaID < numAreas; areaID++) {
               sb.append("Area" + areaID + ":");
               int[] vals = toInts(b, offset, areaID);
               for (int i = 0; i < vals.length; i++) {
                  sb.append(vals[i]);
                  if (i != vals.length - 1) {
                     sb.append(',');
                  } else {
                     sb.append(' ');
                  }
               }
               sb.append('\t');
            }
         }
         return sb.toString();
      }
   }

   /**
    * The position for reading the end offset of the area.
    * <br>
    * <br>
    * @param area 0 index
    * @return
    */
   private int getLineAreaEndOffset(int offset, int area) {
      return offset + lineHeaderByteSize + (area * byteSizeAreaOffsets);
   }

   public ByteObject getTech() {
      return tech;
   }

   /**
    * 
    */
   public boolean isContained(byte[] ar, int offset, int area, int value) {
      checkArea(area);
      if (isOffsetTable()) {
         int len = getAreaEndOffsetPatch(ar, offset, area);
         int start = getAreaStartOffsetPatch(ar, offset, area);
         for (int i = start; i < len; i = i + patchByteSize) {
            int head = ar[i];
            int val = ByteArrayStaticUtilz.getValue(ar, i + 1, dataByteSize);
            if (value >= val && value <= val + head) {
               return true;
            }
         }
      } else {
         int readOffset = getAreaStartOffsetNum(ar, offset, area);
         for (int i = 0; i < numDatas; i++) {
            int val = ByteArrayStaticUtilz.getValue(ar, readOffset, dataByteSize);
            if (val == value) {
               return true;
            }
            readOffset += dataByteSize;
         }
      }
      return false;
   }

   /**
    * Removes the value in factor
    * <br>
    * <br>
    * @param b
    * @param rid
    * @param area
    * @return
    */
   public byte[] removeInt(byte[] b, int offset, int area, int rid) {
      int[] id = toInts(b, offset, area);
      if (IntUtils.contains(id, rid, 0, id.length)) {
         int[] ne = cdc.getUC().getIU().remove(id, rid);
         b = clearInt(b, offset, area);
         for (int i = 0; i < ne.length; i++) {
            b = addIntSize(b, offset, area, ne[i]);
         }
      }
      return b;
   }

   public int[] toInts(byte[] data, int offset, int area) {
      checkArea(area);
      if (data[offset] == 0) {
         //no specials
      }
      if (isOffsetTable()) {
         int start = getAreaStartOffsetPatch(data, offset, area);
         int endOffset = getAreaEndOffsetPatch(data, offset, area);

         //read the patches
         if (endOffset != 0) {
            int size = 0;
            for (int i = start; i < endOffset; i = i + patchByteSize) {
               //at b[i] we have the number of elements in the current chunk
               size += data[i];
            }
            int[] ar = new int[size];
            int count = 0;
            for (int i = start; i < endOffset; i = i + patchByteSize) {
               int patchHeader = data[i];
               int val = ByteArrayStaticUtilz.getValue(data, i + 1, dataByteSize);
               for (int j = 0; j < patchHeader; j++) {
                  ar[count] = val + j;
                  count++;
               }
            }
            return ar;
         }
      } else {
         int[] ar = new int[numDatas];
         int readOffset = getAreaStartOffsetNum(data, offset, area);
         for (int i = 0; i < numDatas; i++) {
            ar[i] = ByteArrayStaticUtilz.getValue(data, readOffset, dataByteSize);
            readOffset += dataByteSize;
         }
         return ar;
      }
      return new int[0];
   }

   //#mdebug
   public IDLog toDLog() {
      return toStringGetUCtx().toDLog();
   }

   public String toString() {
      return Dctx.toString(this);
   }

   public void toString(Dctx dc) {
      dc.root(this, "IntAreaDecoder");
      toStringPrivate(dc);
   }

   public String toString1Line() {
      return Dctx.toString1Line(this);
   }

   private void toStringPrivate(Dctx dc) {
      dc.append(" Areas=" + numAreas);
      dc.append(" dataByteSize=" + dataByteSize);
      dc.append(" numDatas=" + numDatas);
   }

   public void toString1Line(Dctx dc) {
      dc.root1Line(this, "IntAreaDecoder");
      toStringPrivate(dc);
   }

   public UCtx toStringGetUCtx() {
      return cdc.getUC();
   }

   //#enddebug
   public String toString(byte[] data) {
      return toString(data, 0);
   }

   public String toString(byte[] data, int offset) {
      Dctx dc = new Dctx(toStringGetUCtx());
      toString(dc, data, offset);
      return dc.toString();
   }

   public void toString(Dctx dc, byte[] data, int offset) {
      dc.append(" Areas=" + numAreas);
      dc.append(" dataByteSize=" + dataByteSize);
      dc.append(" numDatas=" + numDatas);
      dc.nl();
      for (int i = 0; i < numAreas; i++) {
         dc.append(ByteArrayStaticUtilz.getValue(data, getLineAreaEndOffset(offset, i), byteSizeAreaOffsets));
         dc.append(", ");
      }
      dc.nl();
      for (int i = 0; i < numAreas; i++) {
         dc.append("#" + i + " ");
         int[] vals = toInts(data, offset, i);
         if (vals == null || vals.length == 0) {
            dc.append("no values");
         } else {
            dc.debugAlone(vals, ",");
         }
         dc.append(" [" + getAreaStartOffset(data, offset, i));
         dc.append(",");
         dc.append(getAreaEndOffset(data, offset, i));
         dc.append("]");
         dc.append(" Len=" + getAreaByteLength(data, offset, i));
      }
   }

}
