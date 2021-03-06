package com.soussidev.kotlin.rx_java2_lib.RxRecyclerview;

import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Created by Soussi on 14/09/2017.
 */

public class ObservableAdapter<T> extends RecyclerView.Adapter<ObservableAdapter.ViewHolder<T>> {

    private final AdapterSource<T> source;
    private final PublishSubject<ViewEvent> itemEvent = PublishSubject.create();
    private final PublishSubject<Boolean> requestScrollEvent = PublishSubject.create();
    private final CompositeDisposable changeWatcher = new CompositeDisposable();

    public ObservableAdapter(AdapterSource<T> source) {
        this.source = source;
    }

    @Override public ViewHolder<T> onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = source.getLayout(viewType);
        try {
            //noinspection unchecked
            return new ViewHolder<>((BindableView<T>) LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
        } catch (InflateException e) {
            try {
                Timber.e(e, "Error inflating view %s %s", layout, parent.getContext().getResources().getResourceEntryName(layout));
            } catch (Resources.NotFoundException rnfe) {
                Timber.e(e, "Error inflating view %s", layout);
            }
            throw e;
        }
    }

    @Override public void onBindViewHolder(ViewHolder<T> holder, int position) {
        holder.bind(source.get(position));
    }

    @Override public int getItemViewType(int position) {
        return source.getViewType(position);
    }

    @Override public int getItemCount() {
        return source.getCount();
    }

    @Override public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        //initial notify
        notifyDataSetChanged();
        changeWatcher.clear();
        changeWatcher.add(source.onNotifyRequested()
                .subscribe(new Consumer<DiffUtil.DiffResult>() {
                               @Override
                               public void accept(@NonNull DiffUtil.DiffResult diffResult) throws Exception {
                                   diffResult.dispatchUpdatesTo(new SimpleUpdateCallback() {
                                       @Override public void onInserted(int position, int count) {
                                           if (position == 0) {
                                               requestScrollEvent.onNext(Boolean.TRUE);
                                           }
                                       }
                                   });
                                   diffResult.dispatchUpdatesTo(ObservableAdapter.this);
                               }
                           },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                throwable.printStackTrace();
                                Timber.e(throwable, "Error watching source data");
                            }
                        }));
    }

    @Override public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        //TODO check if all holders are also detached at this point
        changeWatcher.clear();
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @Override public void onViewAttachedToWindow(final ViewHolder<T> holder) {
        super.onViewAttachedToWindow(holder);
        final Observable<Object> objectObservable = holder.holderView.onObservableEvent();
        if (objectObservable != null) {
            //reattach item reference when holder shown without bind change event
            final int adapterPosition = holder.getAdapterPosition();
            if (holder.item == null) {
                holder.reAttach(source.get(adapterPosition));
            }

            holder.disposable = objectObservable
                    .filter(new Predicate<Object>() {
                        @Override public boolean test(@NonNull Object o) throws Exception {
                            return o != null;
                        }
                    })
                    .map(new Function<Object, ViewEvent>() {
                        @Override public ViewEvent apply(@NonNull Object o) throws Exception {
                            return new ViewEvent<>(holder, o);
                        }
                    })
                    .doOnError(new Consumer<Throwable>() {
                        @Override
                        public void accept(@NonNull Throwable throwable) throws Exception {
                            throwable.printStackTrace();
                            Timber.e("ObservableAdapter Error: %s", throwable.getMessage());
                        }
                    })
                    .subscribe(new Consumer<ViewEvent>() {
                        @Override
                        public void accept(@NonNull ViewEvent viewEvent) throws Exception {
                            itemEvent.onNext(viewEvent);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(@NonNull Throwable throwable) throws Exception {
                            throwable.printStackTrace();
                            Timber.e(throwable, "Error:viewHolder:event");
                        }
                    });
        }
    }

    @Override public void onViewDetachedFromWindow(ViewHolder<T> holder) {
        holder.detach();
        super.onViewDetachedFromWindow(holder);
    }

    @Override public void onViewRecycled(ViewHolder<T> holder) {
        holder.recycle();
        super.onViewRecycled(holder);
    }

    /**
     * Notify that list inserted new items in front of the existing collection
     * You can use this to detect when you need to force list to scroll up (i.e. when list
     * was scrolled to the top, and UI maintained scroll position after collection was updated)
     * @return observable event
     */
    public Observable<Boolean> onRequestScrollUp() {
        return requestScrollEvent;
    }

    /**
     * Generic method which dispatches viewHolder events (click, longtap, whatever) to
     * the listeners (typically presenters).
     * Data object is of type Object, but the real type depends on an implementation inside
     * specific view (makes use of {@link BindableView#onObservableEvent()} method)
     * @return observable event dispatcher
     */
    public Observable<ViewEvent> onItemEvent() {
        return itemEvent;
    }

    /**
     * Generic ViewHolder for the purposes of ObservableAdapter use
     */
    @SuppressWarnings("unused")
    public static class ViewHolder<T> extends RecyclerView.ViewHolder {

        /**
         * AutoCasts interface to {@link View} type
         * @return View to construct ViewHolder
         */
        private static View autoCast(BindableView view) {
            if (view instanceof View) {
                return (View) view;
            }
            throw new IllegalStateException("BindableView cannot be cast automatically [" + view + "]." +
                    " Does it extend any View class?");
        }

        private BindableView<T> holderView;
        private T item;
        private Disposable disposable;

        ViewHolder(BindableView<T> itemView) {
            super(autoCast(itemView));
            this.holderView = itemView;
        }

        void bind(T item) {
            this.item = item;
            holderView.bindItem(item);
        }

        void recycle() {
            detach();
        }

        void detach() {
            this.item = null;
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
            disposable = null;
        }

        void reAttach(T item) {
            this.item = item;
        }

        @Nullable
        T getItem() {
            return item;
        }

        @Override public String toString() {
            return "ViewHolder{" +
                    "holderView=" + holderView +
                    ",item=" + item +
                    ",pos=" + getAdapterPosition() +
                    '}';
        }
    }

    /**
     * Handler class for adapter item events
     */
    public static class ViewEvent<T> {
        private WeakReference<ViewHolder<T>> holder;
        private Object data;

        ViewEvent(ViewHolder<T> holder, Object data) {
            this.holder = new WeakReference<>(holder);
            this.data = data;
        }

        @Nullable public ViewHolder<T> getView() {
            return holder.get();
        }

        @Nullable public T getItem() {
            if (holder.get() != null) {
                return holder.get().getItem();
            }
            return null;
        }

        public Object getData() {
            return data;
        }

        @Override public String toString() {
            return "ViewEvent{" +
                    "holder=" + holder.get() +
                    ",data=" + data +
                    '}';
        }
    }

    /**
     * Helper class for better readability
     */
    static class SimpleUpdateCallback implements ListUpdateCallback {

        @Override public void onInserted(int position, int count) {}

        @Override public void onRemoved(int position, int count) {}

        @Override public void onMoved(int fromPosition, int toPosition) {}

        @Override public void onChanged(int position, int count, Object payload) {}
    }
}
