package net.kullo.android.thirdparty;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class NullViewHolder extends RecyclerView.ViewHolder {

    public NullViewHolder(View itemView) {
        super(itemView);
        throw new AssertionError("This is a dummy type. Do not construct NullViewHolder");
    }
}
