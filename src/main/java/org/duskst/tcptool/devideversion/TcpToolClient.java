package org.duskst.tcptool.devideversion;

import org.duskst.util.FileUtil;
import org.duskst.util.Util;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.Properties;

/**
 * 客户端
 * @date 2017年12月2日
 * @author duskst
 **/
public class TcpToolClient extends MainTool {

    private static final String TOOL_NAME = "TcpClient";

    private Text text_ip;
    private Text text_port;
    private Text text_send;
    private Text text_acct;
    private Text text_timo;
    private Text text_headLength;
    private Combo combo_charSet;
    private Button check_rec;
    private Button output_ctl;
    private Label label_Status;

    public static void main(String[] args) {
        TcpToolClient ttc = new TcpToolClient();
        ttc.open();
    }

    @Override
    public void openInternal() {
        super.open(TOOL_NAME);
    }

    @Override
    public void drawDetailPan(Composite parentWidget) {

        Properties p = getConfig().getProperties();
        String ip = p.getProperty("sendIp");
        String port = p.getProperty("sendPort");
        String timeout = p.getProperty("timeout");
        String reqFileFolder = p.getProperty("reqFileFolder");

        /*
         * 绘制参数设置模块
         */
        int posY_set = 3;
        Group group_setting = createGroupAbs(parentWidget, "", 10, posY_set, 564, 40);
        drawSettingPan(group_setting, ip, port, timeout);

        /*
         * 请求数据区域
         */
        // "设置组件"高度 40 ，留白4
        int posY_send = posY_set + 44;
        Group group_send = createGroupAbs(parentWidget, "发送", 10, posY_send, 564, 365);
        drawSendPan(group_send, reqFileFolder);

        /*
         * 接收数据区域
         */
        Group group_receive = createGroupAbs(parentWidget, "接收", 10, 413, 564, 206);
        text_acct = new Text(group_receive, 2632);
        text_acct.setBounds(10, 20, 544, 176);

        //通讯状态
        label_Status = createLabelAbs(parentWidget, "", 10, 625, 250, 27);

        //发送按钮
        Button button_send = createButtonAbs(parentWidget, "发送", 410, 625, 80, 27);
        button_send.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                text_acct.setText("");
                label_Status.setText("");
                String send = text_send.getText();
                String ip = text_ip.getText();
                String charSet = combo_charSet.getText();
                int port = Integer.parseInt(text_port.getText());
                int timeOut = Integer.parseInt(text_timo.getText());
                try {

                    Socket socket = new Socket(ip, port);
                    socket.setSoTimeout(timeOut * 1000);
                    OutputStream os = socket.getOutputStream();

                    int headLength = 0;
                    try {
                        headLength = Integer.parseInt(text_headLength.getText());
                        int realLength = send.getBytes(charSet).length;
                        if (headLength > 0) {
                            int fixNum = Util.power(10, headLength);
                            send = String.valueOf(fixNum + realLength).substring(1) + send;
                        }
                    } catch (Exception e2) {
                        //异常时长度设为0
                    }
                    os.write(send.getBytes(charSet));
                    os.flush();

                    if (output_ctl.getSelection()) {
                        socket.shutdownOutput();//其实按照长度发的时候不用shutdown也行，但是若发送长度小于头标明的长度，服务端可能会等
                    }

                    label_Status.setText("已发送: " + new Timestamp(System.currentTimeMillis()));

                    InputStream is = socket.getInputStream();
                    if (check_rec.getSelection()) {
                        //TODO 考虑另起线程读
                        String res = FileUtil.readAccordingLengthFixed(is, charSet, OFFSET_DEFAULT, headLength);
                        text_acct.append(res);
                        label_Status.setText("已收到返回: " + new Timestamp(System.currentTimeMillis()));
                    }

                    is.close();
                    os.close();
                    socket.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    showWarningBox("错误", e1.getMessage());
                }
            }
        });

    }

    /**
     * 参数设置模块
     *
     * @param group_setting "设置"模块容器
     * @param ip             发送ip
     * @param port           发送端口
     * @param timeout        超时时间
     */
    private void drawSettingPan(Group group_setting, String ip, String port, String timeout) {
        // 组内的位置是相对父组件的
        int posY_set_lab = 16;
        int posY_set_inp = posY_set_lab - 3;
        createLabelAbs(group_setting, "地址", 10, posY_set_lab, 24, 17);
        text_ip = createTextAbs(group_setting, ip, 40, posY_set_inp, 97, 23);

        createLabelAbs(group_setting, "端口", 147, posY_set_lab, 24, 17);
        text_port = createTextAbs(group_setting, port, 177, posY_set_inp, 55, 23);

        createLabelAbs(group_setting, "超时(s)", 242, posY_set_lab, 40, 17);
        text_timo = createTextAbs(group_setting, timeout, 288, posY_set_inp, 30, 23);

        createLabelAbs(group_setting, "头长", 328, posY_set_lab, 24, 17);
        text_headLength = createTextAbs(group_setting, ORI_HEAD_LENGTH, 358, posY_set_inp, 30, 23);

        createLabelAbs(group_setting, "字符集", 398, posY_set_lab, 36, 17);
        combo_charSet = new Combo(group_setting, 0);
        combo_charSet.setItems("UTF-8", "GBK");
        combo_charSet.setBounds(440, posY_set_inp, 61, 25);
        combo_charSet.select(0);

        check_rec = createCheckAbs(group_setting, "接收", 512, posY_set_lab, 50, 17);
    }

    /**
     * 绘制发送区域组件内容
     * @param group_send 发送区域组件
     * @param reqFileFolder 请求文件目录
     */
    private void drawSendPan(Group group_send, String reqFileFolder) {
        //输出流控制
        output_ctl = createCheckAbs(group_send, "发送后关闭输出流", 212, 0, 100, 17);
        output_ctl.setSelection(true);
        output_ctl.setToolTipText("头长为0时切勿去掉勾选，否则会假死");
        output_ctl.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                //去除勾选需校验头长度
                if (!output_ctl.getSelection()) {
                    int headLength = Integer.parseInt(text_headLength.getText());
                    if (headLength == 0) {
                        showWarningBox("警告", "头长度为0，发送后不关闭输出流会导致工具假死！");
                    }
                }
            }
        });

        //清除发送数据
        Button button_clear = createButtonAbs(group_send, "清除", 452, 0, 50, 19);
        button_clear.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                text_send.setText("");
            }
        });

        // 读取文件
        int posY_send_row_1 = 20;
        createLabelAbs(group_send, "文件", 10, posY_send_row_1, 24, 17);
        final Text text_filePath = createTextAbs(group_send, reqFileFolder, 40, posY_send_row_1, 440, 22);
        Button button_selectFolder = createButtonAbs(group_send, "读取", 488, posY_send_row_1, 70, 22);
        button_selectFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog fd = new FileDialog(group_send.getShell());
                fd.setText("选择文件");
                String inUsePath = text_filePath.getText();
                fd.setFilterPath(inUsePath);
                String selectedFilePath = fd.open();
                if (selectedFilePath != null) {
                    text_filePath.setText(selectedFilePath);

                    String fileEncoding = "UTF-8";
                    try {
                        fileEncoding = FileUtil.getCharset(new File(selectedFilePath));
                    } catch (Exception e1) {
                        showWarningBox("警告", "文件编码获取失败，将使用utf-8编码读取文件！");
                    }
                    String textContent = FileUtil.readResFile(selectedFilePath, fileEncoding);
                    text_send.setText(textContent);
                }
            }
        });

        int posY_send_row_text = posY_send_row_1 + 25;
        text_send = new Text(group_send, 2880);
        text_send.setBounds(10, posY_send_row_text, 544, 315);
    }

}


