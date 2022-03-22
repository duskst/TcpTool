package org.duskst.tcptool;

import java.net.Socket;

/**
 * socket客户端处理接口
 *
 * @author duskst
 * <p>
 *   Created on 2017年12月2日 下午8:06:55
 * </p>
 */
public interface HandlerSocket {
    /**
     * 处理方法
     * @param client 请求socket
     **/
    void process(Socket client);
}
