// RecentMenu.java

package net.sf.gogui.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import net.sf.gogui.util.ObjectUtil;
import net.sf.gogui.util.Platform;
import net.sf.gogui.util.PrefUtil;

/** Menu for recent item.
    Handles removing duplicates and storing the items between sessions. */
public final class RecentMenu
{
    /** Listener for events generated by RecentMenu. */
    public interface Listener
    {
        void itemSelected(String label, String value);
    }

    /** The maximum number of items.
        If set to a value larger than 10, only the first 10 items will have
        a mnemonic key. */
    public static final int MAX_ITEMS = 10;

    /** Constructor.
        @param text The label of the menu. Supports marking the mnemonics in
        the label with a preceeding '&amp;' (like in Qt).
        @param path The absolute path name of the node in
        java.util.prefs.Preferences that is used to store the menu items.
        @param listener The callback to be called if a menu item is
        selected. */
    public RecentMenu(String text, String path, Listener listener)
    {
        assert listener != null;
        m_path = path;
        m_listener = listener;
        m_menu = new GuiMenu(text);
        m_actionListener = (ActionEvent event) -> {
            RecentMenuItem item = (RecentMenuItem)event.getSource();
            String label = item.getRecentMenuLabel();
            String value = item.getRecentMenuValue();
            m_listener.itemSelected(label, value);
        };
        get();
        updateEnabled();
    }

    /** *  Add a new item at the top.If the new number of items is greater than MAX_ITEM, the oldest item
        is removed.
     * @param label
     * @param value */
    public void add(String label, String value)
    {
        addNewItem(label, value);
        put();
    }

    public int getCount()
    {
        return m_menu.getMenuComponentCount();
    }

    /** Don't modify the items in this menu!
     * @return  */
    public GuiMenu getMenu()
    {
        return m_menu;
    }

    public String getValue(int i)
    {
        return getItem(i).getRecentMenuValue();
    }

    public void remove(int i)
    {
        m_menu.remove(getItem(i));
        relabel();
    }

    public void setLabel(int i, String label)
    {
        getItem(i).setRecentMenuLabel(label);
        getItem(i).setPosition(i);
        put();
    }

    /** Set menu enabled if not empty, disabled otherwise. */
    public void updateEnabled()
    {
        int count = getCount();
        m_menu.setEnabled(count > 0);
    }

    private final String m_path;

    private final ActionListener m_actionListener;

    private final Listener m_listener;

    private final GuiMenu m_menu;

    private void addNewItem(String label, String value)
    {
        for (int i = 0; i < getCount(); ++i)
            if (getValue(i).equals(value))
                m_menu.remove(i);
        JMenuItem item = new RecentMenuItem(label, value, m_actionListener);
        m_menu.add(item, 0);
        // There might be several oldest items to remove, if the list in the
        // preferences was created with a version of GoGui compiled with a
        // different value for MAX_ITEM.
        while (getCount() > MAX_ITEMS)
            m_menu.remove(getCount() - 1);
        relabel();
    }

    private void get()
    {
        Preferences prefs = PrefUtil.getNode(m_path);
        if (prefs == null)
            return;
        int size = prefs.getInt("size", 0);
        if (size < 0)
            size = 0;
        m_menu.removeAll();
        for (int i = 0; i < size; ++i)
        {
            prefs = PrefUtil.getNode(m_path + "/" + i);
            if (prefs == null)
                break;
            String label = prefs.get("label", null);
            String value = prefs.get("value", null);
            if (label == null || value == null)
                continue;
            addNewItem(label, value);
        }
    }

    private RecentMenuItem getItem(int i)
    {
        return (RecentMenuItem)m_menu.getItem(i);
    }

    private String getLabel(int i)
    {
        return getItem(i).getRecentMenuLabel();
    }

    private void put()
    {
        Preferences prefs = PrefUtil.createNode(m_path);
        if (prefs == null)
            return;
        int size = getCount();
        prefs.putInt("size", size);
        for (int i = 0; i < size; ++i)
        {
            prefs = PrefUtil.createNode(m_path + "/" + (size - i - 1));
            if (prefs == null)
                break;
            prefs.put("label", getLabel(i));
            prefs.put("value", getValue(i));
        }
    }

    /** Compute all new labels (including the number and mnemonic key) after
        changes were made. */
    private void relabel()
    {
        int size = getCount();
        for (int i = 0; i < size; ++i)
            getItem(i).setPosition(i);
    }
}

final class RecentMenuItem
    extends JMenuItem
{
    public RecentMenuItem(String label, String value, ActionListener listener)
    {
        setRecentMenuLabel(label);
        m_value = value;
        if (! ObjectUtil.equals(label, value))
            setToolTipText(value);
        addActionListener(listener);
    }

    public String getRecentMenuLabel()
    {
        return m_label;
    }

    public String getRecentMenuValue()
    {
        return m_value;
    }

    public void setPosition(int i)
    {
        String text;
        String mnemonic;
        // Use 1-9 and 0 as mnemonics for the first 10 items
        if (! Platform.isMac() && i < 9)
        {
            text = Integer.toString(i + 1) + ": " + m_label;
            mnemonic = Integer.toString(i + 1);
        }
        else if (! Platform.isMac() && i == 9)
        {
            text = "10: " + m_label;
            mnemonic = "0";
        }
        else
        {
            text = m_label;
            mnemonic = "";
        }
        setText(text);
        if (! mnemonic.equals(""))
        {
            KeyStroke keyStroke = KeyStroke.getKeyStroke(mnemonic);
            int code = keyStroke.getKeyCode();
            setMnemonic(code);
        }
   }

    public void setRecentMenuLabel(String label)
    {
        m_label = label;
    }

    private String m_label;

    private final String m_value;
}
