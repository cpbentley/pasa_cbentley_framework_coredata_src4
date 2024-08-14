package pasa.cbentley.framework.core.data.src4.interfaces;

/**
 * A listener interface for receiving Record Changed/Added/Deleted events from a record store. 
 * 
 * @author Charles-Philip Bentley
 *
 */
public interface IRecordListener {

   public void recordAdded(IRecordStore recordStore, int recordId);

   public void recordChanged(IRecordStore recordStore, int recordId);

   public void recordDeleted(IRecordStore recordStore, int recordId);

}
