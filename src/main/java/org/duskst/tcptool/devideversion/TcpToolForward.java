package org.duskst.tcptool.devideversion;

import org.duskst.tcptool.HandlerSocket;
import org.duskst.tcptool.SocketStopAbleThread;
import org.duskst.tcptool.TcpToolConfig;
import org.duskst.util.FileUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 转发
 * @date 2017年12月2日
 * @author duskst
 **/
public class TcpToolForward extends MainTool {
    private static final String TOOL_NAME = "TcpForward";
    private final String CONFIG_FILENAME = "TcpToolForwardConfig.properties";

    private int totalHeight = 0;
    private static final int ITEM_HEIGHT = 38, separator = 5;
    private static final int SHELL_WIDTH = 688, SHELL_HEIGHT = 370;
    /**
     * 采用了网格布局，这里设定的控件大小实际是没意义的
     */
    private static final int PLACE_HOLDER_SIZE = 0;

    private final List<ListenItem> listenItemList = new ArrayList<>();
    private static final String[] defaultCharSet = new String[]{"UTF-8", "GBK"};

    private final ListenItem toolListenItem = new ListenItem();
    private final ListenItem.ItemProperty defaultItemProperty = toolListenItem.new ItemProperty();


    public static void main(String[] argv) {
        TcpToolForward ttf = new TcpToolForward();
        ttf.open();
    }

    @Override
    public void openInternal() {
        super.open(TOOL_NAME);
    }

    @Override
    public void drawDetailPan(Composite parentWidget) {

        int posY = -5;
        parentWidget.setSize(SHELL_WIDTH, SHELL_HEIGHT);

        // 头部
        int headHeight = ITEM_HEIGHT - 8;
        Group headGroup = createGroupAbs(parentWidget, "", 5, posY, SHELL_WIDTH - 46, headHeight);
        initHeaderGroup(headGroup);

        // 监听列表框
        int scHeight = SHELL_HEIGHT - 100;
        final Composite cp = addScWidget(parentWidget, 5, posY += headHeight, SHELL_WIDTH - 27, scHeight);
        cp.setLayout(new FillLayout(SWT.VERTICAL));

        //读取配置文件初始化 监听列表
        initListenList(cp);

        // 添加按钮
        Button addButton = createButtonAbs(parentWidget, "添加监听", 50, posY += scHeight + 5, 75, 26);
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addItem(cp);
                cp.forceFocus();
            }
        });

        // 保存配置按钮
        Button savButton = createButtonAbs(parentWidget, "保存配置", 450, posY, 75, 26);
        savButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                StringBuilder configContent = new StringBuilder();
                for (ListenItem listenItem : listenItemList) {
                    configContent.append(listenItem.toString()).append("\n");
                }
                writeConfig(configContent.toString());
            }
        });

        this.getTopShell().addShellListener(new ShellAdapter() {
            @Override
            public void shellClosed(ShellEvent arg0) {
                // 直接关闭 UI 界面 端口监听线程还在，导致后台进程无法关闭，因此，退出时主动关闭 socket 相关资源
                for (ListenItem listenItem : listenItemList) {
                    listenItem.stopListen();
                }
            }
        });
    }

    /**
     * 初始化工具头部标签
     *
     * @param headGroup 控件组
     */
    private void initHeaderGroup(Group headGroup) {
        GridLayout layout = new GridLayout();
        layout.numColumns = 15;
        layout.marginTop = -12;
        layout.makeColumnsEqualWidth = true;
        headGroup.setLayout(layout);

        GridData gdata1 = new GridData(GridData.FILL_HORIZONTAL);
        gdata1.horizontalSpan = 1;
        GridData gdata2 = new GridData(GridData.FILL_HORIZONTAL);
        gdata2.horizontalSpan = 2;
        GridData gdata3 = new GridData(GridData.FILL_HORIZONTAL);
        gdata3.horizontalSpan = 3;

        createLabelCenter(headGroup, "启用", PLACE_HOLDER_SIZE, PLACE_HOLDER_SIZE).setLayoutData(gdata1);
        createLabelCenter(headGroup, "描述", PLACE_HOLDER_SIZE, PLACE_HOLDER_SIZE).setLayoutData(gdata2);
        createLabelCenter(headGroup, "端口", PLACE_HOLDER_SIZE, PLACE_HOLDER_SIZE).setLayoutData(gdata2);
        createLabelCenter(headGroup, "头长", PLACE_HOLDER_SIZE, PLACE_HOLDER_SIZE).setLayoutData(gdata2);
        createLabelCenter(headGroup, "字符集", PLACE_HOLDER_SIZE, PLACE_HOLDER_SIZE).setLayoutData(gdata2);
        createLabelCenter(headGroup, "转发ip", PLACE_HOLDER_SIZE, PLACE_HOLDER_SIZE).setLayoutData(gdata3);
        Label closeLabel = createLabel(headGroup, "关闭流", PLACE_HOLDER_SIZE, PLACE_HOLDER_SIZE);
        closeLabel.setLayoutData(gdata1);
        closeLabel.setToolTipText("转发后关闭输出流");
        createLabelCenter(headGroup, "删除", PLACE_HOLDER_SIZE, PLACE_HOLDER_SIZE).setLayoutData(gdata2);
        headGroup.layout();
    }

    private void initListenList(Composite cp) {
        TcpToolConfig tcpToolConfig = getConfig();
        Properties prp = tcpToolConfig.getProperties();
        Set<Map.Entry<Object, Object>> entries = prp.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            String sysName = (String) entry.getKey();
            String valueStr = (String) entry.getValue();
            String[] valueArray = valueStr.split(",");
            if (valueArray.length != 5) {
                showWarningBox("错误", "配置错误" + sysName + "=" + valueStr);
                continue;
            }
            ListenItem.ItemProperty itemProperty = toolListenItem.new ItemProperty(sysName, valueArray);
            listenItemList.add(new ListenItem(cp, itemProperty));
        }
    }

    private void addItem(Composite cp) {
        listenItemList.add(new ListenItem(cp));
    }

    /**
     * 监听组件实例类
     * @date 2017年12月2日
     * @author duskst
     **/
    private class ListenItem {


        // 监听实例参数对象
        private class ItemProperty {
            private String sysName = "";
            private String port = "18001";
            private String headLength = "6";
            private String charSet = "UTF-8";
            private String forwardIP = "";
            private boolean closeOutput = true;//默认关闭转发输出流

            ItemProperty() {

            }

            ItemProperty(String sysName, String[] values) {
                this.sysName = sysName;
                this.port = values[0];
                this.headLength = values[1];
                this.charSet = values[2];
                this.forwardIP = values[3];
                this.closeOutput = Boolean.parseBoolean(values[4]);
            }
        }

        private static final int RE_TIMEOUT = 30;
        private String sysName;
        private int inUsePort;
        private int inUseHeadLength;
        private String inUseCharSet;
        private String inUseForwardIP;
        private boolean closeOutputFlag = true;//默认关闭转发输出流
        private SocketStopAbleThread tcpListener;

        ListenItem() {

        }

        // 使用默认 监听实例参数对象 构建监听实例
        ListenItem(Composite cp) {
            this(cp, defaultItemProperty);
        }

        // 监听实例构建
        ListenItem(Composite cp, ItemProperty itemProperty) {

            Group tmpItem = createGroup(cp, "", SHELL_WIDTH - 50, ITEM_HEIGHT);

            GridLayout layout = new GridLayout();
            layout.numColumns = 15;
            layout.marginTop = -7;
            layout.horizontalSpacing = 7;
            layout.makeColumnsEqualWidth = true;

            tmpItem.setLayout(layout);

            drawSettingPan(tmpItem, cp, itemProperty);

            tmpItem.layout();

            cp.setSize(SHELL_WIDTH - 48, totalHeight += ITEM_HEIGHT + separator);
            cp.layout();
        }

        /**
         * 参数设置模块
         *
         * @param item         监听组件
         * @param cp           监听列表控件，监听组件的容器
         * @param itemProperty 监听参数
         */
        private void drawSettingPan(final Group item, final Composite cp, ItemProperty itemProperty) {

            GridData gdata1 = new GridData(GridData.FILL_BOTH);
            gdata1.horizontalSpan = 1;
            GridData gdata2 = new GridData(GridData.FILL_BOTH);
            gdata2.horizontalSpan = 2;
            GridData gdata3 = new GridData(GridData.FILL_BOTH);
            gdata3.horizontalSpan = 3;

            //switch
            final Button buttonOpen = createCheck(item, "", PLACE_HOLDER_SIZE, PLACE_HOLDER_SIZE);
            buttonOpen.setLayoutData(gdata1);
            buttonOpen.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (buttonOpen.getSelection()) {
                        restartListen();
                    } else {
                        stopListen();
                    }
                }
            });

            // sysName
            final Text sysNameText = createText(item, itemProperty.sysName, PLACE_HOLDER_SIZE, PLACE_HOLDER_SIZE);
            sysNameText.setLayoutData(gdata2);
            sysName = sysNameText.getText();
            sysNameText.addListener(SWT.FocusOut, e -> sysName = sysNameText.getText());

            // port
            final Text text_port = createText(item, itemProperty.port, PLACE_HOLDER_SIZE, PLACE_HOLDER_SIZE);
            text_port.setLayoutData(gdata2);
            inUsePort = Integer.parseInt(text_port.getText());
            text_port.addListener(SWT.FocusOut, e -> {
                try {
                    int newPort = Integer.parseInt(text_port.getText());
                    if (newPort != inUsePort) {
                        inUsePort = newPort;
                        restartListen();// 更改端口需要重启监听线程
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                    showWarningBoxLocal("错误", "端口设置错误");
                }
            });

            // headLength
            final Text text_headLength = createText(item, itemProperty.headLength, PLACE_HOLDER_SIZE, PLACE_HOLDER_SIZE);
            text_headLength.setLayoutData(gdata2);
            inUseHeadLength = Integer.parseInt(text_headLength.getText());
            text_headLength.addListener(SWT.FocusOut, e -> {
                try {
                    inUseHeadLength = Integer.parseInt(text_headLength.getText());
                } catch (Exception e1) {
                    e1.printStackTrace();
                    showWarningBoxLocal("错误", "头长设置错误");
                }
            });

            // charSet
            final Combo combo_CharSet = new Combo(item, 0);
            combo_CharSet.setItems(defaultCharSet);
            combo_CharSet.setText(itemProperty.charSet);
            combo_CharSet.setLayoutData(gdata2);
            inUseCharSet = combo_CharSet.getText();
            combo_CharSet.addListener(SWT.FocusOut, e -> inUseCharSet = combo_CharSet.getText());
            combo_CharSet.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    inUseCharSet = combo_CharSet.getText();
                }
            });

            // forwardIp
            final Text text_reIp = createText(item, itemProperty.forwardIP, PLACE_HOLDER_SIZE, PLACE_HOLDER_SIZE);
            text_reIp.setLayoutData(gdata3);
            inUseForwardIP = text_reIp.getText();
            text_reIp.addListener(SWT.FocusOut, event -> inUseForwardIP = text_reIp.getText());

            // close output
            final Button output_ctl = createCheck(item, "", PLACE_HOLDER_SIZE, PLACE_HOLDER_SIZE);
            output_ctl.setLayoutData(gdata1);
            output_ctl.setSelection(itemProperty.closeOutput);
            output_ctl.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    closeOutputFlag = output_ctl.getSelection();
                }
            });

            Button delButton = createButton(item, "删除", PLACE_HOLDER_SIZE, PLACE_HOLDER_SIZE);
            delButton.setLayoutData(gdata2);
            final ListenItem listenItem = this;
            delButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    listenItem.stopListen();
                    listenItemList.remove(listenItem);
                    item.dispose();
                    cp.setSize(SHELL_WIDTH - 48, totalHeight -= ITEM_HEIGHT + separator);
                    cp.layout();
                }
            });
        }

        /**
         * 重启端口监听
         */
        private void restartListen() {
            stopListen();
            startListen();
        }

        /**
         * 启动监听
         */
        private void startListen() {
            final HandlerSocket hs = this::handlerSocket;
            tcpListener = new SocketStopAbleThread(inUsePort, hs, "TcpListenThread-" + sysName);
        }

        /**
         * 关闭监听
         */
        private void stopListen() {
            if (tcpListener != null) {
                tcpListener.shutDownSocketAndListenThread();
            }
        }

        /**
         * socket 处理类
         * @param socket 请求socket
         */
        private void handlerSocket(Socket socket) {
            OutputStream output = null;
            try {
                InputStream input = socket.getInputStream();
                String inputStr = FileUtil.readAccordingLengthFixed(input, inUseCharSet, OFFSET_DEFAULT, inUseHeadLength);
                String res = forwardRequest(inputStr);
                byte[] resBuf = res.getBytes(inUseCharSet);

                output = socket.getOutputStream();
                output.write(resBuf);

            } catch (Exception e) {
                e.printStackTrace();
                showWarningBoxLocal("错误", e.getMessage());
            } finally {
                try {
                    if (socket != null) {
                        if (output != null) {
                            output.flush();
                        }
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    showWarningBoxLocal("错误", e.getMessage());
                }
            }
        }

        /**
         * 转发请求
         *
         * @param inputStr 请求报文字符串
         * @return 响应报文
         */
        private String forwardRequest(String inputStr) {
            String resStr = "";
            Socket socket = null;
            try {
                socket = new Socket(inUseForwardIP, inUsePort);
                socket.setSoTimeout(RE_TIMEOUT * 1000);
                OutputStream os = socket.getOutputStream();
                os.write(inputStr.getBytes(inUseCharSet));
                os.flush();
                if (closeOutputFlag) {
                    socket.shutdownOutput();//其实按照长度发的时候不用shutdown也行，但是若发送长度小于头标明的长度，服务端可能会等
                }
                InputStream is = socket.getInputStream();
                resStr = FileUtil.readAccordingLengthFixed(is, inUseCharSet, OFFSET_DEFAULT, inUseHeadLength);
            } catch (Exception e1) {
                e1.printStackTrace();
                showWarningBoxLocal("转发异常", e1.getMessage());
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showWarningBoxLocal("关闭异常", e.getMessage());
                }
            }
            return resStr;
        }

        private void showWarningBoxLocal(String title, String msg) {
            showWarningBox(title + sysName, msg);
        }

        @Override
        public String toString() {
            return this.sysName + "=" + this.inUsePort + "," + this.inUseHeadLength + "," + this.inUseCharSet + "," + this.inUseForwardIP + "," + this.closeOutputFlag;
        }
    }

    @Override
    protected TcpToolConfig getConfig() {
        String configContent = "#name=port,headLength,charSet,forwardIp,shutDownOutput\nServer1=32046,6,UTF-8,192.168.0.1,true\n";
        return new TcpToolConfig(CONFIG_FILENAME, configContent);
    }

    private void writeConfig(String configContent) {
        configContent = "#name=port,headLength,charSet,forwardIp,shutDownOutput\n" + configContent;
        new TcpToolConfig(CONFIG_FILENAME, configContent, true);
    }
}
