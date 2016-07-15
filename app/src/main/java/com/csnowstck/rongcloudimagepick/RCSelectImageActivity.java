package com.csnowstck.rongcloudimagepick;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by cqll on 2016/6/17.
 */
public class RCSelectImageActivity extends Activity {
    private TextView mTvSend;
    private GridView mGridlist;
    private ListView mCatalogListview;
    private RelativeLayout mCatalogView;
    private List<PicItem> mAllItemList;//所有的图片列表
    private Map<String, List<PicItem>> mItemMap;//保存目录和目录里图片
    private List<String> mCatalogList;//目录列表
    private String mCurrentCatalog = "";//当前目录
    private Uri mTakePictureUri;//拍照保存的图片地址
    private int perWidth;//图片宽度
    private int maxSelected;//最大选择的照片,默认为9

    private PicTypeBtn mPicType;
    private PreviewBtn mPreviewBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_rongcloud_picsel);
        initView();
        initEvent();
    }

    public void initView() {

        mTvSend=(TextView)findViewById(R.id.send);
        mPicType= (PicTypeBtn) findViewById(R.id.pic_type);
        mPreviewBtn= (PreviewBtn) findViewById(R.id.preview);
        mGridlist= (GridView) findViewById(R.id.gridlist);
        mCatalogListview= (ListView) findViewById(R.id.catalog_listview);
        mCatalogView= (RelativeLayout) findViewById(R.id.catalog_window);

        mTvSend.setEnabled(false);
        this.mPicType.init(this);
        this.mPicType.setEnabled(false);
        this.mPreviewBtn.init(this);
        this.mPreviewBtn.setEnabled(false);

        maxSelected=getIntent().getIntExtra("maxSelected",9);
        if (Build.VERSION.SDK_INT >= 23) {
            int checkPermission = this.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE");
            if (checkPermission != 0) {
                if (this.shouldShowRequestPermissionRationale("android.permission.READ_EXTERNAL_STORAGE")) {
                    this.requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 100);
                } else {
                    (new AlertDialog.Builder(this)).setMessage("您需要在设置里打开存储空间权限。").setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(DialogInterface dialog, int which) {
                            RCSelectImageActivity.this.requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 100);
                        }
                    }).setNegativeButton("取消", (android.content.DialogInterface.OnClickListener) null).create().show();
                }

                return;
            }
        }
        this.initData();
    }

    private void initData() {
        updatePictureItems();
        mGridlist.setAdapter(new GridViewAdapter());
        mPicType.setEnabled(true);
        mPicType.setTextColor(Color.WHITE);
        mCatalogListview.setAdapter(new CatalogAdapter());
        AlbumBitmapCacheHelper.init(this);
        this.perWidth = (getResources().getDisplayMetrics().widthPixels - CommonUtils.dip2px(this, 4.0F)) / 3;
    }

    public void initEvent() {

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });



        mGridlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    ArrayList list = new ArrayList();
                    if(mCurrentCatalog.isEmpty()) {
                        list.addAll(mAllItemList);
                    } else {
                        list.addAll((Collection)mItemMap.get(mCurrentCatalog));
                    }

                    Intent preIntent = new Intent(RCSelectImageActivity.this, RCPreviewImageActivity.class);
                    preIntent.putExtra("picList", list);
                    preIntent.putExtra("index", position - 1);
                    startActivityForResult(preIntent, 0);
                }
            }
        });
        mTvSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent data = new Intent();
                ArrayList list = new ArrayList();
                Iterator i$ = mItemMap.keySet().iterator();
                //遍历目录
                while (i$.hasNext()) {
                    String catalog = (String) i$.next();
                    Iterator i$1 = mItemMap.get(catalog).iterator();
                    //遍历图片
                    while (i$1.hasNext()) {
                        PicItem pic = (PicItem) i$1.next();
                        if (pic.selected) {
                            list.add("file://" + pic.uri);
                        }

                    }
                }
                data.putExtra("android.intent.extra.RETURN_RESULT", list);
                setResult(RESULT_OK, data);
                finish();
            }
        });
        //显示目录
        mPicType.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(View.VISIBLE==mCatalogView.getVisibility()){
                    mCatalogView.setVisibility(View.GONE);
                }else {
                    mCatalogView.setVisibility(View.VISIBLE);
                }

            }
        });

        mPreviewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList list = new ArrayList();
                Intent intent = new Intent(RCSelectImageActivity.this, RCPreviewImageActivity.class);
                Iterator i$ = mItemMap.keySet().iterator();
                while (i$.hasNext()) {
                    String catalog = (String) i$.next();
                    Iterator i$1 = mItemMap.get(catalog).iterator();
                    while (i$1.hasNext()) {
                        PicItem pic = (PicItem) i$1.next();
                        if (pic.selected) {
                            list.add(pic);
                        }
                    }
                }
                intent.putExtra("picList", list);
                startActivityForResult(intent, 0);

            }
        });

        mCatalogView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && mCatalogView.getVisibility() == View.VISIBLE) {
                    mCatalogView.setVisibility(View.GONE);
                }
                return true;
            }
        });

        mCatalogListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String catalog;
                if (position == 0) {
                    catalog = "";
                } else {
                    catalog = mCatalogList.get(position - 1);
                }
                if (catalog.equals(mCurrentCatalog)) {
                    mCatalogView.setVisibility(View.GONE);
                } else {
                    mCurrentCatalog = catalog;
                    mCatalogView.setVisibility(View.GONE);
                    TextView typeTv = (TextView) view.findViewById(R.id.name);
                    mPicType.setText(typeTv.getText().toString().trim());

                    ((GridViewAdapter) mGridlist.getAdapter()).notifyDataSetChanged();
                    ((CatalogAdapter) mCatalogListview.getAdapter()).notifyDataSetChanged();
                }
            }
        });
    }

    private void updatePictureItems() {
        String[] projection = new String[]{"_data", "date_added"};
        String orderBy = "datetaken DESC";
        Cursor cursor = this.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, (String) null, (String[]) null, orderBy);
        this.mAllItemList = new ArrayList();
        this.mCatalogList = new ArrayList();
        this.mItemMap = new ArrayMap();
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();

            do {
                PicItem item = new PicItem();
                item.uri = cursor.getString(0);
                this.mAllItemList.add(item);
                int last = item.uri.lastIndexOf("/");
                String catalog;
                if (last == 0) {
                    catalog = "/";
                } else {
                    int itemList = item.uri.lastIndexOf("/", last - 1);
                    catalog = item.uri.substring(itemList + 1, last);
                }

                if (this.mItemMap.containsKey(catalog)) {
                    ((List) this.mItemMap.get(catalog)).add(item);
                } else {
                    ArrayList itemList1 = new ArrayList();
                    itemList1.add(item);
                    this.mItemMap.put(catalog, itemList1);
                    this.mCatalogList.add(catalog);
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
    }


    private class CatalogAdapter extends BaseAdapter {
        private LayoutInflater mInflater = RCSelectImageActivity.this.getLayoutInflater();

        public CatalogAdapter() {
        }

        public int getCount() {
            return RCSelectImageActivity.this.mItemMap.size() + 1;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            CatalogAdapter.ViewHolder holder;
            if (convertView == null) {
                view = this.mInflater.inflate(R.layout.rc_picsel_catalog_listview, parent, false);
                holder = new CatalogAdapter.ViewHolder();
                holder.image = (ImageView) view.findViewById(R.id.image);
                holder.name = (TextView) view.findViewById(R.id.name);
                holder.number = (TextView) view.findViewById(R.id.number);
                holder.selected = (ImageView) view.findViewById(R.id.selected);
                view.setTag(holder);
            } else {
                holder = (CatalogAdapter.ViewHolder) convertView.getTag();
            }

            String path;
            //清除缓存里的  image 的tag
            if (holder.image.getTag() != null) {
                path = (String) holder.image.getTag();
                AlbumBitmapCacheHelper.getInstance().removePathFromShowlist(path);
            }

            int num = 0;
            boolean showSelected = false;
            String name;
            Bitmap bitmap;
            BitmapDrawable bd;
            if (position == 0) {
                if (mItemMap.size() == 0) {
                    holder.image.setImageResource(R.drawable.rc_picsel_empty_pic);
                } else {
                    path = ((PicItem) ((List) mItemMap.get(mCatalogList.get(0))).get(0)).uri;//获取第一个目录的第一张图片
                    AlbumBitmapCacheHelper.getInstance().addPathToShowlist(path);
                    holder.image.setTag(path);//设置tag 标识已经添加进缓存
                    bitmap = AlbumBitmapCacheHelper.getInstance().getBitmap(path, perWidth, perWidth, new AlbumBitmapCacheHelper.ILoadImageCallback() {
                        public void onLoadImageCallBack(Bitmap bitmap, String path1, Object... objects) {
                            if (bitmap != null) {
                                BitmapDrawable bd = new BitmapDrawable(RCSelectImageActivity.this.getResources(), bitmap);
                                View v = mGridlist.findViewWithTag(path1);
                                if (v != null) {
                                    v.setBackgroundDrawable(bd);
                                }

                            }
                        }
                    }, new Object[]{Integer.valueOf(position)});
                    if (bitmap != null) {
                        bd = new BitmapDrawable(RCSelectImageActivity.this.getResources(), bitmap);
                        holder.image.setBackgroundDrawable(bd);
                    } else {
                        holder.image.setBackgroundResource(R.drawable.rc_grid_image_default);
                    }
                }

                name = RCSelectImageActivity.this.getResources().getString(R.string.rc_picsel_catalog_allpic);
                holder.number.setVisibility(View.GONE);
                showSelected = mCurrentCatalog.isEmpty();
            } else {
                path = (mItemMap.get(mCatalogList.get(position - 1)).get(0)).uri;
                name =  mCatalogList.get(position - 1);
                num = (mItemMap.get(mCatalogList.get(position - 1))).size();
                holder.number.setVisibility(View.VISIBLE);
                showSelected = name.equals(mCurrentCatalog);
                AlbumBitmapCacheHelper.getInstance().addPathToShowlist(path);
                holder.image.setTag(path);
                bitmap = AlbumBitmapCacheHelper.getInstance().getBitmap(path, perWidth, perWidth, new AlbumBitmapCacheHelper.ILoadImageCallback() {
                    public void onLoadImageCallBack(Bitmap bitmap, String path1, Object... objects) {
                        if (bitmap != null) {
                            BitmapDrawable bd = new BitmapDrawable(RCSelectImageActivity.this.getResources(), bitmap);
                            View v = mGridlist.findViewWithTag(path1);
                            if (v != null) {
                                v.setBackgroundDrawable(bd);
                            }

                        }
                    }
                }, new Object[]{Integer.valueOf(position)});
                if (bitmap != null) {
                    bd = new BitmapDrawable(RCSelectImageActivity.this.getResources(), bitmap);
                    holder.image.setBackgroundDrawable(bd);
                } else {
                    holder.image.setBackgroundResource(R.drawable.rc_grid_image_default);
                }
            }

            holder.name.setText(name);
            holder.number.setText(String.format(RCSelectImageActivity.this.getResources().getString(R.string.rc_picsel_catalog_number), new Object[]{Integer.valueOf(num)}));
            holder.selected.setVisibility(showSelected ? View.VISIBLE : View.INVISIBLE);
            return view;
        }

        private class ViewHolder {
            ImageView image;
            TextView name;
            TextView number;
            ImageView selected;

            private ViewHolder() {
            }
        }
    }

    private class GridViewAdapter extends BaseAdapter {
        private LayoutInflater mInflater = getLayoutInflater();

        public GridViewAdapter() {
        }

        public int getCount() {
            int sum = 1;
            String key;
            if (mCurrentCatalog.isEmpty()) {
                //所有目录下图片的数量
                for (Iterator i$ = mItemMap.keySet().iterator(); i$.hasNext(); sum += ((List) mItemMap.get(key)).size()) {
                    key = (String) i$.next();
                }
            } else {
                sum += ((List) mItemMap.get(mCurrentCatalog)).size();
            }

            return sum;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return (long) position;
        }

        @TargetApi(23)
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position == 0) {
                View item1 = this.mInflater.inflate(R.layout.rc_picsel_grid_camera, parent, false);
                ImageButton view1 = (ImageButton) item1.findViewById(R.id.camera_mask);
                view1.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (Build.VERSION.SDK_INT >= 23) {
                            int checkPermission = v.getContext().checkSelfPermission("android.permission.CAMERA");
                            if (checkPermission != 0) {
                                if (shouldShowRequestPermissionRationale("android.permission.CAMERA")) {
                                    requestPermissions(new String[]{"android.permission.CAMERA"}, 100);
                                } else {
                                    (new AlertDialog.Builder(RCSelectImageActivity.this)).setMessage("您需要在设置里打开相机权限。").setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            RCSelectImageActivity.this.requestPermissions(new String[]{"android.permission.CAMERA"}, 100);
                                        }
                                    }).setNegativeButton("取消", (android.content.DialogInterface.OnClickListener) null).create().show();
                                }

                                return;
                            }
                        }

                        requestCamera();
                    }
                });
                return item1;
            } else {
                final PicItem item;
                if (mCurrentCatalog.isEmpty()) {
                    item = mAllItemList.get(position - 1);
                } else {
                    item = getItemAt(mCurrentCatalog, position - 1);
                }

                View view = convertView;
                final GridViewAdapter.ViewHolder holder;
                if (convertView != null && convertView.getTag() != null) {
                    holder = (GridViewAdapter.ViewHolder) convertView.getTag();
                } else {
                    view = this.mInflater.inflate(R.layout.rc_picsel_grid_item, parent, false);
                    holder = new GridViewAdapter.ViewHolder();
                    holder.image = (ImageView) view.findViewById(R.id.image);
                    holder.mask = view.findViewById(R.id.mask);
                    holder.checkBox = (SelectBox) view.findViewById(R.id.checkbox);
                    view.setTag(holder);
                }

                String path;
                //清除缓存里的  image 的tag
                if (holder.image.getTag() != null) {
                    path = (String) holder.image.getTag();
                    AlbumBitmapCacheHelper.getInstance().removePathFromShowlist(path);
                }

                path = item.uri;
                AlbumBitmapCacheHelper.getInstance().addPathToShowlist(path);
                holder.image.setTag(path);
                Bitmap bitmap = AlbumBitmapCacheHelper.getInstance().getBitmap(path, perWidth, perWidth, new AlbumBitmapCacheHelper.ILoadImageCallback() {
                    public void onLoadImageCallBack(Bitmap bitmap, String path1, Object... objects) {
                        if (bitmap != null) {
                            BitmapDrawable bd = new BitmapDrawable(RCSelectImageActivity.this.getResources(), bitmap);
                            View v = mGridlist.findViewWithTag(path1);
                            if (v != null) {
                                v.setBackgroundDrawable(bd);
                            }

                        }
                    }
                }, new Object[]{Integer.valueOf(position)});
                if (bitmap != null) {
                    BitmapDrawable bd = new BitmapDrawable(RCSelectImageActivity.this.getResources(), bitmap);
                    holder.image.setBackgroundDrawable(bd);
                } else {
                    holder.image.setBackgroundResource(R.drawable.rc_grid_image_default);
                }

                holder.checkBox.setChecked(item.selected);
                holder.checkBox.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (!holder.checkBox.getChecked() && getTotalSelectedNum() == maxSelected) {
                            Toast.makeText(RCSelectImageActivity.this.getApplicationContext(), R.string.rc_picsel_selected_max, Toast.LENGTH_SHORT).show();
                        } else {
                            holder.checkBox.setChecked(!holder.checkBox.getChecked());
                            item.selected = holder.checkBox.getChecked();
                            if (item.selected) {
                                holder.mask.setBackgroundColor(RCSelectImageActivity.this.getResources().getColor(R.color.rc_picsel_grid_mask_pressed));
                            } else {
                                holder.mask.setBackgroundDrawable(RCSelectImageActivity.this.getResources().getDrawable(R.drawable.rc_sp_grid_mask));
                            }

                            updateToolbar();
                        }
                    }
                });
                if (item.selected) {
                    holder.mask.setBackgroundColor(RCSelectImageActivity.this.getResources().getColor(R.color.rc_picsel_grid_mask_pressed));
                } else {
                    holder.mask.setBackgroundDrawable(RCSelectImageActivity.this.getResources().getDrawable(R.drawable.rc_sp_grid_mask));
                }

                return view;
            }
        }

        private class ViewHolder {
            ImageView image;
            View mask;
            SelectBox checkBox;

            private ViewHolder() {
            }
        }
    }

    private void updateToolbar() {
        int sum = this.getTotalSelectedNum();
        if (sum == 0) {
            this.mTvSend.setEnabled(false);
            this.mTvSend.setTextColor(Color.parseColor("#CFCFCF"));
            this.mTvSend.setText(getString(R.string.rc_picsel_toolbar_send));
            this.mPreviewBtn.setEnabled(false);
            this.mPreviewBtn.setText(getString(R.string.rc_picsel_toolbar_preview));
        } else if (sum <= maxSelected) {
            this.mTvSend.setEnabled(true);
            this.mTvSend.setTextColor(Color.WHITE);
            this.mTvSend.setText(String.format(getString(R.string.rc_picsel_toolbar_send_num), new Object[]{Integer.valueOf(sum)}));
            this.mPreviewBtn.setEnabled(true);
            this.mPreviewBtn.setText(String.format(getString(R.string.rc_picsel_toolbar_preview_num), new Object[]{Integer.valueOf(sum)}));
        }

    }

    private PicItem getItemAt(String catalog, int index) {
        if (!this.mItemMap.containsKey(catalog)) {
            return null;
        } else {
            int sum = 0;

            for (Iterator i$ = ((List) this.mItemMap.get(catalog)).iterator(); i$.hasNext(); ++sum) {
                PicItem item = (PicItem) i$.next();
                if (sum == index) {
                    return item;
                }
            }

            return null;
        }
    }

    private int getTotalSelectedNum() {
        int sum = 0;
        Iterator i$ = this.mItemMap.keySet().iterator();

        while (i$.hasNext()) {
            String key = (String) i$.next();
            Iterator i$1 = ((List) this.mItemMap.get(key)).iterator();

            while (i$1.hasNext()) {
                PicItem item = (PicItem) i$1.next();
                if (item.selected) {
                    ++sum;
                }
            }
        }

        return sum;
    }

    protected void requestCamera() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (!path.exists()) {
            path.mkdirs();
        }

        String name = System.currentTimeMillis() + ".jpg";
        File file = new File(path, name);
        this.mTakePictureUri = Uri.fromFile(file);
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra("output", this.mTakePictureUri);
        this.startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED) {
            if (resultCode == RESULT_FIRST_USER) {
                this.setResult(RESULT_OK, data);
                this.finish();
            } else {
                ArrayList list;
                switch (requestCode) {
                    //预览返回
                    case 0:
                        list =data.getParcelableArrayListExtra("picList");
                        Iterator i$=list.iterator();
                        while (i$.hasNext()){
                            PicItem intentData= (PicItem) i$.next();
                            PicItem item=findPicByUri(intentData.uri);
                            item.selected=intentData.selected;
                        }
                        ((CatalogAdapter)mCatalogListview.getAdapter()).notifyDataSetInvalidated();
                        ((GridViewAdapter)mGridlist.getAdapter()).notifyDataSetInvalidated();
                        updateToolbar();
                        break;
                    //拍照
                    case 1:
                        list = new ArrayList();
                        PicItem pic = new PicItem();
                        pic.uri = mTakePictureUri.getPath();
                        list.add(pic);
                        Intent intent = new Intent(this, RCPreviewImageActivity.class);
                        intent.putExtra("picList", list);
                        //获取地址去预览页面
                        this.startActivityForResult(intent, 0);
                        //扫描指定目录下的文件
                        MediaScannerConnection.scanFile(this, new String[]{this.mTakePictureUri.getPath()}, (String[]) null, new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                                //重新查询
                                updatePictureItems();
                            }
                        });
                        break;
                }
            }
        }
    }

    private PicItem findPicByUri(String uri) {
        Iterator i$=mItemMap.keySet().iterator();
        while (i$.hasNext()){
            String type= (String) i$.next();
            Iterator i$1=mItemMap.get(type).iterator();
            while (i$1.hasNext()){
                PicItem verifyItem= (PicItem) i$1.next();
                if(uri.equals(verifyItem.uri)){
                    return verifyItem;
                }
            }
        }
        return null;
    }

    //申请权限
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 100:
                if (grantResults[0] == 0) {
                    if (permissions[0].equals("android.permission.READ_EXTERNAL_STORAGE")) {
                        this.initView();
                    } else if (permissions[0].equals("android.permission.CAMERA")) {
                        this.requestCamera();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(event.getAction()==KeyEvent.KEYCODE_BACK&& this.mCatalogView != null && this.mCatalogView.getVisibility() == View.VISIBLE){
            mCatalogView.setVisibility(View.GONE);
            return true;
        }else {
            return super.onKeyDown(keyCode, event);
        }

    }

    protected void onDestroy() {
        AlbumBitmapCacheHelper.getInstance().uninit();
        super.onDestroy();
    }



    public static class PicItem implements Parcelable {
        String uri;
        boolean selected;

        public PicItem() {
        }

        protected PicItem(Parcel in) {
            uri = in.readString();
            selected = in.readByte() != 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(uri);
            dest.writeByte((byte) (selected ? 1 : 0));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<PicItem> CREATOR = new Creator<PicItem>() {
            @Override
            public PicItem createFromParcel(Parcel in) {
                return new PicItem(in);
            }

            @Override
            public PicItem[] newArray(int size) {
                return new PicItem[size];
            }
        };
    }

    public static class SelectBox extends ImageView {
        private boolean mIsChecked;

        public SelectBox(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.setImageResource(R.drawable.select_check_nor);
        }

        public void setChecked(boolean check) {
            this.mIsChecked = check;
            this.setImageResource(this.mIsChecked ? R.drawable.select_check_sel : R.drawable.select_check_nor);
        }

        public boolean getChecked() {
            return this.mIsChecked;
        }
    }

    public static class PreviewBtn extends LinearLayout {
        private TextView mText;
        private Context mContext;

        public PreviewBtn(Context context, AttributeSet attrs) {
            super(context, attrs);
            mContext = context;
        }

        public void init(Activity root) {
            this.mText = (TextView) root.findViewById(R.id.preview_text);
        }

        public void setText(int id) {
            this.mText.setText(id);
        }

        public void setText(String text) {
            this.mText.setText(text);
        }

        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            int color = enabled ? R.color.rc_picsel_toolbar_send_text_normal : R.color.rc_picsel_toolbar_send_text_disable;
            this.mText.setTextColor(ContextCompat.getColor(mContext, color));
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (this.isEnabled()) {
                switch (event.getAction()) {
                    case 0:
                        this.mText.setVisibility(INVISIBLE);
                        break;
                    case 1:
                        this.mText.setVisibility(VISIBLE);
                }
            }

            return super.onTouchEvent(event);
        }
    }

    public static class PicTypeBtn extends LinearLayout {
        TextView mText;

        public PicTypeBtn(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void init(Activity root) {
            this.mText = (TextView) root.findViewById(R.id.type_text);
        }

        public void setText(String text) {
            this.mText.setText(text);
        }

        public void setTextColor(int color) {
            this.mText.setTextColor(color);
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (this.isEnabled()) {
                switch (event.getAction()) {
                    case 0:
                        this.mText.setVisibility(INVISIBLE);
                        break;
                    case 1:
                        this.mText.setVisibility(VISIBLE);
                }
            }

            return super.onTouchEvent(event);
        }
    }

}

