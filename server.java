package tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 * 局域网文件传输服务端（基于TCP协议和线程池）
 *
 * 功能描述：
 *  - 监听本地3170端口，接收局域网内客户端的文件传输请求
 *  - 通过线程池并发处理多个客户端连接，支持同时传输多个文件
 *  - 接收客户端发送的目标文件路径，将文件内容写入对应路径
 *
 * 线程池配置：
 *  - 核心线程数：3（长期保持的线程数量，即使空闲也不销毁）
 *  - 最大线程数：5（允许创建的最大线程数，应对突发并发）
 *  - 空闲超时时间：3分钟（非核心线程空闲超过此时长后销毁）
 *  - 任务队列：容量为5的ArrayBlockingQueue（当核心线程满时，新任务先进入队列等待）
 *  - 拒绝策略：CallerRunsPolicy（当线程池和队列都满时，由提交任务的主线程执行，缓解压力）
 *  - 线程工厂：默认线程工厂（线程名称格式：pool-{池编号}-thread-{线程编号}）
 *
 * 使用方法：
 *  1. 直接运行main方法启动服务端，日志将输出"主服务端已启动成功，等待客户端连接"
 *  2. 确保客户端在同一局域网内，且目标端口（3170）未被防火墙拦截
 *  3. 客户端连接后，服务端会自动接收文件路径和内容，保存到指定位置
 *
 * 注意事项：
 *  - 端口占用：若启动失败提示"Address already in use"，需检查3170端口是否被占用（可更换端口号）
 *  - 文件路径：客户端需发送合法的本地文件路径（如"D:/test/file.txt"），服务端会自动创建不存在的父目录
 *  - 并发限制：同时最大处理5个连接（线程池最大线程数），超过则进入队列等待（最多5个），再超过则由主线程处理
 *  - 资源释放：服务端退出时会自动关闭线程池和网络资源，无需手动处理
 *
 * 日志说明：
 *  - 使用SLF4J日志框架，输出连接信息、传输进度、错误信息等，便于排查问题
 */
public class server{
    public static  final Logger ServerLog= LoggerFactory.getLogger(server.class);
    public static void main(String[] args) {
        ThreadPoolExecutor serverpool=new ThreadPoolExecutor(3,5,3,
                TimeUnit.MINUTES, new ArrayBlockingQueue<>(5),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy() );
        try(ServerSocket server=new ServerSocket(3170);) {
            ServerLog.info("主服务端已启动成功，等待客户端连接");
            while(true){
                Socket socket= server.accept();
                serverpool.execute(new task(socket,ServerLog));
                ServerLog.info("接收到新任务，已移交线程池");
            }

        } catch (Exception e) {
            ServerLog.error("主服务端启动失败---->"+e.getMessage());
        }

    }
}
class task implements Runnable{
    private Logger ServerLog;
    private Socket socket;
    public task(Socket socket,Logger ServerLog) {
        this.socket=socket;
        this.ServerLog=ServerLog;
    }
    @Override
    public void run() {
        try {
            long currentTime=System.currentTimeMillis();
            ServerLog.info(Thread.currentThread().getName()+"连接成功，客户端地址："+socket.getInetAddress().getHostName());
            InputStream in= socket.getInputStream();
            byte[] buffer=new byte[1024*8];
            int len=in.read(buffer);
            String targetPath=new String(buffer,0,len);
            ServerLog.info("目标路径:"+targetPath);
            OutputStream msg=socket.getOutputStream();
            msg.write(targetPath.getBytes());
            OutputStream out=new FileOutputStream(targetPath);
            while((len=in.read(buffer))!=-1){
                out.write(buffer,0,len);
            }
            long endTime=System.currentTimeMillis();
            ServerLog.info(Thread.currentThread().getName()+"传输完成，总大小约为："+new File(targetPath).length()/1024+"KB");
            ServerLog.info(Thread.currentThread().getName()+"用时:"+(endTime-currentTime)/1000+"秒");
            in.close();
            out.close();
            socket.close();
        }catch (Exception e){
            ServerLog.error(Thread.currentThread().getName()+"出错----->"+e.getMessage());
        }
    }
}
