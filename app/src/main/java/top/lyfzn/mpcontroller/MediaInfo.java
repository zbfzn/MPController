package top.lyfzn.mpcontroller;

public class MediaInfo {
    private String url,Tag;
    private int position;


    public MediaInfo(String url,String tag){
        this.url=url;
        this.Tag=tag;
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
