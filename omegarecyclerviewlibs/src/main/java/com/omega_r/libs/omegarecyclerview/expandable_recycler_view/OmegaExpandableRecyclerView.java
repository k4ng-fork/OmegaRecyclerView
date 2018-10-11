package com.omega_r.libs.omegarecyclerview.expandable_recycler_view;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.omega_r.libs.omegarecyclerview.OmegaRecyclerView;

import java.util.Collections;
import java.util.List;

public class OmegaExpandableRecyclerView extends OmegaRecyclerView {

    //region Recycler

    public OmegaExpandableRecyclerView(Context context) {
        super(context);
    }

    public OmegaExpandableRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public OmegaExpandableRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void initDefaultLayoutManager(@Nullable AttributeSet attrs, int defStyleAttr) {
        if (getLayoutManager() == null) {
            setLayoutManager(new LinearLayoutManager(getContext(), attrs, defStyleAttr, 0)); // TODO: Expandable Layout Manager
        }
    }

    @Override
    public void setLayoutManager(@Nullable LayoutManager layoutManager) {
        if (layoutManager != null && !(layoutManager instanceof LinearLayoutManager)) { // TODO: Expandable Layout Manager
            throw new IllegalStateException("LayoutManager " + layoutManager.toString() + " should be LinearLayoutManager");
        }
        super.setLayoutManager(layoutManager);
    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (!(adapter instanceof Adapter)) throw new IllegalStateException("Adapter should extend OmegaExpandableRecyclerView.Adapter");
        super.setAdapter(adapter);
    }

    // endregion

    //region Adapter
    public static abstract class Adapter<P, CH> extends OmegaRecyclerView.Adapter<BaseViewHolder> {

        private static final int VH_TYPE_GROUP = 0;
        private static final int VH_TYPE_CHILD = 1;

        private FlatGroupingList<P, CH> items = new FlatGroupingList<>(Collections.<Group<P,CH>>emptyList());

        protected abstract GroupViewHolder provideGroupViewHolder(@NonNull ViewGroup viewGroup);
        protected abstract ChildViewHolder provideChildViewHolder(@NonNull ViewGroup viewGroup);

        public Adapter(@NonNull List<Group<P, CH>> groups) {
            items = new FlatGroupingList<>(groups);
        }

        public Adapter() {
            // nothing
        }

        public void setItems(@NonNull List<Group<P, CH>> groups) {
            items = new FlatGroupingList<>(groups);
            tryNotifyDataSetChanged();
        }

        @NonNull
        @Override
        public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int itemType) {
            switch (itemType) {
                case VH_TYPE_CHILD:
                    return provideChildViewHolder(viewGroup);
                case VH_TYPE_GROUP:
                    return provideGroupViewHolder(viewGroup);
            }
            throw new IllegalStateException("Incorrect view type");
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onBindViewHolder(@NonNull BaseViewHolder baseViewHolder, int position) {
            baseViewHolder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.getVisibleItemsCount();
        }

        @Override
        public int getItemViewType(int position) {
            switch (items.getType(position)) {
                case GROUP:
                    return VH_TYPE_GROUP;
                case CHILD:
                    return VH_TYPE_CHILD;
            }
            return super.getItemViewType(position);
        }

        public void expand(P parent) {
            items.onExpandStateChanged(parent, true);

            int positionStart = items.getVisiblePosition(parent) + 1;
            int childsCount = items.getChildsCount(parent);

            if (childsCount > 0) {
                tryNotifyItemChanged(positionStart);
                tryNotifyItemRangeInserted(positionStart, childsCount);
            }
        }

        public void collapse(P parent) {
            items.onExpandStateChanged(parent, false);

            int positionStart = items.getVisiblePosition(parent) + 1;
            int childsCount = items.getChildsCount(parent);

            if (childsCount > 0) {
                notifyItemChanged(positionStart);
                notifyItemRangeRemoved(positionStart, childsCount);
            }
        }

        private void notifyExpandFired(P parent) {
            if (parent == null) return;

            if (items.isExpanded(parent)) {
                collapse(parent);
            } else {
                expand(parent);
            }
        }

        public abstract class GroupViewHolder extends BaseViewHolder<P> {

            private View currentExpandFiringView = itemView;
            private final OnClickListener clickListener = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    notifyExpandFired(getItem());
                }
            };

            protected abstract void onExpand();
            protected abstract void onCollapse();

            public GroupViewHolder(ViewGroup parent, @LayoutRes int res) {
                super(parent, res);
                setExpandFiringView(itemView);
            }

            protected void setExpandFiringView(View firingView) {
                currentExpandFiringView.setOnClickListener(null);
                currentExpandFiringView = firingView;
                currentExpandFiringView.setOnClickListener(clickListener);
            }
        }

        public abstract class ChildViewHolder extends BaseViewHolder<CH> {

            public ChildViewHolder(ViewGroup parent, @LayoutRes int res) {
                super(parent, res);
            }

        }
    }
    //endregion

    //region ViewHolders
    private static abstract class BaseViewHolder<T> extends OmegaRecyclerView.ViewHolder {
        private T item;

        BaseViewHolder(ViewGroup parent, @LayoutRes int res) {
            super(parent, res);
        }

        private void bind(T item) {
            this.item = item;
            onBind(item);
        }

        @NonNull
        T getItem() {
            return item;
        }

        protected abstract void onBind(T item);
    }

    //endregion
}
