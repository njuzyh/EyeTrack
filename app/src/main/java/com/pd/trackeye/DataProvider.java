package com.pd.trackeye;

import android.content.Context;
import android.content.res.TypedArray;

import java.util.ArrayList;

public class DataProvider {

//    private static ArrayList<Integer> getData2(Context context){
//        ArrayList<Integer> data = new ArrayList<>();
//        TypedArray bannerImage = context.getResources().obtainTypedArray(R.array.data_narrow_Image);
//        for (int i = 0; i < 30 ; i++) {
//            int image = bannerImage.getResourceId(i, R.drawable.bg_autumn_tree_min);
//            data.add(image);
//        }
//        bannerImage.recycle();
//        return data;
//    }

//    public static List<PersonData> getList(Context context,int size){
//        if (size==0){
//            size = 16;
//        }
//        ArrayList<PersonData> arr = new ArrayList<>();
//        ArrayList<Integer> data = getData2(context);
//        for (int i=0 ; i< size ; i++){
//            PersonData person = new PersonData();
//            person.setName("小杨逗比"+i);
//            person.setImage(data.get(i));
//            person.setSign("杨充"+i);
//            arr.add(person);
//        }
//        return arr;
//    }

    public static String[] VideoPlayerList = {
            "http://jzvd.nathen.cn/c494b340ff704015bb6682ffde3cd302/64929c369124497593205a4190d7d128-5287d2089db37e62345123a1be272f8b.mp4",
            "http://jzvd.nathen.cn/b201be3093814908bf987320361c5a73/2f6d913ea25941ffa78cc53a59025383-5287d2089db37e62345123a1be272f8b.mp4",
            "http://jzvd.nathen.cn/25a8d119cfa94b49a7a4117257d8ebd7/f733e65a22394abeab963908f3c336db-5287d2089db37e62345123a1be272f8b.mp4",
            "http://jzvd.nathen.cn/c6e3dc12a1154626b3476d9bf3bd7266/6b56c5f0dc31428083757a45764763b0-5287d2089db37e62345123a1be272f8b.mp4",

            "http://jzvd.nathen.cn/c494b340ff704015bb6682ffde3cd302/64929c369124497593205a4190d7d128-5287d2089db37e62345123a1be272f8b.mp4",
            "http://jzvd.nathen.cn/b201be3093814908bf987320361c5a73/2f6d913ea25941ffa78cc53a59025383-5287d2089db37e62345123a1be272f8b.mp4",
            "http://jzvd.nathen.cn/25a8d119cfa94b49a7a4117257d8ebd7/f733e65a22394abeab963908f3c336db-5287d2089db37e62345123a1be272f8b.mp4",
            "http://jzvd.nathen.cn/c6e3dc12a1154626b3476d9bf3bd7266/6b56c5f0dc31428083757a45764763b0-5287d2089db37e62345123a1be272f8b.mp4",

            "http://jzvd.nathen.cn/c494b340ff704015bb6682ffde3cd302/64929c369124497593205a4190d7d128-5287d2089db37e62345123a1be272f8b.mp4",
            "http://jzvd.nathen.cn/b201be3093814908bf987320361c5a73/2f6d913ea25941ffa78cc53a59025383-5287d2089db37e62345123a1be272f8b.mp4",
            "http://jzvd.nathen.cn/25a8d119cfa94b49a7a4117257d8ebd7/f733e65a22394abeab963908f3c336db-5287d2089db37e62345123a1be272f8b.mp4",
            "http://jzvd.nathen.cn/c6e3dc12a1154626b3476d9bf3bd7266/6b56c5f0dc31428083757a45764763b0-5287d2089db37e62345123a1be272f8b.mp4"
    };


    public static String[] VideoPlayerTitle = {
            "1",
            "2",
            "3",
            "4",

            "5",
            "6",
            "7",
            "8",

            "9",
            "10",
            "11",
            "12"
    };


    public static ArrayList<Integer> getData(Context context){
        ArrayList<Integer> data = new ArrayList<>();
        //TypedArray bannerImage = context.getResources().obtainTypedArray(R.array.image_girls);
        for (int i = 0; i < 50 ; i++) {
            int image = R.drawable.image_default;
            data.add(image);
        }
        //bannerImage.recycle();
        return data;
    }

}
