package pasa.cbentley.framework.coredata.src4.index;

import pasa.cbentley.core.src4.utils.DateUtils;

/**
 * Start at one for root date and goes up
 * @author cbentley
 *
 */
public class KeyFunctionDate {

    public final long _rootdate;
    
    public KeyFunctionDate(long rootdate) {
        _rootdate = rootdate;
    }
    
    /**
     * 1 if the the rootdate key
     * @param afterroot # of days after root date, 0,1,2...
     * @return 1 or more
     */
    public int getKeyDaysAfter(int afterroot) {
        if (afterroot < 0) {
            return 1;
        }
        return afterroot + 1;
    }
    /**
     * 
     * @param date
     * @return > 0
     */
    public int getKey(long date) {
        int daysdiff = DateUtils.getDays(_rootdate, date);
        if (daysdiff < 0) {
            return 1;
        }
        return daysdiff + 1;
    }
    
    public long getDate(int key) {
       return _rootdate + DateUtils.MILLIS_IN_A_DAY * (key-1); 
    }
}
