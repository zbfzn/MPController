package top.lyfzn.mpcontroller;

public interface PlayListener {
    boolean complete(Object ob,int position);//播放完成调用
    void paused(Object ob,int position);//播放暂停调用
    void beginPlay(Object ob,int position);//开始播放调用
    void onError(String mediaPlayerErroMes, int what, int extra);//播放遇到错误调用
}
