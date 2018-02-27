package ibatis2mybatis.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * 自定义的文件工具类
 * @author Feiyizhan
 *
 */
public class FileUtil {

	
	/**
	 * 获取文件，如果当前目录没找到，则在当前classPath找
	 * @param fileName
	 * @return
	 */
	public static File getFile(String fileName){
		File file=new File(fileName);
		if(file.exists()) {
		    return file;
		}
		
		URL url = FileUtil.class.getClassLoader().getResource(fileName);
		if(url!=null){
		    try {
                fileName = URLDecoder.decode(url.getFile(),"UTF-8");
                file=new File(fileName);
            } catch (UnsupportedEncodingException e) {}
		}
		return file;
	}
	
	
	/**
     * 获取文件对应的InputStream，如果当前目录没找到，则在当前classPath找。
     * @param fileName
     * @return
	 * @throws FileNotFoundException 
     */
    public static InputStream getFileInput(String fileName) throws FileNotFoundException{
        return new FileInputStream(getFile(fileName));
    }
    
   
}
