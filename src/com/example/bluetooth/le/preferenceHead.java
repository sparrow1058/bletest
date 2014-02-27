package com.example.bluetooth.le;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.Preference;
import android.view.View;
import android.widget.Button;

public class preferenceHead extends Preference{
	private OnClickListener onBackButtonClickListener;
	Button btBack ;
	public preferenceHead(Context context) {
		super(context);
		// TODO 自动生成的构造函数存根
		setLayoutResource(R.layout.preference_head);
	}
   @Override
    protected void onBindView(View view) {
        super.onBindView(view);
      //  btBack = (Button) view.findViewById(R.id.back1);

    }
  public void setOnBackButtonClickListener(OnClickListener onClickListener) {
       this.onBackButtonClickListener = onClickListener;

    }
}
