package com.hitices.autopatrol.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.activity.ShowFullImageActivity;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;
import java.util.List;

public class MediaLocalFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    public static final int INDEX = 1;
    private GridView gridView;
    private static String tag;
    private String[] urls;
    private ImageView showImageView;
    private List<String> imageurls;
    private List<String> videourls;
    //menu
    protected static final String STATE_PAUSE_ON_SCROLL = "STATE_PAUSE_ON_SCROLL";
    protected static final String STATE_PAUSE_ON_FLING = "STATE_PAUSE_ON_FLING";
    protected boolean pauseOnScroll = false;
    protected boolean pauseOnFling = true;

//    private Toolbar toolbar;
    public MediaLocalFragment() {
    }

    public static MediaLocalFragment newInstance() {
        MediaLocalFragment fragment = new MediaLocalFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //base
        urls=getUrls();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the waypoint_preview_waypoint_detail for this fragment
        View view=inflater.inflate(R.layout.fragment_media_list, container, false);
        gridView=view.findViewById(R.id.imageGrid);
        gridView.setAdapter(new ImageAdapter(getActivity()));
        gridView.setOnItemClickListener(onItemClickListener);
        showImageView=view.findViewById(R.id.imageView_local);
        //toolbar
//        toolbar=view.findViewById(R.id.local_toolbar);
//        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
//        getActivity().invalidateOptionsMenu();
//        setHasOptionsMenu(true);
        return view;
    }
    AdapterView.OnItemClickListener  onItemClickListener=new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            /////show image
            showFullImage(i);
        }
    };
    // TODO: Rename method, update argument and hook method into UI event
    @Override
    public void onResume(){
        super.onResume();

        applyScrollListener();
    }
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private  class ImageAdapter extends BaseAdapter{
        //private String[] IMAGE_URLS= getUrls();
        private String[] IMAGE_URLS=urls;
        private LayoutInflater inflater;
        private DisplayImageOptions options;
        ImageAdapter(Context context) {
            inflater=LayoutInflater.from(context);
            options =new DisplayImageOptions.Builder()
                    .showImageOnLoading(R.drawable.ic_stub)
                    .showImageForEmptyUri(R.drawable.ic_empty)
                    .showImageOnFail(R.drawable.ic_error)
                    .cacheOnDisk(true)
                    .cacheInMemory(true)
                    .considerExifParams(true)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .build();
        }
        @Override
        public int getCount(){
            return IMAGE_URLS.length;
        }
        @Override
        public Object getItem(int pos){
            return null;
        }
        @Override
        public long getItemId(int pos){
            return pos;
        }
        @Override
        public View getView(int pos,View convertView,ViewGroup parent){
            final ViewHolder holder;
            View view=convertView;
            if(view == null){
                view=inflater.inflate(R.layout.item_grid_view,parent,false);
                holder=new ViewHolder();
                assert view != null;
                holder.imageView=view.findViewById(R.id.image);
                holder.progressBar=view.findViewById(R.id.progress);
                view.setTag(holder);
            }else {
                holder=(ViewHolder)view.getTag();
            }
            ImageLoaderConfiguration configuration=ImageLoaderConfiguration.createDefault(getActivity());
            ImageLoader.getInstance().init(configuration);
            ImageLoader.getInstance()
                    .displayImage(IMAGE_URLS[pos], holder.imageView, options, new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String imageUri, View view) {
                            holder.progressBar.setProgress(0);
                            holder.progressBar.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                            holder.progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            holder.progressBar.setVisibility(View.GONE);
                        }
                    }, new ImageLoadingProgressListener() {
                        @Override
                        public void onProgressUpdate(String imageUri, View view, int current, int total) {
                            holder.progressBar.setProgress(Math.round(100.0f * current / total));
                        }
                    });
            return view;
        }

    }
    static class ViewHolder{
        ImageView imageView;
        ProgressBar progressBar;
    }
    //base menu o
    public void onCreateOptionsMenu(Menu menu,MenuInflater inflater){
        menu.clear();
        inflater.inflate(R.menu.main_menu,menu);
        Log.e("menu","on local create");
    }
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Log.e("menu","on local prepare");
        MenuItem pauseOnScrollItem = menu.findItem(R.id.item_pause_on_scroll);
        pauseOnScrollItem.setVisible(true);
        pauseOnScrollItem.setChecked(pauseOnScroll);

        MenuItem pauseOnFlingItem = menu.findItem(R.id.item_pause_on_fling);
        pauseOnFlingItem.setVisible(true);
        pauseOnFlingItem.setChecked(pauseOnFling);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_pause_on_scroll:
                pauseOnScroll = !pauseOnScroll;
                item.setChecked(pauseOnScroll);
                applyScrollListener();
                return true;
            case R.id.item_pause_on_fling:
                pauseOnFling = !pauseOnFling;
                item.setChecked(pauseOnFling);
                applyScrollListener();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void showFullImage(int position) {
//        showImageView.setVisibility(View.VISIBLE);
//        ImageLoader.getInstance()
//                .displayImage(urls[position], showImageView);

        Intent intent=new Intent(getActivity(),ShowFullImageActivity.class);
        intent.putExtra("url",urls[position]);
        if(imageurls.contains(urls[position]))
            intent.putExtra("type",0);
        else if(videourls.contains(urls[position]))
            intent.putExtra("type",1);
        else
            intent.putExtra("type",2);
        startActivity(intent);
    }

    private void applyScrollListener() {
        gridView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), pauseOnScroll, pauseOnFling));
    }
    private void getLocalImageUrls(){
        imageurls=new ArrayList<>();
        Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projImage = { MediaStore.Images.Media._ID
                , MediaStore.Images.Media.DATA
                ,MediaStore.Images.Media.SIZE
                ,MediaStore.Images.Media.DISPLAY_NAME};
        Cursor mCursor = getActivity().getContentResolver().query(
                mImageUri,
                projImage,
                MediaStore.Images.Media.MIME_TYPE + "=? or "
                        + MediaStore.Images.Media.MIME_TYPE + "=?",
                new String[]{"image/jpeg", "image/png"},
                MediaStore.Images.Media.DATE_MODIFIED+" desc");
        if(mCursor != null){
            while(mCursor.moveToNext()){
                String path = "file://"+ mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.DATA));
//                String s=new File(path).getParentFile().getAbsolutePath();
                Log.e("image",path);
                imageurls.add(path);
            }
            mCursor.close();
        }
//        Toast.makeText(getActivity(),String.valueOf(urls.size()),Toast.LENGTH_LONG).show();
    }
    private  void getLocalVideoUrls(){
        videourls=new ArrayList<>();
        Uri mImageUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projImage = { MediaStore.Video.Thumbnails._ID
                , MediaStore.Video.Thumbnails.DATA
                ,MediaStore.Video.Media.DURATION
                ,MediaStore.Video.Media.DISPLAY_NAME
                ,MediaStore.Video.Media.SIZE
                ,MediaStore.Video.Media.DATE_MODIFIED};
        Cursor mCursor = getActivity().getContentResolver().query(
                mImageUri,
                projImage,
                MediaStore.Video.Media.MIME_TYPE + "=?",
                new String[]{"video/mp4"},
                MediaStore.Video.Media.DATE_MODIFIED+" desc");
        if(mCursor != null){
            while(mCursor.moveToNext()){
                String path = "file://"+ mCursor.getString(mCursor.getColumnIndex(MediaStore.Video.Media.DATA));
                Log.e("video",path);
                videourls.add(path);
            }
            mCursor.close();
        }
//        Toast.makeText(getActivity(),String.valueOf(urls.size()),Toast.LENGTH_LONG).show();
    }
    private String[] getUrls(){
        List<String> urls=new ArrayList<>();
        getLocalImageUrls();
        getLocalVideoUrls();
        urls.addAll(imageurls);
        urls.addAll(videourls);
        return urls.toArray(new String[0]);
    }
}
