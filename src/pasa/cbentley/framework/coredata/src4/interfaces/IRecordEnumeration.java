package pasa.cbentley.framework.coredata.src4.interfaces;

import pasa.cbentley.framework.coredata.src4.ex.StoreException;
import pasa.cbentley.framework.coredata.src4.ex.StoreInvalidIDException;
import pasa.cbentley.framework.coredata.src4.ex.StoreNotOpenException;

public interface IRecordEnumeration {

   /**
    *  Frees internal resources used by this RecordEnumeration.
    */
   public void destroy();

   /**
    * 
    * @return
    */
   public boolean hasNextElement();

   /**
    * Returns true if more elements exist in the previous direction. 
    * @return
    */
   public boolean hasPreviousElement();

   public boolean isKeptUpdated();

   /**
    * Used to set whether the enumeration will be keep its internal index up to date with the record store record additions/deletions/changes. Note that this should be used carefully due to the potential performance problems associated with maintaining the enumeration with every change. 
    * 
    * @param keepUpdated - if true, the enumerator will keep its enumeration current with any changes in the records of the record store.
    *  Use with caution as there are possible performance consequences. Calling keepUpdated(true) has the same effect as calling RecordEnumeration.rebuild: the enumeration will be updated to reflect the current record set. If false the enumeration will not be kept current and may return recordIds for records that have been deleted or miss records that are added later. It may also return records out of order that have been modified after the enumeration was built. Note that any changes to records in the record store are accurately reflected when the record is later retrieved, either directly or through the enumeration. The thing that is risked by setting this parameter false is the filtering and sorting order of the enumeration when records are modified, added, or deleted.
    */
   public void keepUpdated(boolean keepUpdated);

   public byte[] nextRecord() throws StoreInvalidIDException, StoreNotOpenException, StoreException;

   /**
    * 
    * @return
    * @throws StoreInvalidIDException when no more records are available. Subsequent calls to this method will continue to throw this exception until reset() has been called to reset the enumeration.
    */
   public int nextRecordId() throws StoreInvalidIDException;

   /**
    * Returns the number of records available in this enumeration's set. That is, the number of records that have matched the filter criterion. Note that this forces the RecordEnumeration to fully build the enumeration by applying the filter to all records, which may take a non-trivial amount of time if there are a lot of records in the record store. 
    * @throws     StoreInvalidIDException - when no more records are available. Subsequent calls to this method will continue to throw this exception until reset() has been called to reset the enumeration. 
    * @throws StoreNotOpenException - if the record store is not open 
    * @throws StoreException - if a general record store exception occurs
    * @return
    */
   public int numRecords() throws StoreInvalidIDException, StoreNotOpenException, StoreException;

   /**
    * 
    * @return
    * @throws     StoreInvalidIDException - when no more records are available. Subsequent calls to this method will continue to throw this exception until reset() has been called to reset the enumeration. 
    * @throws StoreNotOpenException - if the record store is not open 
    * @throws StoreException - if a general record store exception occurs
    */
   public byte[] previousRecord() throws StoreInvalidIDException, StoreNotOpenException, StoreException;

   /**
    * 
    * @return
    * @throws StoreInvalidIDException
    */
   public int previousRecordId() throws StoreInvalidIDException;

   /**
    * Request that the enumeration be updated to reflect the current record set. Useful for when a MIDlet makes a number of changes to the record store, and then wants an existing RecordEnumeration to enumerate the new changes. 
    */
   public void rebuild();

   /**
    * Returns the enumeration index to the same state as right after the enumeration was created. 
    */
   public void reset();
}
