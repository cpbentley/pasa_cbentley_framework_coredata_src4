package pasa.cbentley.framework.core.data.src4.db;

import pasa.cbentley.core.src4.io.BADataIS;
import pasa.cbentley.core.src4.io.BADataOS;

/**
 * Objects using record files that export and import data.
 * <br>
 * <br>
 * 
 * @author Charles-Philip Bentley
 *
 */
public interface IDataImportExport {
   /**
    * To be used whem importing within a transaction. The transaction create an empty
    * structure and will delete the old structure at the end, when the import transaction
    * complete successfully.
    * <br>
    * <br>
    * 
    * @param dis
    */
   public void serializeImportAppend(BADataIS dis);

   /**
    * To be used when importing a stand alone structure.
    * <br>
    * <br>
    * Keeps the old data until the import proecude is finish in a shadow file.
    * <br>
    * <br>
    * Delete old store and create a shadow pointer to the shadow file.
    * <br>
    * <br>
    * If the old data is in a shadow file, unshadows the structure. The data is now
    * in the file.
    * <br>
    * <br>
    * 
    * @param dis
    */
   public boolean serializeImportWrite(BADataIS dis);

   /**
    * 
    * @param dos
    */
   public void serializeExport(BADataOS dos);

}
