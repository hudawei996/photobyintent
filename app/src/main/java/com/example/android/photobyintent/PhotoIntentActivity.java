package com.example.android.photobyintent;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;


public class PhotoIntentActivity extends Activity {

    private static final int ACTION_TAKE_PHOTO_B = 1;
    private static final int ACTION_TAKE_PHOTO_S = 2;
    private static final int ACTION_TAKE_VIDEO = 3;

    private static final String BITMAP_STORAGE_KEY = "viewbitmap";
    private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
    private ImageView mImageView;
    private Bitmap mImageBitmap;

    private static final String VIDEO_STORAGE_KEY = "viewvideo";
    private static final String VIDEOVIEW_VISIBILITY_STORAGE_KEY = "videoviewvisibility";
    private VideoView mVideoView;
    private Uri mVideoUri;

    private String mCurrentPhotoPath;

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    private AlbumStorageDirFactory mAlbumStorageDirFactory = null;


    /* Photo album for this application */
    private String getAlbumName() {
        return getString(R.string.album_name);
    }

    /**
     * 获得专辑的文件路径
     *
     * @return
     */
    private File getAlbumDir() {
        File storageDir = null;
        //判断存储位置：是否等于外部存储
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            //获得存储目录
            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());
            //判断这个文件路径如果不存在
            if (storageDir != null) {
                //新建文件路径
                if (!storageDir.mkdirs()) {
                    //如果文件不存在
                    if (!storageDir.exists()) {
                        //打印log,创建文件夹失败
                        Log.d("CameraSample", "failed to create directory");
                        //返回文件路径为空
                        return null;
                    }
                }
            }
            //如果不是外部存储
        } else {
            //提示外部存储没有被加载
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    /**
     * 创建图片文件
     *
     * @return
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name 创建图片文件名用拍照的时间戳来命名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        //获得文件夹名称
        File albumF = getAlbumDir();
        //创建临时文件（文件名，后缀，文件夹名称）
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }

    /**
     * 创建图片文件，返回图片文件
     *
     * @return
     * @throws IOException
     */
    private File setUpPhotoFile() throws IOException {
        //创建图片文件
        File f = createImageFile();
        //获得文件的决定路径
        mCurrentPhotoPath = f.getAbsolutePath();

        return f;
    }

    /**
     * 设置图片
     */
    private void setPic() {
        /*没有足够的内存打来大于两张的相机照片，因此事先测量即将解码的目标图片*/

		/* There isn't enough memory to open up more than a couple camera photos */
        /* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

		/* Get the size of the image */
        //获得图片工厂的设置参数
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;//只是解码成边界
        //按要求解码当前路径的图片
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        //输出的宽，高
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        //计算出哪里需要减少
        /* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            //取小值
            scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        }

		/* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;//图片放大还是缩小
        bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
        //解码成位图
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

		/* Associate the Bitmap to the ImageView */
        mImageView.setImageBitmap(bitmap);
        mVideoUri = null;

        //设置图片控件可见
        mImageView.setVisibility(View.VISIBLE);
        //视频控件不可见
        mVideoView.setVisibility(View.INVISIBLE);
    }

    /**
     * 画廊中添加图片
     */
    private void galleryAddPic() {
        //画廊中的图片
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        //在当前路径新建文件
        File f = new File(mCurrentPhotoPath);
        //格式化文件路径
        Uri contentUri = Uri.fromFile(f);
        //吧文件路径带过去
        mediaScanIntent.setData(contentUri);
        //用广播发送意图
        this.sendBroadcast(mediaScanIntent);
    }

    /**
     * 调度拍照意图
     *
     * @param actionCode
     */
    private void dispatchTakePictureIntent(int actionCode) {
        //意图：媒体存储。动作-图片捕捉
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //动作编码
        switch (actionCode) {

            case ACTION_TAKE_PHOTO_B:
                File f = null;

                try {
                    //设置文件
                    f = setUpPhotoFile();
                    //获得文件的绝对路径
                    mCurrentPhotoPath = f.getAbsolutePath();
                    //拍照意图（意图名称，文件路径）
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                } catch (IOException e) {
                    e.printStackTrace();

                    //如果发生错误，文件置为空
                    f = null;
                    mCurrentPhotoPath = null;
                }
                break;

            default:
                break;
        } // switch

        //开始拍照意图
        startActivityForResult(takePictureIntent, actionCode);
    }

    /**
     * 区分录像意图
     */
    private void dispatchTakeVideoIntent() {
        //意图为视频捕捉
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        //开始视频捕捉意图，返回码ACTION_TAKE_VIDEO=3；
        startActivityForResult(takeVideoIntent, ACTION_TAKE_VIDEO);
    }

    /**
     * 处理小相机拍照
     *
     * @param intent
     */
    private void handleSmallCameraPhoto(Intent intent) {
        //获得意图所带的额外数据
        Bundle extras = intent.getExtras();
        //获得额外数据，并且解析成位图
        mImageBitmap = (Bitmap) extras.get("data");
        //将位图设置给图片显示控件
        mImageView.setImageBitmap(mImageBitmap);

        //将视频地址设置为空
        mVideoUri = null;

        //图片控件显示
        mImageView.setVisibility(View.VISIBLE);
        //视频控件不可见
        mVideoView.setVisibility(View.INVISIBLE);
    }

    /**
     * 处理大相机照片
     */
    private void handleBigCameraPhoto() {
        //如果当前照片路径不为空
        if (mCurrentPhotoPath != null) {
            //设置图片
            setPic();
            //画廊添加图片
            galleryAddPic();
            mCurrentPhotoPath = null;
        }

    }

    /**
     * 处理相机的视频
     *
     * @param intent
     */
    private void handleCameraVideo(Intent intent) {
        //获得返回意图的数据
        mVideoUri = intent.getData();
        //设置视屏的uri
        mVideoView.setVideoURI(mVideoUri);

        //位图设置为空
        mImageBitmap = null;
        //显示视频控件
        mVideoView.setVisibility(View.VISIBLE);
        //隐藏图片控件
        mImageView.setVisibility(View.INVISIBLE);
    }

    Button.OnClickListener mTakePicOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //处理拍照的意图big：1
                    dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
                }
            };

    Button.OnClickListener mTakePicSOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //处理拍照意图small：2
                    dispatchTakePictureIntent(ACTION_TAKE_PHOTO_S);
                }
            };

    Button.OnClickListener mTakeVidOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //处理拍摄视频的意图
                    dispatchTakeVideoIntent();
                }
            };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mImageView = (ImageView) findViewById(R.id.imageView1);
        mVideoView = (VideoView) findViewById(R.id.videoView1);

        mImageBitmap = null;
        mVideoUri = null;

        Button picBtn = (Button) findViewById(R.id.btnIntend);
        setBtnListenerOrDisable(
                picBtn,
                mTakePicOnClickListener,
                MediaStore.ACTION_IMAGE_CAPTURE
        );

        Button picSBtn = (Button) findViewById(R.id.btnIntendS);
        setBtnListenerOrDisable(
                picSBtn,
                mTakePicSOnClickListener,
                MediaStore.ACTION_IMAGE_CAPTURE
        );

        Button vidBtn = (Button) findViewById(R.id.btnIntendV);
        setBtnListenerOrDisable(
                vidBtn,
                mTakeVidOnClickListener,
                MediaStore.ACTION_VIDEO_CAPTURE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
        } else {
            mAlbumStorageDirFactory = new BaseAlbumDirFactory();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTION_TAKE_PHOTO_B: {
                if (resultCode == RESULT_OK) {
                    handleBigCameraPhoto();
                }
                break;
            } // ACTION_TAKE_PHOTO_B

            case ACTION_TAKE_PHOTO_S: {
                if (resultCode == RESULT_OK) {
                    handleSmallCameraPhoto(data);
                }
                break;
            } // ACTION_TAKE_PHOTO_S

            case ACTION_TAKE_VIDEO: {
                if (resultCode == RESULT_OK) {
                    handleCameraVideo(data);
                }
                break;
            } // ACTION_TAKE_VIDEO
        } // switch
    }

    // Some lifecycle callbacks so that the image can survive orientation change
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
        outState.putParcelable(VIDEO_STORAGE_KEY, mVideoUri);
        outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null));
        outState.putBoolean(VIDEOVIEW_VISIBILITY_STORAGE_KEY, (mVideoUri != null));
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
        mVideoUri = savedInstanceState.getParcelable(VIDEO_STORAGE_KEY);
        mImageView.setImageBitmap(mImageBitmap);
        mImageView.setVisibility(
                savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ?
                        ImageView.VISIBLE : ImageView.INVISIBLE
        );
        mVideoView.setVideoURI(mVideoUri);
        mVideoView.setVisibility(
                savedInstanceState.getBoolean(VIDEOVIEW_VISIBILITY_STORAGE_KEY) ?
                        ImageView.VISIBLE : ImageView.INVISIBLE
        );
    }

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
     *
     * @param context The application's environment.
     * @param action  The Intent action to check for availability.
     * @return True if an Intent with the specified action can be sent and
     * responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action) {
        //包管理器
        final PackageManager packageManager = context.getPackageManager();
        //意图
        final Intent intent = new Intent(action);
        //在意图管理器中，找到对应的意图，如果不为空，就是有这个意图
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    /**
     * 设置按键可以点击与否
     *
     * @param btn
     * @param onClickListener
     * @param intentName
     */
    private void setBtnListenerOrDisable(Button btn, Button.OnClickListener onClickListener, String intentName) {
        if (isIntentAvailable(this, intentName)) {
            btn.setOnClickListener(onClickListener);
        } else {
            btn.setText(getText(R.string.cannot).toString() + " " + btn.getText());
            btn.setClickable(false);
        }
    }

}