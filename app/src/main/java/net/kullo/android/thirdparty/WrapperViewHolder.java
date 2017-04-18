package net.kullo.android.thirdparty;

import android.support.v7.widget.RecyclerView;

class WrapperViewHolder<ItemsViewHolder extends RecyclerView.ViewHolder,
        SectionHeaderViewHolder extends RecyclerView.ViewHolder,
        SectionFooterViewHolder extends RecyclerView.ViewHolder>
        extends RecyclerView.ViewHolder {

    SectionHeaderViewHolder headerViewHolder;
    SectionFooterViewHolder footerViewHolder;
    ItemsViewHolder itemViewHolder;

    enum Type {
        Header,
        Footer,
        Item
    }

    @SuppressWarnings("unchecked")
    WrapperViewHolder(Type type, RecyclerView.ViewHolder viewHolder) {
        super(viewHolder.itemView);
        switch (type) {
            case Header:
                headerViewHolder = (SectionHeaderViewHolder) viewHolder;
                break;
            case Footer:
                footerViewHolder = (SectionFooterViewHolder) viewHolder;
                break;
            case Item:
                itemViewHolder = (ItemsViewHolder) viewHolder;
                break;
        }
    }
}
