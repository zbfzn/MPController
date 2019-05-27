package top.lyfzn.mpcontroller;

public interface PlayListener {
    boolean complete(Object ob,int position);
    void paused(Object ob,int position);
    void beginPlay(Object ob,int position);
    void onError(String mediaPlayerErroMes, int what, int extra);
}
