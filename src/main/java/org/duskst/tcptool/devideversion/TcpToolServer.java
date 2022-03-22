package org.duskst.tcptool.devideversion;

import org.duskst.tcptool.HandlerSocket;
import org.duskst.tcptool.SocketStopAbleThread;
import org.duskst.tcptool.XmlFileUtil;
import org.duskst.util.FileUtil;
import org.duskst.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * tcp工具服务端
 *
 * @date 2017年12月2日
 * @author duskst
 **/
public class TcpToolServer extends MainTool {

    private static final String TOOL_NAME = "TcpServer";

    private static final String RE_TIMEOUT = "30";

    private static final int FILELISTHEIGHT = 210;

    private static final String WORKMODE_MANUAL = "1";
    private static final String WORKMODE_SINGLETEST = "2";
    private static final String WORKMODE_AUTOFORWARD = "3";
    private static final String WORKMODE_INTELLIGENCE = "4";

    private int inUsePort;
    private int inUseHeadLength;
    private String inUseCharSet;
    private String inUseForwardIP;
    private String inUseFolderPath;

    private Button output_ctl;

    private Text text_receive;

    private Tree tree;
    private String selectFilePath;
    private TreeItem lastSelected;

    private Label label_Status;

    private Button modeRadio_manual;
    private Button modeRadio_singleTest;
    private Button modeRadio_autoForward;
    private Button modeRadio_intelligence;
    /**
     * 默认手动模式
     */
    private String workMode = WORKMODE_MANUAL;
    /**
     * 是否已手动选择文件-默认为 false
     */
    private boolean hasSelectFile = false;
    /**
     * 需要操作
     */
    private boolean needOperator = true;
    /**
     * 不转发
     */
    private boolean doForward = false;
    /**
     * 默认未做手动操作
     */
    private boolean hasOperated = false;
    /**
     * 默认关闭转发输出流
     */
    private boolean closeOutputFlag = true;

    private Button button_forward;
    private Button button_confirmFile;

    private Label label_listenTag;
    private Text text_listenTag;
    private String listenTagName;

    private Label label_listenValues;
    private Text text_listenValues;
    private List<String> listenTagValuesList;

    private SocketStopAbleThread tcpListener;

    public static void main(String[] args) {
        TcpToolServer tts = new TcpToolServer();
        tts.open();
    }

    @Override
    public void openInternal() {
        super.open(TOOL_NAME);
    }

    /**
     * 绘制详细面板
     */
    @Override
    public void drawDetailPan(Composite parentWidget) {

        Properties p = getConfig().getProperties();
        String port = p.getProperty("listenPort");
        String forwardIp = p.getProperty("forwardIp");
        String propertyFolderPath = p.getProperty("folderPath");

        int posY = 7;
        //参数设置模块
        Group group_setting = createGroupAbs(parentWidget, "参数", 10, posY, 564, 75);
        drawSettingPan(group_setting, port, forwardIp);

        //报文接收显示模块
        Group group_receive = createGroupAbs(parentWidget, "接收", 10, posY += 75 + 2, 564, 277);
        text_receive = new Text(group_receive, 2632);
        text_receive.setBounds(10, 20, 544, 250);

        //目录及模式选择模块
        Group group_folderAndMode = createGroupAbs(parentWidget, "文件列表", 10, posY += 277 + 3, 564, FILELISTHEIGHT + 47);

        createLabelAbs(group_folderAndMode, "目录", 10, 25, 24, 17);
        //当前目录显示框，也可直接粘贴目录，然后打开
        final Text text_folderPath = createTextAbs(group_folderAndMode, propertyFolderPath, 40, 22, 440, 23);
        Button button_selectFolder = createButtonAbs(group_folderAndMode, "浏览", 488, 20, 70, 26);

        //文件列表模块
        //在目录组容器中生成一个填充式的网格布局面板
        final Composite treeParent = new Composite(group_folderAndMode, SWT.NONE);
        treeParent.setLayout(new GridLayout(1, false));
        GridData cpgd = new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1);
        cpgd.heightHint = FILELISTHEIGHT;
        treeParent.setLayoutData(cpgd);
        treeParent.setLocation(10, 45);

        //初始化显示列表
        drawFileListPan(treeParent, propertyFolderPath);

        button_selectFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dd = new DirectoryDialog(TcpToolServer.this.getTopShell());
                dd.setText("选择目录");
                String inusePath = text_folderPath.getText();
                dd.setFilterPath(inusePath);
                String selectedFileFolder = dd.open();
                if (selectedFileFolder != null) {
                    text_folderPath.setText(selectedFileFolder);
                    drawFileListPan(treeParent, selectedFileFolder);
                }
            }
        });

        //状态及操作按钮
        label_Status = createLabelAbs(parentWidget, "", 10, 630, 265, 27);

        button_forward = createButtonAbs(parentWidget, "转发", 330, 625, 80, 27);
        button_forward.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doForward = true;//手动转发，将转发标志置为 true
                hasOperated = true;//已经操作
            }
        });

        button_confirmFile = createButtonAbs(parentWidget, "使用此文件", 430, 625, 80, 27);
        button_confirmFile.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                hasSelectFile = false;
                if (tree.getSelection().length > 0) {
                    Map<String, Object> itemProperty = (Map<String, Object>) tree.getSelection()[0].getData();
                    if (itemProperty != null && ((Boolean) itemProperty.get(KEY_IS_FILE)).booleanValue()) {

                        if (lastSelected != null) {
                            // 切换文件夹重绘文件树时，上次选择的文件组件会disposed
                            if (!lastSelected.isDisposed()) {
                                lastSelected.setForeground(COLOR_UN_SELECTED_TEXT);
                                lastSelected.setBackground(COLOR_UN_SELECTED_BG);
                            }
                        }
                        lastSelected = tree.getSelection()[0];
                        lastSelected.setForeground(COLOR_SELECTED_TEXT);
                        lastSelected.setBackground(COLOR_SELECTED_BG);

                        selectFilePath = (String) itemProperty.get(KEY_PATH);
                        hasSelectFile = true;
                        hasOperated = true;//已操作标志置为 true
                        doForward = false;//选择文件，则将转发标志置为 false

                        if (WORKMODE_SINGLETEST.equals(workMode)) {
                            needOperator = false;//单文件模式，将需要操作标志置为 false -- 这段代码仅对，直接切换到 单报文模式 首次选择文件时有意义
                        }
                    } else {
                        hasOperated = false;
                    }
                }
                if (!hasSelectFile) {
                    showWarningBox("错误", "未选择文件!");
                }
            }
        });

        //启动监听
        startListen();

        this.getTopShell().addShellListener(new ShellAdapter() {
            @Override
            public void shellClosed(ShellEvent arg0) {
                // 直接关闭 UI 界面好像 端口监听线程还在，导致后台进程无法关闭，因此，退出时手动关闭 socket 相关资源
                tcpListener.shutDownSocketAndListenThread();
            }
        });
    }

    /**
     * 启动监听
     */
    private void startListen() {
        final HandlerSocket hs = this::handlerSocket;
        tcpListener = new SocketStopAbleThread(inUsePort, hs, "TcpListenThread");
    }

    /**
     * 重启端口监听
     */
    private void restartListen() {
        tcpListener.shutDownSocketAndListenThread();
        startListen();
    }

    /**
     * socket 处理类
     *
     * @param socket 请求socket
     */
    private void handlerSocket(Socket socket) {
        InputStream input;
        OutputStream output = null;
        try {
            input = socket.getInputStream();

            String inputStr = FileUtil.readAccordingLengthFixed(input, inUseCharSet, OFFSET_DEFAULT, inUseHeadLength);
            if (text_receive.isDisposed()) {
                return;
            }

            final String displayStr = inputStr;
            Display.getDefault().asyncExec(() -> text_receive.setText(displayStr));

            setStatus("已接收,请选文件或转发");

            //needOperator 是模式对应的是否需要操作,不需操作的,可以认为是已经操作过了，两值相反
            hasOperated = !needOperator;

            //此值在 选文件 和 手动转发 两个按钮事件中会修改
            while (!hasOperated) {
                // 等待选择文本
                Thread.sleep(150);
            }

            //智能模式会自动选择文件，因此需再声明一个变量以便修改最终使用的文件而不改变手动选择的文件
            String realFilePath = selectFilePath;
            if (WORKMODE_INTELLIGENCE.equals(workMode)) {
                String listenTagValue = XmlFileUtil.getListenTagValue(displayStr.substring(inUseHeadLength), inUseCharSet, listenTagName);
                if (listenTagValuesList.contains(listenTagValue)) {
                    realFilePath = inUseFolderPath + "/" + listenTagValue + ".xml";
                    doForward = false;
                } else {
                    doForward = true;
                }
            }

            String res;

            //转发
            if (doForward) {
                res = forwardRequest(displayStr);
            } else {
                res = FileUtil.readResFileTrim(realFilePath, inUseCharSet);
                if (inUseHeadLength > 0) {
                    int resLen = res.getBytes(inUseCharSet).length;
                    res = String.valueOf(Util.power(10, inUseHeadLength) + resLen).substring(1) + res;
                }
            }
            byte[] resBuf = res.getBytes(inUseCharSet);

            output = socket.getOutputStream();
            output.write(resBuf);

            setStatus("已返回");
        } catch (Exception e) {
            e.printStackTrace();
            setStatus("文件不存在");
            showWarningBox("错误", e.getMessage());
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 绘制文件列表组件
     */
    private void drawFileListPan(Composite parentWidget, String rootPah) {
        inUseFolderPath = rootPah;
        if (tree != null) {
            tree.dispose();
            parentWidget.getParent().layout();
        }

        tree = createTree(parentWidget);
        tree.setBackground(COLOR_BG);

        TreeItem treeItem = new TreeItem(tree, SWT.NONE);
        //目录名作为根节点
        treeItem.setText(rootPah);

        displayFiles(treeItem, rootPah);
        //默认展开一层
        treeItem.setExpanded(true);

        parentWidget.setSize(550, FILELISTHEIGHT);
        parentWidget.layout();
    }

    /**
     * 参数设置模块
     *
     * @param group_setting "设置"模块容器
     * @param port          监听端口
     * @param forwardIp     转发ip
     */
    private void drawSettingPan(Group group_setting, String port, String forwardIp) {
        int posX = 10, posY = 20;
        createLabelAbs(group_setting, "端口", posX, posY, 24, 17);
        final Text text_port = createTextAbs(group_setting, port, posX += 24 + 6, posY - 3, 55, 23);
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
                showWarningBox("错误", "端口设置错误");
            }
        });

        createLabelAbs(group_setting, "头长", posX += 55 + 6, posY, 24, 17);
        final Text text_headLength = createTextAbs(group_setting, ORI_HEAD_LENGTH, posX += 24 + 6, posY - 3, 25, 23);
        inUseHeadLength = Integer.parseInt(text_headLength.getText());
        text_headLength.addListener(SWT.FocusOut, e -> {
            try {
                inUseHeadLength = Integer.parseInt(text_headLength.getText());
            } catch (Exception e1) {
                e1.printStackTrace();
                showWarningBox("错误", "头长设置错误");
            }
        });

        createLabelAbs(group_setting, "字符集", posX += 25 + 6, posY, 36, 17);
        final Combo combo_CharSet = new Combo(group_setting, 0);
        combo_CharSet.setItems("UTF-8", "GBK");
        combo_CharSet.setBounds(posX += 36 + 6, posY - 4, 58, 25);
        combo_CharSet.select(0);
        inUseCharSet = combo_CharSet.getText();
        combo_CharSet.addListener(SWT.FocusOut, e -> inUseCharSet = combo_CharSet.getText());
        combo_CharSet.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                inUseCharSet = combo_CharSet.getText();
            }
        });

        createLabelAbs(group_setting, "转发ip", posX += 58 + 6, posY, 36, 17);
        final Text text_reIp = createTextAbs(group_setting, forwardIp, posX += 30 + 6, posY - 3, 90, 23);
        inUseForwardIP = text_reIp.getText();
        text_reIp.addListener(SWT.FocusOut, event -> inUseForwardIP = text_reIp.getText());

        output_ctl = new Button(group_setting, SWT.CHECK);
        output_ctl.setBounds(posX += 90 + 6, posY - 6, 115, 27);
        output_ctl.setText("转发后关闭输出流");
        output_ctl.setSelection(true);
        output_ctl.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                closeOutputFlag = output_ctl.getSelection();
            }
        });

        //模式模块
        drawModePan(group_setting);
    }

    /**
     * 转发请求
     *
     * @param inputStr 请求报文字符串
     * @return 响应报文
     */
    private String forwardRequest(String inputStr) {
        String resStr = "";
        int timeOut = Integer.parseInt(RE_TIMEOUT);
        OutputStream os = null;
        InputStream is = null;
        try {
            setStatus("转发中");

            Socket socket = new Socket(inUseForwardIP, inUsePort);
            socket.setSoTimeout(timeOut * 1000);
            os = socket.getOutputStream();

            os.write(inputStr.getBytes(inUseCharSet));
            os.flush();

            if (closeOutputFlag) {
                socket.shutdownOutput();//其实按照长度发的时候不用shutdown也行，但是若发送长度小于头标明的长度，服务端可能会等
            }

            setStatus("已转发");

            is = socket.getInputStream();
            resStr = FileUtil.readAccordingLengthFixed(is, inUseCharSet, OFFSET_DEFAULT, inUseHeadLength);

            setStatus("已收到返回");

            is.close();
            os.close();
            socket.close();
        } catch (Exception e1) {
            e1.printStackTrace();
            showWarningBox("错误", e1.getMessage());
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return resStr;
    }

    /**
     * 绘制模式面板
     *
     * @param group_folder 父容器
     */
    private void drawModePan(Group group_folder) {
        //工作模式
        SelectionAdapter selectionAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (modeRadio_manual.getSelection()) {
                    workMode = WORKMODE_MANUAL;         //-手动模式
                    needOperator = true;                //需要操作
                    output_ctl.setEnabled(true);        //可以设置是否关闭输出
                    button_forward.setEnabled(true);    //可以使用转发按钮
                    button_confirmFile.setEnabled(true);//可以选择文件

                    label_listenTag.setEnabled(false);
                    text_listenTag.setEnabled(false);
                    label_listenValues.setEnabled(false);
                    text_listenValues.setEnabled(false);
                } else if (modeRadio_singleTest.getSelection()) {
                    workMode = WORKMODE_SINGLETEST;     //-单文件模式
                    needOperator = !hasSelectFile;      //切换到单报文模式，根据是否已选择文件设置需要操作标志

                    output_ctl.setEnabled(false);       //不可设置是否关闭输出
                    button_forward.setEnabled(false);   //不可使用转发按钮
                    button_confirmFile.setEnabled(true);//可以选择文件
                    doForward = false;                  //单报文模式不转发

                    label_listenTag.setEnabled(false);
                    text_listenTag.setEnabled(false);
                    label_listenValues.setEnabled(false);
                    text_listenValues.setEnabled(false);
                } else if (modeRadio_autoForward.getSelection()) {
                    workMode = WORKMODE_AUTOFORWARD;    //-自动转发模式
                    needOperator = false;               //不需操作
                    output_ctl.setEnabled(true);        //可以设置是否关闭输出
                    button_forward.setEnabled(false);   //不可使用转发按钮
                    button_confirmFile.setEnabled(false);//不可以选择文件
                    doForward = true;                    //转发模式直接转发

                    label_listenTag.setEnabled(false);
                    text_listenTag.setEnabled(false);
                    label_listenValues.setEnabled(false);
                    text_listenValues.setEnabled(false);
                } else if (modeRadio_intelligence.getSelection()) {
                    workMode = WORKMODE_INTELLIGENCE;   //-智能模式
                    needOperator = false;               //不需操作
                    output_ctl.setEnabled(true);        //可以设置是否关闭输出
                    button_forward.setEnabled(false);   //不可使用转发按钮
                    button_confirmFile.setEnabled(false);//不可以选择文件

                    label_listenTag.setEnabled(true);
                    text_listenTag.setEnabled(true);
                    label_listenValues.setEnabled(true);
                    text_listenValues.setEnabled(true);
                }
            }
        };

        int posX = 10, posY = 47;
        //手动模式-默认-手动选择文件或转发
        modeRadio_manual = createRadioAbs(group_folder, "手动", posX, posY, 40, 20);
        modeRadio_manual.setSelection(true);
        modeRadio_manual.setToolTipText("所有请求手动选择转发或文件返回");
        modeRadio_manual.addSelectionListener(selectionAdapter);

        //单报文模式
        modeRadio_singleTest = createRadioAbs(group_folder, "单报文", posX += 40 + 6, posY, 55, 20);
        modeRadio_singleTest.setToolTipText("所有请求自动使用选择文件返回");
        modeRadio_singleTest.addSelectionListener(selectionAdapter);

        //自动转发模式
        modeRadio_autoForward = createRadioAbs(group_folder, "转发", posX += 55 + 6, posY, 40, 20);
        modeRadio_autoForward.setToolTipText("所有请求自动转发");
        modeRadio_autoForward.addSelectionListener(selectionAdapter);

        //智能模式-自动选择
        modeRadio_intelligence = createRadioAbs(group_folder, "过滤", posX += 40 + 20, posY, 40, 20);
        modeRadio_intelligence.setToolTipText("匹配标签值的报文将自动从'目录'下寻找同名文件返回,支持多值监听，值以','分割");
        modeRadio_intelligence.addSelectionListener(selectionAdapter);

        //监控标签设置
        label_listenTag = createLabelAbs(group_folder, "TAG", posX += 40 + 6, posY, 24, 17);
        text_listenTag = createTextAbs(group_folder, "TRAN_CODE", posX += 24 + 2, posY, 120, 20);
        listenTagName = text_listenTag.getText();
        text_listenTag.addListener(SWT.FocusOut, event -> listenTagName = text_listenTag.getText());
        label_listenTag.setEnabled(false);
        text_listenTag.setEnabled(false);

        //监控标签值
        label_listenValues = createLabelAbs(group_folder, "values", posX += 120 + 6, posY, 36, 17);
        text_listenValues = createTextAbs(group_folder, "", posX += 36 + 2, posY, 140, 20);
        text_listenValues.addListener(SWT.FocusOut, event -> {
            String listenValues = text_listenValues.getText();
            listenTagValuesList = Arrays.asList(listenValues.split(","));
        });
        label_listenValues.setEnabled(false);
        text_listenValues.setEnabled(false);
    }

    private void setStatus(final String message) {
        Display.getDefault().asyncExec(() -> label_Status.setText(message + ": " + new Timestamp(System.currentTimeMillis())));
    }
}
