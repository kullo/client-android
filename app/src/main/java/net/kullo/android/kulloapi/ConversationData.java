/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import android.graphics.Bitmap;

import net.kullo.android.littlehelpers.AddressSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConversationData {
    public String title;
    public AddressSet participants;
    public List<String> participantsTitle = new ArrayList<>();
    public Map<String, String> participantsName = new HashMap<>();
    public Map<String, String> participantsOrganization = new HashMap<>();
    // Use String keys as Address type does not implement Java equality methods
    public Map<String, Bitmap> participantsAvatar = new HashMap<>();
    public int countUnread;
}
