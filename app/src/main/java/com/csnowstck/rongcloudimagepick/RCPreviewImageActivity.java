package com.csnowstck.rongcloudimagepick;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.csnowstck.rongcloudimagepick.RCSelectImageActivity.PicItem;
import com.csnowstck.rongcloudimagepick.photoview.PhotoView;
import com.csnowstck.rongcloudimagepick.photoview.PhotoViewAttacher;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * Created by cqll on 2016/6/17.
 */
public class RCPreviewImageActivity extends Activity {
    private TextView mIndexTotal;
    private View mWholeView;
    private View mToolbarTop;
    private View mToolbarBottom;
    private View mToolbar;
    private ImageView mBtnBack;
    private TextView mTvSend;
    private CheckButton mSelectBox;
    private HackyViewPager mViewPager;
    private ArrayList<PicItem> mItemList;
    private int mCurrentIndex;
    private boolean mFullScreen;
    private int maxSelected;
    private AnimatorSet mShowAnimatorSet,mHideAnimatorSet;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_rc_preview_image);
        initView();
        initEvent();
    }

    public void initView() {
        mToolbarTop = this.findViewById(R.id.toolbar_top);
        mToolbar = this.findViewById(R.id.toolbar);
        mIndexTotal = (TextView) this.findViewById(R.id.index_total);
        mBtnBack = (ImageView) this.findViewById(R.id.back);
        mTvSend = (TextView) this.findViewById(R.id.send);
        mWholeView = this.findViewById(R.id.whole_layout);
        mViewPager = (HackyViewPager) this.findViewById(R.id.viewpager);
        mToolbarBottom = this.findViewById(R.id.toolbar_bottom);
        mSelectBox = new CheckButton(this.findViewById(R.id.select_check), R.drawable.select_check_nor, R.drawable.select_check_sel);

        maxSelected=getIntent().getIntExtra("maxSelected",9);
        mCurrentIndex = getIntent().getIntExtra("index", 0);
        mItemList = getIntent().getParcelableArrayListExtra("picList");
        mIndexTotal.setText(String.format("%d/%d", new Object[]{Integer.valueOf(this.mCurrentIndex + 1), Integer.valueOf(this.mItemList.size())}));
        if (Build.VERSION.SDK_INT >= 11) {
            mWholeView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        int result = 0;
        int resourceId = this.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = this.getResources().getDimensionPixelSize(resourceId);
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(this.mToolbarTop.getLayoutParams());
        lp.setMargins(0, result, 0, 0);
        mToolbarTop.setLayoutParams(lp);

        mSelectBox.setText(R.string.rc_picprev_select);
        mSelectBox.setChecked((mItemList.get(mCurrentIndex)).selected);


        mViewPager.setAdapter(new PreviewAdapter());
        mViewPager.setCurrentItem(this.mCurrentIndex);

        updateToolbar();

    }


    public void initEvent() {
        mBtnBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("picList", mItemList);
                setResult(-1, intent);
                finish();
            }
        });

        mTvSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent data = new Intent();
                ArrayList list = new ArrayList();
                Iterator i$ = mItemList.iterator();

                while (i$.hasNext()) {
                    PicItem item = (PicItem) i$.next();
                    if (item.selected) {
                        list.add("file://" + item.uri);
                    }
                }

                if (list.size() == 0) {
                    mSelectBox.setChecked(true);
                    list.add("file://" +  mItemList.get(mCurrentIndex));
                }

                data.putExtra("android.intent.extra.RETURN_RESULT", list);
                setResult(1, data);
                finish();
            }
        });


        mSelectBox.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!mSelectBox.getChecked() && getTotalSelectedNum() == maxSelected) {
                    Toast.makeText(RCPreviewImageActivity.this, R.string.rc_picsel_selected_max, Toast.LENGTH_SHORT).show();
                } else {
                    mSelectBox.setChecked(!mSelectBox.getChecked());
                    (mItemList.get(mCurrentIndex)).selected = mSelectBox.getChecked();
                    updateToolbar();
                }
            }
        });

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                mCurrentIndex = position;
                mIndexTotal.setText(String.format("%d/%d", new Object[]{Integer.valueOf(position + 1), Integer.valueOf(mItemList.size())}));
                mSelectBox.setChecked( mItemList.get(position).selected);
            }

            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private void updateToolbar() {
        int selNum = this.getTotalSelectedNum();
        if (mItemList.size() == 1 && selNum == 0) {
            mTvSend.setText(R.string.rc_picsel_toolbar_send);
        } else {
            if (selNum == 0) {
                this.mTvSend.setText(R.string.rc_picsel_toolbar_send);
            } else if (selNum <= maxSelected) {
                this.mTvSend.setText(String.format(this.getResources().getString(R.string.rc_picsel_toolbar_send_num), new Object[]{Integer.valueOf(selNum)}));
            }

        }
    }


    private int getTotalSelectedNum() {
        int sum = 0;

        for(int i = 0; i < this.mItemList.size(); ++i) {
            if((mItemList.get(i)).selected) {
                ++sum;
            }
        }

        return sum;
    }
    private class PreviewAdapter extends PagerAdapter {
        private PreviewAdapter() {
        }

        public int getCount() {
            return mItemList.size();
        }

        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        public Object instantiateItem(ViewGroup container, int position) {
            final PhotoView photoView = new PhotoView(container.getContext());
            photoView.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
                public void onViewTap(View view, float x, float y) {
                    mFullScreen = !mFullScreen;
                    View decorView;
                    byte uiOptions;
                    if(mFullScreen) {
                        if(Build.VERSION.SDK_INT < 16) {
                            getWindow().setFlags(WindowManager.LayoutParams.SCREEN_ORIENTATION_CHANGED, WindowManager.LayoutParams.SCREEN_ORIENTATION_CHANGED);
                        } else {
                            decorView = getWindow().getDecorView();
                            uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
                            decorView.setSystemUiVisibility(uiOptions);
                        }

                        if(mHideAnimatorSet==null){
                            mHideAnimatorSet=new AnimatorSet();
                            int statusHeight= ScreenUtil.getStatusBarHeight(RCPreviewImageActivity.this);
                            mHideAnimatorSet.playTogether(ObjectAnimator.ofFloat(mToolbar,"translationY",-mToolbar.getHeight()- statusHeight), ObjectAnimator.ofFloat(mToolbarBottom,"translationY",mToolbarBottom.getHeight()));
                            mHideAnimatorSet.setDuration(400);
                        }
                        mHideAnimatorSet.start();
                    } else {
                        if(Build.VERSION.SDK_INT < 16) {
                            getWindow().setFlags(WindowManager.LayoutParams.SCREEN_ORIENTATION_CHANGED, WindowManager.LayoutParams.SCREEN_ORIENTATION_CHANGED);
                        } else {
                            decorView = getWindow().getDecorView();
                            uiOptions = View.OVER_SCROLL_ALWAYS;
                            decorView.setSystemUiVisibility(uiOptions);
                        }

                        if(mShowAnimatorSet==null){
                            mShowAnimatorSet=new AnimatorSet();
                            mShowAnimatorSet.playTogether(ObjectAnimator.ofFloat(mToolbar,"translationY",0), ObjectAnimator.ofFloat(mToolbarBottom,"translationY",0));
                            mShowAnimatorSet.setDuration(400);
                        }

                        mShowAnimatorSet.start();

                    }

                }
            });
            container.addView(photoView, -1, -1);
            String path = mItemList.get(position).uri;
            //大小不一样要重新添加图片缓存
            AlbumBitmapCacheHelper.getInstance().removePathFromShowlist(path);
            AlbumBitmapCacheHelper.getInstance().addPathToShowlist(path);
            Bitmap bitmap = AlbumBitmapCacheHelper.getInstance().getBitmap(path, 0, 0, new AlbumBitmapCacheHelper.ILoadImageCallback() {
                public void onLoadImageCallBack(Bitmap bitmap, String p, Object... objects) {
                    if(bitmap != null) {
                        photoView.setImageBitmap(bitmap);
                    }
                }
            }, new Object[]{Integer.valueOf(position)});
            if(bitmap != null) {
                photoView.setImageBitmap(bitmap);
            } else {
                photoView.setImageResource(R.drawable.rc_grid_image_default);
            }

            return photoView;
        }

        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == 4) {
            Intent intent = new Intent();
            intent.putExtra("picList", this.mItemList);
            this.setResult(-1, intent);
        }
        return super.onKeyDown(keyCode, event);
    }

    private class CheckButton {
        private View rootView;
        private ImageView image;
        private TextView text;
        private boolean checked = false;
        private int nor_resId;
        private int sel_resId;

        public CheckButton(View root, @DrawableRes int norId, @DrawableRes int selId) {
            this.rootView = root;
            this.image = (ImageView) root.findViewById(R.id.image);
            this.text = (TextView) root.findViewById(R.id.text);
            this.nor_resId = norId;
            this.sel_resId = selId;
            this.image.setImageResource(this.nor_resId);
        }

        public void setChecked(boolean check) {
            this.checked = check;
            this.image.setImageResource(this.checked ? this.sel_resId : this.nor_resId);
        }

        public boolean getChecked() {
            return this.checked;
        }

        public void setText(int resId) {
            this.text.setText(resId);
        }

        public void setText(CharSequence chars) {
            this.text.setText(chars);
        }

        public void setOnClickListener(@Nullable View.OnClickListener l) {
            this.rootView.setOnClickListener(l);
        }
    }
}
