package pasa.cbentley.framework.core.data.src4.ex;

public class StoreNotOpenException extends StoreException {
   public StoreNotOpenException() {

   }

   public StoreNotOpenException(String message) {
      super(message);
   }

   public StoreNotOpenException(String message, Throwable cause) {
      super(message, cause);
   }
}
