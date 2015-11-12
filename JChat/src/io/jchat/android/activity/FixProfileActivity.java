package io.jchat.android.activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.model.UserInfo;
import cn.jpush.im.api.BasicCallback;
import io.jchat.android.R;
import io.jchat.android.application.JPushDemoApplication;
import io.jchat.android.tools.BitmapLoader;
import io.jchat.android.tools.DialogCreator;
import io.jchat.android.tools.HandleResponseCode;
import io.jchat.android.tools.SharePreferenceManager;
import io.jchat.android.view.CircleImageView;

/**
 * Created by Ken on 2015/1/26.
 */
public class FixProfileActivity extends BaseActivity {

    private static final String TAG = "FixProfileActivity";

    private Button mFinishBtn;
    private EditText mNickNameEt;
    private ImageView mAvatarIv;
    private String mPath;
    private ProgressDialog mDialog;
    private Dialog mSetAvatarDialog;
    private Context mContext;
    // 裁剪后图片的宽(X)和高(Y), 720 X 720的正方形。
    private static int OUTPUT_X = 720;
    private static int OUTPUT_Y = 720;
    private Uri mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (null != savedInstanceState) {
            String nickName = savedInstanceState.getString("savedNickName");
            mNickNameEt.setText(nickName);
        }
        setContentView(R.layout.activity_fix_profile);
        mContext = this;
        mNickNameEt = (EditText) findViewById(R.id.nick_name_et);
        mAvatarIv = (CircleImageView) findViewById(R.id.avatar_iv);
        mFinishBtn = (Button) findViewById(R.id.finish_btn);
        mAvatarIv.setOnClickListener(listener);
        mFinishBtn.setOnClickListener(listener);
        JMessageClient.getUserInfo(JMessageClient.getMyInfo().getUserName(), null);
        SharePreferenceManager.setCachedFixProfileFlag(true);
        mNickNameEt.requestFocus();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstancedState) {
        savedInstancedState.putString("savedNickName", mNickNameEt.getText().toString());
        super.onSaveInstanceState(savedInstancedState);
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.avatar_iv:
                    showSetAvatarDialog();
                    break;
                case R.id.finish_btn:
                    String nickName = mNickNameEt.getText().toString().trim();
                    if (nickName != null && !nickName.equals("")) {
                        final ProgressDialog dialog = new ProgressDialog(mContext);
                        dialog.setMessage(mContext.getString(R.string.saving_hint));
                        dialog.show();
                        dialog.getWindow().setLayout((int) (0.8 * mWidth), WindowManager.LayoutParams.WRAP_CONTENT);
                        UserInfo myUserInfo = JMessageClient.getMyInfo();
                        myUserInfo.setNickname(nickName);
                        JMessageClient.updateMyInfo(UserInfo.Field.nickname, myUserInfo, new BasicCallback() {
                            @Override
                            public void gotResult(final int status, String desc) {
                                //更新跳转标志
                                SharePreferenceManager.setCachedFixProfileFlag(false);
                                if (dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                                if (status != 0) {
                                    Toast.makeText(mContext, mContext.getString(R.string.nickname_save_failed),
                                            Toast.LENGTH_SHORT).show();
                                }
                                startMainActivity();
                            }
                        });
                    } else {
                        Toast.makeText(FixProfileActivity.this, FixProfileActivity.this
                                .getString(R.string.nickname_not_null_toast), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    break;
            }
        }
    };

    public void showSetAvatarDialog() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.take_photo_btn:
                        mSetAvatarDialog.cancel();
                        takePhoto();
                        break;
                    case R.id.pick_picture_btn:
                        mSetAvatarDialog.cancel();
                        selectImageFromLocal();
                        break;
                }
            }
        };
        mSetAvatarDialog = DialogCreator.createSetAvatarDialog(this, listener);
        mSetAvatarDialog.show();
        mSetAvatarDialog.getWindow().setLayout((int) (0.8 * mWidth), WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void startMainActivity() {
        Intent intent = new Intent();
        intent.setClass(FixProfileActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void takePhoto() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File destDir = new File(JPushDemoApplication.PICTURE_DIR);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            File file = new File(JPushDemoApplication.PICTURE_DIR,
                    JMessageClient.getMyInfo().getUserName() + ".png");
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            mPath = file.getAbsolutePath();
            startActivityForResult(intent, JPushDemoApplication.REQUEST_CODE_TAKE_PHOTO);
        } else {
            Toast.makeText(this, this.getString(R.string.sdcard_not_exist_toast), Toast.LENGTH_SHORT).show();
        }
    }

    public void selectImageFromLocal() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
        } else {
            intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        startActivityForResult(intent, JPushDemoApplication.REQUEST_CODE_SELECT_PICTURE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }
        if (requestCode == JPushDemoApplication.REQUEST_CODE_TAKE_PHOTO) {
            if (mPath != null) {
                mUri = Uri.fromFile(new File(mPath));
                cropRawPhoto(mUri);
            }
        } else if (requestCode == JPushDemoApplication.REQUEST_CODE_SELECT_PICTURE) {
            if (data != null) {
                Uri selectedImg = data.getData();
                if (selectedImg != null) {
                    String[] filePathColumn = { MediaStore.Images.Media.DATA };
                    Cursor cursor = this.getContentResolver()
                            .query(selectedImg, filePathColumn, null, null, null);
                    try {
                        if (null == cursor) {
                            String path = selectedImg.getPath();
                            File file = new File(path);
                            if (file.isFile()) {
                                copyAndCrop(file);
                                return;
                            }else {
                                Toast.makeText(this, this.getString(R.string.picture_not_found),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }else if (!cursor.moveToFirst()) {
                            Toast.makeText(this, this.getString(R.string.picture_not_found),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String path = cursor.getString(columnIndex);
                        if (path != null) {
                            File file = new File(path);
                            if (!file.isFile()) {
                                Toast.makeText(this, this.getString(R.string.picture_not_found),
                                        Toast.LENGTH_SHORT).show();
                                cursor.close();
                            } else {
                                //如果是选择本地图片进行头像设置，复制到临时文件，并进行裁剪
                                copyAndCrop(file);
                                cursor.close();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        } else if (requestCode == JPushDemoApplication.REQUEST_CODE_CROP_PICTURE) {
            //裁剪后得到返回的bitmap
            Bitmap bitmap = decodeUriAsBitmap(mUri);
            File file = new File(mUri.getPath());
            if (file.isFile()) {
                if (file.delete()) {
                    Log.d(TAG, "delete temp file success!");
                }
            }
            UserInfo myInfo = JMessageClient.getMyInfo();
            String path = BitmapLoader.saveBitmapToLocal(bitmap, this, myInfo.getUserName());
            uploadUserAvatar(path);
        }
    }

    /**
     * 裁剪图片
     */
    public void cropRawPhoto(Uri uri) {

        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");

        // 设置裁剪
        intent.putExtra("crop", "true");

        // aspectX , aspectY :宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX , outputY : 裁剪图片宽高
        intent.putExtra("outputX", OUTPUT_X);
        intent.putExtra("outputY", OUTPUT_Y);
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        this.startActivityForResult(intent, JPushDemoApplication.REQUEST_CODE_CROP_PICTURE);
    }

    /**
     * 复制后裁剪文件
     *
     * @param file 要复制的文件
     */
    private void copyAndCrop(final File file) {
        boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            final Dialog dialog = DialogCreator.createLoadingDialog(this, this.getString(R.string.loading));
            dialog.show();
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        File destDir = new File(JPushDemoApplication.PICTURE_DIR);
                        if (!destDir.exists()) {
                            destDir.mkdirs();
                        }
                        final File tempFile = new File(JPushDemoApplication.PICTURE_DIR,
                                JMessageClient.getMyInfo().getUserName() + ".png");
                        FileOutputStream fos = new FileOutputStream(tempFile);
                        byte[] bt = new byte[1024];
                        int c;
                        while ((c = fis.read(bt)) > 0) {
                            fos.write(bt, 0, c);
                        }
                        //关闭输入、输出流
                        fis.close();
                        fos.close();

                        FixProfileActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.dismiss();
                                mUri = Uri.fromFile(tempFile);
                                cropRawPhoto(mUri);
                            }
                        });
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        FixProfileActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.dismiss();
                            }
                        });
                    }
                }
            });
            thread.start();
        }else {
            Toast.makeText(this, this.getString(R.string.sdcard_not_exist_toast), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap decodeUriAsBitmap(Uri uri) {
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    /**
     * 上传头像
     * @param path 要上传的文件路径
     */
    private void uploadUserAvatar(final String path) {
        mDialog = new ProgressDialog(this);
        mDialog.setCancelable(false);
        mDialog.setMessage(this.getString(R.string.updating_avatar_hint));
        mDialog.show();
        JMessageClient.updateUserAvatar(new File(path), new BasicCallback() {
            @Override
            public void gotResult(int status, final String desc) {
                if (mDialog.isShowing()) {
                    mDialog.dismiss();
                }
                if (status == 0) {
                    Log.i(TAG, "Update avatar succeed path " + path);
                    loadUserAvatar(path);
                    Toast.makeText(FixProfileActivity.this,
                            FixProfileActivity.this.getString(R.string.avatar_modify_succeed_toast),
                            Toast.LENGTH_SHORT).show();
                } else {
                    HandleResponseCode.onHandle(mContext, status, false);
                }
            }
        });
    }

    private void loadUserAvatar(String path) {
        final Bitmap bitmap = BitmapLoader.getBitmapFromFile(path, (int) (100 * mDensity),
                (int) (100 * mDensity));
        mAvatarIv.setImageBitmap(bitmap);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
