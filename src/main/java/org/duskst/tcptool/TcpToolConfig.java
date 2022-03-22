package org.duskst.tcptool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Properties;

/**
 * TCP 工具配置类处理
 * @date 2018/01/17 11:08
 * @author duskst
 **/
public class TcpToolConfig {

    private Properties properties;

    public Properties getProperties() {
        return properties;
    }

    private static final String DEFAULT_CHARSET = "UTF-8";

    public TcpToolConfig(String configFileName, String content) {
        String filePath = getSysPath() + configFileName;
        createConfigFile(filePath, content, false);
        loadConfig(filePath);
    }

    public TcpToolConfig(String configFileName, String content, boolean overWrite) {
        String filePath = getSysPath() + configFileName;
        createConfigFile(filePath, content, overWrite);
        loadConfig(filePath);
    }

    public void createConfigFile(String filePath, String content, boolean overWrite) {
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (!overWrite) {
                return;
            }
        }
        PrintWriter pw = null;
        try {
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos, DEFAULT_CHARSET);
            pw = new PrintWriter(osw);
            pw.write(content);
            pw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    private void loadConfig(String filePath) {
        this.properties = new Properties();
        BufferedReader br = null;
        try {
            FileInputStream fis = new FileInputStream(filePath);
            br = new BufferedReader(new InputStreamReader(fis, DEFAULT_CHARSET));
            this.properties.load(br);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取当前class运行时目录，public 时获得的路径好像不是我期望的，以后有时间再看
     * @return 执行文件所在目录
     */
    private String getSysPath() {
        String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

        int pos = path.toUpperCase().indexOf(".JAR");
        if (pos != -1) {
            try {
                // 截取.jar 第一次出现的位置
                String strPath = path.substring(0, pos);
                // 获取 .jar上一层文件夹
                path = strPath.substring(0, strPath.lastIndexOf("/") + 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            path = TcpToolConfig.class.getResource("").getPath();
        }

        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        return path;
    }
}
