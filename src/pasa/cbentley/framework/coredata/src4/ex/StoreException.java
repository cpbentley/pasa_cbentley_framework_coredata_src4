package pasa.cbentley.framework.coredata.src4.ex;

public class StoreException extends RuntimeException {

   private Throwable cause;

   public StoreException() {

   }

   public StoreException(String message) {
      super(message);
   }

   public StoreException(String message, Throwable cause) {
      super(message);
      this.cause = cause;
   }

   public Throwable getCause() {
      return cause;
   }
}
