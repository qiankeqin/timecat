package com.time.cat.ui.modules.routines.card_view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.time.cat.R;
import com.time.cat.TimeCatApp;
import com.time.cat.data.database.DB;
import com.time.cat.data.model.DBmodel.DBRoutine;
import com.time.cat.data.model.events.PersistenceEvents;
import com.time.cat.ui.base.BaseFragment;
import com.time.cat.ui.modules.routines.RoutinesFragment;

import java.util.Date;
import java.util.List;


public class RoutinesCardListFragment extends BaseFragment implements RoutinesFragment.OnScrollBoundaryDecider {


    List<DBRoutine> mDBRoutines;
    OnRoutineSelectedListener mRoutineSelectedCallback;
    ArrayAdapter adapter;
    ListView listview;

    Drawable ic;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_routines_card_list, container, false);
        listview = rootView.findViewById(R.id.routines_list);

        View empty = rootView.findViewById(android.R.id.empty);
        listview.setEmptyView(empty);

        mDBRoutines = DB.routines().findAllForActiveUser();

        ic = new IconicsDrawable(getContext()).icon(CommunityMaterial.Icon.cmd_clock).colorRes(R.color.agenda_item_title).paddingDp(8).sizeDp(40);

        adapter = new RoutinesListAdapter(getActivity(), R.layout.item_routines_list, mDBRoutines);
        listview.setAdapter(adapter);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(getTag(), "Activity " + activity.getClass().getName() + ", " + (activity instanceof OnRoutineSelectedListener));
        // If the container activity has implemented
        // the callback interface, set it as listener
        if (activity instanceof OnRoutineSelectedListener) {
            mRoutineSelectedCallback = (OnRoutineSelectedListener) activity;
        }
    }

    private View createRoutineListItem(LayoutInflater inflater, final DBRoutine dbRoutine) {

        int hour = new Date(dbRoutine.getBeginTs()).getHours();
        int minute = new Date(dbRoutine.getBeginTs()).getMinutes();

        String strHour = String.valueOf(hour >= 10 ? hour : "0" + hour);
        String strMinute = ":" + String.valueOf(minute >= 10 ? minute : "0" + minute);

        View item = inflater.inflate(R.layout.item_routines_list, null);

        ((TextView) item.findViewById(R.id.routines_list_item_hour)).setText(strHour);
        ((TextView) item.findViewById(R.id.routines_list_item_minute)).setText(strMinute);
        ((TextView) item.findViewById(R.id.routines_list_item_name)).setText(dbRoutine.name());
        ((ImageButton) item.findViewById(R.id.imageButton2)).setImageDrawable(ic);
        ((TextView) item.findViewById(R.id.routines_list_item_subtitle)).setText("pautas asociadas");
        View overlay = item.findViewById(R.id.routine_list_item_container);
        overlay.setTag(dbRoutine);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DBRoutine r = (DBRoutine) view.getTag();
                if (mRoutineSelectedCallback != null && r != null) {
                    Log.d(getTag(), "Click at " + r.name());
                    mRoutineSelectedCallback.onRoutineSelected(r);
                } else {
                    Log.d(getTag(), "No callback set");
                }

            }
        };

        overlay.setOnClickListener(clickListener);
        overlay.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (view.getTag() != null)
                    showDeleteConfirmationDialog((DBRoutine) view.getTag());
                return true;
            }
        });
        return item;
    }

    void showDeleteConfirmationDialog(final DBRoutine  r) {
        String message = String.format(getString(R.string.remove_routine_message_short), r.name());

        new MaterialDialog.Builder(getActivity())
                .title(getString(R.string.remove_routine_dialog_title))
                .content(message)
                .positiveText(getString(R.string.dialog_yes_option))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(MaterialDialog dialog, DialogAction which) {

                        notifyDataChanged();
                    }
                })
                .negativeText(getString(R.string.dialog_no_option))
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    public void notifyDataChanged() {
        Log.d(getTag(), "Routines - Notify data change");
        new ReloadItemsTask().execute();
    }

    @Override
    public void onStart() {
        super.onStart();
        TimeCatApp.eventBus().register(this);
    }

    @Override
    public void onStop() {
        TimeCatApp.eventBus().unregister(this);
        super.onStop();
    }

    // Method called from the event bus
    @SuppressWarnings("unused")
    public void onEvent(Object evt) {
        if (evt instanceof PersistenceEvents.ActiveUserChangeEvent) {
            notifyDataChanged();
        }
    }

    @Override
    public boolean canRefresh() {
        return false;
    }

    @Override
    public boolean canLoadMore() {
        return false;
    }


    // Container Activity must implement this interface
    public interface OnRoutineSelectedListener {
        void onRoutineSelected(DBRoutine r);

        void onCreateRoutine();
    }

    private class RoutinesListAdapter extends ArrayAdapter<DBRoutine> {

        public RoutinesListAdapter(Context context, int layoutResourceId, List<DBRoutine> items) {
            super(context, layoutResourceId, items);
        }

        @SuppressLint("ViewHolder")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final LayoutInflater layoutInflater = getActivity().getLayoutInflater();
            if (mDBRoutines.size() != 0) {
                return createRoutineListItem(layoutInflater, mDBRoutines.get(position));
            }
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.design_drawer_item, parent, false);
            }
            return convertView;
        }

    }

    private class ReloadItemsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            mDBRoutines = DB.routines().findAllForActiveUser();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            adapter.clear();
            for (DBRoutine r : mDBRoutines) {
                adapter.add(r);
            }
            adapter.notifyDataSetChanged();
        }
    }


}