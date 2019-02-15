// BlackWhiteEmptySet.java

package net.sf.gogui.go;

import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;

/** A set containing one element for Black, one for White, and one for
    Empty.
 * @param <T> */
public class BlackWhiteEmptySet<T>
{
    public BlackWhiteEmptySet()
    {
    }

    public BlackWhiteEmptySet(T elementBlack, T elementWhite, T elementEmpty)
    {
        m_elementBlack = elementBlack;
        m_elementWhite = elementWhite;
        m_elementEmpty = elementEmpty;
    }

    public T get(GoColor c)
    {
        if (null == c) {
            assert c == EMPTY;
            return m_elementEmpty;
        } else switch (c) {
            case BLACK:
                return m_elementBlack;
            case WHITE:
                return m_elementWhite;
            default:
                assert c == EMPTY;
                return m_elementEmpty;
        }
    }

    public void set(GoColor c, T element)
    {
        if (null == c) {
            assert c == EMPTY;
            m_elementEmpty = element;
        } else switch (c) {
            case BLACK:
                m_elementBlack = element;
                break;
            case WHITE:
                m_elementWhite = element;
                break;
            default:
                assert c == EMPTY;
                m_elementEmpty = element;
                break;
        }
    }

    private T m_elementBlack;

    private T m_elementWhite;

    private T m_elementEmpty;
}
