package top.lyfzn.mpcontroller;
/**
 * Create By zbfzn 2019/4/6
 * Play()、Start()不能同时使用
 */

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class MediaPlayerControllerView extends RelativeLayout{

    private RelativeLayout control;
    private SeekBar progress_seekbar,volume_seekbar;
    private TextView currentPosition,totalLength,volume_percent,volume_img,startApause,last,next,isLoadingNotice;
    private MediaPlayer mediaPlayer;
    private PlayerControl playerControl;
    private List<MediaInfo> playQueue=new ArrayList<>();
    private PlayListener listner;
    private boolean hasListener=false,hasInit=false,isShowToast=false,onErrorAutoNext=false,onPrepared=false;
    private int playPosition=-1;
    private Context mcontext;
    private Handler progress,volume;
    private Runnable progress_r,volume_r;
    private int progress_position=0,volume_position=0,total_length=0,max_volume=0,history_volume_percent=50;
    private AudioManager audioManager;
    public MediaPlayerControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.media_player_controller_layout,this);
        initView();
        initAction();
    }

   private void initView(){
        progress_seekbar=findViewById(R.id.progress_seekbar);
        volume_seekbar=findViewById(R.id.seekbar_volume_progress);

        currentPosition=findViewById(R.id.current_position);
        totalLength=findViewById(R.id.total_length);
        last=findViewById(R.id.last);
        startApause=findViewById(R.id.start_pause);
        next=findViewById(R.id.next);
        volume_img=findViewById(R.id.volume_img);
        volume_percent=findViewById(R.id.volume_percent);
        isLoadingNotice=findViewById(R.id.isloading_notice);

        control=findViewById(R.id.control_layout);
    }

    public void initPlayer(Context mcontext,boolean controllerVisible){
        mediaPlayer=new MediaPlayer();
        audioManager=(AudioManager) mcontext.getSystemService(Context.AUDIO_SERVICE);
        hasInit=true;
        this.mcontext=mcontext;
        setControllerVisiblity(controllerVisible);
        progress=new Handler();
        volume=new Handler();
        progress_r=new Runnable() {
            @Override
            public void run() {
                if(hasInit&&playPosition>=0){
                    if(onPrepared){
                        progress_position=mediaPlayer.getCurrentPosition();
                    }
                    currentPosition.setText(timeFormat(progress_position));
                    if(total_length==0){
                        progress_seekbar.setProgress(0);
                    }else {
                        progress_seekbar.setProgress((int)(progress_position*1.0/total_length*100));
                    }

                }
                progress.postDelayed(this,10);
            }
        };
        volume_r=new Runnable() {
            @Override
            public void run() {
                if(max_volume==0){
                    max_volume=audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    history_volume_percent=audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                }
                int nowVolume=audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int pro=(int)(nowVolume*1.0/max_volume*100);
                volume_percent.setText(volumeProgressFormat(pro));
                volume_seekbar.setProgress(pro);
                volume.postDelayed(this,10);
            }
        };
       progress.postDelayed(progress_r,0);
       volume.postDelayed(volume_r,0);
    }

    public void setIsShowPlayStatusChangeToast(boolean isShow){
        isShowToast=isShow;
    }
    public void setOnErrorAutoNext(boolean autoNext){
        onErrorAutoNext=autoNext;
    }
    public int addToPlayQueue(MediaInfo mediaInfo){
        if(mediaInfo.notNull()){
            mediaInfo.setPosition(playQueue.size());
            playQueue.add(mediaInfo);
            return playQueue.size()-1;
        }
        return -1;
    }
    public void addListToPlayQueue(List<MediaInfo> mediaInfos){
        int count=playQueue.size();
        for(MediaInfo mediaInfo:mediaInfos){
            mediaInfo.setPosition(count);
            playQueue.add(mediaInfo);
            count++;
        }

    }
    public boolean isPlaying(){
        if(hasInit&&playPosition>=0&&onPrepared){
            return mediaPlayer.isPlaying();
        }
        return false;
    }
    public boolean Play(MediaInfo mediaInfo){
        if(hasInit){
            if(mediaInfo.notNull()){
                mediaInfo.setPosition(playQueue.size());
                playQueue.add(mediaInfo);
            }else{
                return false;
            }
            return playerControl.play(mediaInfo);
        }
        return false;
    }
    public boolean Play(){
        if(hasInit){
            playPosition=-1;
            return playerControl.play();
        }
        return false;
    }
    public boolean Start(){
        if(hasInit){
            if(playPosition>=0){
                if(!mediaPlayer.isPlaying()){
                    if(!onPrepared){
                        toastS("资源加载中...");
                        startApause.setBackgroundResource(R.drawable.bofang);
                    }
                    return playerControl.play();
                }
            }
        }
        return false;
    }
    public void Pause(){
        if(hasInit){
            playerControl.pause();
        }
    }
    public void Last(){
        if(playPosition>=0){
            playPosition=(playPosition-1)%playQueue.size();
            playerControl.play(playQueue.get(playPosition));
        }
    }
    public void Next(){
        if(playPosition>=0){
            playPosition=(playPosition+1)%playQueue.size();
            playerControl.play(playQueue.get(playPosition));
        }
    }
    public boolean Destory(){
        if(hasInit){
            playPosition=-1;
            playQueue.clear();
            mediaPlayer.stop();
            mediaPlayer=null;
            hasInit=false;
            volume.removeCallbacks(volume_r);
            progress.removeCallbacks(progress_r);
            return true;
        }
        return false;
    }
    public void ClearQueue(){
        if(hasInit){
            mediaPlayer.stop();
            playQueue.clear();
            playPosition=-1;
        }
    }


    public void setPlayChangeListenser(PlayListener listener){
        if(listener!=null){
            this.listner=listener;
            hasListener=true;
        }
    }
    public int getNowPlayPosition(){
        return playPosition;
    }
    public List<MediaInfo> getPlayQueue(){
        List<MediaInfo> queue = new ArrayList<>(playQueue);
        return queue;
    }


    private void setControllerVisiblity(boolean visiblity){
        if(visiblity){
            control.setVisibility(VISIBLE);
            playerControl.startApause();
            playerControl.last();
            playerControl.next();
            playerControl.volumeControl();
            playerControl.progressControl();
            playerControl.startApause();
        }else{
            control.setVisibility(GONE);
        }

    }
   private void initAction() {
        playerControl=new PlayerControl() {
            @Override
            public boolean play() {
                if(playPosition>=0){
                    if(!mediaPlayer.isPlaying()&&onPrepared) {
                        mediaPlayer.start();
                        startApause.setBackgroundResource(R.drawable.zanting);
                        if(hasListener){
                            listner.beginPlay(playPosition);
                        }
                    }
                    return true;
                }else {
                    if (playQueue.size() > 0) {
                        return play(playQueue.get(0));
                    } else {
                        return false;
                    }
                }
            }

            @Override
            public boolean play(MediaInfo mediaInfo) {
                try {
                    startApause.setBackgroundResource(R.drawable.bofang);
                    progress_position=0;
                    total_length=0;
                    onPrepared=false;
                    isLoadingNotice.setVisibility(VISIBLE);
                    playPosition=mediaInfo.getPosition();
                    mediaPlayer.stop();
                    mediaPlayer=new MediaPlayer();
                    mediaPlayer.setDataSource(mediaInfo.getUrl());
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mediaPlayer.prepareAsync();
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mediaPlayer.start();
                            onPrepared=true;
                            isLoadingNotice.setVisibility(GONE);
                            total_length=mediaPlayer.getDuration();
                            totalLength.setText(timeFormat(total_length));
                            startApause.setBackgroundResource(R.drawable.zanting);
                            toastS("缓冲完成，即将播放");
                            if(hasListener){
                                listner.beginPlay(playPosition);
                            }
                        }
                    });
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            playPosition=(playPosition+1)%playQueue.size();
                            play(playQueue.get(playPosition));
                            if(hasListener){
                                listner.complete(playPosition);
                            }
                        }
                    });
                    mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            toastS("播放错误,error_mesg:"+"("+what+","+extra+")");
                            startApause.setBackgroundResource(R.drawable.bofang);
                            Log.d(TAG, "OnError - Error code: " + what + " Extra code: " + extra);
                            switch (what) {
                                case -1004:
                                    Log.d(TAG, "MEDIA_ERROR_IO");
                                    break;
                                case -1007:
                                    Log.d(TAG, "MEDIA_ERROR_MALFORMED");
                                    break;
                                case 200:
                                    Log.d(TAG, "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
                                    break;
                                case 100:
                                    Log.d(TAG, "MEDIA_ERROR_SERVER_DIED");
                                    break;
                                case -110:
                                    Log.d(TAG, "MEDIA_ERROR_TIMED_OUT");
                                    break;
                                case 1:
                                    Log.d(TAG, "MEDIA_ERROR_UNKNOWN");
                                    break;
                                case -1010:
                                    Log.d(TAG, "MEDIA_ERROR_UNSUPPORTED");
                                    break;
                            }
                            switch (extra) {
                                case 800:
                                    Log.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING");
                                    break;
                                case 702:
                                    Log.d(TAG, "MEDIA_INFO_BUFFERING_END");
                                    break;
                                case 701:
                                    Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE");
                                    break;
                                case 802:
                                    Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE");
                                    break;
                                case 801:
                                    Log.d(TAG, "MEDIA_INFO_NOT_SEEKABLE");
                                    break;
                                case 1:
                                    Log.d(TAG, "MEDIA_INFO_UNKNOWN");
                                    break;
                                case 3:
                                    Log.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START");
                                    break;
                                case 700:
                                    Log.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING");
                                    break;
                            }
                            if(hasListener){
                                listner.onError("Loading Resouce Error",what,extra);
                            }
                            return !onErrorAutoNext;
                        }
                    });
                    toastS("缓冲中...");
                }catch (IOException e){
                    return false;
                }
                return true;
            }

            @Override
            public void pause() {
                if(playPosition>=0){
                    if(mediaPlayer.isPlaying()){
                        mediaPlayer.pause();
                        startApause.setBackgroundResource(R.drawable.bofang);
                        if(hasListener){
                            listner.paused(playPosition);
                        }
                    }
                }

            }

            @Override
            public void startApause() {
                startApause.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(playPosition>=0){
                            if(mediaPlayer.isPlaying()){
                                Pause();
                            }else{
                                Start();
                            }
                        }

                    }
                });
            }

            @Override
            public void last() {
                last.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                       Last();
                    }
                });

            }

            @Override
            public void next() {
                next.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                       Next();
                    }
                });
            }

            @Override
            public void volumeControl() {
                volume_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if(progress==0){
                            volume_img.setBackgroundResource(R.drawable.jingyin);
                        }else{
                            volume_img.setBackgroundResource(R.drawable.shengyin);
                        }

                        volume_percent.setText(volumeProgressFormat(progress));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        volume.removeCallbacks(volume_r);
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,(int)(seekBar.getProgress()*1.0/100*max_volume),0);
                        volume.postDelayed(volume_r,0);
                    }
                });
            }

            @Override
            public void progressControl() {
                progress_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        int toPosition=(int)(progress*1.0/100*total_length);
                        currentPosition.setText(timeFormat(toPosition));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                            progress.removeCallbacks(progress_r);
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        int progress=seekBar.getProgress();
                        int toPosition=(int)(progress*1.0/100*total_length);
                        if(playPosition>=0){
                            mediaPlayer.seekTo(toPosition);
                        }
                        MediaPlayerControllerView.this.progress.postDelayed(progress_r,0);
                    }
                });
            }
        };

        control.setVisibility(GONE);
        volume_img.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)==0){
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,history_volume_percent,0);
                }else{
                    history_volume_percent=audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0,0);
                }
            }
        });

    }
    private String timeFormat(int media_time){
        int seconds=media_time/1000;
        StringBuilder minute=new StringBuilder(""+seconds/60);
        StringBuilder second=new StringBuilder(""+seconds%60);
        if(minute.length()<=1){
            minute=new StringBuilder("0"+minute.toString());
        }
        if(second.length()<=1){
            second=new StringBuilder("0"+second.toString());
        }
        String s=minute+":"+second;
        return s;
    }
    private String volumeProgressFormat(int progress){
        return progress+"%";
    }

    private void toastS(String mesg){
        if(isShowToast)Toast.makeText(mcontext, mesg, Toast.LENGTH_SHORT).show();
    }
    private void toastL(String mesg){
        if(isShowToast)Toast.makeText(mcontext, mesg, Toast.LENGTH_LONG).show();
    }

}


interface PlayerControl{
   boolean play();
    boolean play(MediaInfo mediaInfo);
    void pause();
    void startApause();
    void last();
    void next();
    void volumeControl();
    void progressControl();
}

