package com.dingmouren.easymvp.ui.category;

import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.widget.ProgressBar;

import com.dingmouren.easymvp.MyApplication;
import com.dingmouren.easymvp.R;
import com.dingmouren.easymvp.base.BaseFragment;
import com.dingmouren.easymvp.bean.gank.GankContent;
import com.dingmouren.easymvp.event.NightModeChangeEvent;
import com.dingmouren.easymvp.ui.category.layouts.CategoryItemViewProvider;
import com.dingmouren.easymvp.util.NetworkUtil;
import com.dingzi.greendao.GankContentDao;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import me.drakeet.multitype.MultiTypeAdapter;

/**
 * Created by dingmouren on 2016/12/13.
 */

public class CategoryDetailFragment extends BaseFragment implements CategoryContract.View {

    @BindView(R.id.swipe_refresh)  SwipeRefreshLayout mSwipeRefresh;
    @BindView(R.id.recycler) RecyclerView mRecycler;
    @BindView(R.id.pb) ProgressBar mPbLoadMore;
    private String mType;
    private LinearLayoutManager mLinearLayoutManager;
    private CategoryPresenter mPresenter;
    public List<Object> mItems;
    private MultiTypeAdapter mMultiTypeAdapter;
    private QueryBuilder<GankContent> mQueryBuilder;
    @Override
    protected int setLayoutResourceID() {
        return R.layout.fragment_category_detail;
    }

    @Override
    protected void init() {
        mType = getArguments().getString("type");
        //布局容器
        mItems = new ArrayList<>();
        mMultiTypeAdapter = new MultiTypeAdapter(mItems);
        //注册布局
        mMultiTypeAdapter.register(GankContent.class,new CategoryItemViewProvider());

        //获取greendao的QueryBuilder
        mQueryBuilder = MyApplication.getDaoSession().getGankContentDao().queryBuilder().where(GankContentDao.Properties.Type.eq(mType));
    }

    @Override
    protected void setUpView() {
        EventBus.getDefault().register(this);//注册事件总线
        //SwipeRefresh相关
        if (mSwipeRefresh != null){
            mSwipeRefresh.setColorSchemeResources(R.color.main_color);//设置进度动画的颜色
            mSwipeRefresh.setProgressViewOffset(true,0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,24,getResources().getDisplayMetrics()));
            mSwipeRefresh.setOnRefreshListener(()->mPresenter.requestData());//每次下拉刷新时都会清空数据，然后加载第一页的数据
        }

        //RecyclerView相关
        mLinearLayoutManager = new LinearLayoutManager(mRecycler.getContext());
        mRecycler.setHasFixedSize(true);
        mRecycler.setLayoutManager(mLinearLayoutManager);
        mRecycler.setAdapter(mMultiTypeAdapter);
        //Presenter相关
        mPresenter = new CategoryPresenter((CategoryContract.View)this);

    }

    @Override
    protected void setUpData() {


        if (null != mQueryBuilder
                && 0 < mQueryBuilder.count()
                && !NetworkUtil.isAvailable(getActivity())){
            mItems.clear();//清空集合，为了保险起见
            mItems.addAll(mQueryBuilder.list());
            mMultiTypeAdapter.notifyDataSetChanged();
        }else {
            setRefresh(true);
            mPresenter.requestData();
        }
        mPresenter.addScrollListener();//滚动的监听

    }


    @Override
    public void setRefresh(boolean refresh) {
        if (refresh){
           mSwipeRefresh.setRefreshing(true);
        }else {
            new Handler().postDelayed(()->mSwipeRefresh.setRefreshing(refresh),500);//延时消失加载的loading
        }
    }


    @Override
    public RecyclerView getRecyclerView() {
        return mRecycler;
    }

    @Override
    public LinearLayoutManager getLayoutManager() {
        return mLinearLayoutManager;
    }

    @Override
    public String getType() {
        return mType;
    }

    @Override
    public void setData() {
        mMultiTypeAdapter.notifyDataSetChanged();
        setRefresh(false);
        loadMore(false);
    }

    @Override
    public List<Object> getItems() {
        return mItems;
    }

    @Override
    public void loadMore(boolean loadMore) {
        if (loadMore){
            mPbLoadMore.setVisibility(View.VISIBLE);
        }else {
            mPbLoadMore.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean isRefreshing() {
        return mSwipeRefresh.isRefreshing();
    }

    //接收到改变模式的通知，重新刷新视图
    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    public void changeNightMode(NightModeChangeEvent event){
        mMultiTypeAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);//解绑事件总线
    }
}
