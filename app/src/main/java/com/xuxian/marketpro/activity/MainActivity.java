package com.xuxian.marketpro.activity;

import android.app.Activity;
import android.os.Bundle;

import com.xuxian.marketpro.R;
import com.xuxian.marketpro.activity.supers.SuperSherlockActivity;

public class MainActivity extends SuperSherlockActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected Activity getActivity() {
        return null;
    }

    @Override
    protected void init() {

    }

    @Override
    protected void initTitleBar() {

    }

    @Override
    protected void initFindViewById() {

    }

    @Override
    protected void setListener() {

    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//    }



}
