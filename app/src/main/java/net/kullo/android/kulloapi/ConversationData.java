/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import android.graphics.Bitmap;

import net.kullo.android.littlehelpers.AddressSet;

import java.util.ArrayList;
import java.util.List;

public class ConversationData {
    public String mTitle;
    public AddressSet mParticipants;
    public List<String> mParticipantsTitles = new ArrayList<>();
    public List<Bitmap> mParticipantsAvatars = new ArrayList<>();
    public int mCountUnread;
}
