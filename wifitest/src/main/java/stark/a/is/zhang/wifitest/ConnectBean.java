package stark.a.is.zhang.wifitest;

/**
 * Created by opple on 18/4/23.
 */

public class ConnectBean {
    private long successTime;
    private int connectCount;
    public long getSuccessTime(){
        return successTime;
    }
    public void setSuccessTime(long successTime){
        this.successTime = successTime;
    }

    public int getConnectCount() {
        return connectCount;
    }

    public void setConnectCount(int connectCount) {
        this.connectCount = connectCount;
    }
}
