package pasa.cbentley.framework.coredata.src4.db;

import pasa.cbentley.byteobjects.src4.core.interfaces.IByteObject;
import pasa.cbentley.byteobjects.src4.ctx.IBOTypesBOC;

/**
 * Decides the kind of cache implementation to use
 * <br>
 * <br>
 * 
 * @author Charles Bentley
 *
 */
public interface ICacheTech extends IByteObject {

   public static final int CACHE_BASIC_SIZE             = A_OBJECT_BASIC_SIZE + 5;

   public static final int CACHE_FLAG_1_TRANSACTION     = 1;

   /**
    * When set, the {@link IByteCache#getBytes(int)} returns a reference of the array used for caching.
    * <br>
    * <br>
    * This is good for read only use of the cache.
    * <br>
    * <br>
    * When modifying
    */
   public static final int CACHE_FLAG_2_REFERENCE       = 2;

   public static final int OBJECT_TYPE                  = IBOTypesBOC.TYPE_027_CONFIG;

   public static final int CACHE_OFFSET_01_FLAG         = A_OBJECT_BASIC_SIZE + 1;

   public static final int CACHE_OFFSET_06_TYPE1        = A_OBJECT_BASIC_SIZE + 1;

   public static final int CACHE_OFFSET_02_GROW2        = A_OBJECT_BASIC_SIZE + 1;

   /**
    * The starting size of the cache frame.
    */
   public static final int CACHE_OFFSET_03_START_SIZE_4 = A_OBJECT_BASIC_SIZE + 1;

   public static final int CACHE_OFFSET_04_MAX_SIZE_4   = A_OBJECT_BASIC_SIZE + 1;

   public static final int CACHE_OFFSET_05_PASSIVE1     = A_OBJECT_BASIC_SIZE + 1;

   public static final int CACHE_MODE_0_DEFAULT         = 0;

   public static final int CACHE_MODE_1_TRANSACTION     = 1;

}
