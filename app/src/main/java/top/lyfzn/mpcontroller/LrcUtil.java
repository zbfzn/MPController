package top.lyfzn.mpcontroller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LrcUtil {
    /**
     * 此类用来解析LRC文件 将解析完整的LRC文件放入一个LrcInfo对象中 并且返回这个LrcInfo对象s author:java_mzd
     */
    public static class LrcParser {
        private LrcInfo lrcinfo = new LrcInfo();

        private long currentTime = 0;//存放临时时间
        private int currentTime_int=0;
        private long lastLineTime=-1;
        private int lastLineTime_int=-1;
        private String currentContent = null;//存放临时歌词
        private Map<Long, LrcContent> maps = new HashMap<>();//用户保存所有的歌词和时间点信息间的映射关系的Map
        private Map<Integer,LrcContent> maps_int=new HashMap<>();



        /**
         * 根据文件路径，读取文件，返回一个输入流
         *
         * @param path
         *            路径
         * @return 输入流
         * @throws FileNotFoundException
         */
        private InputStream readLrcFile(String path) throws FileNotFoundException {
            File f = new File(path);
            InputStream ins = new FileInputStream(f);
            return ins;
        }

        public LrcInfo parser(String path) throws Exception {
            InputStream in = readLrcFile(path);
            lrcinfo = parser(in);
            return lrcinfo;

        }
        public LrcInfo parserLrc(String lrc){
            String[] lines=lrc.split("\n");
            for(String line:lines){
                parserLine(line);
            }
            lrcinfo.setInfos(maps);
            lrcinfo.setInfos_int(maps_int);
            return lrcinfo;
        }

        /**
         * 将输入流中的信息解析，返回一个LrcInfo对象
         *
         * @param inputStream
         *            输入流
         * @return 解析好的LrcInfo对象
         * @throws IOException
         */
        public LrcInfo parser(InputStream inputStream) throws IOException {
            // 三层包装
            InputStreamReader inr = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inr);
            // 一行一行的读，每读一行，解析一行
            String line = null;
            while ((line = reader.readLine()) != null) {
                parserLine(line);
            }
            // 全部解析完后，设置info
            lrcinfo.setInfos(maps);
            lrcinfo.setInfos_int(maps_int);
            return lrcinfo;
        }

        /**
         * 利用正则表达式解析每行具体语句
         * 并在解析完该语句后，将解析出来的信息设置在LrcInfo对象中
         *
         * @param str
         */
        private void parserLine(String str) {
            // 取得歌曲名信息
            if (str.startsWith("[ti:")) {
                String title = str.substring(4, str.length() - 1);
                System.out.println("title--->" + title);
                lrcinfo.setTitle(title);

            }// 取得歌手信息
            else if (str.startsWith("[ar:")) {
                String singer = str.substring(4, str.length() - 1);
                System.out.println("singer--->" + singer);
                lrcinfo.setSinger(singer);

            }// 取得专辑信息
            else if (str.startsWith("[al:")) {
                String album = str.substring(4, str.length() - 1);
                System.out.println("album--->" + album);
                lrcinfo.setAlbum(album);

            }// 通过正则取得每句歌词信息
            else {
                // 设置正则规则
                String reg = "\\[(\\d{2}:\\d{2}\\.\\d{2})\\]";
                // 编译
                Pattern pattern = Pattern.compile(reg);
                Matcher matcher = pattern.matcher(str);
                    int times=0;
                // 如果存在匹配项，则执行以下操作
                while (matcher.find()&&times<1) {
                    // 得到匹配的所有内容
                    String msg = matcher.group();
                    // 得到这个匹配项开始的索引
                    int start = matcher.start();
                    // 得到这个匹配项结束的索引
                    int end = matcher.end();

                    // 得到这个匹配项中的组数
                    int groupCount = matcher.groupCount();
                    // 得到每个组中内容
                    for (int i = 0; i <= groupCount; i++) {
                        String timeStr = matcher.group(i);
                        if (i == 1) {
                            // 将第二组中的内容设置为当前的一个时间点
                            currentTime = strToLong(timeStr,true);
                            currentTime_int=strToInt(timeStr,true);
                        }
                    }

                    // 得到时间点后的内容
                    String[] content = pattern.split(str);
                    // 输出数组内容
                    for (int i = 0; i < content.length; i++) {
                        if (i == content.length - 1) {
                            // 将内容设置为当前内容
                            currentContent = content[i];
                        }
                    }
                    if(content.length==0){
                        currentContent="";
                    }
                    // 设置时间点和内容的映射
                    if(lastLineTime!=-1){
                        maps.get(lastLineTime).setLength(currentTime-lastLineTime);
                    }
                    if(lastLineTime_int!=-1){
                        maps_int.get(lastLineTime_int).setLength(currentTime-lastLineTime);
                    }
                    maps.put(currentTime, new LrcContent(currentContent,999999999));
                    maps_int.put(currentTime_int, new LrcContent(currentContent,999999999));
                    System.out.println("put---currentTime--->" + currentTime
                            + "----currentContent---->" + currentContent
                            +"-----lastLength-----"+(currentTime-lastLineTime));
                    lastLineTime=currentTime;
                    lastLineTime_int=currentTime_int;

                        times++;
                }

                if(times<1){
                    // 设置正则规则
                     reg = "\\[(\\d{2}:\\d{2}:\\d{2})\\]";
                    // 编译
                     pattern = Pattern.compile(reg);
                     matcher = pattern.matcher(str);
                    // 如果存在匹配项，则执行以下操作
                    while (matcher.find()&&times<1) {
                        // 得到匹配的所有内容
                        String msg = matcher.group();
                        // 得到这个匹配项开始的索引
                        int start = matcher.start();
                        // 得到这个匹配项结束的索引
                        int end = matcher.end();

                        // 得到这个匹配项中的组数
                        int groupCount = matcher.groupCount();
                        // 得到每个组中内容
                        for (int i = 0; i <= groupCount; i++) {
                            String timeStr = matcher.group(i);
                            if (i == 1) {
                                // 将第二组中的内容设置为当前的一个时间点
                                currentTime = strToLong(timeStr,false);
                                currentTime_int=strToInt(timeStr,false);
                            }
                        }

                        // 得到时间点后的内容
                        String[] content = pattern.split(str);
                        // 输出数组内容
                        for (int i = 0; i < content.length; i++) {
                            if (i == content.length - 1) {
                                // 将内容设置为当前内容
                                currentContent = content[i];
                            }
                        }
                        if(content.length==0){
                            currentContent="";
                        }
                        // 设置时间点和内容的映射
                        if(lastLineTime!=-1){
                            maps.get(lastLineTime).setLength(currentTime-lastLineTime);
                        }
                        if(lastLineTime_int!=-1){
                            maps_int.get(lastLineTime_int).setLength(currentTime-lastLineTime);
                        }
                        maps.put(currentTime, new LrcContent(currentContent,999999999));
                        maps_int.put(currentTime_int, new LrcContent(currentContent,999999999));
                        System.out.println("put---currentTime--->" + currentTime
                                + "----currentContent---->" + currentContent
                                +"-----lastLength-----"+(currentTime-lastLineTime));
                        lastLineTime=currentTime;
                        lastLineTime_int=currentTime_int;

                        times++;
                    }
                }
            }
        }

        /**
         * 将解析得到的表示时间的字符转化为Long型
         *
         *
         *            字符形式的时间点
         * @return Long形式的时间
         */
        private long strToLong(String timeStr,boolean standard) {
            // 因为给如的字符串的时间格式为XX:XX.XX,返回的long要求是以毫秒为单位
            // 1:使用：分割 2：使用.分割
            if(standard){
                String[] s = timeStr.split(":");
                int min = Integer.parseInt(s[0]);
                String[] ss = s[1].split("\\.");
                int sec = Integer.parseInt(ss[0]);
                int mill = Integer.parseInt(ss[1]);
                return min * 60 * 1000 + sec * 1000 + mill;
            }else {
                String[] s = timeStr.split(":");
                int min = Integer.parseInt(s[0]);
                int sec = Integer.parseInt(s[1]);
                int mill = Integer.parseInt(s[2]);
                return min * 60 * 1000 + sec * 1000 + mill;
            }

        }
        private int strToInt(String timeStr,boolean standard) {
            if (standard) {
                String[] s = timeStr.split(":");
                int min = Integer.parseInt(s[0]);
                String[] ss = s[1].split("\\.");
                int sec = Integer.parseInt(ss[0]);
                int mill = Integer.parseInt(ss[1]);
                return min * 60 + sec;
            } else {
                String[] s = timeStr.split(":");
                int min = Integer.parseInt(s[0]);
                int sec = Integer.parseInt(s[1]);
                int mill = Integer.parseInt(s[2]);
                return min * 60 + sec;
            }
        }


    }
    /**
     * 用来封装歌词信息的类
     * @author Administrator
     *
     */
    public static class LrcInfo {
        private String title;//歌曲名
        private String singer;//演唱者
        private String album;//专辑
        private Map<Long,LrcContent> infos;//保存歌词信息和时间点一一对应的Map
        private Map<Integer,LrcContent> infos_int;

        public Map<Integer, LrcContent> getInfos_int() {
            return infos_int;
        }

        public void setInfos_int(Map<Integer, LrcContent> infos_int) {
            this.infos_int = infos_int;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSinger() {
            return singer;
        }

        public void setSinger(String singer) {
            this.singer = singer;
        }

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            this.album = album;
        }

        public Map<Long, LrcContent> getInfos() {
            return infos;
        }

        public void setInfos(Map<Long, LrcContent> infos) {
            this.infos = infos;
        }

        //以下为getter()  setter()

    }

    public static class LrcContent {
        private String content;
        private long length;
        public LrcContent(String content,int length){
            this.content=content;
            this.length=length;
        }

        public String getContent() {
            return content;
        }

        public long getLength() {
            return length;
        }

        public void setLength(long length) {
            this.length = length;
        }
    }

}
