package pasa.cbentley.framework.coredata.src4.index;

import pasa.cbentley.byteobjects.src4.core.interfaces.IByteObject;
import pasa.cbentley.framework.coredata.src4.ctx.IBOTypesCoreData;

/**
 * Specifies the {@link IntAreaDecoder} structure that divides integer collecting into areas.
 * <br>
 * <br>
 * General Items to the IntAreaDecoder
 * <li>Specifies if
 * <li>
 * 
 * <li>Line: a line is composed of 1 or more areas. Line may have a header to locate starting area offsets.
 * <li>Area: An area is composed of values or patches.
 * <li>Patch = 2-3 for 2,3,4. Max size of values inside a patch is 255 because a patch is coded on 1 byte.
 * <br>
 * <br>
 * 
 * @author Charles-Philip Bentley
 *
 */
public interface ITechAreaInt extends IByteObject {

   public static final int FLAG_DISPLAY_1_HEADER                 = 1;

   public static final int FLAG_EMPTY_DOWN                       = 64;

   public static final int FLAG_EMPTY_UP                         = 128;

   /**
    * Maximum value for the patch length.
    */
   public static final int PATCH_MAX_HEADER                      = 255;

   /**
    * Sub type of model
    */
   public static final int AREA_TYPE                             = IBOTypesCoreData.TYPE_203_AREA_INDEX;

   public static final int AREA_BASIC_SIZE                       = A_OBJECT_BASIC_SIZE + 8;

   /**
    * When set to 1, 2 bytes are used
    */
   public static final int LINE_FLAG_1_TWO_BYTES                 = 1;

   /**
    * When {@link ITechAreaInt#LINE_FLAG_1_TWO_BYTES} is set to 0
    * <li>0 => three bytes
    * <li>set 4 bytes
    */
   public static final int LINE_FLAG_2_THREE_BYTES               = 2;

   public static final int LINE_FLAG_5_SPECIFIC_DATA_SIZE        = 1 << 4;

   /**
    * When this flag value is zero. All lines have this 1 byte header.
    * <br>
    * <br>
    * These flags tells whether a line is following the settings of the decoder or if it modifies some values.
    * <br>
    * <br>
    * 
    */
   public static final int LINE_OFFSET_01_FLAG                   = 0;

   /**
    * Redefines the data byte size.
    * <br>
    * <br>
    * When only one line has a 4 bytes data, only that line modifies the data byte size.
    */
   public static final int LINE_OFFSET_02_DATA_BYTE_SIZE         = 1;

   /**
    * By default, all line use the same data byte size.
    * <br>
    * <br>
    * 
    * When this flag is set, at least one line has a header specifiying a bigger byte size.
    */
   public static final int MAIN_FLAG_1_DATA_BYTE_SIZE_DIFF       = 1;

   /**
    * When all lines and areas will only hold a single value.
    * <br>
    * This greatly reduces byte header information
    */
   public static final int MAIN_FLAG_2_FUNCTION                  = 2;

   public static final int MAIN_OFFSET_01_FLAG                   = A_OBJECT_BASIC_SIZE;

   /**
    * Used.
    * Otherwise each line decides itself.
    * <br>
    * <br>
    * When used in an Index, linked to {@link ITechBoIndex#INDEX_OFFSET_03_VALUE_BYTESIZE1}
    */
   public static final int MAIN_OFFSET_02_DATA_BYTE_SIZE1        = A_OBJECT_BASIC_SIZE + 1;

   /**
    * Line header size is computed here.
    */
   public static final int MAIN_OFFSET_03_LINE_HEADER_SIZE1      = A_OBJECT_BASIC_SIZE + 2;

   /**
    * The number of areas per lines. 1-based index
    * <br>
    * <br>
    * When used in an Index, linked to {@link ITechBoIndex#INDEX_OFFSET_08_NUM_AREAS2}
    */
   public static final int MAIN_OFFSET_04_NUM_AREAS2             = A_OBJECT_BASIC_SIZE + 3;

   /**
    * Bytesize of offsets to patched areas in the line table of offsets.
    * <br>
    * <br>
    * Thus this value is only used when {@link ITechAreaInt#MAIN_OFFSET_06_NUM_DATA_PER_AREAS2} is 0.
    * When first created, a base value of 1 or 2 is recommended. Decoder will increase to 3 or 4 when needed.
    * <br>
    * <li> 1 means only up to 255 offset value
    * <br>
    * 
    * Maybe re-computed when Line becomes too big.
    * <br>
    * <br>
    */
   public static final int MAIN_OFFSET_05_BYTE_SIZE_AREA_OFFSET1 = A_OBJECT_BASIC_SIZE + 5;

   /**
    * 0 means patches are used and each line has a table of area offsets to locate the start of each areas.
    * <br>
    * <br>
    * 1 or more X means area offsets can be computed since all areas have the exact same number of bytes.
    * 
    * <li> {@link ITechAreaInt#DATA_0_LINE_OFFSET_TABLE}
    * <li> {@link ITechAreaInt#DATA_1_PER_AREA}
    * <li> {@link ITechAreaInt#DATA_2_PER_AREA}
    */
   public static final int MAIN_OFFSET_06_NUM_DATA_PER_AREAS2    = A_OBJECT_BASIC_SIZE + 6;

   /**
    * each line has a table of area offsets to locate the start of each areas.
    */
   public static final int DATA_0_LINE_OFFSET_TABLE              = 0;

   /**
    * Each area has 1 data of {@link ITechAreaInt#MAIN_OFFSET_05_BYTE_SIZE_AREA_OFFSET1} number of bytes
    */
   public static final int DATA_1_PER_AREA                       = 1;

   /**
    * Each area has 2 datas. no need for an offset table
    */
   public static final int DATA_2_PER_AREA                       = 2;

}
