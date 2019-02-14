// SgfProperties.java

package net.sf.gogui.game;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/** Unknown SGF properties.
    Non-type-checked SGF properties for preserving unknown properties.
    Should only be used for unknown properties.
    @todo Iteration is inefficient, reimplement (but without sacrificing
    const-correctness) */
public final class SgfProperties
    implements ConstSgfProperties
{
    public SgfProperties()
    {
        m_properties = new TreeMap<>();
    }

    public SgfProperties(ConstSgfProperties props)
    {
        this();
        if (props == null)
            return;
        ((SgfProperties)props).m_properties.entrySet().forEach((entry) -> {
            add(entry.getKey(), entry.getValue());
        });
    }

    public void add(String key, ArrayList<String> values)
    {
        ArrayList<String> put = m_properties.put(key, new ArrayList<>(values));
    }

    /**
     *
     * @param key
     * @param value
     */
    public void add(String key, String value)
    {
        ArrayList<String> values = new ArrayList<>();
        values.add(value);
        m_properties.put(key, values);
    }

    /** *  Get a list of all keys.This list is a copy of the keys at the time of the function call.
     * @return  */
    @Override
    public ArrayList<String> getKeys()
    {
        return new ArrayList<>(m_properties.keySet());
    }

    /** Return number of values for a key.
     * @param key
        @return Number of values or -1, if key does not exist. */
    @Override
    public int getNumberValues(String key)
    {
        ArrayList<String> values = m_properties.get(key);
        if (values == null)
            return -1;
        return values.size();
    }

    @Override
    public String getValue(String key, int index)
    {
        return m_properties.get(key).get(index);
    }

    @Override
    public boolean hasKey(String key)
    {
        return (getNumberValues(key) >= 0);
    }

    @Override
    public boolean isEmpty()
    {
        return (m_properties.isEmpty());
    }

    public void remove(String key)
    {
        m_properties.remove(key);
    }

    private Map<String,ArrayList<String>> m_properties;
}
