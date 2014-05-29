package jp.co.spookies.android.benchmark;

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;

public class Benchmark extends Activity {
	static {
		System.loadLibrary("diffusion");
	}
	private int tLimit = 1000;
	private int drawInterval = 10;
	private DiffusionView diffusionView = null;
	private Date startTime = null;
	private Handler handler = new Handler();
	private ProgressBar progressBar = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.main);
		FrameLayout layout = (FrameLayout) findViewById(R.id.plot_area);
		diffusionView = new DiffusionView(this);
		layout.addView(diffusionView);
		progressBar = (ProgressBar) findViewById(R.id.progressbar);
	}

	@Override
	public void onPause() {
		super.onPause();
		diffusionView.stop();
		finish();
	}

	public void onStartButtonClicked(View view) {
		EditText tLimitForm = (EditText) findViewById(R.id.t_limit);
		tLimit = Integer.parseInt(tLimitForm.getText().toString());

		// 計算途中のものは停止する
		diffusionView.stop();

		// プログレスバーの初期化
		progressBar.setMax(tLimit / drawInterval);
		progressBar.setProgress(0);

		// 開始時間の記録
		startTime = new Date();

		diffusionView.start();
	}

	private void onProgress() {
		progressBar.setProgress(progressBar.getProgress() + 1);
		if (progressBar.getProgress() == progressBar.getMax()) {
			// 終了処理
			final Date stopTime = new Date();
			handler.post(new Runnable() {
				@Override
				public void run() {
					long time = stopTime.getTime() - startTime.getTime();
					AlertDialog.Builder dialog = new AlertDialog.Builder(
							Benchmark.this);
					dialog.setTitle(R.string.finish);
					dialog.setMessage(Long.toString(time) + "ミリ秒");
					dialog.show();
				}
			});
		}
	}

	class DiffusionView extends SurfaceView {
		private int xSize;
		private int ySize;
		private float[] temperatures;
		private float[] tmp;
		private int[] colors;
		private float maxTemperature = 100f;
		private float minTemperature = 0f;
		private float Dfu = 1f;
		private float dt = 0.2f;
		private float dx = 1f;
		private float Dfudtdx2 = Dfu * dt / (dx * dx);
		private boolean stopFlag;
		private Thread thread;

		public DiffusionView(Context context) {
			super(context);
		}

		public void init() {
			xSize = getWidth();
			ySize = getHeight();
			temperatures = new float[xSize * ySize];
			tmp = new float[temperatures.length];
			colors = new int[temperatures.length];
			stopFlag = false;
			for (int i = 0; i < temperatures.length; i++) {
				temperatures[i] = maxTemperature;
			}
			int pad = xSize * (ySize - 1);
			for (int i = 0; i < xSize; i++) {
				temperatures[i] = minTemperature;
				temperatures[i + pad] = minTemperature;
			}
			pad = xSize - 1;
			for (int i = 0; i < temperatures.length; i += xSize) {
				temperatures[i] = minTemperature;
				temperatures[i + pad] = minTemperature;
			}
		}

		public void start() {
			init();
			RadioGroup mode = (RadioGroup) Benchmark.this
					.findViewById(R.id.mode);
			final boolean isJni = mode.getCheckedRadioButtonId() == R.id.mode_native ? true
					: false;
			thread = new Thread() {
				@Override
				public void run() {
					update();
					for (int t = 0; t < tLimit; t++) {
						if (isJni) {
							// C実装のメソッドで処理
							calculateJni();
						} else {
							// Java実装のメソッドで処理
							calculate();
						}
						if (stopFlag) {
							break;
						}
						if (t % drawInterval == drawInterval - 1) {
							// 画面の更新
							update();
							onProgress();
						}
					}
				}
			};
			thread.start();
		}

		public void stop() {
			stopFlag = true;
			if (thread != null) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			thread = null;
		}

		private void calculate() {
			for (int x = 1; x < xSize - 1; x++) {
				for (int y = 1; y < ySize - 1; y++) {
					int i = index(x, y);
					float t0 = temperatures[i];
					float t1 = temperatures[index(x - 1, y)];
					float t2 = temperatures[index(x + 1, y)];
					float t3 = temperatures[index(x, y - 1)];
					float t4 = temperatures[index(x, y + 1)];
					tmp[i] = t0 + (t1 + t2 + t3 + t4 - 4 * t0) * Dfudtdx2;
				}
			}
			for (int i = 0; i < temperatures.length; i++) {
				temperatures[i] = tmp[i];
			}
		}

		private native void calculateJni();

		private void update() {
			Canvas canvas = getHolder().lockCanvas();
			if (canvas == null) {
				return;
			}
			for (int i = 0; i < temperatures.length; i++) {
				colors[i] = temperatureToColor(temperatures[i]);
			}
			canvas.drawBitmap(colors, 0, xSize, 0, 0, xSize, ySize, false, null);
			getHolder().unlockCanvasAndPost(canvas);
		}

		private int index(int x, int y) {
			return x + xSize * y;
		}

		private int temperatureToColor(float temperature) {
			int degree;
			if (temperature <= minTemperature) {
				degree = 0;
			} else if (temperature >= maxTemperature) {
				degree = 255;
			} else {
				degree = (int) ((temperature - minTemperature)
						/ (maxTemperature - minTemperature) * 255f);
			}
			int d = degree % 64;
			switch (degree / 64) {
			case 0:
				return Color.rgb(0, 4 * d, 255);
			case 1:
				return Color.rgb(0, 255, 4 * (63 - d));
			case 2:
				return Color.rgb(4 * d, 255, 0);
			case 3:
			default:
				return Color.rgb(255, 4 * (63 - d), 0);
			}
		}
	}
}
