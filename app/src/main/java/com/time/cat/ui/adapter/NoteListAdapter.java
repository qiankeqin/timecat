package com.time.cat.ui.adapter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.time.cat.R;
import com.time.cat.data.database.DB;
import com.time.cat.data.model.DBmodel.DBNote;
import com.time.cat.ui.modules.main.MainActivity;
import com.time.cat.ui.modules.operate.InfoOperationActivity;
import com.time.cat.ui.modules.routines.RoutinesFragment;
import com.time.cat.util.override.ToastUtil;
import com.time.cat.util.string.TimeUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author dlink
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2018/3/16
 * @discription null
 * @usage null
 */
public class NoteListAdapter extends RecyclerView.Adapter<NoteListAdapter.ViewHolder> implements RoutinesFragment.OnScrollBoundaryDecider{
    private List<DBNote> dataSet;
    private Activity activity;
    private int position;

    public NoteListAdapter(List<DBNote> dbNoteList, Activity activity) {
        dataSet = (dbNoteList == null) ? new ArrayList<>() : dbNoteList;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (activity == null) {
            activity = (MainActivity) parent.getContext();
        }
        View itemView = LayoutInflater.from(activity).inflate(R.layout.base_list_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final DBNote dbNote = dataSet.get(getItemCount() - position - 1);
        holder.base_tv_title.setText(dbNote.getTitle());
        holder.base_tv_content.setText(dbNote.getContent());
        Date date = TimeUtil.formatGMTDateStr(dbNote.getCreated_datetime());
        if (date != null) {
            holder.base_tv_time.setText(date.toLocaleString());
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent2DialogActivity = new Intent(activity, InfoOperationActivity.class);
            intent2DialogActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent2DialogActivity.putExtra(InfoOperationActivity.TO_SAVE_STR, dbNote.getContent());
            Bundle bundle = new Bundle();
            bundle.putSerializable(InfoOperationActivity.TO_UPDATE_NOTE, dbNote);
            intent2DialogActivity.putExtras(bundle);
            activity.startActivity(intent2DialogActivity);
            ToastUtil.i("修改笔记");
        });
        holder.itemView.setOnLongClickListener(v -> {
            new MaterialDialog.Builder(activity)
                    .content("确定删除这个笔记吗？")
                    .positiveText("删除")
                    .onPositive((dialog, which) -> {
                        DB.notes().deleteAndFireEvent(dbNote);
                        notifyItemRemoved(position);
                    })
                    .negativeText("取消")
                    .onNegative((dialog, which) -> dialog.dismiss()).show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    @Override
    public boolean canRefresh() {
        return false;
    }

    @Override
    public boolean canLoadMore() {
        return false;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView base_tv_title, base_tv_content, base_tv_time;

        public ViewHolder(View itemView) {
            super(itemView);
            base_tv_title = itemView.findViewById(R.id.base_tv_title);
            base_tv_content = itemView.findViewById(R.id.base_tv_content);
            base_tv_time = itemView.findViewById(R.id.base_tv_time);
        }
    }
}
