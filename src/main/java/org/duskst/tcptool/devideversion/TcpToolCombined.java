package org.duskst.tcptool.devideversion;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 *
 * 集成在一个窗口的工具，由于客户端工具没有多线程，因此发送后若等待返回工具会未响应。
 * @date 2017年12月2日
 * @author duskst
 */
public class TcpToolCombined extends MainTool {

    private static final String TOOL_NAME = "TcpUnion";

    public static void main(String[] args) {
        TcpToolCombined ttc = new TcpToolCombined();
        ttc.open();
    }

    @Override
    public void openInternal() {
        super.open(TOOL_NAME);
    }

    @Override
    public void drawDetailPan(Composite parentWidget) {

        Shell topShell = getTopShell();

        topShell.setLayout(new FillLayout());
        topShell.setSize(610, 725);

        //标签页夹
        CTabFolder tabFolder = new CTabFolder(topShell, SWT.NONE);
        setCTabFolderBackground(tabFolder);

        String fixedStr = "-------------------------";
        //客户端标签页
        CTabItem tabItemClient = new CTabItem(tabFolder, SWT.NONE);
        tabItemClient.setText(fixedStr + "Client" + fixedStr);

        //客户端标签页内部组件容器
        Composite compositeClient = new Composite(tabFolder, SWT.NONE);
        compositeClient.setBackground(COLOR_BG);
        tabItemClient.setControl(compositeClient);
        //绘制客户端
        drawClient(compositeClient);

        //服务端标签页
        CTabItem tabItemServer = new CTabItem(tabFolder, SWT.NONE);
        tabItemServer.setText(fixedStr + "Server" + fixedStr);

        //服务端标签页内部组件容器
        Composite compositeServer = new Composite(tabFolder, SWT.NONE);
        compositeServer.setBackground(COLOR_BG);
        tabItemServer.setControl(compositeServer);
        //绘制服务端
        drawServer(compositeServer);

    }

    /**
     * 客户端
     *
     * @param parenTabItem 页签容器
     */
    private void drawClient(Composite parenTabItem) {
        TcpToolClient client = new TcpToolClient();
        client.drawDetailPan(parenTabItem);
    }

    /**
     * 绘制服务端详细面板
     */
    private void drawServer(Composite parenTabItem) {
        TcpToolServer server = new TcpToolServer();
        // server 关闭UI时需要将监听停掉，所以需要知道顶层组件shell，以便设置监听
        server.setTopShell(getTopShell());
        server.drawDetailPan(parenTabItem);
    }

    /**
     * 设置颜色
     * 参考 http://www.blogjava.net/Javawind/archive/2008/06/06/206397.html
     * @param folder 选项卡
     * @date 2022/03/15 16:54
     **/
    private void setCTabFolderBackground(final CTabFolder folder) {
        Display display = Display.getDefault();

        final Color titleFore = display.getSystemColor(SWT.COLOR_TITLE_FOREGROUND);

        final Color titleBack = display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND);
        final Color titleBackGrad = display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT);

        final Color titleInactiveBackGrad = display.getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT);
        final Color titleInactiveBack = display.getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND);

        Listener listener = e -> {
            switch (e.type) {
                case SWT.Activate:
                    folder.setSelectionForeground(titleFore);
                    folder.setSelectionBackground(new Color[]{titleBack, titleBackGrad}, new int[]{100}, true);
                    break;

                case SWT.Deactivate:
                    folder.setSelectionForeground(titleFore);
                    folder.setSelectionBackground(new Color[]{titleInactiveBack, titleInactiveBackGrad}, new int[]{100}, true);
                    break;
                default:
                    break;
            }
        };
        folder.addListener(SWT.Activate, listener);
        folder.addListener(SWT.Deactivate, listener);
    }
}
