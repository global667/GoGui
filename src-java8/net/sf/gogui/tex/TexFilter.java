// TexFilter.java

package net.sf.gogui.tex;

import java.io.File;
import javax.swing.filechooser.FileFilter;
import net.sf.gogui.util.FileUtil;

/** File filter for accepting LaTeX files. */
public class TexFilter
    extends FileFilter
{
    @Override
    public boolean accept(File file)
    {
        if (file.isDirectory())
            return true;
        return FileUtil.hasExtension(file, "tex");
    }

    @Override
    public String getDescription()
    {
        return "LaTex files (*.tex)";
    }

}
