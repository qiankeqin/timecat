package com.time.cat.ui.modules.notes.backup;

import com.time.cat.data.database.DB;
import com.time.cat.data.model.DBmodel.DBNote;
import com.time.cat.data.model.DBmodel.DBUser;
import com.time.cat.data.model.APImodel.Note;
import com.time.cat.data.network.RetrofitHelper;
import com.time.cat.data.model.Converter;

import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * @author dlink
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2018/2/28
 * @discription model层，只与数据有关
 * @usage null
 */
public class NotesDataManager {

    public void refreshData(OnDataChangeListener onDataChangeListener) {
        RetrofitHelper.getNoteService().getNotesAll() //获取Observable对象
                .subscribeOn(Schedulers.newThread())//请求在新的线程中执行
                .observeOn(Schedulers.io())         //请求完成后在io线程中执行
                .doOnNext(new Action1<ArrayList<Note>>() {
                    @Override
                    public void call(ArrayList<Note> noteList) {
                        if (noteList == null || noteList.size() <= 0)
                            return;
                        for (Note note : noteList) {
                            DB.notes().safeSaveNote(note);
                        }
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())//最后在主线程中执行
                .subscribe(new Subscriber<ArrayList<Note>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        //请求失败
//                        ToastUtil.show("同步[ 笔记 ]到本地出现错误");
//                        LogUtil.e("同步[ 笔记 ]到本地出现错误" + e.toString());
                    }

                    @Override
                    public void onNext(ArrayList<Note> task) {
                        //请求成功
//                        ToastUtil.show("成功同步[ 笔记 ]");
//                        LogUtil.e("成功同步[ 笔记 ]: " + task.toString());
                    }
                });


        List<DBNote> dbNoteList = DB.notes().findAll();
        if (dbNoteList == null || dbNoteList.size() <= 0) {
            return;
        }
        List<DBNote> adapterDBNoteList = new ArrayList<>();
        DBUser dbUser = DB.users().getActive();
        for (int i = dbNoteList.size()-1; i >= 0; i--) {
            if ((dbNoteList.get(i).getOwner().equals(Converter.getOwnerUrl(dbUser)))) {
                adapterDBNoteList.add(dbNoteList.get(i));
            }
        }

        if (adapterDBNoteList.size() >= 0) {
            if (onDataChangeListener != null) {
                onDataChangeListener.onDataChange(adapterDBNoteList);
            }
        }
    }

    public interface OnDataChangeListener {
        void onDataChange(List<DBNote> adapterDBNoteList);
    }
}
