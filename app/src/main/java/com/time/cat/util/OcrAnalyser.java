package com.time.cat.util;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.microsoft.projectoxford.vision.contract.Line;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.contract.Region;
import com.microsoft.projectoxford.vision.contract.Word;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;
import com.microsoft.projectoxford.vision.rest.WebServiceRequest;
import com.timecat.commonjar.contentProvider.SPHelper;
import com.time.cat.data.Constants;
import com.time.cat.data.network.UploadUtil;
import com.time.cat.R;
import com.time.cat.TimeCatApp;
import com.time.cat.ui.base.BaseActivity;
import com.time.cat.data.model.APImodel.ImageUpload;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class OcrAnalyser {
    //别人的 00b0e581e4124a2583ea7dba57aaf281
    // 我自己的 56c87e179c084cfaae9b70a2f58fa8d3 56c87e179c084cfaae9b70a2f58fa8d3
    //彭露的 9e88939475894dec85a2019fd36243be
    //进发的 eac11887004a4c88a7c3f527d6852bb3
    //王岩2 cc750e4c195d497391e9fe18f6d21bae
    static ArrayList<String> keys;
    private static OcrAnalyser instance = new OcrAnalyser();

    static {
        keys = new ArrayList<>();
        keys.add("9e88939475894dec85a2019fd36243be");
        keys.add("56c87e179c084cfaae9b70a2f58fa8d3");
        keys.add("eac11887004a4c88a7c3f527d6852bb3");
        keys.add("cc750e4c195d497391e9fe18f6d21bae");
        keys.add("ca5041c264f04f2e8c09f789ac19dbf1");
        keys.add("2a681f188c9c43b3a6581f0b3d4e5de7");
        keys.add("00b0e581e4124a2583ea7dba57aaf281");
    }

    int currentIndex = 0;
//    VisionServiceRestClient client = new VisionServiceRestClient(keys.get(currentIndex));
    VisionServiceRestClient client = new VisionServiceRestClient("9e88939475894dec85a2019fd36243be");

    private String img_path;
    private boolean verticalOrientation = true;
    Observable.OnSubscribe<OCR> onSubscribe = new Observable.OnSubscribe<OCR>() {
        @Override
        public void call(Subscriber<? super OCR> subscriber) {
            client.setOnTimeUseUp(new WebServiceRequest.OnResult() {
                @Override
                public void onTimeUseUp() {
                    //返回403
                    currentIndex = (currentIndex + 1) % keys.size();
                    client = new VisionServiceRestClient(keys.get(currentIndex));
                    if (SPHelper.getString(Constants.DIY_OCR_KEY, "").equals("")) {
                        subscriber.onError(new IOException(TimeCatApp.getInstance().getResources().getString(R.string.ocr_useup_toast)));
                    } else {
                        subscriber.onError(new IOException("time out"));
                    }
                }

                @Override
                public void onSuccess() {

                }
            });
            byte[] data = IOUtil.getBytes(img_path);
            try {
                String ocr = client.recognizeText(data, LanguageCodes.AutoDetect, verticalOrientation);
                if (!TextUtils.isEmpty(ocr)) {
                    OCR ocrItem = new Gson().fromJson(ocr, new TypeToken<OCR>() {
                    }.getType());
                    subscriber.onNext(ocrItem);
                }
            } catch (VisionServiceException e) {
                e.printStackTrace();
                subscriber.onError(e);
            } catch (IOException e) {
                e.printStackTrace();
                subscriber.onError(e);
            }
        }
    };
    private byte[] img;
    Observable.OnSubscribe<OCR> onSubscribe1 = new Observable.OnSubscribe<OCR>() {
        @Override
        public void call(Subscriber<? super OCR> subscriber) {

            try {
                client.setOnTimeUseUp(new WebServiceRequest.OnResult() {
                    @Override
                    public void onTimeUseUp() {
                        //返回403
                        currentIndex = (currentIndex + 1) % keys.size();
                        client = new VisionServiceRestClient(keys.get(currentIndex));
                        if (SPHelper.getString(Constants.DIY_OCR_KEY, "").equals("")) {
                            subscriber.onError(new IOException(TimeCatApp.getInstance().getResources().getString(R.string.ocr_useup_toast)));
                        } else {
                            subscriber.onError(new IOException("time out"));
                        }
                    }

                    @Override
                    public void onSuccess() {

                    }
                });
                String ocr = client.recognizeText(img, LanguageCodes.AutoDetect, verticalOrientation);

                if (!TextUtils.isEmpty(ocr)) {
                    OCR ocrItem = new Gson().fromJson(ocr, new TypeToken<OCR>() {
                    }.getType());
                    subscriber.onNext(ocrItem);
                }
            } catch (VisionServiceException e) {
                e.printStackTrace();
                subscriber.onError(e);
            } catch (IOException e) {
                e.printStackTrace();
                subscriber.onError(e);
            }
        }
    };
    private String searchPicPath;
    Observable.OnSubscribe<ImageUpload> onSubscribe2 = new Observable.OnSubscribe<ImageUpload>() {
        @Override
        public void call(Subscriber<? super ImageUpload> subscriber) {

            try {
                String json = UploadUtil.uploadFile(new File(searchPicPath));
                if (!TextUtils.isEmpty(json)) {
                    ImageUpload imageUpload = new Gson().fromJson(json, new TypeToken<ImageUpload>() {
                    }.getType());
                    subscriber.onNext(imageUpload);
                } else {
                    subscriber.onError(new Throwable("上传失败"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                subscriber.onError(e);
            }
        }
    };

    public static OcrAnalyser getInstance() {
        return instance;
    }

    public void uploadImage(BaseActivity activity, String fileName, ImageUploadCallBack callback) {
        this.searchPicPath = fileName;
        Observable.create(onSubscribe2).subscribeOn(Schedulers.io()).compose(activity.bindToLifecycle()).observeOn(AndroidSchedulers.mainThread()).subscribe(s -> callback.onSuccess(s), throwable -> {
            callback.onFail(throwable);
        });
    }

    public void analyse(BaseActivity activity, String img_path, boolean isVertical, CallBack callback) {
//        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
        String diykey = SPHelper.getString(Constants.DIY_OCR_KEY, "");
        if (!TextUtils.isEmpty(diykey) && !keys.contains(diykey)) {
            keys.add(0, SPHelper.getString(Constants.DIY_OCR_KEY, ""));
            currentIndex = 0;
            client = new VisionServiceRestClient(keys.get(currentIndex));
        }
        if (callback == null) return;
        int time = SPHelper.getInt(Constants.OCR_TIME, 0) + 1;
        SPHelper.save(Constants.OCR_TIME, time);
        this.img_path = img_path;
        this.verticalOrientation = isVertical;
        Observable.create(onSubscribe).subscribeOn(Schedulers.io()).compose(activity.bindToLifecycle()).observeOn(AndroidSchedulers.mainThread()).subscribe(s -> callback.onSuccess(s), throwable -> {
            callback.onFail(throwable);
            SPHelper.save(Constants.SHOULD_SHOW_DIY_OCR, true);
        });
    }

    public String getPassedMiscSoftText(OCR ocr) {

        String result = "";
        for (Region reg : ocr.regions) {
            for (Line line : reg.lines) {
                for (Word word : line.words) {
                    result += word.text + " ";
                }
                result += "\n";
            }
            result += "\n\n";
        }
        if (ocr.language.equalsIgnoreCase(LanguageCodes.ChineseSimplified) || ocr.language.equalsIgnoreCase(LanguageCodes.ChineseTraditional)) {
            result = result.replaceAll(" ", "");
        }
        if (TextUtils.isEmpty(result)) result = "no text found";
        return result;
    }

    public interface ImageUploadCallBack {
        void onSuccess(ImageUpload imageUpload);

        void onFail(Throwable throwable);
    }

    public interface CallBack {
        void onSuccess(OCR ocr);

        void onFail(Throwable throwable);
    }

}
