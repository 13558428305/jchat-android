package io.jchat.android.activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.callback.GetAvatarBitmapCallback;
import cn.jpush.im.android.api.callback.GetUserInfoCallback;
import cn.jpush.im.android.api.model.Conversation;
import cn.jpush.im.android.api.model.GroupInfo;
import cn.jpush.im.android.api.model.UserInfo;
import cn.jpush.im.android.eventbus.EventBus;
import io.jchat.android.R;
import io.jchat.android.application.JPushDemoApplication;
import io.jchat.android.controller.FriendInfoController;
import io.jchat.android.entity.Event;
import io.jchat.android.tools.BitmapLoader;
import io.jchat.android.tools.DialogCreator;
import io.jchat.android.tools.HandleResponseCode;
import io.jchat.android.view.FriendInfoView;

public class FriendInfoActivity extends BaseActivity {

    private FriendInfoView mFriendInfoView;
    private FriendInfoController mFriendInfoController;
    private String mTargetID;
    private long mGroupID;
    private UserInfo mUserInfo;
    private String mNickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_info);
        mFriendInfoView = (FriendInfoView) findViewById(R.id.friend_info_view);
        mTargetID = getIntent().getStringExtra(JPushDemoApplication.TARGET_ID);
        mGroupID = getIntent().getLongExtra(JPushDemoApplication.GROUP_ID, 0);
        Conversation conv;
        conv = JMessageClient.getSingleConversation(mTargetID);
        if (conv == null) {
            conv = JMessageClient.getGroupConversation(mGroupID);
            GroupInfo groupInfo = (GroupInfo) conv.getTargetInfo();
            mUserInfo = groupInfo.getGroupMemberInfo(mTargetID);
        } else {
            mUserInfo = (UserInfo) conv.getTargetInfo();
        }
        mFriendInfoView.initModule();
        //先从Conversation里获得UserInfo展示出来
        mFriendInfoView.initInfo(mUserInfo);
        mFriendInfoController = new FriendInfoController(mFriendInfoView, this);
        mFriendInfoView.setListeners(mFriendInfoController);
        //更新一次UserInfo
        final Dialog dialog = DialogCreator.createLoadingDialog(FriendInfoActivity.this,
                FriendInfoActivity.this.getString(R.string.loading));
        dialog.show();
        JMessageClient.getUserInfo(mTargetID, new GetUserInfoCallback() {
            @Override
            public void gotResult(int status, String desc, final UserInfo userInfo) {
                dialog.dismiss();
                if (status == 0) {
                    mNickname = userInfo.getNickname();
                    mFriendInfoView.initInfo(userInfo);
                } else {
                    HandleResponseCode.onHandle(FriendInfoActivity.this, status, false);
                }
            }
        });

    }

    /**
     * 如果是群聊，使用startActivity启动聊天界面，如果是单聊，setResult然后
     * finish掉此界面
     */
    public void startChatActivity() {
        if (mGroupID != 0) {
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(JPushDemoApplication.TARGET_ID, mTargetID);
            intent.setClass(this, ChatActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent();
            intent.putExtra("returnChatActivity", true);
            intent.putExtra(JPushDemoApplication.NICKNAME, mNickname);
            setResult(JPushDemoApplication.RESULT_CODE_FRIEND_INFO, intent);
        }
        Conversation conv = JMessageClient.getSingleConversation(mTargetID);
        //如果会话为空，使用EventBus通知会话列表添加新会话
        if (conv == null) {
            conv = Conversation.createSingleConversation(mTargetID);
            EventBus.getDefault().post(new Event.StringEvent(mTargetID));
        }
        finish();
    }

    public String getNickname() {
        return mNickname;
    }


    //点击头像预览大图，若此时UserInfo还是空，则再取一次
    public void startBrowserAvatar() {
        final Dialog dialog = DialogCreator.createLoadingDialog(this, this.getString(R.string.loading));
        dialog.show();
        mUserInfo.getBigAvatarBitmap(new GetAvatarBitmapCallback() {
            @Override
            public void gotResult(int status, String desc, Bitmap bitmap) {
                if (status == 0) {
                    String path = BitmapLoader.saveBitmapToLocal(bitmap);
                    Intent intent = new Intent();
                    intent.putExtra("browserAvatar", true);
                    intent.putExtra("avatarPath", path);
                    intent.setClass(FriendInfoActivity.this, BrowserViewPagerActivity.class);
                    startActivity(intent);
                }else {
                    HandleResponseCode.onHandle(FriendInfoActivity.this, status, false);
                }
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(JPushDemoApplication.NICKNAME, mNickname);
        setResult(JPushDemoApplication.RESULT_CODE_FRIEND_INFO, intent);
        finish();
        super.onBackPressed();
    }
}
