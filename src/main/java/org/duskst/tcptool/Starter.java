package org.duskst.tcptool;

import org.duskst.tcptool.devideversion.MainTool;
import org.duskst.tcptool.devideversion.TcpToolClient;
import org.duskst.tcptool.devideversion.TcpToolCombined;
import org.duskst.tcptool.devideversion.TcpToolForward;
import org.duskst.tcptool.devideversion.TcpToolServer;

/**
 * TCP 工具启动类
 * @date 2018/01/17 11:08
 * @author duskst
 **/
public class Starter {

    /**
     * 服务端工具
     */
    private static final String TOOL_TYPE_0 = "0";

    /**
     * 客户端工具
     */
    private static final String TOOL_TYPE_1 = "1";

    /**
     * 集合工具
     */
    private static final String TOOL_TYPE_2 = "2";
    /**
     * 转发工具
     */
    private static final String TOOL_TYPE_3 = "3";

    public static void main(String[] args) {

        String toolType = TOOL_TYPE_1;

        if (args.length > 0 && args[0] != null && !"".equals(args[0])) {
            toolType = args[0];
        }
        MainTool window;
        switch (toolType) {
            case TOOL_TYPE_0:
                window = new TcpToolServer();
                break;
            case TOOL_TYPE_1:
                window = new TcpToolClient();
                break;
            case TOOL_TYPE_2:
                window = new TcpToolCombined();
                break;
            case TOOL_TYPE_3:
                window = new TcpToolForward();
                break;
            default:
                System.out.println("wrong input!");
                return;
        }
        window.open();
    }
}
