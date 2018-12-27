package com.kalinin.mpt.helpers;



public class StringUtils {
    public static int countMatches(CharSequence str, char c) {
        int res = 0;

        for (int i = 0; i < str.length(); i++)
            if (str.charAt(i) == c)
                res++;

        return res;
    }
}
