package org.duskst.tcptool;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 工具类
 * @date 2018/01/17 11:08
 * @author duskst
 **/
public class XmlFileUtil {

    public static String getListenTagValue(String inputStr, String charSet, String tagName) {
        InputStream inputStream = null;
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        spf.setValidating(false);

        try {
            SAXParser saxParser = spf.newSAXParser();
            inputStream = new ByteArrayInputStream(inputStr.getBytes(charSet));
            XmlParserHandler xpHandler = new XmlParserHandler(tagName);
            saxParser.parse(inputStream, xpHandler);
            if (xpHandler.isHasDealt()) {
                return xpHandler.getListenTagValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }

}


