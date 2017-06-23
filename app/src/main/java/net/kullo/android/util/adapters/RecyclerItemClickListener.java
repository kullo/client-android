/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.util.adapters;

import android.view.View;

public interface RecyclerItemClickListener {
    void onClick(View v, int position);
    boolean onLongClick(View v, int position);
}
