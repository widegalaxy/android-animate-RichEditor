package com.stone.richeditor;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

/**
 * 这是一个富文本编辑器，给外部提供insertImage接口，添加的图片跟当前光标所在位置有关
 * 
 * @author xmuSistone
 * 
 */
@SuppressLint({ "NewApi", "InflateParams" })
public class RichTextEditor extends ScrollView {
	private static final int EDIT_PADDING = 10; // edittext常规padding是10dp
	private static final int EDIT_FIRST_PADDING_TOP = 10; // 第一个EditText的paddingTop值

	private int viewTagIndex = 1; // 新生的view都会打一个tag，对每个view来说，这个tag是唯一的。
	private LinearLayout allLayout; // 这个是所有子view的容器，scrollView内部的唯一一个ViewGroup
	private LayoutInflater inflater;
	private OnKeyListener keyListener; // 所有EditText的软键盘监听器
	private OnClickListener btnListener; // 图片右上角红叉按钮监听器
	private OnFocusChangeListener focusListener; // 所有EditText的焦点监听listener
	private EditText lastFocusEdit; // 最近被聚焦的EditText
	private LayoutTransition mTransitioner; // 只在图片View添加或remove时，触发transition动画
	private int editNormalPadding = 0;

	public RichTextEditor(Context context) {
		this(context, null);
	}

	public RichTextEditor(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public RichTextEditor(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		inflater = LayoutInflater.from(context);

		// 1. 初始化allLayout
		allLayout = new LinearLayout(context);
		allLayout.setOrientation(LinearLayout.VERTICAL);
		allLayout.setBackgroundColor(Color.WHITE);
		setupLayoutTransitions();
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		addView(allLayout, layoutParams);

		// 2. 初始化键盘退格监听
		// 主要用来处理点击回删按钮时，view的一些列合并操作
		keyListener = new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN
						&& event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
					EditText edit = (EditText) v;
					onBackspacePress(edit);
				}
				return false;
			}
		};

		// 3. 图片叉掉处理
		btnListener = new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				RelativeLayout parentView = (RelativeLayout) v.getParent();
				onImageCloseClick(parentView);
			}
		};

		focusListener = new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					lastFocusEdit = (EditText) v;
				}
			}
		};

		LinearLayout.LayoutParams firstEditParam = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		editNormalPadding = dip2px(EDIT_PADDING);
		EditText firstEdit = createEditText("input here",
				dip2px(EDIT_FIRST_PADDING_TOP));
		allLayout.addView(firstEdit, firstEditParam);
		lastFocusEdit = firstEdit;
	}

	/**
	 * 处理软键盘backSpace回退事件
	 * 
	 * @param editTxt
	 *            光标所在的文本输入框
	 */
	private void onBackspacePress(EditText editTxt) {
		int startSelection = editTxt.getSelectionStart();
		// 只有在光标已经顶到文本输入框的最前方，在判定是否删除之前的图片，或两个View合并
		if (startSelection == 0) {
			int editIndex = allLayout.indexOfChild(editTxt);
			View preView = allLayout.getChildAt(editIndex - 1); // 如果editIndex-1<0,
																// 则返回的是null
			if (null != preView) {
				if (preView instanceof RelativeLayout) {
					// 光标EditText的上一个view对应的是图片
					onImageCloseClick(preView);
				} else if (preView instanceof EditText) {
					// 光标EditText的上一个view对应的还是文本框EditText
					String str1 = editTxt.getText().toString();
					EditText preEdit = (EditText) preView;
					String str2 = preEdit.getText().toString();

					// 合并文本view时，不需要transition动画
					allLayout.setLayoutTransition(null);
					allLayout.removeView(editTxt);
					allLayout.setLayoutTransition(mTransitioner); // 恢复transition动画

					// 文本合并
					preEdit.setText(str2 + str1);
					preEdit.requestFocus();
					preEdit.setSelection(str2.length(), str2.length());
					lastFocusEdit = preEdit;
				}
			}
		}
	}

	/**
	 * 处理图片叉掉的点击事件
	 * 
	 * @param view
	 *            整个image对应的relativeLayout view
	 * @type 删除类型 0代表backspace删除 1代表按红叉按钮删除
	 */
	private void onImageCloseClick(View view) {
		allLayout.removeView(view);
	}

	/**
	 * 生成文本输入框
	 */
	private EditText createEditText(String hint, int paddingTop) {
		EditText editText = (EditText) inflater.inflate(R.layout.edit_item1,
				null);
		editText.setOnKeyListener(keyListener);
		editText.setTag(viewTagIndex++);
		editText.setPadding(editNormalPadding, paddingTop, editNormalPadding, 0);
		editText.setHint(hint);
		editText.setOnFocusChangeListener(focusListener);
		return editText;
	}

	/**
	 * 生成图片View
	 */
	private RelativeLayout createImageLayout() {
		RelativeLayout layout = (RelativeLayout) inflater.inflate(
				R.layout.edit_imageview, null);
		layout.setTag(viewTagIndex++);
		View closeView = layout.findViewById(R.id.image_close);
		closeView.setTag(layout.getTag());
		closeView.setOnClickListener(btnListener);
		return layout;
	}

	/**
	 * 根据绝对路径添加view
	 * 
	 * @param imagePath
	 */
	public void insertImage(String imagePath) {
		Bitmap bmp = getScaledBitmap(imagePath, getWidth());
		insertImage(bmp);
	}

	/**
	 * 插入一张图片
	 */
	public void insertImage(Bitmap bitmap) {
		String lastEditStr = lastFocusEdit.getText().toString();
		int cursorIndex = lastFocusEdit.getSelectionStart();
		String editStr1 = lastEditStr.substring(0, cursorIndex).trim();
		int lastEditIndex = allLayout.indexOfChild(lastFocusEdit);

		if (lastEditStr.length() == 0 || editStr1.length() == 0) {
			// 如果EditText为空，或者光标已经顶在了editText的最前面，则直接插入图片，并且EditText下移即可
			addImageViewAtIndex(lastEditIndex, bitmap);
		} else {
			// 如果EditText非空且光标不在最顶端，则需要添加新的imageView和EditText
			lastFocusEdit.setText(editStr1);
			String editStr2 = lastEditStr.substring(cursorIndex).trim();
			if (allLayout.getChildCount() - 1 == lastEditIndex
					|| editStr2.length() > 0) {
				addEditTextAtIndex(lastEditIndex + 1, editStr2);
			}

			addImageViewAtIndex(lastEditIndex + 1, bitmap);

			lastFocusEdit.requestFocus();
			lastFocusEdit.setSelection(editStr1.length(), editStr1.length());
		}
		hideKeyBoard();
	}

	/**
	 * 隐藏小键盘
	 */
	public void hideKeyBoard() {
		InputMethodManager imm = (InputMethodManager) getContext()
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(lastFocusEdit.getWindowToken(), 0);
	}

	/**
	 * 在特定位置插入EditText
	 * 
	 * @param index
	 *            位置
	 * @param editStr
	 *            EditText显示的文字
	 */
	private void addEditTextAtIndex(final int index, String editStr) {
		EditText editText2 = createEditText("", getResources()
				.getDimensionPixelSize(R.dimen.edit_padding_top));
		editText2.setText(editStr);

		// 请注意此处，EditText添加、或删除不触动Transition动画
		allLayout.setLayoutTransition(null);
		allLayout.addView(editText2, index);
		allLayout.setLayoutTransition(mTransitioner); // remove之后恢复transition动画
	}

	/**
	 * 添加
	 * 
	 * @param index
	 * @param bmp
	 */
	private void addImageViewAtIndex(final int index, Bitmap bmp) {
		final RelativeLayout imageLayout = createImageLayout();
		ImageView imageView = (ImageView) imageLayout
				.findViewById(R.id.edit_imageView);
		imageView.setImageBitmap(bmp);

		// 调整imageView的高度
		int imageHeight = getWidth() * bmp.getHeight() / bmp.getWidth();
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, imageHeight);
		imageView.setLayoutParams(lp);

		// onActivityResult无法触发动画，此处post处理
		allLayout.postDelayed(new Runnable() {
			@Override
			public void run() {
				allLayout.addView(imageLayout, index);
			}
		}, 200);
	}

	/**
	 * 根据view的宽度，动态缩放bitmap尺寸
	 * 
	 * @param width
	 *            view的宽度
	 */
	private Bitmap getScaledBitmap(String filePath, int width) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, options);
		int sampleSize = options.outWidth > width ? options.outWidth / width
				+ 1 : 1;
		options.inJustDecodeBounds = false;
		options.inSampleSize = sampleSize;
		return BitmapFactory.decodeFile(filePath, options);
	}

	/**
	 * 初始化transition动画
	 */
	private void setupLayoutTransitions() {
		mTransitioner = new LayoutTransition();
		allLayout.setLayoutTransition(mTransitioner);
		mTransitioner.setDuration(300);
	}

	/**
	 * dp和pixel转换
	 * 
	 * @param dipValue
	 *            dp值
	 * @return 像素值
	 */
	public int dip2px(float dipValue) {
		float m = getContext().getResources().getDisplayMetrics().density;
		return (int) (dipValue * m + 0.5f);
	}
}
