package org.duskst.tcptool.devideversion;

import lombok.Getter;
import lombok.Setter;
import org.duskst.tcptool.TcpToolConfig;
import org.duskst.util.FileUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 绘制抽象类
 * @date 2017年12月2日
 * @author duskst
 **/
public abstract class MainTool {

    /**
     * 报文默认偏移
     */
    protected static final int OFFSET_DEFAULT = 0;
    /**
     * 默认报文头长
     */
    protected static final String ORI_HEAD_LENGTH = "0";

    @Getter
    @Setter
    private Shell topShell;

    /**
     * 文件夹颜色
     */
    protected static final Color COLOR_FOLDER = new Color(Display.getDefault(), 67, 198, 241);
    /**
     * 选择文件文本颜色
     */
    protected static final Color COLOR_SELECTED_TEXT = new Color(Display.getDefault(), 255, 255, 0);
    /**
     * 选择文件背景颜色
     */
    protected static final Color COLOR_SELECTED_BG = new Color(Display.getDefault(), 4, 4, 57);
    /**
     * 未选择文件文本颜色
     */
    protected static final Color COLOR_UN_SELECTED_TEXT = new Color(Display.getDefault(), 0, 0, 0);
    /**
     * 未选择文件背景颜色
     */
    protected static final Color COLOR_UN_SELECTED_BG = new Color(Display.getDefault(), 240, 238, 238);
    /**
     * 容器背景颜色
     */
    protected static final Color COLOR_BG = new Color(Display.getDefault(), 240, 238, 238);

    protected static final String KEY_PATH = "filePath";
    protected static final String KEY_IS_FILE = "isFile";
    /**
     * 配置文件名及默认路径
     */
    private static final String CONFIG_FILENAME = "TcpToolConfig.properties";
    private static final String FOLDER_DEFAULT = "E:/TcpTool/ResXml/router";
    private static final String REQ_FOLDER_DEFAULT = "E:/TcpTool/ReqXml";

    public void open() {
        openInternal();
    }

    /**
     * 启动工具
     */
    public abstract void openInternal();

    /**
     * 图形工具打开方法
     * @param title 标题
     */
    protected void open(String title) {

        Display display = Display.getDefault();
        int sysHeight = display.getClientArea().height;
        int sysWidth = display.getClientArea().width;
        int width = 600;
        int height = 700;

        createContents(title, width, height, sysWidth, sysHeight);

        this.topShell.open();
        this.topShell.layout();
        while (!this.topShell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    /**
     * 创建面板主方法
     * @param title 标题
     * @param width 默认宽度
     * @param height 默认高度
     * @param sysWidth 显示器宽度
     * @param sysHeight 显示器高度
     */
    private void createContents(String title, int width, int height, int sysWidth, int sysHeight) {
        this.topShell = new Shell();
        this.topShell.setSize(width, height);
        this.topShell.setText(title);
        this.topShell.setLocation(sysWidth / 2 - width / 2, sysHeight / 2 - height / 2);

        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("META-INF/icon/" + title + ".png");
        Image img = new Image(topShell.getDisplay(), resourceAsStream);
        topShell.setImage(img);

        drawDetailPan(topShell);
    }

    /**
     * 面板详情绘制
     */
    public abstract void drawDetailPan(Composite parentWidget);

    /**
     * 创建默认配置文件，并读取配置（如果已经存在配置文件则直接读取）
     * @return Config
     */
    protected TcpToolConfig getConfig() {
        String configContent = "####client###\n" + "sendIp=127.0.0.1\n" + "sendPort=18001\n" + "timeout=30\n" + "reqFileFolder=" + REQ_FOLDER_DEFAULT + "\n\n" + "####server###\n" + "listenPort=32046\n" + "forwardIp=192.168.0.1\n" + "folderPath=" + FOLDER_DEFAULT + "\n";
        return new TcpToolConfig(CONFIG_FILENAME, configContent);
    }

    /**
     * 错误提示
     * @param tile 提示框标题
     * @param message 提示信息
     */
    protected void showWarningBox(final String tile, final String message) {
        Display.getDefault().asyncExec(() -> {
            MessageBox mBox = new MessageBox(topShell, SWT.ARROW_UP);
            mBox.setText(tile);
            mBox.setMessage(message);
            mBox.open();
        });
    }

    /**
     * server 显示文件列表
     * @param parentWidget 显示容器
     * @param folderPath 目录路径
     */
    public int displayFiles(TreeItem parentWidget, String folderPath) {
        int height = 0;
        File fileFolder = new File(folderPath);
        if (!fileFolder.exists() || !fileFolder.isDirectory()) {
            System.out.println("file/folder not exist or is not folder!" + fileFolder.getPath());
            return height;
        }

        List<File> files = new ArrayList<>();

        File[] listFiles = fileFolder.listFiles();
        if (listFiles != null && listFiles.length > 0) {
            files = Arrays.asList(listFiles);
        }
        FileUtil.orderFilesByName(files);

        for (File tmpFile : files) {

            Map<String, Object> itemProperty = new HashMap<>(16);
            itemProperty.put(KEY_PATH, tmpFile.getPath());

            if (tmpFile.isDirectory()) {
                //某些特殊文件两个分支都是 false 所以不能在外部创建节点
                TreeItem treeItem = createTreeItem(parentWidget, tmpFile.getName());
                itemProperty.put(KEY_IS_FILE, false);
                treeItem.setData(itemProperty);
                treeItem.setForeground(COLOR_FOLDER);
                displayFiles(treeItem, tmpFile.getPath());

            } else if (tmpFile.isFile()) {

                TreeItem treeItem = createTreeItem(parentWidget, tmpFile.getName());
                itemProperty.put(KEY_IS_FILE, true);
                treeItem.setData(itemProperty);

            }
            height += 20;
        }

        return height;
    }

    /**
     * 退出按钮
     * @param parentWidget 父容器
     */
    protected void createExitButton(Composite parentWidget) {
        Button button_exit = new Button(parentWidget, 0);
        button_exit.setBounds(489, 650, 75, 27);
        button_exit.setText("退出");
        button_exit.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                topShell.close();
            }
        });
    }


//================================================-----常用组件工具----================================================

    /**
     * 向父组件中添加并返回带滚动条的组件（在返回的组件中添加自己的组件，并设置高度，以保证可以滚动展示所有）
     * @param parent 父容器
     * @param x 水平偏移
     * @param y 垂直偏移
     * @param scWidth  sc组件的宽（窗口宽）
     * @param scHeight  sc组件的高（窗口高）
     * @return Composite 返回组件
     */
    public static Composite addScWidget(Composite parent, int x, int y, int scWidth, int scHeight) {
        // ---------创建窗口中的其他界面组件-------------
        // 定义一个ScrolledComposite，式样为深陷型、带水平滚动条、带垂直滚动条
        ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        scrolledComposite.setBounds(x, y, scWidth, scHeight);
        // 定义一个面板Composite，用此面板来容纳其他的组件
        Composite composite = new Composite(scrolledComposite, SWT.NONE);
        // 这里没用setBounds是因为composite和scrolledComposite的左上角自动重合
        // 设置composite被scrolledComposite控制
        scrolledComposite.setContent(composite);

        return composite;
    }

    /**
     * 没用成功
     * @param parent 父组件
     * @param height 滚动窗口大小
     * @return Composite
     */
    protected Composite addScWidget2(Composite parent, int height) {
        Composite composite = new Composite(parent, SWT.NONE);//在目录组容器中生成一个填充式的网格布局面板
        composite.setLayout(new GridLayout(1, false));
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1);
        gridData.heightHint = height;
        composite.setLayoutData(gridData);
        return composite;
    }

    //===========================================label start===========================================

    /**
     * 创建非定位 label
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param width 宽度
     * @param height 高度
     * @return Label
     */
    protected Label createLabel(Composite parentWidget, String initText, int width, int height) {
        Label label = new Label(parentWidget, 0);
        label.setText(initText);
        label.setSize(width, height);
        return label;
    }

    /**
     * 创建绝对定位 label
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param x 水平位置
     * @param y 垂直位置
     * @param width 宽度
     * @param height 高度
     * @return Label
     */
    protected Label createLabelAbs(Composite parentWidget, String initText, int x, int y, int width, int height) {
        Label label = createLabel(parentWidget, initText, width, height);
        label.setLocation(x, y);
        return label;
    }

    /**
     * 创建非定位 居中 label
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param width 宽度
     * @param height 高度
     * @return Label
     */
    protected Label createLabelCenter(Composite parentWidget, String initText, int width, int height) {
        Label label = new Label(parentWidget, SWT.SHADOW_NONE);
        label.setText(initText);
        label.setSize(width, height);
        label.setAlignment(SWT.CENTER);
        return label;
    }

    //===========================================Text start===========================================

    /**
     * 创建非定位 Text
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param width 宽度
     * @param height 高度
     * @return Text
     */
    private Text createText(Composite parentWidget, String initText, int width, int height, int style) {
        Text text = new Text(parentWidget, style);
        text.setText(initText);
        text.setSize(width, height);
        return text;
    }

    /**
     * 创建绝对定位 Text
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param x 水平位置
     * @param y 垂直位置
     * @param width 宽度
     * @param height 高度
     * @return Text
     */
    protected Text createTextAbs(Composite parentWidget, String initText, int x, int y, int width, int height) {
        Text text = createText(parentWidget, initText, width, height, 2048);
        text.setLocation(x, y);
        return text;
    }

    /**
     * 创建非定位 Text
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param width 宽度
     * @param height 高度
     * @return Text
     */
    protected Text createText(Composite parentWidget, String initText, int width, int height) {
        return createText(parentWidget, initText, width, height, 2048);
    }

    /**
     * 创建非定位只读 Text
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param width 宽度
     * @param height 高度
     * @return Text
     */
    protected Text createTextReadonly(Composite parentWidget, String initText, int width, int height) {
        return createText(parentWidget, initText, width, height, SWT.READ_ONLY);
    }

    //===========================================Group start ===========================================
    /**
     * 创建绝对定位 Group
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param x 水平位置
     * @param y 垂直位置
     * @param width 宽度
     * @param height 高度
     * @return Group
     */
    protected Group createGroupAbs(Composite parentWidget, String initText, int x, int y, int width, int height) {
        Group group = new Group(parentWidget, SWT.NONE);
        group.setBounds(x, y, width, height);
        group.setText(initText);
        return group;
    }

    /**
     * 创建非定位 Group
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param width 宽度
     * @param height 高度
     * @return Group
     */
    protected Group createGroup(Composite parentWidget, String initText, int width, int height) {
        Group group = new Group(parentWidget, SWT.NONE);
        group.setSize(width, height);
        group.setText(initText);
        return group;
    }

    //===========================================Tree start ===========================================

    /**
     * 创建 Tree
     * @param parentWidget 父组件
     * @return Tree
     */
    protected Tree createTree(Composite parentWidget) {
        Tree tree = new Tree(parentWidget, SWT.SINGLE);
        tree.setLayoutData(new GridData(GridData.FILL_BOTH));
        return tree;
    }

    /**
     * 创建 TreeItem
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @return TreeItem
     */
    protected TreeItem createTreeItem(TreeItem parentWidget, String initText) {
        TreeItem treeItem = new TreeItem(parentWidget, SWT.NONE);
        treeItem.setText(initText);
        return treeItem;
    }
    //===========================================Tree end ===========================================

    //===========================================Button start ===========================================

    /**
     * 创建绝对定位 按钮
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param x 水平位置
     * @param y 垂直位置
     * @param width 宽度
     * @param height 高度
     * @return Button
     */
    private Button createButtonAbs(Composite parentWidget, String initText, int x, int y, int width, int height, int style) {
        Button button = new Button(parentWidget, style);
        button.setBounds(x, y, width, height);
        button.setText(initText);
        return button;
    }

    /**
     * 创建非定位 按钮
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param width 宽度
     * @param height 高度
     * @return Button
     */
    private Button createButton(Composite parentWidget, String initText, int width, int height, int style) {
        Button button = new Button(parentWidget, style);
        button.setSize(width, height);
        button.setText(initText);
        return button;
    }

    /**
     * 创建绝对定位 普通按钮
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param x 水平位置
     * @param y 垂直位置
     * @param width 宽度
     * @param height 高度
     * @return Button
     */
    protected Button createButtonAbs(Composite parentWidget, String initText, int x, int y, int width, int height) {
        return createButtonAbs(parentWidget, initText, x, y, width, height, SWT.NONE);
    }

    /**
     * 创建非定位 普通按钮
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param width 宽度
     * @param height 高度
     * @return Button
     */
    protected Button createButton(Composite parentWidget, String initText, int width, int height) {
        return createButton(parentWidget, initText, width, height, SWT.NONE);
    }

    /**
     * 创建绝对定位 CheckButton 默认不选择
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param x 水平位置
     * @param y 垂直位置
     * @param width 宽度
     * @param height 高度
     * @return Button
     */
    protected Button createCheckAbs(Composite parentWidget, String initText, int x, int y, int width, int height) {
        return createButtonAbs(parentWidget, initText, x, y, width, height, SWT.CHECK);
    }

    /**
     * 创建非定位 CheckButton 默认不选择
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param width 宽度
     * @param height 高度
     * @return Button
     */
    protected Button createCheck(Composite parentWidget, String initText, int width, int height) {
        return createButton(parentWidget, initText, width, height, SWT.CHECK);
    }

    /**
     * 创建绝对定位 radio
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param x 水平位置
     * @param y 垂直位置
     * @param width 宽度
     * @param height 高度
     * @return Button
     */
    protected Button createRadioAbs(Composite parentWidget, String initText, int x, int y, int width, int height) {
        return createButtonAbs(parentWidget, initText, x, y, width, height, SWT.RADIO);
    }

    /**
     * 创建非定位 radio
     * @param parentWidget 父组件
     * @param initText 初始文本
     * @param width 宽度
     * @param height 高度
     * @return Button
     */
    protected Button createRadio(Composite parentWidget, String initText, int width, int height) {
        return createButton(parentWidget, initText, width, height, SWT.RADIO);
    }
    //===========================================Button end===========================================

}
