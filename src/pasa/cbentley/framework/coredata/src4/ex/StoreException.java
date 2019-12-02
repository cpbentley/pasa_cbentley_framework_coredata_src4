package pasa.cbentley.framework.coredata.src4.ex;


public class StoreException extends RuntimeException {
   public StoreException() {

   }

   public StoreException(String message) {
      super(message);
   }

   public StoreException(String message, Throwable cause) {
      super(message, cause);
   }
}
