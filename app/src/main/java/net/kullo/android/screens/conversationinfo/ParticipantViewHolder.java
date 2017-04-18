/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.conversationinfo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import net.kullo.android.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class ParticipantViewHolder extends RecyclerView.ViewHolder {

    private Context mContext;
    private TextView nameOrganizationText;
    private TextView addressText;
    private CircleImageView image;

    public ParticipantViewHolder(Context context, View itemView) {
        super(itemView);
        mContext = context;
        nameOrganizationText = (TextView) itemView.findViewById(R.id.participant_name_organization);
        addressText = (TextView) itemView.findViewById(R.id.participant_address);
        image = (CircleImageView) itemView.findViewById(R.id.participant_avatar);
    }

    public void setValues(String address, String nameOrganization, Bitmap avatar) {
        addressText.setText(address);
        nameOrganizationText.setText(nameOrganization);
        image.setImageBitmap(avatar);
    }

    public void setSelected(boolean selected) {
        if (selected) {
            itemView.setBackgroundColor(mContext.getResources().getColor(R.color.kulloSelectionColor));
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }
}
