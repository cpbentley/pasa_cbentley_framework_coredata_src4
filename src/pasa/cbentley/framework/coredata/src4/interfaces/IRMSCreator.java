package pasa.cbentley.framework.coredata.src4.interfaces;

import pasa.cbentley.framework.coredata.src4.ex.StoreException;
import pasa.cbentley.framework.coredata.src4.ex.StoreNotFoundException;

/**
 * Interface to a basic database system storing byte arrays.
 * <br>
 * Also provides feedback methods for managing failures
 * @author Mordan
 *
 */
public interface IRMSCreator {


   /**
    * 
    * @param recordStoreName
    * @throws StoreException
    */
   public void deleteRecordStore(final String recordStoreName) throws StoreException;

   /**
    * Base int value of {@link IByteStore} 
    * @return
    */
   public int getBase();

   /**
    * 
    * @return
    */
   public String[] listRecordStores();

   /**
    * Open (and possibly create) a record store associated with the given MIDlet suite. If this method is called by a MIDlet when the record store is already open by a MIDlet in the MIDlet suite, this method returns a reference to the same RecordStore object.
    * @param recordStoreName the MIDlet suite unique name for the record store, consisting of between one and 32 Unicode characters inclusive.
    * @param createIfNecessary if true, the record store will be created if necessary 
    * @return object for the record store 
    * @throws StoreException - if a record store-related exception occurred. security based 
    * @throws StoreNotFoundException - if the record store could not be found 
    * @throws IllegalArgumentException - if recordStoreName is invalid
    */
   public IRecordStore openRecordStore(String recordStoreName, boolean createIfNecessary) throws StoreException, StoreNotFoundException;
}
