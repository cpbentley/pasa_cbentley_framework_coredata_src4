package pasa.cbentley.framework.coredata.src4.ex;

public class StoreNotFoundException extends StoreException {
   public StoreNotFoundException() {

   }

   public StoreNotFoundException(String message) {
      super(message);
   }

   public StoreNotFoundException(String message, Throwable cause) {
      super(message, cause);
   }
}
