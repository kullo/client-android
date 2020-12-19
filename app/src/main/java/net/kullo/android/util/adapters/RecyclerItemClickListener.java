/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.util.adapters;

import android.view.View;

public interface RecyclerItemClickListener {
    void onClick(View v, int position);
    boolean onLongClick(View v, int position);
}
