// AnalyzeDefinition.java

package net.sf.gogui.gtp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.StringUtil;

/** Definition of an analyze command.
    See GoGui documentation, chapter "Analyze Commands".
    This class is immutable. */
public class AnalyzeDefinition
{
    public AnalyzeDefinition(String line)
    {
        String array[] = line.split("/");
        String typeStr = array[0];
        switch (typeStr) {
            case "bwboard":
                m_type = AnalyzeType.BWBOARD;
                break;
            case "cboard":
                m_type = AnalyzeType.CBOARD;
                break;
            case "dboard":
                m_type = AnalyzeType.DBOARD;
                break;
            case "eplist":
                m_type = AnalyzeType.EPLIST;
                break;
            case "gfx":
                m_type = AnalyzeType.GFX;
                break;
            case "hstring":
                m_type = AnalyzeType.HSTRING;
                break;
            case "hpstring":
                m_type = AnalyzeType.HPSTRING;
                break;
            case "param":
                m_type = AnalyzeType.PARAM;
                break;
            case "plist":
                m_type = AnalyzeType.PLIST;
                break;
            case "pspairs":
                m_type = AnalyzeType.PSPAIRS;
                break;
            case "pstring":
                m_type = AnalyzeType.PSTRING;
                break;
            case "string":
                m_type = AnalyzeType.STRING;
                break;
            case "sboard":
                m_type = AnalyzeType.SBOARD;
                break;
            case "var":
                m_type = AnalyzeType.VAR;
                break;
            case "varb":
                m_type = AnalyzeType.VARB;
                break;
            case "varc":
                m_type = AnalyzeType.VARC;
                break;
            case "varp":
                m_type = AnalyzeType.VARP;
                break;
            case "varpo":
                m_type = AnalyzeType.VARPO;
                break;
            case "varw":
                m_type = AnalyzeType.VARW;
                break;
            default:
                m_type = AnalyzeType.NONE;
                break;
        }
        m_label = array[1];
        m_command = array[2];
    }

    public AnalyzeDefinition(AnalyzeType type, String label, String command)
    {
        m_type = type;
        m_label = label;
        m_command = command;
    }

    public String getCommand()
    {
        return m_command;
    }

    public String getLabel()
    {
        return m_label;
    }

    public AnalyzeType getType()
    {
        return m_type;
    }

    /** *  Should the response be shown as text.Returns true for types that should be shown (not necessarily only)
        as text to the user.
        That is string and variation commands.
     * @return  */
    public boolean isTextType()
    {
        return m_type == AnalyzeType.STRING
            || m_type == AnalyzeType.HSTRING
            || m_type == AnalyzeType.HPSTRING
            || m_type == AnalyzeType.PSTRING
            || m_type == AnalyzeType.VAR
            || m_type == AnalyzeType.VARC
            || m_type == AnalyzeType.VARW
            || m_type == AnalyzeType.VARB
            || m_type == AnalyzeType.VARP
            || m_type == AnalyzeType.VARPO;
    }

    public boolean needsColorArg()
    {
        return (m_command.contains("%c"));
    }

    public boolean needsFileArg()
    {
        return (m_command.contains("%f"));
    }

    public boolean needsFileOpenArg()
    {
        return (m_command.contains("%r"));
    }

    public boolean needsFileSaveArg()
    {
        return (m_command.contains("%w"));
    }

    public boolean needsOnlyPointArg()
    {
        return (needsPointArg()
                && ! needsColorArg()
                && ! needsFileArg()
                && ! needsFileOpenArg()
                && ! needsFileSaveArg()
                && ! needsPointListArg()
                && ! needsStringArg()
                && ! needsOptStringArg());
    }

    public boolean needsOnlyPointAndColorArg()
    {
        return (needsPointArg() && needsColorArg()
                && ! needsFileArg()
                && ! needsFileOpenArg()
                && ! needsFileSaveArg()
                && ! needsPointListArg()
                && ! needsStringArg()
                && ! needsOptStringArg());
    }

    public boolean needsPointArg()
    {
        return (m_command.contains("%p"));
    }

    public boolean needsPointListArg()
    {
        return (m_command.contains("%P") || m_type == AnalyzeType.EPLIST);
    }

    public boolean needsStringArg()
    {
        return (m_command.contains("%s"));
    }

    public boolean needsOptStringArg()
    {
        return (m_command.contains("%o"));
    }

    public static ArrayList<AnalyzeDefinition>
        read(ArrayList<String> supportedCommands, File analyzeCommands,
             String programAnalyzeCommands)
        throws ErrorMessage
    {
        if (analyzeCommands != null)
        {
            try
            {
                Reader fileReader = new FileReader(analyzeCommands);
                BufferedReader reader = new BufferedReader(fileReader);
                return readConfig(reader, analyzeCommands.getName(), null);
            }
            catch (FileNotFoundException e)
            {
                throw new ErrorMessage("File \"" + analyzeCommands
                                       + "\" not found");
            }
        }
        else if (programAnalyzeCommands != null)
        {
            Reader stringReader = new StringReader(programAnalyzeCommands);
            BufferedReader reader = new BufferedReader(stringReader);
            return readConfig(reader,
                              "program response to gogui-analyze_commands",
                              null);
        }
        else
        {
            String resource = "net/sf/gogui/gui/analyze-commands";
            URL url = ClassLoader.getSystemClassLoader().getResource(resource);
            if (url == null)
                return new ArrayList<>();
            try
            {
                InputStream inputStream = url.openStream();
                Reader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(inputStreamReader);
                return readConfig(reader, "builtin default commands",
                                  supportedCommands);
            }
            catch (IOException e)
            {
                throw new ErrorMessage(e.getMessage());
            }
        }
    }

    private final AnalyzeType m_type;

    private final String m_label;

    private final String m_command;

    private static ArrayList<AnalyzeDefinition>
        readConfig(BufferedReader reader, String name,
                   ArrayList<String> supportedCommands) throws ErrorMessage
    {
        ArrayList<AnalyzeDefinition> result
            = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        try
        {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null)
            {
                ++lineNumber;
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '#')
                {
                    String array[] = line.split("/");
                    if (array.length < 3 || array.length > 5)
                        throw new ErrorMessage("Error in " + name + " line "
                                               + lineNumber);
                    if (supportedCommands != null)
                    {
                        String[] cmdArray
                            = StringUtil.splitArguments(array[2].trim());
                        if (cmdArray.length == 0
                            || ! supportedCommands.contains(cmdArray[0]))
                            continue;
                    }
                    String label = array[1];
                    if (labels.contains(label))
                        continue;
                    labels.add(label);
                    result.add(new AnalyzeDefinition(line));
                }
            }
            return result;
        }
        catch (IOException e)
        {
            throw new ErrorMessage("Error reading " + name);
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (IOException e)
            {
                throw new ErrorMessage("Error reading " + name);
            }
        }
    }
}
