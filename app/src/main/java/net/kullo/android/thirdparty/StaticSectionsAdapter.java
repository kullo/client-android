package net.kullo.android.thirdparty;

import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

/**
 * A proxy adapter used to partition the original data into sections and provide an optional
 * header and footer for each section.
 *
 * Use this class when the final sections are independent of the data and you can provide
 * a separate adapter for each section.
 *
 * Performance considerations: All design decisions assume that the number of sections is
 * relatively small compared to the number of items.
 *
 * @param <ItemViewHolder>
 * @param <SectionHeaderViewHolder>
 * @param <SectionFooterViewHolder>
 */
@SuppressWarnings("unused")
abstract public class StaticSectionsAdapter<
    ItemViewHolder extends RecyclerView.ViewHolder,
    SectionHeaderViewHolder extends RecyclerView.ViewHolder,
    SectionFooterViewHolder extends RecyclerView.ViewHolder
    > extends RecyclerView.Adapter<WrapperViewHolder<ItemViewHolder, SectionHeaderViewHolder, SectionFooterViewHolder>> {

    private static final String TAG = "StaticSectionsAdapter";

    private List<? extends RecyclerView.Adapter<ItemViewHolder>> mItemsAdapters;

    /**
     * Integer values in storage
     *   -2: element is a header
     *   -1: element is a footer
     * >= 0: element is an item from itemsAdapter
     *
     * When element is an item from itemsAdapter, storage stores the position in the itemsAdapter.
     * When element is a header of footer, supplementaryElementSectionIds stores the corresponding
     * section id.
     */
    private static final int ELEMENT_IS_HEADER = -2;
    private static final int ELEMENT_IS_FOOTER = -1;

    // leave non-negative view type ids for the original adapter
    private static final int VIEW_TYPE_HEADER = -1;
    private static final int VIEW_TYPE_FOOTER = -2;

    protected StaticSectionsAdapter(RecyclerView.Adapter<ItemViewHolder> itemsAdapters) {
        this(Collections.singletonList(itemsAdapters));
    }

    protected StaticSectionsAdapter(List<? extends RecyclerView.Adapter<ItemViewHolder>> itemsAdapters) {
        mItemsAdapters = itemsAdapters;
        connectAdapters();
    }

    /**
     * A sectionId must uniquely identify a section.
     *
     * Ids need not be contiguous or sorted.
     *
     * This is called for every element in the list whenever data changes, so make sure your
     * implementation is fast
     */
    protected abstract boolean sectionHasHeader(int sectionIndex);
    protected abstract boolean sectionHasFooter(int sectionIndex);
    protected abstract SectionHeaderViewHolder onCreateSectionHeaderViewHolder(ViewGroup parent);
    protected abstract SectionFooterViewHolder onCreateSectionFooterViewHolder(ViewGroup parent);
    protected abstract void onBindHeaderViewHolder(SectionHeaderViewHolder vh, int sectionIndex);
    protected abstract void onBindFooterViewHolder(SectionFooterViewHolder vh, int sectionIndex);

    @Override
    final public int getItemViewType(int position) {
        Pair<Integer, Integer> content = findElement(position);
        switch (content.second) {
            case ELEMENT_IS_HEADER:
                return VIEW_TYPE_HEADER;
            case ELEMENT_IS_FOOTER:
                return VIEW_TYPE_FOOTER;
            default:
                return content.first;
        }
    }

    @Override
    final public WrapperViewHolder<ItemViewHolder, SectionHeaderViewHolder, SectionFooterViewHolder>
    onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER: {
                SectionHeaderViewHolder viewHolder = onCreateSectionHeaderViewHolder(parent);
                return new WrapperViewHolder<>(WrapperViewHolder.Type.Header, viewHolder);
            }
            case VIEW_TYPE_FOOTER: {
                SectionFooterViewHolder viewHolder = onCreateSectionFooterViewHolder(parent);
                return new WrapperViewHolder<>(WrapperViewHolder.Type.Footer, viewHolder);
            }
            default: {
                ItemViewHolder viewHolder = mItemsAdapters.get(viewType).onCreateViewHolder(parent, viewType);
                return new WrapperViewHolder<>(WrapperViewHolder.Type.Item, viewHolder);
            }
        }
    }

    @Override
    final public void onBindViewHolder(WrapperViewHolder<ItemViewHolder, SectionHeaderViewHolder, SectionFooterViewHolder> holder, int position) {
        Pair<Integer, Integer> content = findElement(position);
        switch (content.second) {
            case ELEMENT_IS_HEADER: {
                onBindHeaderViewHolder(
                        holder.headerViewHolder,
                        content.first);
            }
            break;
            case ELEMENT_IS_FOOTER: {
                onBindFooterViewHolder(
                        holder.footerViewHolder,
                        content.first);
            }
            break;
            default: {
                mItemsAdapters.get(content.first).onBindViewHolder(
                        holder.itemViewHolder,
                        content.second);
            }
        }
    }

    @Override
    final public int getItemCount() {
        int count = 0;
        for (int key = 0; key < mItemsAdapters.size(); ++key) {
            count += getSectionSize(key);
        }
        return count;
    }

    private void connectAdapters() {
        for (int i = 0; i < mItemsAdapters.size(); ++i) {
            RecyclerView.Adapter<ItemViewHolder> itemsAdapter = mItemsAdapters.get(i);
            final int sectionIndex = i;
            itemsAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    StaticSectionsAdapter.this.notifyDataSetChanged();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    int targetAdapterPositionStart = calculatePosition(sectionIndex, positionStart);
                    StaticSectionsAdapter.this.notifyItemRangeChanged(targetAdapterPositionStart, itemCount);
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    int targetAdapterPositionStart = calculatePosition(sectionIndex, positionStart);
                    StaticSectionsAdapter.this.notifyItemRangeInserted(targetAdapterPositionStart, itemCount);
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    int targetAdapterPositionStart = calculatePosition(sectionIndex, positionStart);
                    StaticSectionsAdapter.this.notifyItemRangeRemoved(targetAdapterPositionStart, itemCount);
                }

                @Override
                public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                    if (itemCount != 1) {
                        throw new AssertionError(
                            "onItemRangeMoved with itemCount other than 1 is not supported. " +
                            "This is because RecyclerView.Adapter.notifyItemMove does not have a count parameter. " +
                            "No idea how to implement that. Feel free to submit a PR.");
                    }
                    int targetAdapterFromPosition = calculatePosition(sectionIndex, fromPosition);
                    int targetAdapterToPosition = calculatePosition(sectionIndex, toPosition);
                    StaticSectionsAdapter.this.notifyItemMoved(
                        targetAdapterFromPosition, targetAdapterToPosition);
                }
            });
        }
    }

    private int calculatePosition(int atSection, int positionInSection) {
        int pos = 0;
        for (int section = 0; section < atSection; ++section) {
            pos += getSectionSize(section);
        }

        if (sectionHasHeader(atSection)) pos += 1;
        pos += positionInSection;

        return pos;
    }

    private int getSectionSize(int key) {
        int out = 0;
        if (sectionHasHeader(key)) out += 1;
        out += mItemsAdapters.get(key).getItemCount();
        if (sectionHasFooter(key)) out += 1;
        return out;
    }

    private Pair<Integer, Integer> findElement(final int pos) {
        int sectionKey = 0;
        int previousSectionsSizeSum = 0;
        int currentSectionSize;
        while (true) {
            currentSectionSize = getSectionSize(sectionKey);

            if (previousSectionsSizeSum+currentSectionSize-1 >= pos) {
                break;
            }

            previousSectionsSizeSum += currentSectionSize;
            sectionKey += 1;
        }

        int positionInSection = pos-previousSectionsSizeSum;
        if (sectionHasHeader(sectionKey)) {
            if (positionInSection == 0) {
                return new Pair<>(sectionKey, ELEMENT_IS_HEADER);
            } else if (positionInSection == currentSectionSize-1 && sectionHasFooter(sectionKey)) {
                return new Pair<>(sectionKey, ELEMENT_IS_FOOTER);
            } else {
                int elementPosition = positionInSection - 1;
                return new Pair<>(sectionKey, elementPosition);
            }
        } else {
            if (positionInSection == currentSectionSize-1 && sectionHasFooter(sectionKey)) {
                return new Pair<>(sectionKey, ELEMENT_IS_FOOTER);
            } else {
                return new Pair<>(sectionKey, positionInSection);
            }
        }

    }
}
