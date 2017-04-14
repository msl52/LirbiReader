package org.ebookdroid.droids.mupdf.codec;

import com.foobnix.android.utils.TxtUtils;

import android.graphics.RectF;

public class TextWord extends RectF {
    public String w;
    private RectF original;

    public TextWord() {
        super();
        w = new String();
    }

    public TextWord(TextChar tc) {
        w = new String();
        Add(tc);
    }

    @Override
    public String toString() {
        return "" + w + left + " " + top + " " + right + " " + bottom;
    }

    public void Add(TextChar tc) {
        super.union(tc);
        w = w.concat(new String(new char[] { tc.c }));
    }

    public RectF getOriginal() {
        return original;
    }

    public void setOriginal(RectF original) {
        this.original = new RectF(original);
    }

    public static String asString(TextWord[][] input, boolean isFiltering) {
        if (input == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (TextWord[] line : input) {

            for (int i = 0; i < line.length; i++) {
                TextWord word = line[i];
                // delete '-' from last word
                if (i == line.length - 1 && word.w.endsWith("-")) {
                    builder.append(word.w.replaceAll("-$", ""));
                } else {
                    builder.append(word.getWord() + " ");
                }
            }

        }
        return isFiltering ? TxtUtils.filterString(builder.toString()) : builder.toString();
    }

    public String getWord() {
        return w;
    }
}