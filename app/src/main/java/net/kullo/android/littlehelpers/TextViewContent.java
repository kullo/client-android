/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextViewContent {
    private static Pattern HTML_LINK_PATTERN = Pattern.compile(
            "<a href=\"([^\"]+)\">([^<]+)</a>");

    public static void injectHtmlIntoTextView(
        @NonNull final TextView target,
        @NonNull final String textAsHtml,
        @NonNull final LinkClickedListener linkClickListener
    ) {
        final Spannable content = getSpannableFromHtml(textAsHtml, linkClickListener);
        target.setText(content);
        target.setMovementMethod(LinkMovementMethod.getInstance()); // make links clickable
    }

    abstract public static class LinkClickedListener {
        protected LinkClickedListener() {
        }

        abstract protected void onClicked(Uri target);
    }

    private static class PositionedSpan {
        final CharacterStyle span;
        final int start;
        final int end;

        PositionedSpan(CharacterStyle span, int start, int end) {
            this.span = span;
            this.start = start;
            this.end = end;
        }
    }

    @NonNull
    public static Spannable getSpannableFromHtml(
            @NonNull final String messageTextAsHtml,
            @NonNull final LinkClickedListener linkClickedListener) {
        final Matcher matcher = HTML_LINK_PATTERN.matcher(messageTextAsHtml);

        List<PositionedSpan> spans = new LinkedList<>();
        int inputPosCurrent = 0;

        StringBuilder outText = new StringBuilder();
        while (matcher.find()) {
            int inputPosLinkStart = matcher.start();
            int inputPosLinkEnd = matcher.end();

            if (inputPosLinkStart > inputPosCurrent) {
                outText.append(unescapeHtmlSpecialChars(
                    messageTextAsHtml.substring(inputPosCurrent, inputPosLinkStart)
                ));
            }

            // append link
            final String inputLinkTarget = matcher.group(1);
            final String inputLinkText = matcher.group(2);

            // unescape here to ensure spans are stored with the final positions
            final String outputLinkTarget = unescapeHtmlSpecialChars(inputLinkTarget);
            final String outputLinkText = unescapeHtmlSpecialChars(inputLinkText);

            int outputPosLinkStart = outText.length();
            int outputPosLinkEnd = outText.length() + outputLinkText.length();

            // Try TouchableSpan from
            // https://github.com/klinker24/Android-TextView-LinkBuilder/blob/master/library/src/main/java/com/klinker/android/link_builder/TouchableSpan.java
            // if this turns out to be insufficiently powerful.
            spans.add(new PositionedSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    linkClickedListener.onClicked(Uri.parse(outputLinkTarget));
                }
            }, outputPosLinkStart, outputPosLinkEnd));

            outText.append(outputLinkText);

            inputPosCurrent = inputPosLinkEnd;
        }
        // append rest
        outText.append(unescapeHtmlSpecialChars(
            messageTextAsHtml.substring(inputPosCurrent, messageTextAsHtml.length())
        ));

        Spannable sb = new SpannableString(outText);
        for (PositionedSpan posSpan : spans) {
            sb.setSpan(posSpan.span, posSpan.start, posSpan.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    private static String[] HTML_UNESCAPE_FROM = new String[] {
        "&amp;", "&gt;", "&lt;", "&quot;"
    };

    private static String[] HTML_UNESCAPE_TO = new String[] {
        "&", ">", "<", "\""
    };

    @NonNull
    private static String unescapeHtmlSpecialChars(@NonNull final String text) {
        return StringUtils.replaceEach(text, HTML_UNESCAPE_FROM, HTML_UNESCAPE_TO);
    }

    @NonNull
    public static Spannable highlightSearchResult(@NonNull final String snippetRaw, @NonNull final String boundary) {
        final String patternString = "\\(" + boundary + "\\)(.+?)\\(/" + boundary + "\\)";
        final Pattern pattern = Pattern.compile(patternString);
        final Matcher matcher = pattern.matcher(snippetRaw);

        List<PositionedSpan> spans = new LinkedList<>();
        int inputPosCurrent = 0;

        StringBuilder outText = new StringBuilder();
        while (matcher.find()) {
            int inputPosHighlightStart = matcher.start();
            int inputPosHighlightEnd = matcher.end();

            if (inputPosHighlightStart > inputPosCurrent) {
                outText.append(snippetRaw.substring(inputPosCurrent, inputPosHighlightStart));
            }

            // append link
            final String highlightText = matcher.group(1);
            int outputPosHighlightStart = outText.length();
            int outputPosHighlightEnd = outText.length() + highlightText.length();

            spans.add(new PositionedSpan(
                new StyleSpan(android.graphics.Typeface.BOLD),
                outputPosHighlightStart,
                outputPosHighlightEnd));
            outText.append(highlightText);

            inputPosCurrent = inputPosHighlightEnd;
        }
        // append rest
        outText.append(snippetRaw.substring(inputPosCurrent, snippetRaw.length()));

        Spannable out = new SpannableString(outText);
        for (PositionedSpan posSpan : spans) {
            out.setSpan(posSpan.span, posSpan.start, posSpan.end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return out;
    }
}
