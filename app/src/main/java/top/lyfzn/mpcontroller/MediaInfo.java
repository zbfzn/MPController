package top.lyfzn.mpcontroller;

public class MediaInfo {
    private String url,Tag,lrc="";
    private int position=-1;


    public MediaInfo(String url,String tag){
        this.url=url;
        this.Tag=tag;
    }
    public MediaInfo(String url,String tag,String lrc){
        this.url=url;
        this.Tag=tag;
        if(lrc!=null){
            this.lrc=lrc;
        }
    }
    public String getUrl() {
        return url;
    }
    public String getTag() {
        return Tag;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getLrc() {
        return lrc;
    }

    public void setLrc(String lrc) {
        if(lrc==null){
            return;
        }
        this.lrc = lrc;
    }

    public  boolean notNull(){
        if(this.Tag==null){
            return false;
        } if(this.url==null){
            return false;
        }
        return true;
    }
    public boolean equalse(MediaInfo mediaInfo){
        if(!mediaInfo.url.equals(mediaInfo.url)){
            return false;
        }
        if(!mediaInfo.Tag.equals(mediaInfo.Tag)){
            return false;
        }
        return true;
    }
}
