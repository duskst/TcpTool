package org.duskst.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * 文件处理工具类
 *
 * @author duskst
 * @date 2017年12月20日 下午2:31:02
 **/
public class FileUtil {

    public static final String SLASH = "/";
    public static final String B_SLASH = "\\";

    /**
     * 将反斜杠替换为斜杠
     * @param ori 原始字符串
     * @date 2022/03/11 14:39
     **/
    public static String replaceBSlash(String ori) {
        if (ori == null) {
            return null;
        }
        return ori.replace(B_SLASH, SLASH);
    }

    /**
     *
     * 根据文件名排序文件列表(汉字间排序有问题)
     *
     * @param fileList 文件列表
     *
     */
    public static void orderFilesByName(List<File> fileList) {
        fileList.sort((o1, o2) -> {
            if (o1.isDirectory() && o2.isFile()) {
                return -1;
            }
            if (o1.isFile() && o2.isDirectory()) {
                return 1;
            }
            return o1.getName().compareTo(o2.getName());
        });
    }


    public static String readResFile(String path, String codeSet) {
        return readResFile(path, codeSet, false);
    }

    public static String readResFileTrim(String path, String codeSet) {
        return readResFile(path, codeSet, true);
    }

    /**
     * 读取响应文件
     * @param path 文件路径
     * @param codeSet 字符集
     * @param trim 是否去除每行的首尾空格
     * @return 文件内容
     */
    public static String readResFile(String path, String codeSet, boolean trim) {

        InputStream fisTmp = null;
        BufferedReader brTmp;
        StringBuilder res = new StringBuilder();
        try {
            fisTmp = new FileInputStream(path);
            brTmp = new BufferedReader(new InputStreamReader(fisTmp, codeSet));
            String line;
            while ((line = brTmp.readLine()) != null) {
                if (trim) {
                    line = line.trim();
                } else {
                    line += "\n";
                }
                res.append(line);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("===---->file not exist:" + path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (fisTmp != null) {
                    fisTmp.close();
                }
            } catch (IOException e) {
                System.out.println("err read file: " + e.getCause() + " : " + e.getMessage());
            }
        }
        return res.toString();
    }

    /**
     * 读取socket输入流
     *
     * @param input  输入流
     * @param charSet 字符集
     * @param offset 偏移
     * @param headLength 报文头长度
     * @return String 数据流
     * @throws Exception 异常
     */
    public static String readAccordingLengthFixed(InputStream input, String charSet, int offset, int headLength) throws Exception {
        StringBuilder resStr = new StringBuilder();
        if (headLength > 0) {

            byte[] headBuffer = new byte[headLength];
            for (offset = 0; offset < headLength; ) {
                int length = input.read(headBuffer, offset, headLength - offset);
                if (length < 0) {
                    throw new Exception("invalid_packet_head");
                }
                offset += length;
            }

            String bodyLen = new String(headBuffer, charSet);
            int bodyLength = Integer.parseInt(bodyLen);
            int totalLength = headLength + bodyLength;

            byte[] resultBuffer = new byte[totalLength];
            System.arraycopy(headBuffer, 0, resultBuffer, 0, headLength);

            while (offset < totalLength) {
                int realLength = input.read(resultBuffer, offset, totalLength - offset);
                if (realLength >= 0) {
                    offset += realLength;
                } else {
                    throw new Exception("invalid_packet_data");
                }
            }

            resStr = new StringBuilder(new String(resultBuffer, charSet));
        } else {
            BufferedReader br = new BufferedReader(new InputStreamReader(input, charSet));
            String line;
            while ((line = br.readLine()) != null) {
                resStr.append(line);
            }
        }
        return resStr.toString();
    }

    /**
     * BOM
     */
    private static final int HEX_EF = 0xEF;
    private static final int HEX_BB = 0xBB;
    private static final int HEX_BF = 0xBF;

    private static final int BOM_LEN = 3;

    /**
     * FF FE
     */
    private static final int HEX_FF = 0xFF;
    private static final int HEX_FE = 0xFE;

    /**
     * 此方法只用来确定读取文件时应该用什么编码解析，并不可作为写入时文件的编码。
     *
     * <P>只用判断GBK和UTF的区别就行了(空文件字符集无意义)
     *	UTF-8 是一种多字节编码的字符集，表示一个Unicode字符时，它可以是1个至3个字节，在表示上有规律：
     *	1字节:  0xxxxxxx
     *	2字节:  110xxxxx 10xxxxxx
     *	3字节:  1110xxxx 10xxxxxx 10xxxxxx
     *
     *	UTF-8 with BOM 与 without BOM 差别只是带BOM的，在文件起始多了三个字节：EF BB BF
     *	GBK 与 UTF-8 均兼容 ASCII(0-255)，差别在汉字，若无汉字，则两种编码文本的内容二进制是完全一样的。GBK的汉字是两个字节编码的(最高位都是1)。UTF8的汉字是三个字节编码，这两种都是变长编码，英文字符都是一个字节。
     *
     *	UTF-16(是Unicode的一种使用方式，考虑到最初Unicode的目的，通常说的Unicode就是UTF-16，因此某些软件UTF16的文件可能显示为Unicode) 。字符都以固定长度存储--2个字节，英文字符也是(ASCII的字节补零一个字节(实际存储的是字符在0-65535内的映射数字))
     *		utf16 开头两个字节是固定的: FF(1111 1111) FE(1111 1110)
     *
     *	其他编码暂时不考虑（utf是国际标准，gbk是国家标准。utf32暂时不考虑），因此只用区分 GBK UTF-8(without BOM) UTF-8(BOM) UTF-16 四种，后两种通过头三字节判断。前两种比较难，只能尽量读取到能区分的部分，若读取完仍无法区分(无汉字)，可默认GBK或UTF8
     *
     *	int l = read();方法读取的是单个字节无符号，返回值为 -1(未读取到) 及 0-255
     *	read(byte[]); 读取到数组的值有符号
     * </P>
     *
     * @param file 文件对象
     * @return charSet (GBK,UTF-8,UTF-8(BOM),UTF-16,Unicode)
     * @throws Exception 异常
     */
    public static String getCharset(File file) throws Exception {

        //测试数据
//		String pathPreFix = "C:/Users/csii-jk/Desktop/111/";
//		path = pathPreFix + "gbk-0l.xml";//头读取长度 -1  OK 默认GBK
//		path = pathPreFix + "gbk-1l.xml";//TODO 读取长度大于0
//		path = pathPreFix + "utf8-0l.xml";//头读取长度 -1 OK 默认 GBK
//		path = pathPreFix + "utf8-1l.xml";//TODO 读取长度大于0
//		path = pathPreFix + "utf8-BOM-0l.xml";//头读取长度 3 OK UTF-8(BOM)
//		path = pathPreFix + "utf8-BOM-1l.xml";//头读取长度 3 OK UTF-8(BOM)
//		path = pathPreFix + "UTF16-0l.xml";//头读取长度 2 OK UTF-16
//		path = pathPreFix + "UTF16-1l.xml";//头读取长度 2 + 内容 1 = 3 OK UTF-16
//		file = new File(path);

        BufferedInputStream bin = null;
        try {

            bin = new BufferedInputStream(new FileInputStream(file));

            byte[] head = new byte[3];
            String charSet;
            int readLen = bin.read(head);

            if (readLen == -1) {
                //没读取到内容，空文件，默认GBK即可，无影响
                charSet = "GBK";

            } else {
                //由于java 中只有有符号数，因此读取到byte中的字节查看时会当做有符号数，此行计算会将byte(8位)转为int(32位)，会根据符号位补位，然后由补位后的32位int计算出10进制值。仍是有符号数；
                //实际上文件中的字符(实际上所有的字符无论是文件中的还是其他输入流的)的每个字节都是无符号数，用来表示字符映射位置(多字节字符只是规则复杂，实际也是无符号数表示映射)；
                //因此，使用 [位与] 操作，与 0xFF 做 [与运算] 将 int 的高24位清除，只保留低 8位(实际上就是原始字节信息)，这样就将符号去掉了。
                //可以查看无参的 read() 方法，里面就有 [& 0xFF] 操作

                int value_1st_byte = head[0] & 0xFF;
                int value_2nd_byte = head[1] & 0xFF;
                int value_3st_byte = head[2] & 0xFF;

                if (value_1st_byte == HEX_EF && value_2nd_byte == HEX_BB && value_3st_byte == HEX_BF) {
                    charSet = "UTF-8(BOM)";
                } else if (value_1st_byte == HEX_FF && value_2nd_byte == HEX_FE) {
                    charSet = "UTF-16";
                } else if (value_1st_byte == HEX_FE && value_2nd_byte == HEX_FF) {
                    charSet = "Unicode";
                } else {
                    // gbk 或者 utf-8(without BOM)
                    if (readLen < BOM_LEN) {
                        //文件内容小于三个字节，因为UTF-8编码的汉字需要三个字节，则文件不可能是UTF-8的汉字，只能是GBK的汉字或字母，及UTF-8的字母，默认GBK即可正常解析。
                        //最多两个字节第一个是ASCII，则是GBK
                        charSet = "GBK";
                    } else {

                        bin.close();
                        bin = null;
                        //重新打开，方便循环
                        bin = new BufferedInputStream(new FileInputStream(file));

                        int curByte = -1;
                        int nextByte = -1;

                        // 下面的循环实际是遍历文本内容看是否有非UTF-8字符的。
                        boolean isUTF8 = true;

                        // int length = bin.available();
                        long contentSize = file.length();
                        int asciiCharCount = 0;
                        while ((curByte = bin.read()) != -1) {
                            if ((curByte < 0x80)) {
                                //[0x00 - 0x80) ascii 码，忽略
                                asciiCharCount++;
                            } else if (curByte < 0xC0) {
                                //0xC0 1100 0000
                                //[0x80 - 0xC0) 为无效 UTF-8 字符
                                isUTF8 = false;
                                break;
                            } else if (curByte < 0xE0) {
                                //0xE0 1110 0000
                                //[0xC0 - 0xE0) 为两字节 UTF-8 字符//110x xxxx 10xxxxxx
                                //判断下一字节是否满足UTF-8编码规则
                                //若已经到文件尾 nextByte = -1 ，直接  & : 1111 1111 & 1100 0000 = 0100 0000 也是可以判断的。
                                nextByte = bin.read();
                                if ((nextByte & 0xC0) != 0x80) {
                                    //第二个字符不是  10xxxxxx ，不满足UTF-8编码规则，非UTF-8编码。
                                    isUTF8 = false;
                                    break;
                                }
                            } else if (curByte < 0xF0) {
                                //0xF0 1111 0000
                                //[0xE0 - 0xF0) 为三字节 UTF-8 字符//1110 xxxx 10xxxxxx 10xxxxxx
                                //判断下一字节是否满足UTF-8编码规则
                                nextByte = bin.read();
                                if ((nextByte & 0xC0) != 0x80) {
                                    //第二个字符不是  10xxxxxx ，不满足UTF-8编码规则，非UTF-8编码。
                                    isUTF8 = false;
                                    break;
                                } else {
                                    nextByte = bin.read();
                                    if ((nextByte & 0xC0) != 0x80) {
                                        //第三个字符不是  10xxxxxx ，不满足UTF-8编码规则，非UTF-8编码。
                                        isUTF8 = false;
                                        break;
                                    }
                                }
                            }
                        }
                        charSet = isUTF8 ? "UTF-8" : "GBK";
                        if (asciiCharCount == contentSize) {
                            System.out.println("all chars are ASCII! ====---> " + file.getPath());
                            //全是ASCII字符的文件可以认为编码时GBK，不影响解析。
                            charSet = "GBK";
                        }
                    }
                }
            }
            return charSet;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw e;
        } finally {
            bin.close();
        }
    }

    /**
     * 追加到文件
     * @param writePath 路径
     * @param charSet 字符集
     * @param content 内容
     * @date 2022/03/11 09:13
     * @author duskst
     **/
    public static void writeToFileAppend(String writePath, String charSet, String content) {
        writeToFile(writePath, charSet, content, false);
    }

    /**
     * 覆盖写到文件
     * @param writePath 路径
     * @param charSet 字符集
     * @param content 内容
     * @date 2022/03/11 09:13
     * @author duskst
     **/
    public static void writeToFileOverW(String writePath, String charSet, String content) {
        writeToFile(writePath, charSet, content, true);
    }

    /**
     * 将字符串写入文件
     * @param writePath 文件路径
     * @param charSet 字符集
     * @param content 内容
     * @param overWriteFlag true 覆盖文件内容， false - 追加到文件
     * @date 2022/03/11 09:12
     **/
    public static void writeToFile(String writePath, String charSet, String content, boolean overWriteFlag) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(writePath), charSet));
            if (overWriteFlag) {
                bw.write(content);
            } else {
                bw.append(content);
            }
            bw.flush();
            bw.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            if (bw != null) {
                try {
                    bw.flush();
                    bw.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
