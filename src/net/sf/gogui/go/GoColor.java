// GoColor.java

package net.sf.gogui.go;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Player color / state of a point on the board (black, white, empty). */
public enum GoColor
{
    /** Black stone or black player. */
    BLACK
    {
        @Override
        public String getCapitalizedName()
        {
            return "Black";
        }

        @Override
        public String getUppercaseLetter()
        {
            return "B";
        }

        @Override
        public boolean isBlackWhite()
        {
            return true;
        }

        @Override
        public GoColor otherColor()
        {
            return WHITE;
        }
    },

    /** White stone or white player. */
    WHITE
    {
        @Override
        public String getCapitalizedName()
        {
            return "White";
        }

        @Override
        public String getUppercaseLetter()
        {
            return "W";
        }

        @Override
        public boolean isBlackWhite()
        {
            return true;
        }

        @Override
        public GoColor otherColor()
        {
            return BLACK;
        }
    },

    /** Empty intersection. */
    EMPTY
    {
        @Override
        public String getCapitalizedName()
        {
            return "Empty";
        }

        @Override
        public String getUppercaseLetter()
        {
            return "E";
        }

        @Override
        public boolean isBlackWhite()
        {
            return false;
        }

        @Override
        public GoColor otherColor()
        {
            return EMPTY;
        }
    };

    public static final List<GoColor> BLACK_WHITE;

    public static final List<GoColor> WHITE_BLACK;

    public static final List<GoColor> BLACK_WHITE_EMPTY;

    static
    {
        List<GoColor> blackWhite = new ArrayList<>(2);
        blackWhite.add(BLACK);
        blackWhite.add(WHITE);
        BLACK_WHITE = Collections.unmodifiableList(blackWhite);

        List<GoColor> whiteBlack = new ArrayList<>(2);
        whiteBlack.add(WHITE);
        whiteBlack.add(BLACK);
        WHITE_BLACK = Collections.unmodifiableList(whiteBlack);

        List<GoColor> blackWhiteEmpty = new ArrayList<>(3);
        blackWhiteEmpty.add(BLACK);
        blackWhiteEmpty.add(WHITE);
        blackWhiteEmpty.add(EMPTY);
        BLACK_WHITE_EMPTY = Collections.unmodifiableList(blackWhiteEmpty);
    }

    /** *  Return color name if used for specifying player.Returns the capitalized color name (e.g.
        "Black" for BLACK).
        This name will also potentially be internationalized in the future.
     * @return  */
    public abstract String getCapitalizedName();

    /** *  Return uppercase letter identifying the color.Returns "B", "W", or "E".
        This letter is not internationalized,
        such that it can be used for instance in standard language independent
        game results (e.g. "W+3").
     * @return  */
    public abstract String getUppercaseLetter();

    /** Check if color is black or white.
        @return <code>true</code>, if color is <code>BLACK</code> or
        <code>WHITE</code>. */
    public abstract boolean isBlackWhite();

    /** Return other color.
        @return <code>BLACK</code> for <code>WHITE</code>, <code>WHITE</code>
        for <code>BLACK</code>, <code>EMPTY</code> for <code>EMPTY</code>. */
    public abstract GoColor otherColor();
}
