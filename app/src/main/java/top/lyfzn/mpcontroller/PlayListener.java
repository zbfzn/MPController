package top.lyfzn.mpcontroller;

public interface PlayListener {
    boolean complete(int position);
    void paused(int position);
    void beginPlay(int position);
    void onError(String mediaPlayerErroMes, int what, int extra);
}
