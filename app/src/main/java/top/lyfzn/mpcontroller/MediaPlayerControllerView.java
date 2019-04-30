package top.lyfzn.mpcontroller;
/**
 * Create By zbfzn 2019/4/6
 * Play()、Start()不能同时使用
 */

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.shaohui.bottomdialog.BottomDialog;

import static android.content.ContentValues.TAG;

public class MediaPlayerControllerView extends RelativeLayout{
    public static final int SHUNXU=0,LIEBIAO=1,DANQU=2,RANDOM=3;
    private  int playModel;//0顺序播放,1列表循环,2单曲循环,3随机播放
    private RelativeLayout control;
    private SeekBar progress_seekbar,volume_seekbar;
    private TextView currentPosition,totoalLength,volume_percent,volume_img,startApause,last,next,isLoadingNotice,playModel_tv,play_list_tv,media_tag_tv,lrc_tv;
    private MediaPlayer mediaPlayer;
    private PlayerControl playerControl;
    private List<MediaInfo> playQueue=new ArrayList<>();
    private PlayListener listner;
    private boolean hasListener=false,hasInit=false,isShowToast=false,onErrorAutoNext=false,onPrepared=false,isShowLrc=true,nextPlay=false,isErrorPlay=false;
    private int playPosition=-1,error_playPosition=-1;
    private Context mcontext;
    private AppCompatActivity appCompatActivity;
    private Handler progress,volume,resouce_ready,lrc_h,textColor_h;
    private Runnable progress_r,volume_r,resouce_ready_r,lrc_r,textColor_r;
    private int progress_position=0,volume_position=0,totoal_length=0,max_volume=0,history_volume_percent=50;
    private AudioManager audioManager;
    private Spanned spanned_lrc=Html.fromHtml("");
    private Map<Long, LrcUtil.LrcContent> lrcinfo=new HashMap<>();
    private AttributeSet attrs;
    private String lrcCrossColor="#eeee00";

    public MediaPlayerControllerView(Context context) {
        super(context);
        inflate(context, R.layout.media_player_controller_layout,this);
    }

    public MediaPlayerControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.media_player_controller_layout,this);
        initView();
        mcontext=context;
        hasInit=true;
        this.attrs=attrs;
        setAttrs(mcontext,attrs);
    }
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initAction();
        setChild();
        setControllerVisiblity(false);//控制面板默认不可见
    }

    private void initView(){
        progress_seekbar=findViewById(R.id.progress_seekbar);
        volume_seekbar=findViewById(R.id.seekbar_volume_progress);

        currentPosition=findViewById(R.id.current_position);
        totoalLength=findViewById(R.id.total_length);
        last=findViewById(R.id.last);
        startApause=findViewById(R.id.start_pause);
        next=findViewById(R.id.next);
        volume_img=findViewById(R.id.volume_img);
        volume_percent=findViewById(R.id.volume_percent);
        isLoadingNotice=findViewById(R.id.isloading_notice);
        playModel_tv=findViewById(R.id.play_model);
        play_list_tv=findViewById(R.id.play_list);
        media_tag_tv=findViewById(R.id.media_tag);
        lrc_tv=findViewById(R.id.lrc_show);

        control=findViewById(R.id.control_layout);
    }
    private void setAttrs(Context context,AttributeSet attrs){
        TypedArray arr=context.obtainStyledAttributes(attrs, R.styleable.MediaPlayerControllerView);

        try{
            int color=arr.getColor(R.styleable.MediaPlayerControllerView_controllerBackground,Color.parseColor("#aaaaaa"));
            if(color>=0){
                setControlBackground(color);
            }else{
                setControlBackground(Color.parseColor("#aaaaaa"));
            }
        }catch (Exception e){
            setControlBackground(Color.parseColor("#aaaaaa"));
        }
          try{
              Drawable drawable=arr.getDrawable(R.styleable.MediaPlayerControllerView_controllerBackground);
              if(drawable!=null){
                  setControlBackground(drawable);
              }else {
                  setControlBackground(Color.parseColor("#aaaaaa"));
              }
          }catch (Exception e){
              setControlBackground(Color.parseColor("#aaaaaa"));
          }
        arr.recycle();
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
        margin_bottom=dp2px(65)+dp2px(15)+dp2px(25);
        LayoutParams layoutParamRV=new LayoutParams(width_child,height_child);
        layoutParamRV.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
        layoutParamRV.bottomMargin=margin_bottom;
        view.setLayoutParams(layoutParamRV);

    }

    public void initPlayer(AppCompatActivity appCompatActivity, boolean controllerVisible){
        mediaPlayer=new MediaPlayer();
        audioManager=(AudioManager) mcontext.getSystemService(Context.AUDIO_SERVICE);
        this.appCompatActivity=appCompatActivity;
        setControllerVisiblity(controllerVisible);
        progress=new Handler();
        volume=new Handler();
        resouce_ready=new Handler();
        lrc_h=new Handler();
        progress_r=new Runnable() {
            @Override
            public void run() {
                if(hasInit&&playPosition>=0){
                    if(onPrepared){
                        progress_position=mediaPlayer.getCurrentPosition();
                    }
                    currentPosition.setText(timeFormat(progress_position));
                    if(totoal_length==0){
                        progress_seekbar.setProgress(0);
                    }else {
                        progress_seekbar.setProgress((int)(progress_position*1.0/totoal_length*100));
                    }

                }
                lrc_tv.setVisibility(isShowLrc?VISIBLE:GONE);
                progress.postDelayed(this,0);
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
    public void setIsShowLrc(boolean isshow){
        isShowLrc=isshow;
    }
    public void setChildOnBindViewListener(OnBindPlayerViewListener onBindViewListener){
        onBindViewListener.OnBindView(getChildAt(1));
    }
    public void setControlBackground(int color){
        control.setBackgroundColor(color);
    }
    public void setControlBackground(Drawable background){
        control.setBackground(background);
    }
    public void setMediaTagTextColor(int color){
        media_tag_tv.setTextColor(color);
    }
    public void setLrcCrossColor(String color){
        lrcCrossColor=color;
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
    public void setPlayModel(int playModel){
        this.playModel=playModel%4;
        switch (this.playModel){
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
    public void SeekTo(int toSeconds){
        if(hasInit&playPosition>=0&totoal_length>0){
            int mill=toSeconds*1000;
            int pro=(int)(mill%totoal_length)*100;
            mediaPlayer.seekTo(mill);
        }
    }
    public boolean Destory(){//在OnDestory()中调用
        if(hasInit){
            playPosition=-1;
            playQueue.clear();
            mediaPlayer.stop();
            mediaPlayer=null;
            hasInit=false;
            nextPlay=true;
            progress_position=0;
            totoal_length=0;
            progress_seekbar.setProgress(0);
            volume.removeCallbacks(volume_r);
            progress.removeCallbacks(progress_r);
            resouce_ready.removeCallbacks(resouce_ready_r);
            if(lrc_r!=null){
                lrc_h.removeCallbacks(lrc_r);
            }
            if(textColor_r!=null){
                textColor_h.removeCallbacks(textColor_r);
            }
            lrc_tv.setText("");
            media_tag_tv.setText("");
            return true;
        }
        return false;
    }
    public void ReplaceQueue(List<MediaInfo> newQueue){
        if(hasInit){
            playQueue=newQueue;
            Play(playQueue.get(0));
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
            if(getChildCount()>=2){
                View child=getChildAt(1);
                child.setVisibility(VISIBLE);
            }
        }else{
            control.setVisibility(GONE);
            if(getChildCount()>=2){
                View child=getChildAt(1);
                child.setVisibility(GONE);
            }
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
                    totoal_length=0;
                    onPrepared=false;
                    spanned_lrc=Html.fromHtml("");
                    if(lrc_r!=null) {
                        lrc_h.removeCallbacks(lrc_r);
                    }
                    nextPlay=true;
                    isErrorPlay=false;
                    error_playPosition=-1;
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
                            totoal_length=mediaPlayer.getDuration();
                            totoalLength.setText(timeFormat(totoal_length));
                            showLrc(mediaInfo.getLrc());
                            nextPlay=false;
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
                            boolean isBreak=false;
                            if(hasListener){
                                isBreak=listner.complete(playPosition);
                            }
                            if(!isBreak)
                            playCompleted();
                        }
                    });
                    mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            isErrorPlay=true;
                            error_playPosition=playPosition;
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
                }catch (NullPointerException e){
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
                        if(playPosition>=0&&!isErrorPlay){
                            if(mediaPlayer.isPlaying()){
                                Pause();
                            }else{
                                Start();
                            }
                        } else if(isErrorPlay){
                            startApause.setBackgroundResource(R.drawable.zanting);
                            playerControl.play(playQueue.get(error_playPosition));
                        }
                        else {
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
                        int toPosition=(int)(progress*1.0/100*totoal_length);
                        currentPosition.setText(timeFormat(toPosition));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                            progress.removeCallbacks(progress_r);
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        int progress=seekBar.getProgress();
                        int toPosition=(int)(progress*1.0/100*totoal_length);
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
                    final BottomDialog bottomDialog= BottomDialog.create(appCompatActivity.getSupportFragmentManager())
                            .setLayoutRes(R.layout.dialog_layout)
                            .setDimAmount(0.1f)// Dialog window 背景色深度 范围：0 到 1，默认是0.2f
                            .setCancelOutside(true);     // 点击外部区域是否关闭，默认true
                            bottomDialog.setViewListener(new BottomDialog.ViewListener() {      // 可以进行一些必要对View的操作
                        @Override
                        public void bindView(View v) {
                            final ListView listView=v.findViewById(R.id.list_view);
                            TextView toPlayposition=v.findViewById(R.id.to_playposition);
                            Button bt=v.findViewById(R.id.dialog_cancel);
                            final SearchView searchView=v.findViewById(R.id.search_v);
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
                                        listView.setSelection(playPosition);
                                    }
                                }
                            });
                            searchView.clearFocus();
                            listView.requestFocus();
                            searchView.setSubmitButtonEnabled(true);
                            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                                @Override
                                public boolean onQueryTextSubmit(String query) {
                                    searchView.clearFocus();
                                    listView.requestFocus();
                                    for(int i=0;i<playQueue.size();i++){
                                        MediaInfo tmp=playQueue.get(i);
                                        if(tmp.getTag().contains(query)){
                                            listView.setSelection(tmp.getPosition());
                                            return true;
                                        }
                                    }
                                    toastS("没有相匹配Tag的资源");
                                    return true;
                                }

                                @Override
                                public boolean onQueryTextChange(String newText) {
                                    return false;
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
    private void showLrc(String lrc){
        if(lrc.equals("")){
            spanned_lrc=Html.fromHtml("暂无歌词");
            lrc_tv.setText(spanned_lrc);
        }else{
            try {
                LrcUtil.LrcParser lp = new LrcUtil.LrcParser();
                final LrcUtil.LrcInfo info = lp.parserLrc(lrc);
                final Map<Integer, LrcUtil.LrcContent> lrcinfo_1 = info.getInfos_int();
                if(lrc_r!=null) {
                    lrc_h.removeCallbacks(lrc_r);
                }
                lrc_r=new Runnable() {
                    private String line="";
                    private int position=-1,str_position=0;
                    private long length=999999999;
                    private int startProgress=0;
                    private boolean isNextLine=true;
                    @Override
                    public void run() {
                        int pro=progress_position;
                        int key=pro/1000;
                        LrcUtil.LrcContent lrcC=lrcinfo_1.get(key);
                        if(key!=position){
                            isNextLine=true;
                            str_position=0;
                        }
                        if(lrcC!=null&&isNextLine){
                            position=key;
                            startProgress=pro;
                            line=lrcC.getContent();
                            length=lrcC.getLength();

                            textColor_h=new Handler();
                            textColor_r=new Runnable() {
                                @Override
                                public void run() {
                                    if(nextPlay){
                                        line="";
                                        spanned_lrc=Html.fromHtml("");
                                        lrc_tv.setText(spanned_lrc);
                                        return;
                                    }
                                    double percent=((progress_position-startProgress)*1.0)/(int)length;
                                    if(percent-1.2>0){
                                        percent=1.0;
                                    }
                                    str_position=(int)(line.length()*(percent+0.1));
                                    if(str_position>line.length()){
                                        str_position=line.length();
                                    }
                                    if(length==999999999){
                                        str_position=line.length();
                                    }
                                    System.out.println(startProgress+"----"+progress_position+"---"+length+"---"+line+"----"+(progress_position-startProgress)+"----"+((progress_position-startProgress)*1.0)/(int)length);
                                    try{
                                        lrc_tv.setText(setTextColor(line,str_position));
                                    }catch (Exception e){
                                        lrc_tv.setText("");
                                    }

                                    textColor_h.postDelayed(this,0);
                                }
                            };
                            textColor_h.postDelayed(textColor_r,0);
                            isNextLine=false;
                        }
                            lrc_h.postDelayed(lrc_r,0);
                    }
                };
                lrc_h.postDelayed(lrc_r,0);
            }catch (Exception e){
                spanned_lrc=Html.fromHtml("暂无歌词");
                lrc_tv.setText(spanned_lrc);
            }
        }
    }
    private Spanned setTextColor(String str, int position){
        if(position>str.length()){
            position=str.length();
        }
        if(position<0){
           position=0;
        }
        try {
            String str1 = str.substring(0, position);
            String str2 = str.substring(position, str.length());
            return Html.fromHtml("<font color='"+lrcCrossColor+"'>" + str1 + "</font>" + str2);
        }catch (Exception e){
            return Html.fromHtml(str);
        }
    }
    private boolean isSupportAppcompatActivity(){
        return appCompatActivity!=null;
    }
    private void Stop(){
        if(hasInit){
            mediaPlayer.stop();
            startApause.setBackgroundResource(R.drawable.bofang);
        }
    }
    private void whenQueueEmpity(){
        Destory();
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
        if(playQueue.size()<=0){
            whenQueueEmpity();
            return;
        }
        switch (playModel){
            case 0:
                int position=playPosition+1;
                if(position>playQueue.size()-1){
                    Stop();
                    playPosition=-1;
                    System.out.println("没有下一曲");
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
        if(playQueue.size()<=0){
            whenQueueEmpity();
            return;
        }
        switch (playModel){
            case 0:
                int position=playPosition+1;
                if(!(position>playQueue.size()-1)){
                    playPosition=position;
                    playerControl.play(playQueue.get(playPosition));
                }else {
                    if(playQueue.size()>0){
                        playPosition=0;
                        playerControl.play(playQueue.get(playPosition));
                    }else {
                        playPosition=-1;
                        System.out.println("没有下一曲");
                    }
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
    private void playLast(int start,int end){
        if(playQueue.size()<=0){
            whenQueueEmpity();
            return;
        }
        switch (playModel){
            case 0:
                int position=playPosition-1;
                if(position<0){
                    position=playQueue.size()-1;
                    playPosition=position;
                    playerControl.play(playQueue.get(playPosition));
                }else {
                    playPosition=position;
                    playerControl.play(playQueue.get(playPosition));
                }
                break;
            case 1:
                int position1=(playPosition-1)%playQueue.size();
                if(position1<0){
                    position1=playQueue.size()-1;
                }
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


