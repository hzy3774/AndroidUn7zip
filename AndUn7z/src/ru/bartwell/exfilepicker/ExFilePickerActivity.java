package ru.bartwell.exfilepicker;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.hu.andun7z.R;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ExFilePickerActivity extends Activity implements OnLongClickListener {

	final private String[] videoExtensions = { "avi", "mp4", "3gp", "mov" };
	final private String[] imagesExtensions = { "jpeg", "jpg", "png", "gif", "bmp", "wbmp" };

	final private boolean DEBUG = false;
	final private String TAG = "ExFilePicker";

	private boolean s_onlyOneItem = false;
	private List<String> s_filterExclude;
	private List<String> s_filterListed;
	private int s_choiceType;
	private int s_sortType;

	private LruCache<String, Bitmap> bitmapsCache;

	private AbsListView absListView;
	private View emptyView;
	private ArrayList<File> filesList = new ArrayList<File>();
	private ArrayList<String> selected = new ArrayList<String>();
	private File currentDirectory;
	private boolean isMultiChoice = false;
	private ImageButton change_view;
	private TextView header_title;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.efp__main_activity);

		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		final int cacheSize = maxMemory / 8;
		bitmapsCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return getBitmapSize(bitmap) / 1024;
			}
		};

		Intent intent = getIntent();
		s_onlyOneItem = intent.getBooleanExtra(ExFilePicker.SET_ONLY_ONE_ITEM, false);
		if (intent.hasExtra(ExFilePicker.SET_FILTER_EXCLUDE)) {
			s_filterExclude = Arrays.asList(intent.getStringArrayExtra(ExFilePicker.SET_FILTER_EXCLUDE));
		}
		if (intent.hasExtra(ExFilePicker.SET_FILTER_LISTED)) {
			s_filterListed = Arrays.asList(intent.getStringArrayExtra(ExFilePicker.SET_FILTER_LISTED));
		}
		s_choiceType = intent.getIntExtra(ExFilePicker.SET_CHOICE_TYPE, ExFilePicker.CHOICE_TYPE_ALL);
		
		s_sortType = intent.getIntExtra(ExFilePicker.SET_SORT_TYPE, ExFilePicker.SORT_NAME_ASC);

		emptyView = getLayoutInflater().inflate(R.layout.efp__empty, null);
		addContentView(emptyView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		setAbsListView();
		showSecondHeader(false);

		File path = null;
		if (intent.hasExtra(ExFilePicker.SET_START_DIRECTORY)) {
			String startPath = intent.getStringExtra(ExFilePicker.SET_START_DIRECTORY);
			if (startPath != null && startPath.length() > 0) {
				File tmp = new File(startPath);
				if (tmp.exists() && tmp.isDirectory()) path = tmp;
			}
		}
		if (path == null) {
			path = new File("/");
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) path = Environment.getExternalStorageDirectory();
		}
		readDirectory(path);

		header_title = (TextView) findViewById(R.id.title);
		updateTitle();

		ImageButton new_folder = (ImageButton) findViewById(R.id.menu_new_folder);
		if (!intent.getBooleanExtra(ExFilePicker.DISABLE_NEW_FOLDER_BUTTON, false)) {
			new_folder.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					AlertDialog.Builder alert = new AlertDialog.Builder(ExFilePickerActivity.this);
					alert.setTitle(R.string.efp__new_folder);
					View alertView = LayoutInflater.from(ExFilePickerActivity.this).inflate(R.layout.efp__new_folder, null);
					final TextView name = (TextView) alertView.findViewById(R.id.name);
					alert.setView(alertView);
					alert.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (name.length() > 0) {
								File file = new File(currentDirectory, name.getText().toString());
								if (file.exists()) Toast.makeText(ExFilePickerActivity.this, R.string.efp__folder_already_exists, Toast.LENGTH_SHORT).show();
								else {
									file.mkdir();
									if (file.isDirectory()) {
										readDirectory(currentDirectory);
										Toast.makeText(ExFilePickerActivity.this, R.string.efp__folder_created, Toast.LENGTH_SHORT).show();
									} else Toast.makeText(ExFilePickerActivity.this, R.string.efp__folder_not_created, Toast.LENGTH_SHORT).show();
								}
							}
						}
					});
					alert.setNegativeButton(android.R.string.cancel, null);
					alert.show();
				}
			});
			new_folder.setOnLongClickListener(this);
		} else new_folder.setVisibility(ImageButton.GONE);

		ImageButton sort1 = (ImageButton) findViewById(R.id.menu_sort1);
		ImageButton sort2 = (ImageButton) findViewById(R.id.menu_sort2);
		if (!intent.getBooleanExtra(ExFilePicker.DISABLE_SORT_BUTTON, false)) {
			OnClickListener listener = new OnClickListener() {
				@Override
				public void onClick(View v) {
					AlertDialog.Builder alert = new AlertDialog.Builder(ExFilePickerActivity.this);
					alert.setTitle(R.string.efp__sort);
					alert.setItems(R.array.efp__sorting_types, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								s_sortType = ExFilePicker.SORT_NAME_ASC;
								break;
							case 1:
								s_sortType = ExFilePicker.SORT_NAME_DESC;
								break;
							case 2:
								s_sortType = ExFilePicker.SORT_SIZE_ASC;
								break;
							case 3:
								s_sortType = ExFilePicker.SORT_SIZE_DESC;
								break;
							case 4:
								s_sortType = ExFilePicker.SORT_DATE_ASC;
								break;
							case 5:
								s_sortType = ExFilePicker.SORT_DATE_DESC;
								break;

							}
							sort();
						}
					});
					alert.show();
				}
			};
			sort1.setOnClickListener(listener);
			sort1.setOnLongClickListener(this);
			sort2.setOnClickListener(listener);
			sort2.setOnLongClickListener(this);
		} else {
			sort1.setVisibility(ImageButton.GONE);
			sort2.setVisibility(ImageButton.GONE);
		}

		change_view = (ImageButton) findViewById(R.id.menu_change_view);
		setMenuItemView();
		change_view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setAbsListView();
				setMenuItemView();
			}
		});
		change_view.setOnLongClickListener(this);

		ImageButton cancel = (ImageButton) findViewById(R.id.menu_cancel);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				disableMultiChoice();
				showSecondHeader(false);
			}
		});
		cancel.setOnLongClickListener(this);

		ImageButton ok1 = (ImageButton) findViewById(R.id.menu_ok1);
		View ok1_delimiter = findViewById(R.id.ok1_delimiter);
		if (s_onlyOneItem && s_choiceType == ExFilePicker.CHOICE_TYPE_DIRECTORIES) {
			ok1.setVisibility(ImageButton.VISIBLE);
			ok1_delimiter.setVisibility(ImageButton.VISIBLE);
			ok1.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ArrayList<String> list = new ArrayList<String>();
					String parent;
					File parentFile = currentDirectory.getParentFile();
					if (parentFile == null) {
						parent = "";
						list.add("/");
					} else {
						parent = parentFile.getAbsolutePath();
						if (!parent.endsWith("/")) parent += "/";
						list.add(currentDirectory.getName());
					}
					ExFilePickerParcelObject object = new ExFilePickerParcelObject(parent, list, 1);
					complete(object);
				}
			});
			ok1.setOnLongClickListener(this);
		} else {
			ok1.setVisibility(ImageButton.GONE);
			ok1_delimiter.setVisibility(ImageButton.GONE);
		}

		ImageButton select_all = (ImageButton) findViewById(R.id.menu_select_all);
		ImageButton deselect = (ImageButton) findViewById(R.id.menu_deselect);
		ImageButton invert = (ImageButton) findViewById(R.id.menu_invert);
		if (s_onlyOneItem) {
			select_all.setVisibility(ImageButton.GONE);
			deselect.setVisibility(ImageButton.GONE);
			invert.setVisibility(ImageButton.GONE);
		} else {
			select_all.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					selected.clear();
					for (int i = 0; i < filesList.size(); i++)
						selected.add(filesList.get(i).getName());
					((BaseAdapter) absListView.getAdapter()).notifyDataSetChanged();
				}
			});
			select_all.setOnLongClickListener(this);

			deselect.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					selected.clear();
					((BaseAdapter) absListView.getAdapter()).notifyDataSetChanged();
				}
			});
			deselect.setOnLongClickListener(this);

			invert.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ArrayList<String> tmp = new ArrayList<String>();
					for (int i = 0; i < filesList.size(); i++) {
						String filename = filesList.get(i).getName();
						if (!selected.contains(filename)) tmp.add(filename);
					}
					selected = tmp;
					((BaseAdapter) absListView.getAdapter()).notifyDataSetChanged();
				}
			});
			invert.setOnLongClickListener(this);
		}

		ImageButton ok2 = (ImageButton) findViewById(R.id.menu_ok2);
		ok2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (selected.size() > 0) {
					complete(null);
				} else {
					disableMultiChoice();
				}
			}
		});
		ok2.setOnLongClickListener(this);
	}

	@Override
	public boolean onLongClick(View v) {
		Toast toast = Toast.makeText(ExFilePickerActivity.this, v.getContentDescription(), Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.LEFT | Gravity.TOP, v.getLeft(), v.getBottom() + v.getBottom() / 2);
		toast.show();
		return true;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			if (event.getAction() == KeyEvent.ACTION_UP) {
				if (isMultiChoice) {
					disableMultiChoice();
				} else {
					File parentFile = currentDirectory.getParentFile();
					if (parentFile == null) {
						complete(null);
					} else {
						readDirectory(parentFile);
						updateTitle();
					}
				}
			} else if (event.getAction() == KeyEvent.ACTION_DOWN && (event.getFlags() & KeyEvent.FLAG_LONG_PRESS) == KeyEvent.FLAG_LONG_PRESS) {
				selected.clear();
				complete(null);
			}
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	private void disableMultiChoice() {
		showSecondHeader(false);
		isMultiChoice = false;
		selected.clear();
		if (s_choiceType == ExFilePicker.CHOICE_TYPE_FILES && !s_onlyOneItem) {
			readDirectory(currentDirectory);
		}
		((BaseAdapter) absListView.getAdapter()).notifyDataSetChanged();
	}

	private void showSecondHeader(boolean show) {
		if (show) {
			findViewById(R.id.header1).setVisibility(View.GONE);
			findViewById(R.id.header2).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.header1).setVisibility(View.VISIBLE);
			findViewById(R.id.header2).setVisibility(View.GONE);
		}
	}

	private void updateTitle() {
		header_title.setText(currentDirectory.getName());
	}

	private void complete(ExFilePickerParcelObject object) {
		if (object == null) {
			String path = currentDirectory.getAbsolutePath();
			if (!path.endsWith("/")) path += "/";
			object = new ExFilePickerParcelObject(path, selected, selected.size());
		}
		Intent intent = new Intent();
		intent.putExtra(ExFilePickerParcelObject.class.getCanonicalName(), object);
		setResult(RESULT_OK, intent);
		finish();
	}

	private void readDirectory(File path) {
		currentDirectory = path;
		filesList.clear();
		File[] files = path.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (s_choiceType == ExFilePicker.CHOICE_TYPE_DIRECTORIES && !files[i].isDirectory()) continue;
				if (files[i].isFile()) {
					String extension = getFileExtension(files[i].getName());
					if (s_filterListed != null && !s_filterListed.contains(extension)) continue;
					if (s_filterExclude != null && s_filterExclude.contains(extension)) continue;
				}
				filesList.add(files[i]);
			}
		}

		sort();
	}

	private void sort() {
		Collections.sort(filesList, new Comparator<File>() {
			@Override
			public int compare(File file1, File file2) {
				boolean isDirectory1 = file1.isDirectory();
				boolean isDirectory2 = file2.isDirectory();
				if (isDirectory1 && !isDirectory2) return -1;
				if (!isDirectory1 && isDirectory2) return 1;
				switch (s_sortType) {
				case ExFilePicker.SORT_NAME_DESC:
					return file2.getName().toLowerCase(Locale.getDefault()).compareTo(file1.getName().toLowerCase(Locale.getDefault()));
				case ExFilePicker.SORT_SIZE_ASC:
					return Long.valueOf(file1.length()).compareTo(Long.valueOf(file2.length()));
				case ExFilePicker.SORT_SIZE_DESC:
					return Long.valueOf(file2.length()).compareTo(Long.valueOf(file1.length()));
				case ExFilePicker.SORT_DATE_ASC:
					return Long.valueOf(file1.lastModified()).compareTo(Long.valueOf(file2.lastModified()));
				case ExFilePicker.SORT_DATE_DESC:
					return Long.valueOf(file2.lastModified()).compareTo(Long.valueOf(file1.lastModified()));
				}
				// Default, ExFilePicker.SORT_NAME_ASC
				return file1.getName().toLowerCase(Locale.getDefault()).compareTo(file2.getName().toLowerCase(Locale.getDefault()));
			}
		});
		((BaseAdapter) absListView.getAdapter()).notifyDataSetChanged();
	}

	private void setMenuItemView() {
		if (absListView.getId() == R.id.gridview) {
			change_view.setImageResource(attrToResId(R.attr.efp__ic_action_list));
			change_view.setContentDescription(getString(R.string.efp__action_list));
		} else {
			change_view.setImageResource(attrToResId(R.attr.efp__ic_action_grid));
			change_view.setContentDescription(getString(R.string.efp__action_grid));
		}
	}

	private void setAbsListView() {
		int curView, nextView;
		if (absListView == null || absListView.getId() == R.id.gridview) {
			curView = R.id.gridview;
			nextView = R.id.listview;
		} else {
			curView = R.id.listview;
			nextView = R.id.gridview;
		}
		absListView = (AbsListView) findViewById(nextView);
		absListView.setEmptyView(emptyView);
		FilesListAdapter adapter = new FilesListAdapter(this, (nextView == R.id.listview) ? R.layout.efp__list_item : R.layout.efp__grid_item);
		if (nextView == R.id.listview) ((ListView) absListView).setAdapter(adapter);
		else ((GridView) absListView).setAdapter(adapter);
		absListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (position < filesList.size()) {
					File file = filesList.get(position);
					if (isMultiChoice) {
						CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
						if (checkBox.isChecked()) {
							checkBox.setChecked(false);
							setItemBackground(view, false);
							selected.remove(file.getName());
						} else {
							if (s_onlyOneItem) {
								selected.clear();
								((BaseAdapter) absListView.getAdapter()).notifyDataSetChanged();
							}
							checkBox.setChecked(true);
							setItemBackground(view, true);
							selected.add(file.getName());
						}
					} else {
						if (file.isDirectory()) {
							readDirectory(file);
							updateTitle();
							absListView.setSelection(0);
						} else {
							selected.add(file.getName());
							complete(null);
						}
					}
				}
			}
		});

		if (s_choiceType != ExFilePicker.CHOICE_TYPE_FILES || !s_onlyOneItem) {
			absListView.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					if (!isMultiChoice) {
						isMultiChoice = true;
						if (position < filesList.size()) {
							File file = filesList.get(position);
							if (s_choiceType != ExFilePicker.CHOICE_TYPE_FILES || file.isFile()) selected.add(file.getName());
						}

						if (s_choiceType == ExFilePicker.CHOICE_TYPE_FILES && !s_onlyOneItem) {
							ArrayList<File> tmpList = new ArrayList<File>();
							for (int i = 0; i < filesList.size(); i++) {
								File file = filesList.get(i);
								if (file.isFile()) tmpList.add(file);
							}
							filesList = tmpList;
						}

						((BaseAdapter) absListView.getAdapter()).notifyDataSetChanged();

						showSecondHeader(true);
						return true;
					}
					return false;
				}
			});
		}
		findViewById(curView).setVisibility(View.GONE);
		absListView.setVisibility(View.VISIBLE);
	}

	private void setItemBackground(View view, boolean state) {
		view.setBackgroundResource(state ? attrToResId(R.attr.efp__selected_item_background) : 0);
	}

	int attrToResId(int attr) {
		TypedArray a = getTheme().obtainStyledAttributes(new int[] { attr });
		return a.getResourceId(0, 0);
	}

	@SuppressLint("DefaultLocale")
	private String getFileExtension(String fileName) {
		int index = fileName.lastIndexOf(".");
		if (index == -1) return "";
		return fileName.substring(index + 1, fileName.length()).toLowerCase(Locale.getDefault());
	}

	private int getBitmapSize(Bitmap bitmap) {
		if (Build.VERSION.SDK_INT >= 12) {
			return new OldApiHelper().getBtimapSize(bitmap);
		}
		return bitmap.getRowBytes() * bitmap.getHeight();
	}

	private void addBitmapToCache(String key, Bitmap bitmap) {
		if (getBitmapFromCache(key) == null) {
			bitmapsCache.put(key, bitmap);
		}
	}

	private Bitmap getBitmapFromCache(String key) {
		return bitmapsCache.get(key);
	}

	class FilesListAdapter extends BaseAdapter {
		private Context mContext;
		private int mResource;

		public FilesListAdapter(Context context, int resource) {
			mContext = context;
			mResource = resource;
		}

		@Override
		public int getCount() {
			return filesList.size();
		}

		@Override
		public Object getItem(int position) {
			return filesList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			File file = filesList.get(position);

			convertView = LayoutInflater.from(mContext).inflate(mResource, parent, false);
			ImageView thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);

			CheckBox checkbox = (CheckBox) convertView.findViewById(R.id.checkbox);
			if (selected.contains(file.getName())) {
				checkbox.setChecked(true);
				setItemBackground(convertView, true);
			} else {
				checkbox.setChecked(false);
				setItemBackground(convertView, false);
			}
			if (isMultiChoice) checkbox.setVisibility(View.VISIBLE);

			if (file.isDirectory()) {
				thumbnail.setImageResource(R.drawable.efp__ic_folder);
			} else if(getFileExtension(file.getName()).endsWith("7z")){
				thumbnail.setImageResource(R.drawable.efp__ic_7z);
			}
			else {
				if (Build.VERSION.SDK_INT >= 5 && (Arrays.asList(videoExtensions).contains(getFileExtension(file.getName())) 
						|| Arrays.asList(imagesExtensions).contains(getFileExtension(file.getName())))) {
					Bitmap bitmap = getBitmapFromCache(file.getAbsolutePath());
					if (bitmap == null) new ThumbnailLoader(thumbnail).execute(file);
					else thumbnail.setImageBitmap(bitmap);
				} else thumbnail.setImageResource(R.drawable.efp__ic_file);
			}

			TextView filename = (TextView) convertView.findViewById(R.id.filename);
			filename.setText(file.getName());

			TextView filesize = (TextView) convertView.findViewById(R.id.filesize);
			if (filesize != null) {
				if (file.isFile()) filesize.setText(getHumanFileSize(file.length()));
				else filesize.setText("");
			}

			return convertView;
		}

		String getHumanFileSize(long size) {
			String[] units = getResources().getStringArray(R.array.efp__size_units);
			for (int i = units.length - 1; i >= 0; i--) {
				if (size >= Math.pow(1024, i)) {
					return Math.round((size / Math.pow(1024, i))) + " " + units[i];
				}
			}
			return size + " " + units[0];
		}

		class ThumbnailLoader extends AsyncTask<File, Void, Bitmap> {
			private final WeakReference<ImageView> imageViewReference;

			public ThumbnailLoader(ImageView imageView) {
				imageViewReference = new WeakReference<ImageView>(imageView);
			}

			@TargetApi(Build.VERSION_CODES.ECLAIR)
			@Override
			protected Bitmap doInBackground(File... arg0) {
				Bitmap thumbnailBitmap = null;
				File file = arg0[0];
				if (DEBUG) Log.d(TAG, "Loading thumbnail");
				if (file != null) {
					if (DEBUG) Log.d(TAG, file.getAbsolutePath());
					try {
						ContentResolver crThumb = getContentResolver();
						if (Arrays.asList(videoExtensions).contains(getFileExtension(file.getName()))) {
							if (DEBUG) Log.d(TAG, "Video");
							Cursor cursor = crThumb.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Video.Media._ID }, MediaStore.Video.Media.DATA + "='" + file.getAbsolutePath() + "'", null, null);
							if (cursor != null) {
								if (DEBUG) Log.d(TAG, "Cursor is not null");
								if (cursor.getCount() > 0) {
									if (DEBUG) Log.d(TAG, "Cursor has data");
									cursor.moveToFirst();
									thumbnailBitmap = MediaStore.Video.Thumbnails.getThumbnail(crThumb, cursor.getInt(0), MediaStore.Video.Thumbnails.MICRO_KIND, null);
								}
								cursor.close();
							}
						} else if (Arrays.asList(imagesExtensions).contains(getFileExtension(file.getName()))) {
							if (DEBUG) Log.d(TAG, "Image");
							Cursor cursor = crThumb.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Images.Media._ID }, MediaStore.Images.Media.DATA + "='" + file.getAbsolutePath() + "'", null, null);
							if (cursor != null) {
								if (DEBUG) Log.d(TAG, "Cursor is not null");
								if (cursor.getCount() > 0) {
									if (DEBUG) Log.d(TAG, "Cursor has data");
									cursor.moveToFirst();
									thumbnailBitmap = MediaStore.Images.Thumbnails.getThumbnail(crThumb, cursor.getInt(0), MediaStore.Images.Thumbnails.MINI_KIND, null);
								}
								cursor.close();
							}
						}
						if (DEBUG) Log.d(TAG, "Finished");
					} catch (Exception e) {
						e.printStackTrace();
					} catch (Error e) {
						e.printStackTrace();
					}
				}
				if (DEBUG) Log.d(TAG, "Thumbnail: " + (thumbnailBitmap == null ? "null" : "Ok"));
				if (thumbnailBitmap != null) addBitmapToCache(file.getAbsolutePath(), thumbnailBitmap);
				return thumbnailBitmap;
			}

			@Override
			protected void onPostExecute(Bitmap bitmap) {
				if (imageViewReference != null) {
					final ImageView imageView = imageViewReference.get();
					if (imageView != null) {
						if (bitmap == null) imageView.setImageResource(R.drawable.efp__ic_file);
						else imageView.setImageBitmap(bitmap);
					}
				}
			}

		}

	}

	// Need for backward compatibility to Android 1.6
	private class OldApiHelper {
		@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
		private int getBtimapSize(Bitmap bitmap) {
			return bitmap.getByteCount();
		}
	}

}
