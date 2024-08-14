package pasa.cbentley.framework.core.data.src4.db;

import pasa.cbentley.core.src4.logging.IStringable;

/**
 * Interface used for debugging ByteStore content 
 * <br>
 * <br>
 * Tells how to interpret the byte array for displaying it as a string
 * <br>
 * @author Charles Bentley
 *
 */
public interface IByteInterpreter extends IStringable {

   public static final int    FLAG_OPTION_1_HEADER = 1;

   public static final int    FLAG_OPTION_2_DATA   = 2;

   public static final String BAD_FORMAT           = "Bytes not Fitting";

   public static final int    OPTION_ALL           = FLAG_OPTION_2_DATA | FLAG_OPTION_1_HEADER;

   /**
    * Display string may contain image path starting with a [img]/
    * 
    * @param data
    * @param offset where to start reading data
    * @param options
    * @return
    */
   public String getDisplayString(byte[] data, int offset, int options);

}
