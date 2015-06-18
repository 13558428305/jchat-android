package io.jchat.android.view;


import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import cn.jpush.im.android.api.JMessageClient;
import io.jchat.android.R;


public class LoginView extends LinearLayout {

    private TextView mTitle;
	private EditText mUserId;
	private EditText mPassword;
	private Button mLoginBtn;
	private Button mRegistBtnOnlogin;
    private Listener mListener;
	private CheckBox mTestEvnCB;
    private Context mContext;
	private static final boolean DEV_FLAG = false;

	public LoginView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.mContext = context;
	}
	public void initModule() {
        mTitle = (TextView) findViewById(R.id.title_bar_title);
		mUserId = (EditText) findViewById(R.id.username);
		mPassword = (EditText) findViewById(R.id.password);
		mLoginBtn = (Button) findViewById(R.id.login_btn);
		mRegistBtnOnlogin = (Button) findViewById(R.id.register_btn);
		mTestEvnCB = (CheckBox) findViewById(R.id.testEvn_cb);
        mTitle.setText(mContext.getString(R.string.app_name));
		initTestEvnCB();
	}

	private void initTestEvnCB(){
		if(!DEV_FLAG){
			mTestEvnCB.setVisibility(View.GONE);
		}
		Boolean isTestEvn = invokeIsTestEvn();
		mTestEvnCB.setChecked(isTestEvn);
	}

	private Boolean invokeIsTestEvn(){
		try {
			Method method = JMessageClient.class.getDeclaredMethod("isTestEnvironment");
			Object result = method.invoke(null);
			return (Boolean)result;
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public void setListeners(OnClickListener onClickListener) {
		mLoginBtn.setOnClickListener(onClickListener);
		mRegistBtnOnlogin.setOnClickListener(onClickListener);
	}

	public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener onCheckedChangeListener){
		mTestEvnCB.setOnCheckedChangeListener(onCheckedChangeListener);
	}

	public String getUserId(){
		return mUserId.getText().toString().trim();
	}
	
	public String getPassword(){
		return mPassword.getText().toString().trim();
	}
	
	public void userNameError(Context context) {
		Toast.makeText(context, context.getString(R.string.username_not_null_toast), Toast.LENGTH_SHORT).show();
	}
	
	public void passwordError(Context context) {
		Toast.makeText(context, context.getString(R.string.password_not_null_toast), Toast.LENGTH_SHORT).show();
	}

    public void setListener(Listener listener){
        this.mListener = listener;
    }

    public interface Listener {
        void onSoftKeyboardShown(int softKeyboardHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Rect rect = new Rect();
        Activity activity = (Activity)getContext();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        int statusBarHeight = rect.top;
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
//        int screenHeight = activity.getWindowManager().getDefaultDisplay().getHeight();
        int screenHeight = dm.heightPixels;
        int diff = (screenHeight - statusBarHeight) - height;
        if(mListener != null){
            mListener.onSoftKeyboardShown(diff);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
