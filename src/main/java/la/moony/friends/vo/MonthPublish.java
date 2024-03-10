package la.moony.friends.vo;


public class MonthPublish {

    private final String month;
    private final long count;

    public MonthPublish(String month, long count) {
        this.month = month;
        this.count = count;
    }

    public String getMonth() {
        return month;
    }

    public long getCount() {
        return count;
    }
}
