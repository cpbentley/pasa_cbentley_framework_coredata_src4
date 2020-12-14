package pasa.cbentley.framework.coredata.src4.index;

import pasa.cbentley.framework.coredata.src4.db.IByteInterpreter;

/**
 * Reads and Stores Integers into a byte array divided into column areas.
 * <br>
 * <br>
 * 
 * @author Charles-Philip Bentley
 *
 */
public interface IByteInteger extends IByteInterpreter {

   /**
    * Returns all the values stored in this line specified area.
    * <br>
    * <br>
    * 
    * @param line
    * @param offset
    * @param area column area. starts at 0.
    * @return
    * @throws throw new IllegalArgumentException() when area is not valid
    */
   public int[] toInts(byte[] line, int offset, int area);

   /**
    * True when value is stored in area. Header of whole starts at offset.
    * <br>
    * <br>
    * 
    * @param line
    * @param offset
    * @param area 0 index
    * @param value
    * @return
    * @throws throw new IllegalArgumentException() when area is not valid
    */
   public boolean isContained(byte[] line, int offset, int area, int value);

   /**
    * Adds the value into the area. The way is the value is stored relative to existing
    * values inside this area is implementation specific.
    * <br>
    * @param line Line data.
    * @param offset the offset in the byte array
    * @param area 0 based index
    * @param value the value to be added
    * @return the byte array with the value added
    * @throws throw new IllegalArgumentException() when area is not valid
    */
   public byte[] addIntToBytes(byte[] line, int offset, int area, int value);

   /**
    * Removes all integers
    * 
    * @param line
    * @param offset
    * @param area
    * @return
    */
   public byte[] clearInt(byte[] line, int offset, int area);

   /**
    * 
    * @param line
    * @param offset
    * @param area
    * @param value
    * @return
    */
   public byte[] removeInt(byte[] line, int offset, int area, int value);

   /**
    * Creates an empty Line with no values.
    * <br>
    * <br>
    * 
    * @return
    */
   public byte[] createLine();

}
