package pasa.cbentley.framework.coredata.src4.ex;

public class StoreInvalidIDException extends StoreException {
   public StoreInvalidIDException() {

   }

   public StoreInvalidIDException(String message) {
      super(message);
   }

   public StoreInvalidIDException(String message, Throwable cause) {
      super(message, cause);
   }
}
