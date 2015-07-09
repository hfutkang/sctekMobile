package cn.ingenic.glasssync.utils.internal;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;

/**
 * The {@link YesNoPreference} is a preference to show a dialog with Yes and No
 * buttons.
 * <p>
 * This preference will store a boolean into the SharedPreferences.
 */
public class YesNoPreference extends DialogPreference {
	private boolean mWasPositiveResult;

	public YesNoPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public YesNoPreference(Context context, AttributeSet attrs) {
		//this(context, attrs, com.android.internal.R.attr.yesNoPreferenceStyle);
		//TODO:merge style
		super(context, attrs);
	}

//	public YesNoPreference(Context context) {
//		super(context);
//	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (callChangeListener(positiveResult)) {
			setValue(positiveResult);
		}
	}

	/**
	 * Sets the value of this preference, and saves it to the persistent store
	 * if required.
	 * 
	 * @param value
	 *            The value of the preference.
	 */
	public void setValue(boolean value) {
		mWasPositiveResult = value;

		persistBoolean(value);

		notifyDependencyChange(!value);
	}

	/**
	 * Gets the value of this preference.
	 * 
	 * @return The value of the preference.
	 */
	public boolean getValue() {
		return mWasPositiveResult;
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getBoolean(index, false);
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue,
			Object defaultValue) {
		setValue(restorePersistedValue ? getPersistedBoolean(mWasPositiveResult)
				: (Boolean) defaultValue);
	}

	@Override
	public boolean shouldDisableDependents() {
		return !mWasPositiveResult || super.shouldDisableDependents();
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		if (isPersistent()) {
			// No need to save instance state since it's persistent
			return superState;
		}

		final SavedState myState = new SavedState(superState);
		myState.wasPositiveResult = getValue();
		return myState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (!state.getClass().equals(SavedState.class)) {
			// Didn't save state for us in onSaveInstanceState
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState myState = (SavedState) state;
		super.onRestoreInstanceState(myState.getSuperState());
		setValue(myState.wasPositiveResult);
	}

	private static class SavedState extends BaseSavedState {
		boolean wasPositiveResult;

		public SavedState(Parcel source) {
			super(source);
			wasPositiveResult = source.readInt() == 1;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(wasPositiveResult ? 1 : 0);
		}

		public SavedState(Parcelable superState) {
			super(superState);
		}

		@SuppressWarnings("unused")
		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

}
