// Display.java

package net.sf.gogui.tools.display;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.EMPTY;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.GtpCallback;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpCommand;
import net.sf.gogui.gtp.GtpEngine;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpResponseFormatError;
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.gui.AnalyzeShow;
import net.sf.gogui.gui.GuiBoard;
import net.sf.gogui.gui.GuiBoardUtil;
import net.sf.gogui.gui.GuiUtil;
import net.sf.gogui.gui.LiveGfx;
import net.sf.gogui.gui.MessageDialogs;
import net.sf.gogui.gui.StatusBar;
import net.sf.gogui.util.LineReader;
import net.sf.gogui.util.StringUtil;

/** GTP adapter showing the current board in a window. */
public class Display
    extends GtpEngine
    implements LiveGfx.Listener
{
    public Display(String program, boolean verbose)
        throws Exception
    {
        super(null);
        m_size = GoPoint.DEFAULT_SIZE;
        m_board = new Board(m_size);
        m_frame = new JFrame();
        m_frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        WindowAdapter windowAdapter;
        windowAdapter = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeFrame();
            }
        };
        m_frame.addWindowListener(windowAdapter);
        Container contentPane = m_frame.getContentPane();
        m_guiBoard = new GuiBoard(m_size);
        m_guiBoard.setListener(new GuiBoard.Listener() {
                @Override
                public void contextMenu(GoPoint point, Component invoker,
                                        int x, int y)
                {
                }

                @Override
                public void fieldClicked(GoPoint point,
                                         boolean modifiedSelect)
                {
                    cbFieldClicked(point, modifiedSelect);
                }
            });
        contentPane.add(m_guiBoard);
        m_statusBar = new StatusBar();
        m_statusBar.showMoveText(false);
        contentPane.add(m_statusBar, BorderLayout.SOUTH);
        if (! StringUtil.isEmpty(program))
        {
            GtpClient.IOCallback ioCallback;
            ioCallback = new GtpClient.IOCallback() {
                @Override
                public void receivedInvalidResponse(String s) {
                }
                
                @Override
                public void receivedResponse(boolean error, String s) {
                }
                
                @Override
                public void receivedStdErr(String s) {
                    m_lineReader.add(s);
                    while (m_lineReader.hasLines())
                        m_liveGfx.handleLine(m_lineReader.getLine());
                }
                
                @Override
                public void sentCommand(String s) {
                }
                
                private final LineReader m_lineReader = new LineReader();
                
                private final LiveGfx m_liveGfx = new LiveGfx(Display.this);
            };
            m_gtp = new GtpClient(program, null, verbose, ioCallback);
            m_gtp.queryProtocolVersion();
            m_gtp.querySupportedCommands();
            m_guiBoard.setShowCursor(false);
            m_gtp.queryName();
            m_name = m_gtp.getLabel();
            String title = "gogui-display - " + m_name;
            m_frame.setTitle(title);
        }
        else
        {
            m_gtp = null;
            m_name = null;
            m_frame.setTitle("gogui-display");
        }
        registerCommands();
        GuiUtil.setGoIcon(m_frame);
        m_frame.pack();
        m_frame.setVisible(true);
    }

    public void close()
    {
        if (m_gtp != null)
        {
            m_gtp.close();
            m_gtp.waitForExit();
        }
        GuiUtil.invokeAndWait(() -> {
            if (m_frame != null)
            {
                m_messageDialogs.showInfo(m_frame,
                        "GTP stream was closed",
                        "", true);
                showStatus("GTP stream was closed");
            }
            else if (m_gtp == null)
                System.exit(0);
        });
    }

    public void cmdForward(GtpCommand cmd) throws GtpError
    {
        send(cmd.getLine(), cmd.getResponse());
    }

    @Override
    public void cmdName(GtpCommand cmd)
    {
        if (m_gtp == null)
            cmd.setResponse("gogui-display");
        else
            cmd.setResponse(m_name);
    }

    @Override
    public void cmdQuit(GtpCommand cmd) throws GtpError
    {
        if (m_gtp != null)
            send("quit", cmd.getResponse());
        setQuit();
    }

    @Override
    public void handleCommand(GtpCommand cmd) throws GtpError
    {
        showStatus(cmd.getLine());
        super.handleCommand(cmd);
    }

    @Override
    public void interruptCommand()
    {
        if (m_gtp == null)
            return;
        try
        {
            if (m_gtp.isInterruptSupported())
                m_gtp.sendInterrupt();
        }
        catch (GtpError e)
        {
            System.err.println(e);
        }
    }

    @Override
    public void showLiveGfx(final String text)
    {
        assert SwingUtilities.isEventDispatchThread();
        m_guiBoard.clearAll();
        GuiBoardUtil.updateFromGoBoard(m_guiBoard, m_board, false, false);
        AnalyzeShow.showGfx(text, m_guiBoard, m_statusBar, null);
    }

    /** Only accept this board size.
        A value of -1 means accept any size. */
    private int m_size;

    private final Board m_board;

    private GoColor m_color;

    private final GuiBoard m_guiBoard;

    private GoPoint m_fieldClicked;

    private final GtpClient m_gtp;

    private Move m_move;

    private final Object m_mutex = new Object();

    private JFrame m_frame;

    private final StatusBar m_statusBar;

    private final String m_name;

    private final MessageDialogs m_messageDialogs = new MessageDialogs();

    private void cbFieldClicked(GoPoint point, boolean modifiedSelect)
    {
        assert SwingUtilities.isEventDispatchThread();
        if (m_board.getColor(point) != EMPTY)
            return;
        synchronized (m_mutex)
        {
            if (modifiedSelect)
                m_fieldClicked = null;
            else
                m_fieldClicked = point;
            m_mutex.notifyAll();
        }
    }

    private void clearStatus()
    {
        m_statusBar.clear();
    }

    private void closeFrame()
    {
        assert SwingUtilities.isEventDispatchThread();
        if (m_gtp == null)
        {
            if (! m_messageDialogs.showQuestion(m_frame,
                                                "Terminate gogui-display?",
                                                "", "Terminate", true))
                return;
            System.exit(0);
        }
        m_frame.dispose();
        m_frame = null;
    }

    private void cmdBoardsize(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(1);
        int size = cmd.getIntArg(0, 1, GoPoint.MAX_SIZE);
        if (m_gtp != null)
        {
            String command = m_gtp.getCommandBoardsize(size);
            if (command != null)
                send(command);
            command = m_gtp.getCommandClearBoard(size);
            send(command);
        }
        m_size = size;
        GuiUtil.invokeAndWait(() -> {
            m_board.init(m_size);
            if (m_guiBoard.getBoardSize() != m_size)
            {
                m_guiBoard.initSize(m_size);
                m_frame.pack();
            }
            updateFromGoBoard();
        });
    }

    private void cmdClearBoard(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        if (m_gtp != null)
        {
            String command = m_gtp.getCommandClearBoard(m_size);
            send(command);
        }
        GuiUtil.invokeAndWait(() -> {
            m_board.clear();
            updateFromGoBoard();
        });
    }

    private void cmdGenmove(GtpCommand cmd) throws GtpError
    {
        GoColor color = cmd.getColorArg();
        GoPoint point;
        if (m_gtp == null)
        {
            if (m_frame == null)
                // can that happen?
                throw new GtpError("gogui-display terminated");
            m_color = color;
            showStatus("Input move for " + m_color
                       + " (Ctrl-button and click for pass)");
            synchronized (m_mutex)
            {
                try
                {
                    m_mutex.wait();
                }
                catch (InterruptedException e)
                {
                    System.err.println("InterruptedException");
                }
                point = m_fieldClicked;
            }
            cmd.setResponse(GoPoint.toString(point));
        }
        else
        {
            String command = m_gtp.getCommandGenmove(color);
            StringBuilder response = cmd.getResponse();
            send(command, response);
            if (response.toString().trim().equalsIgnoreCase("resign"))
                return;
            try
            {
                point = GtpUtil.parsePoint(response.toString(), m_size);
            }
            catch (GtpResponseFormatError e)
            {
                throw new GtpError(e.getMessage());
            }
        }
        m_move = Move.get(color, point);
        GuiUtil.invokeAndWait(() -> {
            m_board.play(m_move);
            updateFromGoBoard();
            if (m_gtp == null)
                clearStatus();
        });
    }

    private void cmdKomi(GtpCommand cmd) throws GtpError
    {
        if (m_gtp == null)
            return;
        send(cmd.getLine(), cmd.getResponse());
    }

    private void cmdPlaceFreeHandicap(GtpCommand cmd) throws GtpError
    {
        int n = cmd.getIntArg();
        ConstPointList stones = Board.getHandicapStones(m_size, n);
        if  (stones == null)
            throw new GtpError("Invalid number of handicap stones");
        StringBuilder pointList = new StringBuilder(128);
        for (GoPoint p : stones)
        {
            play(BLACK, p);
            if (pointList.length() > 0)
                pointList.append(' ');
            pointList.append(p);
        }
        cmd.setResponse(pointList.toString());
    }

    private void cmdPlay(GtpCommand cmd) throws GtpError
    {
        cmd.checkNuArg(2);
        GoColor color = cmd.getColorArg(0);
        GoPoint point = cmd.getPointArg(1, m_size);
        play(color, point);
    }

    private void cmdSetFreeHandicap(GtpCommand cmd) throws GtpError
    {
        for (int i = 0; i < cmd.getNuArg(); ++i)
            play(BLACK, cmd.getPointArg(i, m_size));
    }

    private void cmdUndo(GtpCommand cmd) throws GtpError
    {
        cmd.checkArgNone();
        undo();
    }

    private void play(GoColor color, GoPoint point) throws GtpError
    {
        if (m_gtp != null)
        {
            String command = m_gtp.getCommandPlay(Move.get(color, point));
            send(command);
        }
        m_move = Move.get(color, point);
        GuiUtil.invokeAndWait(() -> {
            m_board.play(m_move);
            updateFromGoBoard();
        });
    }

    private void registerCommands()
    {
        if (m_gtp != null)
        {
            GtpCallback forwardCallback = (GtpCommand cmd) -> {
                cmdForward(cmd);
            };
            ArrayList<String> commands = m_gtp.getSupportedCommands();
            commands.stream().filter((c) -> !(GtpUtil.isStateChangingCommand(c)
                    || c.equals("help")
                    || c.equals("known_command")
                    || c.equals("komi")
                    || c.equals("list_commands")
                    || c.equals("protocol_version"))).forEachOrdered((c) -> {
                        register(c, forwardCallback);
            });
        }
        register("boardsize", (GtpCommand cmd) -> {
            cmdBoardsize(cmd);
        });
        register("clear_board", (GtpCommand cmd) -> {
            cmdClearBoard(cmd);
        });
        register("genmove", (GtpCommand cmd) -> {
            cmdGenmove(cmd);
        });
        register("komi", this::cmdKomi);
        register("name", this::cmdName);
        register("place_free_handicap", this::cmdPlaceFreeHandicap);
        register("play", this::cmdPlay);
        register("quit", this::cmdQuit);
        register("set_free_handicap", this::cmdSetFreeHandicap);
        register("undo", this::cmdUndo);
        register("version", this::cmdVersion);
    }

    private void send(String cmd, StringBuilder response) throws GtpError
    {
        response.append(m_gtp.send(cmd));
    }

    private void send(String cmd) throws GtpError
    {
        m_gtp.send(cmd);
    }

    private void showStatus(final String text)
    {
        Runnable runnable = () -> {
            if (m_frame != null)
                m_statusBar.setText(text);
        };
        if (SwingUtilities.isEventDispatchThread())
            runnable.run();
        else
            GuiUtil.invokeAndWait(runnable);
    }

    private void undo() throws GtpError
    {
        if (m_gtp != null)
            send("undo");
        GuiUtil.invokeAndWait(() -> {
            m_board.undo();
            updateFromGoBoard();
        });
    }

    private void updateFromGoBoard()
    {
        m_guiBoard.clearAll(); // Live Gfx markup
        GuiBoardUtil.updateFromGoBoard(m_guiBoard, m_board, true, false);
        m_statusBar.clear();
        m_statusBar.setToPlay(m_board.getToMove());
    }
}
