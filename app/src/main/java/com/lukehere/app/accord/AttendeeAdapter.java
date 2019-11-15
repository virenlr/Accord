package com.lukehere.app.accord;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.ui.firestore.paging.LoadingState;
import com.google.android.material.card.MaterialCardView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class AttendeeAdapter extends FirestorePagingAdapter<Attendee, AttendeeAdapter.AttendeesViewHolder> {
    private LayoutInflater mInflater;
    private ListItemClickListener mOnClickListener;
    private SwipeRefreshLayout mPullToRefresh;
    private Context mContext;

    public interface ListItemClickListener {
        void onListItemClick(int clickedItemRegistrationNumber);
    }

    AttendeeAdapter(Context context, SwipeRefreshLayout pullToRefresh, @NonNull FirestorePagingOptions<Attendee> options, ListItemClickListener listener) {
        super(options);
        mContext = context;
        mPullToRefresh = pullToRefresh;
        this.mInflater = LayoutInflater.from(context);
        mOnClickListener = listener;
    }

    @NonNull
    @Override
    public AttendeesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.attendee_list_item, parent, false);
        return new AttendeesViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull AttendeesViewHolder holder, int position, @NonNull Attendee attendee) {
        if (position % 2 == 0) {
            holder.attendeeCard.setCardBackgroundColor(mContext.getResources().getColor(R.color.background_light));
        } else {
            holder.attendeeCard.setCardBackgroundColor(mContext.getResources().getColor(R.color.background_dark));
        }

        holder.itemView.setTag(attendee.getRegistrationNumber());

        holder.registrationNumber.setText(String.valueOf(attendee.getRegistrationNumber()));

        if (attendee.getName().length() > 0) {
            holder.attendeeName.setText(attendee.getName());
        } else {
            holder.attendeeName.setText("-");
        }

        if (attendee.getDesignation().length() > 0) {
            holder.designation.setText(attendee.getDesignation());
        } else {
            holder.designation.setText("-");
        }

        if (attendee.getInstitution().length() > 0) {
            holder.institution.setText(attendee.getInstitution());
        } else {
            holder.institution.setText("-");

        }
    }

    @Override
    protected void onLoadingStateChanged(@NonNull LoadingState state) {
        switch (state) {
            case LOADING_INITIAL:
                mPullToRefresh.setRefreshing(true);
                break;
            case LOADING_MORE:
                mPullToRefresh.setRefreshing(true);
                break;
            case LOADED:
                mPullToRefresh.setRefreshing(false);
                break;
            case FINISHED:
                mPullToRefresh.setRefreshing(false);
                break;
            case ERROR:
                Toast.makeText(mContext, mContext.getString(R.string.error_getting_documents), Toast.LENGTH_SHORT).show();
                retry();
                break;
        }
    }

    class AttendeesViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        MaterialCardView attendeeCard;
        TextView registrationNumber;
        TextView attendeeName;
        TextView designation;
        TextView institution;

        AttendeesViewHolder(@NonNull View itemView) {
            super(itemView);

            registrationNumber = itemView.findViewById(R.id.registration_number);
            attendeeName = itemView.findViewById(R.id.name);
            designation = itemView.findViewById(R.id.designation);
            institution = itemView.findViewById(R.id.institution);
            attendeeCard = itemView.findViewById(R.id.attendee_card);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(@NonNull View v) {
            int registrationNumberAtClickedPosition = (int) v.getTag();
            mOnClickListener.onListItemClick(registrationNumberAtClickedPosition);
        }

    }
}
