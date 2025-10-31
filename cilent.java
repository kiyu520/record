package tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class cilent {
    public static final String targetIP="192.168.43.159";
    public static String targetPath="D:";
    public static String Path="D:\\backroom\\Escape The Backrooms v1.2510.z01";
    public static final int port=3170;
    public static final Logger CilentLog= LoggerFactory.getLogger(cilent.class);
    public static void main(String[] args) {
        try (Socket cilent=new Socket(targetIP,port);){
            CilentLog.info("目标IP地址："+targetIP+"端口号:"+port+"连接成功");
//            Scanner in=new Scanner(System.in);
//            CilentLog.info("输入源文件路径和目标路径:");
//            String path=in.nextLine();
//            String targetPath=in.nextLine();
            long currentTime=System.currentTimeMillis();
            File target=new File(Path);
            CilentLog.info(Path+"--->"+targetPath+"::"+target.getName()+"::"+target.length()/1000+"KB");
            OutputStream outer=cilent.getOutputStream();
            InputStream inner=new FileInputStream(target);
            InputStream servermsg=cilent.getInputStream();
            byte[] buffer=new byte[1024*8];
            int len;
            outer.write((targetPath+"/"+target.getName()).getBytes());
            len=servermsg.read(buffer);
            CilentLog.info("接收服务器确认路径:"+new String(buffer,0,len));
            while((len=inner.read(buffer))!=-1){
                outer.write(buffer,0,len);
            }
            long endTime=System.currentTimeMillis();
            CilentLog.info("用时:"+(endTime-currentTime)/1000+"秒");
            inner.close();
            outer.close();
            cilent.close();
        } catch (IOException e) {
            CilentLog.error("目标IP地址："+targetIP+"端口号:"+port+"连接失败");
        }
    }
}
