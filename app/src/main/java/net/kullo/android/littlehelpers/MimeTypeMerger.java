/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.littlehelpers;

import net.kullo.javautils.RuntimeAssertion;

import java.util.List;

public class MimeTypeMerger {
    public static String merge(List<String> mimeTypes) {
        RuntimeAssertion.require(mimeTypes.size() >= 1);

        String combinedMimeType = mimeTypes.get(0);
        for  (int i = 1; i < mimeTypes.size(); ++i) {
            combinedMimeType = mergeTwo(combinedMimeType, mimeTypes.get(i));
        }
        return combinedMimeType;
    }

    private static String mergeTwo(String currentMimeType, String newMimeType) {
        if (currentMimeType == null || currentMimeType.length() == 0) {
            return newMimeType;
        }

        if (currentMimeType.equals(newMimeType)) {
            // already correct
            return currentMimeType;
        }

        String firstNew = newMimeType.substring(0, newMimeType.indexOf('/'));
        String firstCurrent = currentMimeType.substring(0, currentMimeType.indexOf('/'));
        if (firstCurrent.equals(firstNew)) {
            return firstNew + "/*";
        } else {
            return "*/*";
        }
    }
}
