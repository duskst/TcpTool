package org.duskst.tcptool;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * xml解析
 * @date 2018/01/17 11:08
 * @author duskst
 **/
public class XmlParserHandler extends DefaultHandler {
    private final String listenTagName;
    private String listenTagValue = "";

    private boolean preFound = false;
    private boolean hasDealt = false;
    private boolean skip = false;

    public boolean isHasDealt() {
        return hasDealt;
    }

    public String getListenTagValue() {
        return listenTagValue;
    }

    XmlParserHandler(String listenTagName) {
        this.listenTagName = listenTagName;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        // 如果是监测标签，并且不需跳过，则将发现标志置为true
        // 跳过标志主要是为了加快处理速度(现在只处理一个标签)
        if (!skip && localName.equals(listenTagName)) {
            preFound = true;
        }
    }

    /**
     * 内容读取  --- 自动解析时 每个实体引用 都是单独的事件，会单独调用 此方法，如果 xml 的值中间含有 实体引用，单独一次的解析的调用是拿不到完整值的，
     * 所以不能调用一次直接设置为解析完，可以根据结束标签判断；或者不判断，直接每次都追加，但是需要及时清空追加的内容为下一标签值解析准备。
     *
     * @param ch 字符数组
     * @param start 开始位置
     * @param length 结束位置
     * @date 2022/02/08 11:32
     * @author duskst
     **/
    @Override
    public void characters(char[] ch, int start, int length) {
        // 发现并且不用跳过，则读取标签里的内容
        // 并将处理标志置为true，跳过标志置为 true
        if (!skip && preFound) {
            listenTagValue += this.normalize(new String(ch, start, length)).trim();
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (preFound && !hasDealt) {
            hasDealt = true;
            skip = true;
        }
    }

    /**
     * 字符处理，主要是对实体字符做转换
     * @param s 字符串
     **/
    protected String normalize(String s) {
        StringBuilder str = new StringBuilder();

        // handler 解析xml时，会自动把字符实体转为对应的真实字符，如果需要将解析后的内容直接转发，并且转发程序没有做字符实体转换，纯粹的转发内容，
        // 则需要做将实际字符转换回实体字符
        boolean canonical = false;
        int len = (s != null) ? s.length() : 0;
        for (int i = 0; i < len; ++i) {
            char ch = s.charAt(i);
            switch (ch) {
                case '<':
                    str.append("&lt;");
                    break;
                case '>':
                    str.append("&gt;");
                    break;
                case '&':
                    str.append("&amp;");
                    break;
                case '"':
                    str.append("&quot;");
                    break;
                case '\n':
                case '\r':
                    if (canonical) {
                        str.append("&#");
                        str.append(Integer.toString(ch));
                        str.append(';');
                    }
                    break;
                default:
                    str.append(ch);
            }
        }
        return str.toString();
    }
}
