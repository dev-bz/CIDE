package org.free.cide.app;

import android.os.Handler;
import android.support.annotation.NonNull;

import com.myopicmobile.textwarrior.common.Document;
import com.myopicmobile.textwarrior.common.DocumentProvider;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class FileManager {
    private static String _outMessage = "";
    private static Handler handler;
    private static String helpCode;
    private static String tempCode;
    private final Map<String, FileState> files = new TreeMap<>();

    public FileManager(Handler handler, String ver) {
        FileManager.handler = handler;
        String err = "  printf(\"Hello CIDE v" + ver + " - %d\\n%s language\\n\"," + 2017 + ",lang);\n";
        tempCode = "#ifdef __cplusplus\nconst char*lang=\"c++\";\nextern \"C\"\n#else\nconst char*lang=\"c\";\n#endif\nint printf(const char*,...);\nint main(){\n" + err + "  return 0;//Exit\n}";
        helpCode = "* -------使用帮助--------\n" +
                "* 有文字的菜单项目请自行理解，只说明隐藏的操作项\n" +
                "* 文件面板和导航面板的显示：从屏幕左边或右边拖出\n" +
                "* 长按编辑区：进入文本选择状态及顶部显示编辑动作\n" +
                "* 长按右下角的圆形浮动按钮：在顶部显示编辑动作条\n" +
                "* 单击圆形浮动按钮：列出标识符跳转列表或修正建议\n" +
                "* 长按查找界面左边的搜索按钮：可以关闭搜索对话框\n" +
                "* 长按文件浏览页中的文件：显示相关文件操作菜单\n" +
                "* 点击当前文件标签：显示相关文件操作菜单\n" +
                "* 长按当前文件标签：关闭当前文件\n" +
                "* -------快捷键------\n" +
                "* ctrl - A   全选\n" +
                "* ctrl - S   手动保存\n" +
                "* ctrl - O   交换光标\n" +
                "* ctrl - W   选词\n" +
                "* ctrl - K   加选\n" +
                "* ctrl - N   新建\n" +
                "* ctrl - X   剪切\n" +
                "* ctrl - C   复制\n" +
                "* ctrl - V   粘贴\n" +
                "* ctrl - I   跳转标识符\n" +
                "* ctrl - Z   撤销\n" +
                "* ctrl - Y   重做\n" +
                "* ctrl - -   字体-\n" +
                "* ctrl - =   字体+\n" +
                "* ctrl - U   重命名\n" +
                "* ctrl - H   修正当前行\n" +
                "* ctrl - Q   打开左面板\n" +
                "* ctrl - P   打开右面板\n" +
                "* ctrl - E   编辑\n" +
                "* ctrl - D   补全\n" +
                "* ctrl - F   格式化\n" +
                "* ctrl - R   运行\n" +
                "* ctrl - B   构建\n" +
                "* ctrl - J   文本查找\n" +
                "* ctrl - G   转到行\n" +
                "* ctrl - M   终端模拟器\n" +
                "* ctrl - T   设置";
    }

    public static void postSaved(String fileName) {
        if (handler.hasMessages(0)) {
            handler.removeMessages(0);
            _outMessage += fileName + "\n";
        } else _outMessage = fileName + "\n";
        handler.sendMessageDelayed(handler.obtainMessage(0, _outMessage + "已保存"), 240);
    }

    @NonNull
    public static DocumentProvider getDocument(String filename) throws IOException {
        BufferedReader is;
        if (filename.equals("New")) {
            is = new BufferedReader(new StringReader(tempCode));
        } else if (filename.equals("help")) {
            is = new BufferedReader(new StringReader(helpCode));
        } else
            is = new BufferedReader(new FileReader(filename));
        String l;
        Document d = new Document(null);
        //if (!edit.isWordWrap()) {
        d.setWordWrap(false);
        d.beginBatchEdit();
        int offset = 0;
        while ((l = is.readLine()) != null) {
            l += "\n";
            d.insert(l.toCharArray(), offset, 0, false);
            offset += l.length();
        }
        is.close();
        d.endBatchEdit();
        return new DocumentProvider(d);
    }

    public FileState loadFile(@NonNull String filename) {
        FileState doc = files.get(filename);
        if (doc != null) {
            if (doc.isChanged()) {
                doc = null;
            }
        }
        if (doc == null) {
            try {
                DocumentProvider _hDoc = getDocument(filename);
                files.remove(filename);
                files.put(filename, new FileState(_hDoc, filename));
            } catch (IOException ignored) {
            }
        }
        return files.get(filename);
    }

    public void onCloseFileTab(String fileName) {
        FileState fileState = files.get(fileName);
        if (fileState != null) fileState.save(true);
        files.remove(fileName);
    }

    public void onCloseOtherTab(String fileName) {
        FileState doc = files.get(fileName);
        reloadAll();
        files.clear();
        if (doc != null) files.put(fileName, doc);
    }

    public void reloadAll() {
        Collection<FileState> values = files.values();
        for (FileState file : values) {
            file.reload();
        }
    }

    public long saveAll(boolean auto) {
        long ret = 1;
        Collection<FileState> values = files.values();
        for (FileState file : values) {
            file.save(auto);
            if (file._timestamp > ret) ret = file._timestamp;
        }
        return ret;
    }

    public int countNeedSave() {
        int ret = 0;
        Collection<FileState> values = files.values();
        for (FileState file : values) {
            if (file.checkNeedSave()) ++ret;
        }
        return ret;
    }
}
