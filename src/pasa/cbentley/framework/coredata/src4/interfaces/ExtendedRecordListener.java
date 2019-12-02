package pasa.cbentley.framework.coredata.src4.interfaces;

public interface ExtendedRecordListener extends IRecordListener {

	int RECORD_ADD = 1; 
	
	int RECORD_READ = 2;
	
	int RECORD_CHANGE = 3; 

	int RECORD_DELETE = 4;
	
	int RECORDSTORE_OPEN = 8; 

	int RECORDSTORE_CLOSE = 9; 

	int RECORDSTORE_DELETE = 10; 
	
	void recordEvent(int type, long timestamp, IRecordStore recordStore, int recordId);
  
	void recordStoreEvent(int type, long timestamp, String recordStoreName);	

}
