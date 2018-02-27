package ibatis2mybatis;

import java.beans.Introspector;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import ibatis2mybatis.utils.FileUtil;


/**
 * 转换ibatis配置文件为Mybatis的工具类入口
 * @author Feiyizhan
 *
 */
public class Main {
    /**
     * 配文件后缀名
     */
    static final String[] EXTENSIONS= {"xml"};

    /**
     * @param args
     * @throws DocumentException 
     * @throws FileNotFoundException 
     */
    public static void main(String[] args) {
        String srcDirectory = "src/test/java/src";
        String resultDirectory = "src/test/java/result";
        convertDirectory(srcDirectory,resultDirectory);
    }
    
    /**
     * 转换目录下的所有配置文件（不处理子目录）
     * @param srcDirectory
     * @param resultDirectory
     */
    public static void convertDirectory(String srcDirectory,String resultDirectory) {
        SAXReader reader = new SAXReader();
        Document srcDocument =null;
        //获取源目录和结果目录文件对象
        File srcDirectoryFile = FileUtil.getFile(srcDirectory);
        File resultDirectoryFile = FileUtil.getFile(resultDirectory);
        System.out.println("源目录："+srcDirectoryFile.getAbsolutePath());
        System.out.println("结果目录："+resultDirectoryFile.getAbsolutePath());
        
        if(srcDirectoryFile!=null && srcDirectoryFile.exists() && srcDirectoryFile.isDirectory() 
                && resultDirectoryFile!=null && resultDirectoryFile.exists() && resultDirectoryFile.isDirectory()) {
            
            //根据后缀名过滤源目录下的所有文件（不处理子目录）
            Collection<File> srcFileList = FileUtils.listFiles(srcDirectoryFile,EXTENSIONS,false);
            for(File f:srcFileList) {
                //获取文件名（无路径无后缀名的部分）
                String baseName = FilenameUtils.getBaseName(f.getName());
                //转换为首字母小写的java变量名格式
                String namespace = Introspector.decapitalize(baseName);
                try {
                    //读取并解析文件构建为源文件对象
                    srcDocument = reader.read(FileUtils.openInputStream(f));
                    //转换源文件为结果文件
                    Document resultDocument = convertDocument(srcDocument,namespace);
                    
                    //获取结果文件
                    File resultFile = FileUtils.getFile(resultDirectoryFile, f.getName());
                    //打印结果文件绝对路径
                    System.out.println("文件【"+resultFile.getAbsolutePath()+"】处理成功");
                    //输出转换后的结果到结果文件
                    FileUtils.writeStringToFile(resultFile, fromatXmlDocument(resultDocument), "UTF-8");
                    
                } catch (IOException e) {
                    System.out.println("文件【"+f.getAbsolutePath()+"】处理失败");
                    continue;
                } catch (DocumentException e) {
                    System.out.println("文件【"+f.getAbsolutePath()+"】处理失败");
                    continue;
                }
            }
        }
    }
    
    /**
     * 格式化Xml文档并返回格式化后的字符串
     * @param document
     * @return
     * @throws IOException
     */
    public static String fromatXmlDocument(Document document) throws IOException {
        StringWriter stringWriter = new StringWriter();
        OutputFormat format = new OutputFormat();
        format.setTrimText(false);
        format.setIndentSize(3);
        format.setNewlines(true);
        XMLWriter writer = new XMLWriter(stringWriter, format);
        writer.write(document);
        writer.flush();
        return stringWriter.getBuffer().toString();
    }
    
    /**
     * 转换Document
     * @param srcDocument
     * @param namespace
     * @return
     */
    public static Document convertDocument(Document srcDocument,String namespace) {

        Document document = DocumentHelper.createDocument(); 
        document.addDocType("mapper", "-//mybatis.org//DTD Mapper 3.0//EN", "http://mybatis.org/dtd/mybatis-3-mapper.dtd");
        document.setXMLEncoding(srcDocument.getXMLEncoding());
        Element mapper = DocumentHelper.createElement("mapper");
        mapper.addAttribute("namespace",namespace);
        
        Element srcRootEl = srcDocument.getRootElement();
        List<Element> srcElList = srcRootEl.elements();
        for(Element srcEl :srcElList ) {
            String srcNodeName = srcEl.getName();
            if(srcNodeName.equalsIgnoreCase("sql")) {
                mapper.add(convertSqlElement(srcEl));
            }else if(srcNodeName.equalsIgnoreCase("select")) {
                mapper.add(convertSelectElement(srcEl));
            }else if(srcNodeName.equalsIgnoreCase("insert")) {
                mapper.add(convertInsertElement(srcEl));
            }else if(srcNodeName.equalsIgnoreCase("delete")) {
                mapper.add(convertDeleteElement(srcEl));
            }else if(srcNodeName.equalsIgnoreCase("update")) {
                mapper.add(convertUpdateElement(srcEl));
            }
        }
        
        document.setRootElement(mapper);
        
        return document;
    }
    
    
    /**
     * 打印所有的子节点
     * @param el
     */
    public static void printSubNode(Element el) {
        int count = el.nodeCount();
        for(int i=0;i<count;i++) {
            Node node = el.node(i);
            System.out.println(node.getName()+":"+node.getNodeTypeName()+":"+node.getNodeType());
            System.out.println(el.node(i).asXML());
        }
    }
    
    /**
     * 转换文本
     * @param srcText
     * @return
     */
    public static  String convertText(String srcText) {
        
        if(srcText.indexOf("#")>=0) {
            
            StringBuilder sb = new StringBuilder(srcText.length());
            int beginIndex = -1;
            int endIndex = -1;
            for(int i=0;i<srcText.length();i++) {
                if(srcText.charAt(i)=='#') {
                    if(beginIndex!=-1) {
                        sb.append(srcText.substring(endIndex+1, beginIndex));
                        sb.append("#{");
                        endIndex = i;
                        sb.append(srcText.substring(beginIndex+1, endIndex));
                        sb.append('}');
                        beginIndex= -1;
                    }else {
                        beginIndex = i;  
                    }
                    
                }
            }
            if(endIndex<srcText.length()) {
                sb.append(srcText.substring(endIndex+1));
            }
            return sb.toString().replace("[]", "");
        }else {
            return srcText;
        }
        
    }
    
    /**
     * 处理通用子节点列表
     * @param srcElement
     * @param targetElement
     */
    public static void handleGeneralSubElements(Element srcElement,Element targetElement) {
        
        int count = srcElement.nodeCount();
        for(int i=0;i<count;i++) {
            Node node = srcElement.node(i);
            switch(node.getNodeType()) {
                case Node.TEXT_NODE:
                    String text = node.asXML();
                    if(text.trim().length()>0) {
                        String targetElementName = targetElement.getName();
                        String prepend = srcElement.attributeValue("prepend");
                        
                        if(targetElementName.equalsIgnoreCase("if")) {
                            targetElement.add(DocumentHelper.createText(prepend + " " + convertText(srcElement.getText())));
                        }else if(targetElementName.equalsIgnoreCase("foreach")) {
                            targetElement.add(DocumentHelper.createText("#{item}"));
                        }else {
                            targetElement.add(DocumentHelper.createText(convertText(text))); 
                        }
                        
                    }
                    break;
                case Node.ELEMENT_NODE:
                    String srcNodeName = node.getName();
                    Element subElemnt =(Element) node;
                    if(srcNodeName.equalsIgnoreCase("dynamic")) {
                        targetElement.add(convertDynamicElement(subElemnt));
                    }else if(srcNodeName.equalsIgnoreCase("isNotEmpty")) {
                        targetElement.add(convertIsNotEmptyElement(subElemnt));
                    }else if(srcNodeName.equalsIgnoreCase("isEmpty")) {
                        targetElement.add(convertIsEmptyElement(subElemnt));
                    }else if(srcNodeName.equalsIgnoreCase("iterate")) {
                        targetElement.add(convertIterateElement(subElemnt));
                    }else if(srcNodeName.equalsIgnoreCase("isParameterPresent")) {
                        targetElement.add(convertIsParameterPresentElement(subElemnt));
                    }else if(srcNodeName.equalsIgnoreCase("selectKey")) {
                        targetElement.add(convertSelectKeyElement(subElemnt));
                    }else if(srcNodeName.equalsIgnoreCase("include")) {
                        targetElement.add(subElemnt.createCopy());
                    }else {
                        targetElement.add(subElemnt.createCopy());
                    }
                    
                default:
                    break;
            }
        }
        
    }

    
    /**
     * 转换Sql节点
     * @param srcElement
     * @param targetElement
     */
    public static  Element convertSqlElement(Element srcElement) {
        Element targetElement =DocumentHelper.createElement("sql");
        targetElement.addAttribute("id", srcElement.attributeValue("id"));

        //处理子节点
        handleGeneralSubElements(srcElement,targetElement);
        
        return targetElement;
    }
    
    /**
     * 转换Select节点
     * @param srcElement
     * @param targetElement
     */
    public static  Element convertSelectElement(Element srcElement) {
        Element targetElement =DocumentHelper.createElement("select");
        targetElement.addAttribute("id", srcElement.attributeValue("id"));
        String parameterClass = srcElement.attributeValue("parameterClass");
        if(parameterClass!=null && !parameterClass.isEmpty()) {
            targetElement.addAttribute("parameterType", parameterClass);
        }
        targetElement.addAttribute("resultType", srcElement.attributeValue("resultClass"));

        //处理子节点
        handleGeneralSubElements(srcElement,targetElement);
        
        return targetElement;
    }
    
    
    /**
     * 转换Insert节点
     * @param srcElement
     * @param targetElement
     */
    public static  Element convertInsertElement(Element srcElement) {
        Element targetElement =DocumentHelper.createElement("insert");
        targetElement.addAttribute("id", srcElement.attributeValue("id"));
        String parameterClass = srcElement.attributeValue("parameterClass");
        if(parameterClass!=null && !parameterClass.isEmpty()) {
            targetElement.addAttribute("parameterType", parameterClass);
        }
        //处理子节点
        handleGeneralSubElements(srcElement,targetElement);
        
        return targetElement;
    }
    
    
    /**
     * 转换Delete节点
     * @param srcElement
     * @param targetElement
     */
    public static  Element convertDeleteElement(Element srcElement) {
        Element targetElement =DocumentHelper.createElement("delete");
        targetElement.addAttribute("id", srcElement.attributeValue("id"));
        String parameterClass = srcElement.attributeValue("parameterClass");
        if(parameterClass!=null && !parameterClass.isEmpty()) {
            targetElement.addAttribute("parameterType", parameterClass);
        }
        //处理子节点
        handleGeneralSubElements(srcElement,targetElement);
        
        return targetElement;
    }
    
    /**
     * 转换Update节点
     * @param srcElement
     * @param targetElement
     */
    public static  Element convertUpdateElement(Element srcElement) {
        Element targetElement =DocumentHelper.createElement("update");
        targetElement.addAttribute("id", srcElement.attributeValue("id"));
        String parameterClass = srcElement.attributeValue("parameterClass");
        if(parameterClass!=null && !parameterClass.isEmpty()) {
            targetElement.addAttribute("parameterType", parameterClass);
        }
        //处理子节点
        handleGeneralSubElements(srcElement,targetElement);
        
        return targetElement;
    }    

    /**
     * 转换Dynamic
     * @param srcElement
     * @return
     */
    public static Element convertDynamicElement(Element srcElement) {
        String prependAttrName = srcElement.attributeValue("prepend");
        //默认为trim节点（即空节点）
        Element targetElement =DocumentHelper.createElement("trim");
        targetElement.addAttribute("prefix", "");
        targetElement.addAttribute("suffixOverrides", ",");
        
        if(prependAttrName!=null) {
            prependAttrName = prependAttrName.trim();
            if(prependAttrName.equalsIgnoreCase("where")) {
                targetElement =DocumentHelper.createElement("where");
                
            }else if(prependAttrName.equalsIgnoreCase("and")) {
                // TODO
            }else {
             // TODO
            }
        }else {
            // TODO
        }
        
        handleGeneralSubElements(srcElement,targetElement);
        return targetElement;
    }
    
    /**
     *  转换isNotEmpty
     * @param srcElement
     * @return
     */
    public static Element convertIsNotEmptyElement(Element srcElement) {
        String property = srcElement.attributeValue("property");
        Element targetElement =DocumentHelper.createElement("if");
        targetElement.addAttribute("test", property+"!=null");  
        handleGeneralSubElements(srcElement,targetElement);
        return targetElement;
    }

    /**
     * 转换IsEmpty
     * @param srcElement
     * @return
     */
    public static Element convertIsEmptyElement(Element srcElement) {
        String property = srcElement.attributeValue("property");
        Element targetElement =DocumentHelper.createElement("if");
        targetElement.addAttribute("test", property+"==null");
        handleGeneralSubElements(srcElement,targetElement);
        return targetElement;
    }
    
    /**
     * 转换Iterate
     * @param srcElement
     * @return
     */
    public static Element convertIterateElement(Element srcElement) {
        String conjunction = srcElement.attributeValue("conjunction");
        String open = srcElement.attributeValue("open");
        String close = srcElement.attributeValue("close");
        String property = srcElement.attributeValue("property");
        Element targetElement =DocumentHelper.createElement("foreach");
        targetElement.addAttribute("open", open);
        targetElement.addAttribute("close", close);
        targetElement.addAttribute("separator", conjunction);
        targetElement.addAttribute("collection", property);
        targetElement.addAttribute("item", "item");
        
        handleGeneralSubElements(srcElement,targetElement);
        return targetElement;
    }
    
    /**
     * 转换IsParameterPresent
     * @param srcElement
     * @return
     */
    public static Element convertIsParameterPresentElement(Element srcElement) {
        Element targetElement =DocumentHelper.createElement("trim");
        handleGeneralSubElements(srcElement,targetElement);
        return targetElement;
    }
    
    /**
     * 转换SelectKey
     * @param srcElement
     * @return
     */
    public static Element convertSelectKeyElement(Element srcElement) {
        Element targetElement =DocumentHelper.createElement("selectKey ");
        targetElement.addAttribute("resultType", srcElement.attributeValue("resultClass"));
        targetElement.addAttribute("keyProperty", srcElement.attributeValue("keyProperty"));
        targetElement.addAttribute("order", "AFTER");
        //处理子节点
        handleGeneralSubElements(srcElement,targetElement);
        return targetElement;
    }
    
    
    
}
