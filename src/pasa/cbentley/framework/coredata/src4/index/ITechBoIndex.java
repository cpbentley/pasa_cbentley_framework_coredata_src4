package pasa.cbentley.framework.coredata.src4.index;

import pasa.cbentley.byteobjects.src4.tech.ITechByteObject;
import pasa.cbentley.framework.coredata.src4.ctx.IBOTypesCoreData;
import pasa.cbentley.framework.coredata.src4.db.IDataImportExport;

/**
 * 
 * @author Charles-Philip Bentley
 *
 */
public interface ITechBoIndex extends ITechByteObject {

   public static final int INDEX_BASIC_SIZE                = A_OBJECT_BASIC_SIZE + 20;

   /**
    * When using Chains of integers, the null chain is possible with an empty byte array
    * <br>
    * <br>
    * Only valid when area is 0 or 1 <-> Cannot be used in fixed size areas.
    * <br>
    * <br>
    * 
    * When not set, the chainValue for the header is used.
    */
   public static final int INDEX_FLAG_1_NULL_CHAIN         = 1;

   /**
    * Root chain starts specify the number of elements in the chain.
    * <br>
    * <br>
    * 
    */
   public static final int INDEX_FLAG_2_CHAIN_LENGTH       = 2;

   /**
    * Chain index using tail, the first value is the oldest, the chain is followed
    * and a new maillon is attached. It can be expensive to add a lot. So not good for 
    * read few, write often. Actually, a write is as expensive as reading the whole chain.
    * <br>
    * <br>
    * When this flag is not set, the root chain value is move to the chain store
    * and the new value take its place. 
    */
   public static final int INDEX_FLAG_3_TAIL               = 4;

   /**
    * When set, the values in the chain are sorted. This is automatic in chain patches.
    * 2/08/14 Not implemented
    */
   public static final int INDEX_FLAG_4_CHAIN_SORT         = 1 << 2;

   /**
    * Chains use the patch 3-25 for 25-26-27. Automatically sorts the values.
    * <br>
    * <br>
    * Fixed size data for all, means all datas will have a 1 byte header for the patch.
    * Only nice to use with.
    * <br>
    * <br>
    * 
    */
   public static final int INDEX_FLAG_5_CHAIN_PATCH        = 1 << 4;

   /**
    * Set when Index is shadowed
    * Related to {@link IDataImportExport} shadowing
    */
   public static final int INDEX_FLAG_6_SHADOW             = 1 << 5;

   /**
    * Set when a key returns maximum 1 BID.
    */
   public static final int INDEX_FLAG_7_IS_FUNCTION        = 1 << 6;

   public static final int INDEX_OFFSET_01_FLAG            = A_OBJECT_BASIC_SIZE;

   /**
    * Type of Index
    * <li> {@link ITechBoIndex#TYPE_0_PATCH}
    * <li> {@link ITechBoIndex#TYPE_1_CHAIN}
    * 
    */
   public static final int INDEX_OFFSET_02_TYPE1           = A_OBJECT_BASIC_SIZE + 1;

   /**
    * Bytesize consumed by each values
    */
   public static final int INDEX_OFFSET_03_VALUE_BYTESIZE1 = A_OBJECT_BASIC_SIZE + 2;

   /**
    * Bytesize consumed by auxliary. ie. chain pointer. Caps the maximum number of elements.
    * <br>
    * <br>
    * <li>Usually 1 for maximum chain of 255
    * <li>2 for maximum chain of 65 000 elements
    * <li>3
    */
   public static final int INDEX_OFFSET_04_AUX_BYTESIZE1   = A_OBJECT_BASIC_SIZE + 3;

   /**
    * The starting value of the user key domain. Used in the function to transform the BID.
    * <br>
    * <br>
    * Keys used by {@link IBoIndex} are sequential of integers starting from [{@link IBoIndex#getKeyMin()}, {@link IBoIndex#getKeyMax()}]
    * <br>
    * <br>
    * This value can be a negative number. For example -100. The user key -100 is the user minimum key that is returned with
    * {@link MIndexIntToInts#getKeyMin()}. The actual index key used internally is FIRST_RID + indexRIDHeaderSize.
    * <br>
    * <br>
    * When indexing 
    */
   public static final int INDEX_OFFSET_05_REFERENCE_KEY4  = A_OBJECT_BASIC_SIZE + 4;

   /**
    * Stores the maximum normalized key acceptable by the index. user normalized.
    * <br>
    * <br>
    * Upon index creation, this value is 0.
    * <br>
    * <br>
    * 
    * The value is written to disk before any new key is added to the index.
    * <br>
    * <br>
    * In the -100 to 100 example, max key is 200.
    */
   public static final int INDEX_OFFSET_06_KEY_NORM_MAX4   = A_OBJECT_BASIC_SIZE + 8;

   /**
    * Maximum interval between the current maximum key and a new key, after which the an {@link IllegalArgumentException} is thrown.
    * <br>
    * <br>
    * When {@link IBoIndex#addBidToKey(int, int)} key value is too big, it will throw an {@link IllegalArgumentException}
    * <br>
    * This is a safety measure to prevent a wrong key value from creating a huge empty index, possibly crashing the application.
    * <br>
    * <br>
    * 
    */
   public static final int INDEX_OFFSET_07_KEY_REJECT_2    = A_OBJECT_BASIC_SIZE + 12;

   /**
    * Number of areas to be used.
    * <br>
    * <br>
    * When 0, that's equal to 1
    */
   public static final int INDEX_OFFSET_08_NUM_AREAS2      = A_OBJECT_BASIC_SIZE + 14;

   /**
    * What should be done when the value byte size is bigger than currently encoded?
    * <li>{@link ITechBoIndex#OVER_SIZE_0_EXPAND}
    * <li>{@link ITechBoIndex#OVER_SIZE_1_EXCEPTION}
    * <li>{@link ITechBoIndex#OVER_SIZE_2_TRUNCATE}
    * 
    */
   public static final int INDEX_OFFSET_09_OVER_SIZE_TYPE1 = A_OBJECT_BASIC_SIZE + 16;

   /**
    * Type of Accepting Index.
    * <li>{@link IAcceptor#ACC_TYPE_0_INT}
    * <li>{@link IAcceptor#ACC_TYPE_1_ARRAY}
    * <li>{@link IAcceptor#ACC_TYPE_2_BYTEOBJECT}
    * <li>{@link IAcceptor#ACC_TYPE_3_STRING}
    * <li>{@link IAcceptor#ACC_TYPE_4_INT_DATE}
    * 
    */
   public static final int INDEX_OFFSET_10_ACCESSOR1       = A_OBJECT_BASIC_SIZE + 17;

   public static final int INDEX_OFFSET_11_REF_AREA_SHIFT2 = A_OBJECT_BASIC_SIZE + 18;

   /**
    * Type sub type.
    * 
    */
   public static final int INDEX_TYPE                      = IBOTypesCoreData.TYPE_202_INDEX;

   /**
    * Expand the byte size of all values.
    */
   public static final int OVER_SIZE_0_EXPAND              = 0;

   /**
    * Throw an exception
    */
   public static final int OVER_SIZE_1_EXCEPTION           = 1;

   /**
    * Truncate the value to match the byte size
    */
   public static final int OVER_SIZE_2_TRUNCATE            = 2;

   public static final int TYPE_0_PATCH                    = 0;

   /**
    * 
    */
   public static final int TYPE_1_CHAIN                    = 1;

}
