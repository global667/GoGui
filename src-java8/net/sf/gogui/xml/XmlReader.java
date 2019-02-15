// XmlReader.java

package net.sf.gogui.xml;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import net.sf.gogui.game.GameInfo;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.MarkType;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.StringInfo;
import net.sf.gogui.game.StringInfoColor;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.InvalidKomiException;
import net.sf.gogui.go.InvalidPointException;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;
import net.sf.gogui.sgf.SgfUtil;
import net.sf.gogui.util.ByteCountInputStream;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.ProgressShow;

/** Read files in Jago's XML format.
    This class reads files in Jago's XML format, see
    http://www.rene-grothmann.de/jago. It can understand valid XML files
    according to the go.dtd from the Jago webpage (10/2007) and also handles
    some deviations used by Jago or in the examples used on the Jago
    webpage, see also the appendix "XML Format" of the GoGui documentation.
    The implementation uses SAX for memory efficient parsing of large files. */
public final class XmlReader
{
    /** Construct reader and read.
     * @param in
        @param progressShow Callback to show progress, can be null
        @param streamSize Size of stream if progressShow != null
     * @throws net.sf.gogui.util.ErrorMessage */
    public XmlReader(InputStream in, ProgressShow progressShow,
                     long streamSize)
        throws ErrorMessage
    {
        m_progressShow = progressShow;
        m_streamSize = streamSize;
        if (progressShow != null)
        {
            progressShow.showProgress(0);
            m_byteCountInputStream = new ByteCountInputStream(in);
            in = m_byteCountInputStream;
        }
        try
        {
            m_isFirstElement = true;
            m_isFirstNode = true;
            m_gameInfoPreByoyomi = -1;
            m_root = new Node();
            // Don't create game info yet, because implicit empty root
            // might be truncated later
            m_info = new GameInfo();
            m_node = m_root;
            XMLReader reader = XMLReaderFactory.createXMLReader();
            try
            {
                reader.setFeature("http://xml.org/sax/features/validation",
                                  false);
            }
            catch (SAXException e)
            {
            }
            Handler handler = new Handler();
            reader.setContentHandler(handler);
            reader.setEntityResolver(handler);
            reader.setErrorHandler(handler);
            reader.parse(new InputSource(in));
            int size;
            if (m_isBoardSizeKnown)
                size = m_boardSize;
            else
                size = Math.max(DEFAULT_BOARDSIZE, m_boardSize);
            m_tree = new GameTree(size, m_root);
            m_tree.getGameInfo(m_root).copyFrom(m_info);
            if (m_gameName != null)
                m_root.addSgfProperty("GN", m_gameName);
        }
        catch (SAXException e)
        {
            throw new ErrorMessage(e.getMessage());
        }
        catch (IOException e)
        {
            throw new ErrorMessage(e.getMessage());
        }
        finally
        {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    public GameTree getTree()
    {
        return m_tree;
    }

    public String getWarnings()
    {
        if (m_warnings.isEmpty())
            return null;
        StringBuilder result = new StringBuilder(m_warnings.size() * 80);
        m_warnings.stream().map((s) -> {
            result.append(s);
            return s;
        }).forEachOrdered((_item) -> {
            result.append('\n');
        });
        return result.toString();
    }

    private class Handler
        extends DefaultHandler
    {
        @Override
        public void startElement(String namespaceURI, String name,
                                 String qualifiedName, Attributes atts)
            throws SAXException
        {
            if (m_progressShow != null)
                showProgress();
            checkNoCharacters();
            m_element = name;
            m_atts = atts;
            if (m_isFirstElement)
            {
                if (! m_element.equals("Go"))
                    throw new SAXException("Not a Go game");
                m_isFirstElement = false;
            }
            switch (name) {
                case "Annotation":
                    startInfoElemWithoutFormat();
                    break;
                case "Application":
                    startInfoElemWithFormat();
                    break;
                case "AddBlack":
                    startSetup(BLACK);
                    break;
                case "AddWhite":
                    startSetup(WHITE);
                    break;
                case "Arg":
                    checkParent("SGF");
                    break;
                case "at":
                    checkParent("Black", "White", "AddBlack", "AddWhite", "Delete",
                            "Mark");
                    break;
                case "Black":
                    startMove(BLACK);
                    break;
                case "BlackPlayer":
                    startInfoElemWithFormat();
                    break;
                case "BlackRank":
                    startInfoElemWithFormat();
                    break;
                case "BlackTeam":
                    startInfoElemWithoutFormat();
                    break;
                case "BlackToPlay":
                    startToPlay(BLACK);
                    break;
                case "BoardSize":
                    startInfoElemWithFormat();
                    break;
                case "Comment":
                    startComment();
                    break;
                case "Copyright":
                    startCopyright();
                    break;
                case "Date":
                    startInfoElemWithFormat();
                    break;
                case "Delete":
                    startSetup(EMPTY);
                    break;
                case "Go":
                    startGo();
                    break;
                case "GoGame":
                    startGoGame();
                    break;
                case "Handicap":
                    startInfoElemWithFormat();
                    break;
                case "Information":
                    startInformation();
                    break;
                case "Line":
                    startLine();
                    break;
                case "Komi":
                    startInfoElemWithFormat();
                    break;
                case "Mark":
                    startMark();
                    break;
                case "Node":
                    startNode();
                    break;
                case "Nodes":
                    startNodes();
                    break;
                case "P":
                    startP();
                    break;
                case "Result":
                    startInfoElemWithFormat();
                    break;
                case "Round":
                    startInfoElemWithoutFormat();
                    break;
                case "Rules":
                    startInfoElemWithFormat();
                    break;
                case "Source":
                    startInfoElemWithFormat();
                    break;
                case "SGF":
                    startSGF();
                    break;
                case "Time":
                    startInfoElemWithFormat();
                    break;
                case "User":
                    startInfoElemWithoutFormat();
                    break;
                case "Variation":
                    startVariation();
                    break;
                case "White":
                    startMove(WHITE);
                    break;
                case "WhitePlayer":
                    startInfoElemWithFormat();
                    break;
                case "WhiteRank":
                    startInfoElemWithFormat();
                    break;
                case "WhiteTeam":
                    startInfoElemWithoutFormat();
                    break;
                case "WhiteToPlay":
                    startToPlay(WHITE);
                    break;
                default:
                    setWarning("Ignoring unknown element: " + name);
                    break;
            }
            m_elementStack.push(name);
            m_characters.setLength(0);
        }

        @Override
        public void endElement(String namespaceURI, String name,
                               String qualifiedName) throws SAXException
        {
            m_element = m_elementStack.pop();
            switch (name) {
                case "AddBlack":
                    endSetup(BLACK);
                    break;
                case "AddWhite":
                    endSetup(WHITE);
                    break;
                case "Annotation":
                    m_info.set(StringInfo.ANNOTATION, getCharacters());
                    break;
                case "Arg":
                    m_sgfArgs.add(getCharacters());
                    break;
                case "at":
                    endAt();
                    break;
                case "Black":
                    endMove(BLACK);
                    break;
                case "BlackPlayer":
                    m_info.set(StringInfoColor.NAME, BLACK, getCharacters());
                    break;
                case "BlackRank":
                    m_info.set(StringInfoColor.RANK, BLACK, getCharacters());
                    break;
                case "BlackTeam":
                    m_info.set(StringInfoColor.TEAM, BLACK, getCharacters());
                    break;
                case "BlackToPlay":
                    endToPlay();
                    break;
                case "BoardSize":
                    endBoardSize();
                    break;
                case "Comment":
                    endComment();
                    break;
                case "Copyright":
                    endCopyright();
                    break;
                case "Date":
                    m_info.set(StringInfo.DATE, getCharacters());
                    break;
                case "Delete":
                    endSetup(EMPTY);
                    break;
                case "Go":
                    checkNoCharacters();
                    break;
                case "GoGame":
                    checkNoCharacters();
                    break;
                case "Handicap":
                    endHandicap();
                    break;
                case "Information":
                    checkNoCharacters();
                    break;
                case "Komi":
                    endKomi();
                    break;
                case "Mark":
                    endMark();
                    break;
                case "Node":
                    endNode();
                    break;
                case "Nodes":
                    checkNoCharacters();
                    break;
                case "P":
                    endP();
                    break;
                case "Result":
                    m_info.set(StringInfo.RESULT, getCharacters());
                    break;
                case "Round":
                    m_info.set(StringInfo.ROUND, getCharacters());
                    break;
                case "Rules":
                    m_info.set(StringInfo.RULES, getCharacters());
                    break;
                case "SGF":
                    endSgf();
                    break;
                case "Source":
                    m_info.set(StringInfo.SOURCE, getCharacters());
                    break;
                case "Time":
                    endTime();
                    break;
                case "User":
                    m_info.set(StringInfo.USER, getCharacters());
                    break;
                case "White":
                    endMove(WHITE);
                    break;
                case "WhitePlayer":
                    m_info.set(StringInfoColor.NAME, WHITE, getCharacters());
                    break;
                case "WhiteRank":
                    m_info.set(StringInfoColor.RANK, WHITE, getCharacters());
                    break;
                case "WhiteTeam":
                    m_info.set(StringInfoColor.TEAM, WHITE, getCharacters());
                    break;
                case "WhiteToPlay":
                    endToPlay();
                    break;
                case "Variation":
                    endVariation();
                    break;
                default:
                    break;
            }
            m_characters.setLength(0);
        }

        @Override
        public void characters(char[] ch, int start, int length)
            throws SAXException
        {
            m_characters.append(ch, start, length);
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException
        {
            throwError(e.getMessage());
        }

        /** Return a fake go.dtd, if go.dtd does not exist as file.
            GoGui does not validate the document anyway, but this avoids a
            missing entity error message, if an XML file references go.dtd,
            but it is not found. */
        @Override
        public InputSource resolveEntity(String publicId, String systemId)
        {
            if (systemId == null)
                return null;
            URI uri;
            try
            {
                uri = new URI(systemId);
            }
            catch (URISyntaxException e)
            {
                return null;
            }
            if (! "file".equals(uri.getScheme()))
                return null;
            File file = new File(uri.getPath());
            if (file.exists() || ! "go.dtd".equals(file.getName()))
                return null;
            String text = "<?xml version='1.0' encoding='UTF-8'?>";
            return new InputSource(new ByteArrayInputStream(text.getBytes()));
        }

        @Override
        public void setDocumentLocator(Locator locator)
        {
            m_locator = locator;
        }

        @Override
        public void error(SAXParseException e)
        {
            setWarning(e.getMessage());
        }

        @Override
        public void warning(SAXParseException e)
        {
            setWarning(e.getMessage());
        }
    }

    private static final int DEFAULT_BOARDSIZE = 19;

    private boolean m_isFirstElement;

    private boolean m_isFirstNode;

    private boolean m_isBoardSizeKnown;

    private int m_numberGames;

    private int m_numberTrees;

    /** Board size.
        If board size is not explicitely set, this variable is used to track
        the maximum size necessary for all points seen. */
    private int m_boardSize;

    private int m_lastPercent;

    private final long m_streamSize;

    /** Element stack. */
    private Stack<String> m_elementStack = new Stack<>();

    /** Current node. */
    private Node m_node;

    private Stack<Node> m_variation = new Stack<>();

    private GameInfo m_info = new GameInfo();

    private Node m_root;

    private GameTree m_tree;

    /** Current element */
    private String m_element;

    /** Attributes of current element */
    private Attributes m_atts;

    /** Type of current SGF element. */
    private String m_sgfType;

    /** Arguments of current SGF element. */
    private ArrayList<String> m_sgfArgs = new ArrayList<>();

    /** Characters in current element. */
    private StringBuilder m_characters = new StringBuilder();

    /** Contains strings with warnings. */
    private final Set<String> m_warnings = new TreeSet<>();

    private Locator m_locator;

    /** Current mark type in Mark element. */
    private MarkType m_markType;

    /** Current label in Mark element. */
    private String m_label;

    private String m_gameName;

    private ByteCountInputStream m_byteCountInputStream;

    private final ProgressShow m_progressShow;

    /** Time settings information for current node from legacy SGF
        properties. */
    private int m_byoyomiMoves;

    /** Time settings information for current node from legacy SGF
        properties. */
    private long m_byoyomi;

    /** Time settings information for current node from legacy SGF
        properties. */
    private long m_preByoyomi;

    private long m_gameInfoPreByoyomi;

    /** Has current node inconsistent SGF/FF3 overtime settings properties. */
    private boolean m_ignoreOvertime;

    private String m_paragraphElementText;

    private void checkAttributes(String... atts) throws SAXException
    {
        List<String> list = Arrays.asList(atts);
        for (int i = 0; i < m_atts.getLength(); ++i)
        {
            String name = m_atts.getLocalName(i);
            if (! list.contains(name))
                setWarning("Unknown attribute \"" + name + "\" for element \""
                           + m_element + "\"");
        }
    }

    private void checkNoCharacters() throws SAXException
    {
        if (! getCharacters().trim().equals(""))
            setWarning("Cannot handle text content in element \"" + m_element
                       + "\"");
    }

    private void checkRoot() throws SAXException
    {
        String parent = parentElement();
        if (parent != null)
            throwError("Element \"" + m_element + "\" cannot be child of \""
                       + parent + "\"");
    }

    private void checkParent(String... parents) throws SAXException
    {
        String parent = parentElement();
        if (! Arrays.asList(parents).contains(parent))
            throwError("Element \"" + m_element + "\" cannot be child of \""
                       + parent + "\"");
    }

    private void createNode()
    {
        Node node = new Node();
        if (m_node != null)
            m_node.append(node);
        else if (! m_variation.isEmpty())
            m_variation.peek().getFather().append(node);
        m_node = node;
    }

    private void endAt() throws SAXException
    {
        GoPoint p = getPoint(getCharacters());
        String parent = parentElement();
        switch (parent) {
            case "Black":
                m_node.setMove(Move.get(BLACK, p));
                break;
            case "White":
                m_node.setMove(Move.get(WHITE, p));
                break;
            case "AddBlack":
                m_node.addStone(BLACK, p);
                break;
            case "AddWhite":
                m_node.addStone(WHITE, p);
                break;
            case "Delete":
                m_node.addStone(EMPTY, p);
                break;
            case "Mark":
                if (m_markType != null)
                    m_node.addMarked(p, m_markType);
                if (m_label != null)
                    m_node.setLabel(p, m_label);
                break;
            default:
                break;
        }
    }

    private void endBoardSize() throws SAXException
    {
        int boardSize = parseInt();
        if (boardSize < 1 || boardSize > GoPoint.MAX_SIZE)
            throw new SAXException("Unsupported board size");
        m_isBoardSizeKnown = true;
        m_boardSize = boardSize;
    }

    private void endComment()
    {
        m_node.setComment(getParagraphElementText());
    }

    private void endCopyright()
    {
        m_info.set(StringInfo.COPYRIGHT, getParagraphElementText());
    }

    private void endHandicap() throws SAXException
    {
        int handicap = parseInt();
        if (handicap == 1 || handicap < 0)
            setWarning("Ignoring invalid handicap: " + handicap);
        else
            m_info.setHandicap(handicap);
    }

    private void endKomi() throws SAXException
    {
        String komi = getCharacters();
        try
        {
            m_info.setKomi(Komi.parseKomi(komi));
        }
        catch (InvalidKomiException e)
        {
            setWarning("Invalid komi: " + komi);
        }
    }

    private void endMark() throws SAXException
    {
        // According to the DTD, mark cannot contain
        // text content, but we accept it, if the point is text content
        // instead of a at-subelement or an at-attribute
        String value = getCharacters();
        if (! value.trim().equals(""))
        {
            GoPoint p = getPoint(value);
            if (m_markType != null)
                m_node.addMarked(p, m_markType);
            if (m_label != null)
                m_node.setLabel(p, m_label);
        }
    }

    private void endMove(GoColor c) throws SAXException
    {
        // According to the DTD, Black and White cannot contain text
        // content, but we accept it, if the move is text content instead
        // of a at-subelement or an at-attribute
        String value = getCharacters();
        if (! value.trim().equals(""))
            m_node.setMove(Move.get(c, getPoint(value)));
    }

    private void endNode() throws SAXException
    {
        checkNoCharacters();
        setSgfTimeSettings();
    }

    private void endP() throws SAXException
    {
        m_paragraphElementText =
            m_paragraphElementText + getMergedLines() + "\n";
    }

    private void endSetup(GoColor c) throws SAXException
    {
        // According to the DTD, AddBlack, AddWhite, and Delete cannot contain
        // text content, but we accept it, if the point is text content instead
        // of a at-subelement or an at-attribute
        String value = getCharacters();
        if (! value.trim().equals(""))
            m_node.addStone(c, getPoint(value));
    }

    private void endSgf() throws SAXException
    {
        checkNoCharacters();
        if (m_sgfType == null)
            return;
        switch (m_sgfType) {
            case "AN":
                endSgfInfo(StringInfo.ANNOTATION);
                break;
            case "BL":
                endSgfTimeLeft(BLACK);
                break;
            case "BR":
                endSgfInfo(StringInfoColor.RANK, BLACK);
                break;
            case "BT":
                endSgfInfo(StringInfoColor.TEAM, BLACK);
                break;
            case "CP":
                endSgfInfo(StringInfo.COPYRIGHT);
                break;
            case "DT":
                endSgfInfo(StringInfo.DATE);
                break;
            case "HA":
                endSgfHandicap();
                break;
            case "OB":
                endSgfMovesLeft(BLACK);
                break;
            case "OM":
                endSgfOvertimeMoves();
                break;
            case "OP":
                endSgfOvertimePeriod();
                break;
            case "OT":
                endSgfOvertime();
                break;
            case "OW":
                endSgfMovesLeft(WHITE);
                break;
            case "KM":
                endSgfKomi();
                break;
            case "PB":
                endSgfInfo(StringInfoColor.NAME, BLACK);
                break;
            case "PW":
                endSgfInfo(StringInfoColor.NAME, WHITE);
                break;
            case "PL":
                endSgfPlayer();
                break;
            case "RE":
                endSgfInfo(StringInfo.RESULT);
                break;
            case "RO":
                endSgfInfo(StringInfo.ROUND);
                break;
            case "RU":
                endSgfInfo(StringInfo.RULES);
                break;
            case "SL":
                endSgfSelect();
                break;
            case "WL":
                endSgfTimeLeft(WHITE);
                break;
            case "TM":
                endSgfTime();
                break;
            case "WR":
                endSgfInfo(StringInfoColor.RANK, WHITE);
                break;
            case "WT":
                endSgfInfo(StringInfoColor.TEAM, WHITE);
                break;
            case "US":
                endSgfInfo(StringInfo.USER);
                break;
            default:
                m_node.addSgfProperty(m_sgfType, m_sgfArgs);
                break;
        }
    }

    /** Handle non-root handicap info from SGF properties. */
    private void endSgfHandicap()
    {
        if (m_sgfArgs.isEmpty())
            return;
        try
        {
            int handicap = Integer.parseInt(m_sgfArgs.get(0));
            GameInfo info = m_node.createGameInfo();
            info.setHandicap(handicap);
        }
        catch (NumberFormatException e)
        {
        }
    }

    /** Handle non-root game info from SGF properties. */
    private void endSgfInfo(StringInfo type)
    {
        if (m_sgfArgs.isEmpty())
            return;
        GameInfo info = m_node.createGameInfo();
        info.set(type, m_sgfArgs.get(0));
    }

    /** Handle non-root game info from SGF properties. */
    private void endSgfInfo(StringInfoColor type, GoColor c)
    {
        if (m_sgfArgs.isEmpty())
            return;
        GameInfo info = m_node.createGameInfo();
        info.set(type, c, m_sgfArgs.get(0));
    }

    /** Handle non-root komi from SGF properties. */
    private void endSgfKomi()
    {
        if (m_sgfArgs.isEmpty())
            return;
        try
        {
            Komi komi = Komi.parseKomi(m_sgfArgs.get(0));
            GameInfo info = m_node.createGameInfo();
            info.setKomi(komi);
        }
        catch (InvalidKomiException e)
        {
        }
    }

    private void endSgfMovesLeft(GoColor c)
    {
        if (m_sgfArgs.isEmpty())
            return;
        try
        {
            int movesLeft = Integer.parseInt(m_sgfArgs.get(0));
            if (movesLeft >= 0)
                m_node.setMovesLeft(c, movesLeft);
        }
        catch (NumberFormatException e)
        {
        }
    }

    /** FF4 OT property */
    private void endSgfOvertime()
    {
        if (m_sgfArgs.isEmpty())
            return;
        String value = m_sgfArgs.get(0).trim();
        if (value.equals("") || value.equals("-"))
            return;
        SgfUtil.Overtime overtime = SgfUtil.parseOvertime(value);
        if (overtime == null)
        {
            setWarning("Overtime settings in unknown format");
            m_node.addSgfProperty("OT", value); // Preserve information
        }
        else
        {
            m_byoyomi = overtime.m_byoyomi;
            m_byoyomiMoves = overtime.m_byoyomiMoves;
        }
    }

    /** FF3 OM property */
    private void endSgfOvertimeMoves()
    {
        if (m_sgfArgs.isEmpty())
            return;
        try
        {
            m_byoyomiMoves = Integer.parseInt(m_sgfArgs.get(0));
        }
        catch (NumberFormatException e)
        {
            setWarning("Invalid value for byoyomi moves");
            m_ignoreOvertime = true;
        }
    }

    /** FF3 OP property */
    private void endSgfOvertimePeriod()
    {
        if (m_sgfArgs.isEmpty())
            return;
        try
        {
            m_byoyomi = (long)(Double.parseDouble(m_sgfArgs.get(0)) * 1000);
        }
        catch (NumberFormatException e)
        {
            setWarning("Invalid value for byoyomi time");
            m_ignoreOvertime = true;
        }
    }

    private void endSgfPlayer()
    {
        if (m_sgfArgs.isEmpty())
            return;
        String value = m_sgfArgs.get(0).trim().toLowerCase(Locale.ENGLISH);
        GoColor c;
        switch (value) {
            case "b":
            case "black":
                c = BLACK;
                break;
            case "w":
            case "white":
                c = WHITE;
                break;
            default:
                return;
        }
        m_node.setPlayer(c);
    }

    private void endSgfSelect() throws SAXException
    {
        for (int i = 0; i < m_sgfArgs.size(); ++i)
            m_node.addMarked(getSgfPoint(m_sgfArgs.get(i)), MarkType.SELECT);
    }

    /** Handle BL, WL SGF properties.
        XmlWriter uses these legacy SGF properties to preserve time left
        information that cannot be stored in a timleft-attribute of a move,
        because the node has no move or not a move of the corresponding color.
        Jago's blacktime/whitetime Node-attribute is not defined in
        go.dtd (2007) */
    private void endSgfTimeLeft(GoColor c)
    {
        if (m_sgfArgs.isEmpty())
            return;
        try
        {
            double timeLeft = Double.parseDouble(m_sgfArgs.get(0));
            m_node.setTimeLeft(c, timeLeft);
        }
        catch (NumberFormatException e)
        {
        }
    }

    private void endSgfTime()
    {
        if (m_sgfArgs.isEmpty())
            return;
        String value = m_sgfArgs.get(0).trim();
        if (value.equals("") || value.equals("-"))
            return;
        long preByoyomi = SgfUtil.parseTime(value);
        if (preByoyomi < 0)
        {
            setWarning("Unknown format in time property");
            m_node.addSgfProperty("TM", value); // Preserve information
        }
        else
            m_preByoyomi = preByoyomi;

    }

    private void endTime() throws SAXException
    {
        String value = getCharacters().trim();
        if (value.equals("") || value.equals("-"))
            return;
        long preByoyomi = SgfUtil.parseTime(value);
        if (preByoyomi < 0)
        {
            setWarning("Unknown format in Time element");
            m_node.addSgfProperty("TM", value); // Preserve information
        }
        else
        {
            // Set time settings now but also remember value, because time
            // settings could be overwritten in setSgfTimeSettings() after
            // overtime information is known from SGF element
            m_gameInfoPreByoyomi = preByoyomi;
            TimeSettings timeSettings = new TimeSettings(preByoyomi);
            m_info.setTimeSettings(timeSettings);
        }
    }

    private void endToPlay() throws SAXException
    {
        if (! getCharacters().trim().equals(""))
            setWarning("Ignoring text content in element \"" + m_element
                       + "\"");
    }

    private void endVariation() throws SAXException
    {
        checkNoCharacters();
        m_node = m_variation.pop();
    }

    private String getCharacters()
    {
        return m_characters.toString();
    }

    private String getMergedLines()
    {
        String chars = getCharacters();
        StringBuilder result = new StringBuilder(chars.length());
        BufferedReader reader = new BufferedReader(new StringReader(chars));
        try
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (result.length() > 0)
                    result.append(' ');
                result.append(line);
            }
        }
        catch (IOException e)
        {
            assert(false);
        }
        return result.toString();
    }

    private String getParagraphElementText()
    {
        String text = m_paragraphElementText;
        String mergedLines = getMergedLines();
        // Handle direct text content even if not allowed by DTD
        if (! mergedLines.equals(""))
            text = text + mergedLines + "\n";
        // Remove exactly one trailing newline
        if (text.endsWith("\n"))
            text = text.substring(0, text.length() - 1);
        return text;
    }

    private GoPoint getPoint(String value) throws SAXException
    {
        value = value.trim();
        if (value.equals(""))
            return null;
        GoPoint p;
        try
        {
            if (m_isBoardSizeKnown)
                p = GoPoint.parsePoint(value, m_boardSize);
            else
            {
                p = GoPoint.parsePoint(value, GoPoint.MAX_SIZE);
                if (p != null)
                {
                    m_boardSize = Math.max(m_boardSize, p.getX());
                    m_boardSize = Math.max(m_boardSize, p.getY());
                }
            }
            return p;
        }
        catch (InvalidPointException e)
        {
            throwError(e.getMessage());
        }
        return null; // Unreachable; avoid compiler error
    }

    private GoPoint getSgfPoint(String s) throws SAXException
    {
        s = s.trim().toLowerCase(Locale.ENGLISH);
        if (s.equals(""))
            return null;
        if (s.length() > 2
            || (s.length() == 2 && s.charAt(1) < 'a' || s.charAt(1) > 'z'))
            // Human-readable encoding as used by SmartGo
            return getPoint(s);
        else if (s.length() != 2)
            throwError("Invalid SGF coordinates: " + s);
        if (! m_isBoardSizeKnown)
        {
            // We need to know the boardsize for parsing SGF points to mirror
            // the y-coordinate and the size is not allowed to change later
            m_boardSize = DEFAULT_BOARDSIZE;
            m_isBoardSizeKnown = true;
        }
        if (s.equals("tt") && m_boardSize <= 19)
            return null;
        int x = s.charAt(0) - 'a';
        int y = m_boardSize - (s.charAt(1) - 'a') - 1;
        if (x < 0 || x >= m_boardSize || y < 0 || y >= m_boardSize)
        {
            if (x == m_boardSize && y == -1)
                // Some programs encode pass moves, e.g. as jj for boardsize 9
                return null;
            throwError("Coordinates \"" + s + "\" outside board size "
                       + m_boardSize);
        }
        return GoPoint.get(x, y);
    }

    private void setSgfTimeSettings()
    {
        long preByoyomi = m_preByoyomi;
        if (m_node == m_root && preByoyomi < 0)
            preByoyomi = m_gameInfoPreByoyomi;
        TimeSettings s = null;
        if (preByoyomi > 0
            && (m_ignoreOvertime || m_byoyomi <= 0 || m_byoyomiMoves <= 0))
            s = new TimeSettings(preByoyomi);
        else if (preByoyomi <= 0 && ! m_ignoreOvertime && m_byoyomi > 0
                 && m_byoyomiMoves > 0)
            s = new TimeSettings(0, m_byoyomi, m_byoyomiMoves);
        else if (preByoyomi > 0  && ! m_ignoreOvertime && m_byoyomi > 0
                 && m_byoyomiMoves > 0)
            s = new TimeSettings(preByoyomi, m_byoyomi, m_byoyomiMoves);
        if (s != null)
        {
            if (m_node == m_root)
                m_info.setTimeSettings(s);
            else
                m_node.createGameInfo().setTimeSettings(s);
        }
    }

    private void showProgress()
    {
        int percent;
        if (m_streamSize > 0)
        {
            long count = m_byteCountInputStream.getCount();
            percent = (int)(count * 100 / m_streamSize);
        }
        else
            percent = 100;
        if (percent != m_lastPercent)
            m_progressShow.showProgress(percent);
        m_lastPercent = percent;
    }

    private void startComment() throws SAXException
    {
        checkParent("Nodes", "Node", "Variation");
        checkAttributes();
        m_paragraphElementText = "";
    }

    private void startCopyright() throws SAXException
    {
        checkParent("Information");
        checkAttributes();
        m_paragraphElementText = "";
    }

    private void startGo() throws SAXException
    {
        checkRoot();
        checkAttributes();
    }

    private void startGoGame() throws SAXException
    {
        checkParent("Go");
        checkAttributes("name");
        String name = m_atts.getValue("name");
        if (name != null)
            // Not supported in game.GameInformation, put it in later
            // in SGF properties
            m_gameName = name;
        if (++m_numberGames > 1)
            throwError("Multiple games per file not supported");
    }

    private void startInfoElemWithFormat() throws SAXException
    {
        checkParent("Information");
        checkAttributes("format");
        String format = m_atts.getValue("format");
        if (format == null)
            return;
        format = format.trim().toLowerCase(Locale.ENGLISH);
        if (! format.equals("sgf"))
            setWarning("Unknown format attribute \"" + format
                       + "\" for element \"" + m_element + "\"");
    }

    private void startInfoElemWithoutFormat() throws SAXException
    {
        checkParent("Information");
        checkAttributes();
    }

    private void startInformation() throws SAXException
    {
        checkParent("GoGame");
        checkAttributes();
    }

    private void startLine() throws SAXException
    {
        // Line has no legal parent according to the DTD, so we
        // ignore it
        setWarning("Element \"Line\" cannot be child of element \""
                   + parentElement() + "\"");
    }

    private void startMark() throws SAXException
    {
        checkParent("Node");
        checkAttributes("at", "label", "territory", "type");
        m_markType = null;
        m_label = m_atts.getValue("label");
        String type = m_atts.getValue("type");
        String territory = m_atts.getValue("territory");
        if (type != null)
        {
            switch (type) {
                case "triangle":
                    m_markType = MarkType.TRIANGLE;
                    break;
                case "circle":
                    m_markType = MarkType.CIRCLE;
                    break;
                case "square":
                    m_markType = MarkType.SQUARE;
                    break;
                default:
                    setWarning("Unknown mark type " + type);
                    break;
            }
        }
        if (territory != null)
        {
            switch (territory) {
                case "black":
                    m_markType = MarkType.TERRITORY_BLACK;
                    break;
                case "white":
                    m_markType = MarkType.TERRITORY_WHITE;
                    break;
                default:
                    setWarning("Unknown territory type " + territory);
                    break;
            }
        }
        if (type == null && territory == null && m_label == null)
            m_markType = MarkType.MARK;
        String value = m_atts.getValue("at");
        if (value != null)
        {
            GoPoint p = getPoint(value);
            if (m_markType != null)
                m_node.addMarked(p, m_markType);
            if (m_label != null)
                m_node.setLabel(p, m_label);
        }
    }

    private void startMove(GoColor c) throws SAXException
    {
        checkParent("Node", "Nodes", "Variation");
        if (! parentElement().equals("Node"))
            createNode();
        checkAttributes("annotate", "at", "timeleft", "name", "number");
        String name = m_atts.getValue("name");
        if (name != null)
            // Not supported in game.Node, put it in SGF properties
            m_node.addSgfProperty("N", name);
        if (m_atts.getValue("annotate") != null)
            // Allowed by DTD, but unclear content and not supported in
            // game.Node
            setWarning("Attribute \"annotate\" in element \""
                       + m_element + "\" not supported");
        String value = m_atts.getValue("at");
        if (value != null)
            m_node.setMove(Move.get(c, getPoint(value)));
        value = m_atts.getValue("timeleft");
        if (value != null)
        {
            try
            {
                m_node.setTimeLeft(c, Double.parseDouble(value));
            }
            catch (NumberFormatException e)
            {
            }
        }
    }

    private void startNode() throws SAXException
    {
        checkParent("Nodes", "Variation");
        // blacktime and whitetime are not allowed in the DTD, but used
        // by Jago 5.0
        checkAttributes("blacktime", "name", "whitetime");
        // Don't create new node, if this is the first node and nothing
        // was added to the root node yet. This allows having an implicit
        // root node to handle cases like Comment being the first child of
        // Nodes (example on Jago's webpage) without creating an unnecessary
        // node if the first child of Nodes is a Node
        if (! m_isFirstNode || ! m_node.isEmpty())
            createNode();
        m_isFirstNode = false;
        String name = m_atts.getValue("name");
        if (name != null)
            // Not supported in game.Node, put it in SGF properties
            m_node.addSgfProperty("N", name);
        String value = m_atts.getValue("blacktime");
        if (value != null)
        {
            try
            {
                m_node.setTimeLeft(BLACK, Double.parseDouble(value));
            }
            catch (NumberFormatException e)
            {
            }
        }
        value = m_atts.getValue("whitetime");
        if (value != null)
        {
            try
            {
                m_node.setTimeLeft(WHITE, Double.parseDouble(value));
            }
            catch (NumberFormatException e)
            {
            }
        }
        m_ignoreOvertime = false;
        m_byoyomiMoves = -1;
        m_byoyomi = -1;
        m_preByoyomi = -1;
    }

    private void startNodes() throws SAXException
    {
        checkParent("GoGame");
        checkAttributes();
        if (++m_numberTrees > 1)
            throwError("More than one Nodes element in element GoGame");
    }

    private void startP() throws SAXException
    {
        checkParent("Comment", "Copyright");
        checkAttributes();
    }

    private void startSetup(GoColor c) throws SAXException
    {
        checkParent("Node");
        checkAttributes("at");
        String value = m_atts.getValue("at");
        if (value != null)
            m_node.addStone(c, getPoint(value));
    }

    private void startSGF() throws SAXException
    {
        checkParent("Node");
        checkAttributes("type");
        m_sgfType = m_atts.getValue("type");
        m_sgfArgs.clear();
    }

    private void startToPlay(GoColor c) throws SAXException
    {
        // According to the DTD, BlackToPlay and WhiteToPlay can never
        // occur in a valid document, because they have no legal parent.
        // I assume that they were meant to be child elements of Node
        // and set the player in setup positions
        checkParent("Node");
        checkAttributes();
        m_node.setPlayer(c);
    }

    private void startVariation() throws SAXException
    {
        checkParent("Nodes", "Variation");
        checkAttributes();
        if (m_node == null)
            throwError("Variation without main node");
        assert m_node.hasFather();
        m_variation.push(m_node);
        m_node = null;
    }

    private String parentElement()
    {
        if (m_elementStack.isEmpty())
            return null;
        return m_elementStack.peek();
    }

    private int parseInt() throws SAXException
    {
        try
        {
            return Integer.parseInt(getCharacters());
        }
        catch (NumberFormatException e)
        {
            throw new SAXException("Expected integer in element " + m_element);
        }
    }

    private void setWarning(String message)
    {
        m_warnings.add(message);
    }

    private void throwError(String message) throws SAXException
    {
        if (m_locator != null)
            message = "Line " + m_locator.getLineNumber() + ":"
                + m_locator.getColumnNumber() + ": " + message;
        throw new SAXException(message);
    }
}
