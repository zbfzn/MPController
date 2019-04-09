package top.lyfzn.mpcontroller;
/**
 * Create By zbfzn 2019/4/6
 * Play()、Start()不能同时使用
 */

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.shaohui.bottomdialog.BottomDialog;

import static android.content.ContentValues.TAG;

public class MediaPlayerControllerView extends RelativeLayout{
public static int playModel;//0顺序播放,1列表循环,2单曲循环,3随机播放
    private RelativeLayout control;
    private SeekBar progress_seekbar,volume_seekbar;
    private TextView currentPosition,totalLength,volume_percent,volume_img,startApause,last,next,isLoadingNotice,playModel_tv,play_list_tv,media_tag_tv;
    private MediaPlayer mediaPlayer;
    private PlayerControl playerControl;
    private List<MediaInfo> playQueue=new ArrayList<>();
    private PlayListener listner;
    private boolean hasListener=false,hasInit=false,isShowToast=false,onErrorAutoNext=false,onPrepared=false;
    private int playPosition=-1;
    private Context mcontext;
    private AppCompatActivity appCompatActivity;
    private Handler progress,volume,resouce_ready;
    private Runnable progress_r,volume_r,resouce_ready_r;
    private int progress_position=0,volume_position=0,total_length=0,max_volume=0,history_volume_percent=50;
    private AudioManager audioManager;

    public MediaPlayerControllerView(Context context) {
        super(context);
        inflate(context, R.layout.media_player_controller_layout,this);
    }

    public MediaPlayerControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.media_player_controller_layout,this);
        mcontext=context;
        hasInit=true;
    }
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
        initAction();
        setChild();
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
        playModel_tv=findViewById(R.id.play_model);
        play_list_tv=findViewById(R.id.play_list);
        media_tag_tv=findViewById(R.id.media_tag);

        control=findViewById(R.id.control_layout);
    }
    private void setChild(){
        if(getChildCount()==2){
            marginChildView(getChildAt(1));
        }
    }
    private int dp2px(int dp){
        DisplayMetrics displayMetrics=new DisplayMetrics();
        ((Activity) mcontext).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return (dp*displayMetrics.densityDpi)/displayMetrics.DENSITY_DEFAULT;
    }
    private void marginChildView(View view){
        int width_child,height_child,control_layout_w,control_layout_h,control_main_w,control_main_h,margin_bottom,width,hight;
        LayoutParams layoutParamV=(LayoutParams)view.getLayoutParams();
        width_child=layoutParamV.width;
        height_child=layoutParamV.height;
        margin_bottom=dp2px(70)+dp2px(15);
        LayoutParams layoutParamRV=new LayoutParams(width_child,height_child);
        layoutParamRV.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
        layoutParamRV.bottomMargin=margin_bottom;
        view.setLayoutParams(layoutParamRV);

    }

    public void initPlayer(AppCompatActivity appCompatActivity,boolean controllerVisible){
        mediaPlayer=new MediaPlayer();
        audioManager=(AudioManager) mcontext.getSystemService(Context.AUDIO_SERVICE);
        this.appCompatActivity=appCompatActivity;
        setControllerVisiblity(controllerVisible);
        progress=new Handler();
        volume=new Handler();
        resouce_ready=new Handler();
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
        resouce_ready_r=new Runnable() {
            @Override
            public void run() {
                if(playQueue.size()==0){
                    resouce_ready.postDelayed(this,0);
                }else {
                    toastS("资源准备就绪");
                    isLoadingNotice.setText("资源准备就绪");
                }
            }
        };
       progress.postDelayed(progress_r,0);
       volume.postDelayed(volume_r,0);
       resouce_ready.postDelayed(resouce_ready_r,0);
    }
    public void addViewToAboveController(int resourceId){
        int count=getChildCount();
        if(resourceId>=0&&hasInit&&count<2){
            View view1= inflate(mcontext,resourceId,null);
            addView(view1);
            marginChildView(view1);
        }
    }
    public void addViewToAboveController(int resourceId, OnBindPlayerViewListener onBindViewListener){
        int count=getChildCount();
        if(resourceId>=0&&onBindViewListener!=null&&hasInit&&count<2){
            View view1= inflate(mcontext,resourceId,null);
            addView(view1);
            marginChildView(view1);
            onBindViewListener.OnBindView(view1);
        }
    }

    public void addViewToAboveController(int resourceId,int widthPx,int heightPx){
        int count=getChildCount();
        if(resourceId>=0&&hasInit&&count<2){
            View view1= inflate(mcontext,resourceId,null);
            LayoutParams layoutParams=new LayoutParams(widthPx,heightPx);
            view1.setLayoutParams(layoutParams);
            addView(view1);
            marginChildView(view1);
        }
    }
    public void addViewToAboveController(int resourceId, int widthPx, int heightPx, OnBindPlayerViewListener onBindViewListener){
        int count=getChildCount();
        if(resourceId>=0&&onBindViewListener!=null&&hasInit&&count<2){
            View view1= inflate(mcontext,resourceId,null);
            LayoutParams layoutParams=new LayoutParams(widthPx,heightPx);
            view1.setLayoutParams(layoutParams);
            addView(view1);
            marginChildView(view1);
            onBindViewListener.OnBindView(view1);
        }
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
                if(mediaInfo.getPosition()<0) {
                    mediaInfo.setPosition(playQueue.size());
                    playQueue.add(mediaInfo);
                }
            }else{
                return false;
            }
            return playerControl.play(mediaInfo);
        }
        return false;
    }
    public void Play(int position){
        if(position>=0&&position<playQueue.size()){
            Play(playQueue.get(position));
        }
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
           playLast(0,playPosition);
        }
    }
    public void Next(){
        if(playPosition>=0){
            playNext(playPosition,playQueue.size()-1);
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
            resouce_ready.removeCallbacks(resouce_ready_r);
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
                        toastS("资源列表为空");
                        isLoadingNotice.setText("资源列表为空");
                        isLoadingNotice.setVisibility(VISIBLE);
                        return false;
                    }
                }
            }

            @Override
            public boolean play(final MediaInfo mediaInfo) {
                try {
                    startApause.setBackgroundResource(R.drawable.bofang);
                    progress_position=0;
                    total_length=0;
                    onPrepared=false;
                    isLoadingNotice.setText("资源加载中...");
                    isLoadingNotice.setVisibility(VISIBLE);
                    media_tag_tv.setVisibility(GONE);
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
                            isLoadingNotice.setText("等待资源加载");
                            media_tag_tv.setVisibility(VISIBLE);
                            media_tag_tv.setText(mediaInfo.getTag());
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
                            playCompleted();
                            if(hasListener){
                                listner.complete(playPosition);
                            }
                        }
                    });
                    mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            String str=(playPosition>=0)?playQueue.get(playPosition).getTag():"";
                            toastS("播放‘"+str+"’错误,error_mesg:"+"("+what+","+extra+")");
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
                        }else {
                            Play();
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
        setListenerChangePlayModel();
        play_list_tv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isSupportAppcompatActivity()&&playQueue.size()>0){
                    final BottomDialog bottomDialog=BottomDialog.create(appCompatActivity.getSupportFragmentManager())
                            .setLayoutRes(R.layout.dialog_layout)
                            .setDimAmount(0.1f)// Dialog window 背景色深度 范围：0 到 1，默认是0.2f
                            .setCancelOutside(true);     // 点击外部区域是否关闭，默认true
                            bottomDialog.setViewListener(new BottomDialog.ViewListener() {      // 可以进行一些必要对View的操作
                        @Override
                        public void bindView(View v) {
                            final ListView listView=v.findViewById(R.id.list_view);
                            TextView toPlayposition=v.findViewById(R.id.to_playposition);
                            Button bt=v.findViewById(R.id.dialog_cancel);
                            List<String> tags=new ArrayList<>();
                            for(int i=0;i<playQueue.size();i++){
                                if(playPosition==i){
                                    tags.add(playQueue.get(i).getTag()+"  (正在播放)");
                                }else{
                                    tags.add(playQueue.get(i).getTag());
                                }
                            }

                            ArrayAdapter adapter=new ArrayAdapter(mcontext,android.R.layout.simple_list_item_1,tags);
                            listView.setAdapter(adapter);

                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    Play(position);
                                    bottomDialog.dismiss();
                                }
                            });
                            bt.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    bottomDialog.dismiss();
                                }
                            });
                            toPlayposition.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if(playPosition>=0) {
                                        listView.smoothScrollToPosition(playPosition);
                                    }
                                }
                            });
                        }
                    });
                    bottomDialog.show();
                }else if(isSupportAppcompatActivity()){
                    toastS("资源列表为空");
                    isLoadingNotice.setText("资源列表为空");
                    isLoadingNotice.setVisibility(VISIBLE);
                }

            }
        });

    }
    private boolean isSupportAppcompatActivity(){
        return appCompatActivity!=null;
    }
    private void Stop(){
        if(hasInit){
            mediaPlayer.stop();
        }
    }
    private void setListenerChangePlayModel(){
        playModel_tv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                playModel=(playModel+1)%4;
                switch (playModel){
                    case 0:
                        playModel_tv.setBackgroundResource(R.drawable.shunxubofang);
                        break;
                    case 1:
                        playModel_tv.setBackgroundResource(R.drawable.xunhuan);
                        break;
                    case 2:
                        playModel_tv.setBackgroundResource(R.drawable.danquxunhuan);
                        break;
                    case 3:
                        playModel_tv.setBackgroundResource(R.drawable.suijibofang);
                        break;
                }
            }
        });
    }
    private void playCompleted(){
        switch (playModel){
            case 0:
                int position=playPosition+1;
                if(position>playQueue.size()-1){
                    Stop();
                }else {
                    playPosition=position;
                    playerControl.play(playQueue.get(playPosition));
                }
                break;
            case 1:
                int position1=(playPosition+1)%playQueue.size();
                    playPosition=position1;
                    playerControl.play(playQueue.get(playPosition));
                break;
            case 2:
                playerControl.play(playQueue.get(playPosition));
                break;
            case 3:
                int position3=(int)(Math.random()*playQueue.size());
                playPosition=position3;
                playerControl.play(playQueue.get(playPosition));
                break;
        }
    }
    private void playNext(int start,int end){
        switch (playModel){
            case 0:
                int position=playPosition+1;
                if(position>playQueue.size()-1){
                    Stop();
                }else {
                    playPosition=position;
                    playerControl.play(playQueue.get(playPosition));
                }
                break;
            case 1:
                int position1=(playPosition+1)%playQueue.size();
                playPosition=position1;
                playerControl.play(playQueue.get(playPosition));
                break;
            case 2:
                playerControl.play(playQueue.get(playPosition));
                break;
            case 3:
                int position3=(int)(Math.random()*(end+1))-start;
                playPosition=position3;
                playerControl.play(playQueue.get(playPosition));
                break;
        }
    }
    private void playLast(int start,int end){
        switch (playModel){
            case 0:
                int position=playPosition-1;
                if(position<0){
                    Stop();
                }else {
                    playPosition=position;
                    playerControl.play(playQueue.get(playPosition));
                }
                break;
            case 1:
                int position1=(playPosition-1)%playQueue.size();
                playPosition=position1;
                playerControl.play(playQueue.get(playPosition));
                break;
            case 2:
                playerControl.play(playQueue.get(playPosition));
                break;
            case 3:
                int position3=(int)(Math.random()*(end+1))-start;
                playPosition=position3;
                playerControl.play(playQueue.get(playPosition));
                break;
        }
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

