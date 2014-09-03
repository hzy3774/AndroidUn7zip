package ru.bartwell.exfilepicker;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class ExFilePickerParcelObject implements Parcelable {

	public String path="";
	public ArrayList<String> names=new ArrayList<String>();
	public int count=0;

	public ExFilePickerParcelObject(String path, ArrayList<String> names, int count) {
		this.path=path;
		this.names=names;
		this.count=count;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(path);
		parcel.writeStringList(names);
		parcel.writeInt(count);
	}

	public static final Parcelable.Creator<ExFilePickerParcelObject> CREATOR = new Parcelable.Creator<ExFilePickerParcelObject>() {
		public ExFilePickerParcelObject createFromParcel(Parcel in) {
			return new ExFilePickerParcelObject(in);
		}

		public ExFilePickerParcelObject[] newArray(int size) {
			return new ExFilePickerParcelObject[size];
		}
	};

	private ExFilePickerParcelObject(Parcel parcel) {
		path = parcel.readString();
		parcel.readStringList(names);
		count = parcel.readInt();
	}

}
